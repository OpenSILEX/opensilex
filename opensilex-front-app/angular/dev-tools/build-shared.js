/**
 * This script is used by the opensilex-dev-tools project to hot reload 
 * all Angular plugins on source change for development mode
 */
"use strict";

// Libraries imports
const fs = require('fs');
const chokidar = require('chokidar');
const path = require("path");
const {fork} = require('child_process');

// Base path of this maven module
let modulePath = path.resolve(__dirname, "../../");

// Path where shared library must be copied when regenerated
let outputPath = path.resolve(modulePath, "./target/classes/angular/plugins");

// Source path of the shared library
let sharedSrcPath = path.resolve(__dirname, '../shared');

// Initialize shared library sources watching
let pluginWatcher = chokidar.watch(sharedSrcPath + "/**",
        {
            ignored: /(^|[\/\\])\../,
            persistent: true,
            ignoreInitial: true,
            awaitWriteFinish: true
        });

// Boolean flag to prevent unnecessary rebuild
let isBuilding = false;
let toRebuild = false;

/**
 * Main function to rebuild shared library on file change
 */
function buildSharedLibrary() {
    // If this function is called during a build, jsut indicate that it must be rebuild at the end
    if (isBuilding) {
        toRebuild = true;
    } else {
        // Set building flag to true
        isBuilding = true;
        
        // Run the first part of the shared library build
        fork(path.resolve(modulePath, '../node_modules/.bin/ng'), [
            "build",
            "shared"
        ], {
            cwd: path.resolve(modulePath, 'angular/')
        }).on('exit', () => {
            // Run the second part of the shared library build
            fork(path.resolve(modulePath, '../node_modules/.bin/ng'), [
                "build",
                "--project",
                "plugins",
                "--prod",
                "--modulePath=shared#SharedModule",
                "--pluginName=shared",
                "--outputPath=" + outputPath
            ], {
                cwd: path.resolve(modulePath, 'angular/')
            }).on('exit', () => {
                // Disable building flag
                isBuilding = false;
                
                // If this function has been called during build, call it again
                if (toRebuild) {
                    toRebuild = false;
                    buildSharedLibrary();
                }
            });
        });
    }
}

// On any source files change rebuild the library
pluginWatcher.on('all', buildSharedLibrary);

// Build it at least once
buildSharedLibrary();

