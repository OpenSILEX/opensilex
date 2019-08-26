"use strict";

const fs = require('fs');
const chokidar = require('chokidar');
const path = require("path");
const {fork} = require('child_process');

let modulePath = path.resolve(__dirname, "../../../../");

let outputPath = path.resolve(modulePath, "./target/classes/angular/plugins");

let sharedSrcPath = path.resolve(__dirname, '../shared');

console.log("Initialize source watcher for shared library");
let pluginWatcher = chokidar.watch(sharedSrcPath + "/**",
        {
            ignored: /(^|[\/\\])\../,
            persistent: true,
            ignoreInitial: true,
            awaitWriteFinish: true
        });

let isBuilding = false;
let toRebuild = false;

function buildPlugin() {
    if (isBuilding) {
        toRebuild = true;
    } else {
        isBuilding = true;
        fork(path.resolve(modulePath, '../node_modules/.bin/ng'), [
            "build",
            "shared"
        ], {
            cwd: path.resolve(modulePath, 'src/main/angular/')
        }).on('exit', () => {
            fork(path.resolve(modulePath, '../node_modules/.bin/ng'), [
                "build",
                "--project",
                "plugins",
                "--prod",
                "--modulePath=shared#SharedModule",
                "--pluginName=shared",
                "--outputPath=" + outputPath
            ], {
                cwd: path.resolve(modulePath, 'src/main/angular/')
            }).on('exit', () => {
                // if compile with error no copy
                isBuilding = false;
                if (toRebuild) {
                    toRebuild = false;
                    buildPlugin();
                }
            });
        });
    }
}

pluginWatcher.on('all', buildPlugin);

buildPlugin();

