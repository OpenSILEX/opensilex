/**
 * This script is used by the opensilex-dev-tools project to hot reload 
 * shared library on source change for development mode
 */
"use strict";

// Libraries imports
const fs = require('fs');
const chokidar = require('chokidar');
const path = require("path");
const {fork} = require('child_process');
const argParser = require('yargs-parser');

// Shared library path definition  because all plugin must be rebuild in case of change in this library
let sharedLibPath = path.resolve(__dirname, "../../target/classes/angular/plugins/shared.js");

/**
 * Copy js plugin file to assets/.plugins folder of main application to trigger reloading
 * This function is called at the end of plugin build
 * @param {type} origin
 */
function copyPlugin(origin) {
    let dest = path.resolve(__dirname, "../src/assets/.plugins", path.basename(origin));
    fs.copyFile(origin, dest, (err) => {
        if (err) {
            throw err;
        }
    });
}

// Variables used to manage a plugin build pipe because they must be build one at a time
// Store the list of the current plugin build promise
let buildPipe = [];
// Store plugin build parameters indexed by plugin identifier
let buildParamsByPlugin = {};
// Store current plugin build in the pipe to prevent unnecessary reload indexed by plugin identifier
let pluginBuildCountInPipe = {};

/**
 * Add the plugin defined by it's id to the build pipe
 * If another plugin is currently building, wait for it to finish
 * @param {string} pluginId
 */
function addPluginToBuildPipe(pluginId) {
    // Initialize plugin build count if needed
    if (!pluginBuildCountInPipe.hasOwnProperty(pluginId)) {
        pluginBuildCountInPipe[pluginId] = 0;
    }

    // Increment plugin build count
    pluginBuildCountInPipe[pluginId]++;

    if (buildPipe.length == 0) {
        // If nothing in the build pipe create a promise for this plugin and run it
        buildPipe.push(new Promise(function (_resolve, _reject) {
            createPluginBuildPromise(pluginId)
                    .then(function () {
                        // Decrement plugin build count and remove it from the pipe
                        pluginBuildCountInPipe[pluginId]--;
                        buildPipe.shift();
                        _resolve();
                    })
                    .catch(_reject);
        }));
    } else {
        // If at least one plugin is already building, append plugin build promise to the last one
        let lastBuildPromise = buildPipe[buildPipe.length - 1];
        buildPipe.push(new Promise(function (_resolve, _reject) {
            lastBuildPromise
                    .then(function () {
                        createPluginBuildPromise(pluginId)
                                .then(function () {
                                    // Decrement plugin build count and remove it from the pipe
                                    pluginBuildCountInPipe[pluginId]--;
                                    buildPipe.shift();
                                    _resolve();
                                })
                                .catch(_reject);
                    })
                    .catch(_reject)
        }));
    }
}

/**
 * Create a promise of plugin build and copy it to application assets/.plugins folder if needed
 * 
 * @param {type} pluginId
 * @returns {Promise}
 */
function createPluginBuildPromise(pluginId) {
    return new Promise(function (_resolve, _reject) {
        let currentModulePath = buildParamsByPlugin[pluginId].modulePath
        if (pluginBuildCountInPipe[pluginId] == 1) {
            // Run angular-cli (ng) build with plugin arguments
            fork(path.resolve(currentModulePath, '../node_modules/.bin/ng'), buildParamsByPlugin[pluginId].args, {
                cwd: path.resolve(currentModulePath, 'angular/')
            }).on('exit', () => {
                // If there is no other build for this plugin in pipe, copy it to destination, otherwise ignore this build
                if (pluginBuildCountInPipe[pluginId] == 1) {
                    copyPlugin(buildParamsByPlugin[pluginId].outputFile);
                }

                _resolve();
            });
        } else {
            _resolve();
        }
    });
}

/**
 * Start to watch sources for the given plugin and a plugin to build pipe in case of file change
 * 
 * @param {type} pluginId
 */
function startWatchingSources(pluginId) {
    let srcPath = buildParamsByPlugin[pluginId].srcPath;

    // Initialize chokidar library to watch plugin sources and shared library
    let pluginWatcher = chokidar.watch([
        srcPath + "/**",
        sharedLibPath
    ], {
        ignored: /(^|[\/\\])\../,
        persistent: true,
        ignoreInitial: true,
        awaitWriteFinish: true
    });

    // In case of nay change add plugin to build pipe
    pluginWatcher.on('all', function () {
        addPluginToBuildPipe(pluginId);
    });
}

// Read command line arguments
var argv = argParser(process.argv.slice(2));

// This script require at least one "module" argument
if (argv.hasOwnProperty("module")) {

    // Ensure that module is an array, because library will return only a string if the is onlu one module to process
    var modules = argv.module;
    if (!Array.isArray(argv.module)) {
        modules = [argv.module];
    }

    // Foreach module get all plugins definitions and initialize auto-rebuild for them 
    for (let m in modules) {
        // Define module plugin definitions file
        let module = modules[m];
        let modulePath = path.resolve(__dirname, "../../../", module);
        let jsonDataPath = path.resolve(modulePath, 'angular/opensilex.json');

        if (fs.existsSync(jsonDataPath)) {
            // If file exists get plugins definitions
            let jsonData = require(jsonDataPath);
            let outputPath = path.resolve(modulePath, "./target/classes/angular/plugins");

            for (let i in jsonData.plugins) {
                let plugin = jsonData.plugins[i];

                // Store plugin parameters in global variable for use when rebuild
                buildParamsByPlugin[plugin.id] = {
                    modulePath: modulePath,
                    args: [
                        "build",
                        "--prod",
                        "--modulePath=" + plugin.modulePath + "#" + plugin.moduleClass,
                        "--pluginName=" + plugin.id,
                        "--sharedLibs=shared",
                        "--outputPath=" + outputPath
                    ],
                    srcPath: path.resolve(modulePath, 'angular/src', plugin.id),
                    outputFile: path.resolve(outputPath, plugin.id + ".js")
                }

                // Start watching plugin sources to trigger rebuild when needed
                startWatchingSources(plugin.id);
            }
        }
    }
}