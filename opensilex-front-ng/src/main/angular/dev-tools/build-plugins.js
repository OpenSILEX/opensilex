"use strict";

const fs = require('fs');
const chokidar = require('chokidar');
const path = require("path");
const {fork} = require('child_process');

var argv = require('yargs-parser')(process.argv.slice(2));

if (argv.hasOwnProperty("module")) {
    let modulePath = path.resolve(__dirname, "../../../../../", argv.module);
    let jsonData = require(path.resolve(modulePath, 'src/main/angular/opensilex.json'));

    let outputPath = path.resolve(modulePath, "./target/classes/angular/plugins");

    function copyPlugin(origin) {
        let dest = path.resolve(__dirname, "../src/assets/.plugins", path.basename(origin));
        fs.copyFile(origin, dest, (err) => {
            if (err) {
                throw err;
            }
            console.log('Copy ' + origin + ' to ' + dest);
        });
    }

    for (let i in jsonData.plugins) {
        let plugin = jsonData.plugins[i];

        let pluginSrcPath = path.resolve(modulePath, 'src/main/angular/src', plugin.id);

        let args = [
            "build",
            "--prod",
            "--modulePath=" + plugin.modulePath + "#" + plugin.moduleClass,
            "--pluginName=" + plugin.id,
            "--sharedLibs=shared",
            "--outputPath=" + outputPath
        ];

        let outputFile = path.resolve(outputPath, plugin.id + ".js");

        (function (_modulePath, _pluginSrcPath, _outputFile, _args) {
            console.log("init watcher", _pluginSrcPath);
            let pluginWatcher = chokidar.watch(_pluginSrcPath + "/**", {
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
                    fork(path.resolve(_modulePath, '../node_modules/.bin/ng'), _args, {
                        cwd: path.resolve(_modulePath, 'src/main/angular/')
                    }).on('exit', () => {
                        // if compile with error no copy
                        isBuilding = false;
                        if (toRebuild) {
                            toRebuild = false;
                            buildPlugin();
                        } else {
                            copyPlugin(_outputFile);
                        }
                    });
                }
            }

            pluginWatcher.on('all', buildPlugin);

            buildPlugin();

        })(modulePath, pluginSrcPath, outputFile, args);
    }
} else {
    // return error status
}