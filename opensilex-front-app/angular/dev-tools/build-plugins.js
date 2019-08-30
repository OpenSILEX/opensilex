"use strict";

const fs = require('fs');
const chokidar = require('chokidar');
const path = require("path");
const {fork} = require('child_process');

let sharedLibPath = path.resolve(__dirname, "../../target/classes/angular/plugins/shared.js");

function copyPlugin(origin) {
    let dest = path.resolve(__dirname, "../src/assets/.plugins", path.basename(origin));
    fs.copyFile(origin, dest, (err) => {
        if (err) {
            throw err;
        }
//            console.log('Copy ' + origin + ' to ' + dest);
    });
}

let buildPipe = [];
let buildParamsByPlugin = {};
let pluginBuildCountInPipe = {};

function addPluginToBuildPipe(pluginId) {
//        console.log('Add plugin to build pipe', pluginId);
    if (!pluginBuildCountInPipe.hasOwnProperty(pluginId)) {
        pluginBuildCountInPipe[pluginId] = 0;
    }
    pluginBuildCountInPipe[pluginId]++;

    if (buildPipe.length == 0) {
        buildPipe.push(new Promise(function (_resolve, _reject) {
            createPluginBuildPromise(pluginId)
                    .then(function () {
//                            console.log('Plugin built !', pluginId);
                        pluginBuildCountInPipe[pluginId]--;
                        buildPipe.shift();
                        _resolve();
                    })
                    .catch(_reject);
        }));
    } else {
        let lastBuildPromise = buildPipe[buildPipe.length - 1];
        buildPipe.push(new Promise(function (_resolve, _reject) {
            lastBuildPromise
                    .then(function () {
                        createPluginBuildPromise(pluginId)
                                .then(function () {
//                                        console.log('Plugin built !', pluginId);
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

function createPluginBuildPromise(pluginId) {
    return new Promise(function (_resolve, _reject) {
        let currentModulePath = buildParamsByPlugin[pluginId].modulePath
//            console.log('Create plugin promise', pluginId, pluginBuildCountInPipe[pluginId]);
        if (pluginBuildCountInPipe[pluginId] == 1) {
            fork(path.resolve(currentModulePath, '../node_modules/.bin/ng'), buildParamsByPlugin[pluginId].args, {
                cwd: path.resolve(currentModulePath, 'angular/')
            }).on('exit', () => {
                // TODO: if compile with error no copy
//                    console.log('Plugin file created !', pluginId);
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

function startWatchingSources(pluginId) {
    let srcPath = buildParamsByPlugin[pluginId].srcPath;

    console.log("Initialize source watcher for", pluginId);
    let pluginWatcher = chokidar.watch([
        srcPath + "/**",
        sharedLibPath
    ], {
        ignored: /(^|[\/\\])\../,
        persistent: true,
        ignoreInitial: true,
        awaitWriteFinish: true
    });

    pluginWatcher.on('all', function () {
        addPluginToBuildPipe(pluginId);
    });
}

var argv = require('yargs-parser')(process.argv.slice(2));

if (argv.hasOwnProperty("module")) {
    var modules = argv.module;
    if (!Array.isArray(argv.module)) {
        modules = [argv.module];
    }

    for (let m in modules) {
        let module = modules[m];

        let modulePath = path.resolve(__dirname, "../../../", module);
        let jsonData = require(path.resolve(modulePath, 'angular/opensilex.json'));

        let outputPath = path.resolve(modulePath, "./target/classes/angular/plugins");

        for (let i in jsonData.plugins) {
            let plugin = jsonData.plugins[i];

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

            startWatchingSources(plugin.id);
        }
    }
}