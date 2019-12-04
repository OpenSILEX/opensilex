/**
 * This script start Vue server and all dependent OpenSilex extensions with automatic rebuild
 */
"use strict";

const argParser = require('yargs-parser');
const path = require("path");
const { fork } = require('child_process');
const chokidar = require('chokidar');
const fs = require('fs');

const rootPath = path.resolve(__dirname, "../../");

// Read command line arguments
var argv = argParser(process.argv.slice(2));

// Load module array argument
var modules = [];
if (argv.hasOwnProperty("module")) {
    if (!Array.isArray(argv.module)) {
        modules = [argv.module];
    } else {
        for (let i = 0; i < argv.module.length; i++) {
            modules.push(argv.module[i]);
        }
    }
}

function tellDevServer() {
    let pseudoScript = "export default { \"last-dev-update\": " + Date.now() + "};"
    fs.writeFile(path.resolve(__dirname, "src/opensilex.dev.ts"), pseudoScript, function(err) {
        if (err) {
            console.log("Error while updating modules, please restart the dev server", err);
        }
    });
}

function cleanPath(directory) {
    let cleanPath = directory.replace(/\\/g, '/');
    return cleanPath;
}

function copyFile(filePath, destPath) {
    fs.copyFile(filePath, destPath, (err) => {
        if (err) {
            console.error("Error during copy from", filePath, "to", destPath);
        }
    });
}

function getWatcher(sourceDirectory, destDirectory, moduleName) {
    return new Promise(function(resolve, reject) {
        try {
            let resolved = false;
            let normalizedPath = cleanPath(sourceDirectory);
            let libFile = moduleName + ".umd.min.js";
            let destPath = cleanPath(destDirectory) + "/" + libFile;
            let watcher = chokidar.watch(
                normalizedPath + "/", {
                    ignored: [
                        function(testedPath) {
                            if (normalizedPath == testedPath) {
                                return false;
                            }
                            if (testedPath.startsWith(normalizedPath + "/dist")) {
                                return false;
                            }

                            return true;
                        }
                    ],
                    persistent: true,
                    ignoreInitial: true,
                    awaitWriteFinish: true
                }
            );

            let libUpdate = function(filePath) {
                if (filePath.endsWith(libFile)) {
                    console.log("Library update detected", libFile);
                    tellDevServer();
                    copyFile(filePath, destPath);
                    if (!resolved) {
                        resolved == true;
                        resolve();
                    }
                }
            }
            watcher.on("add", libUpdate);
            watcher.on("change", libUpdate);

        } catch (err) {
            reject(err);
        }
    });
}

modules = ["opensilex", "opensilex-core"];

let promises = [];
for (let i = 0; i < modules.length; i++) {
    let moduleName = modules[i];

    let modulePath = path.resolve(rootPath, moduleName, "front");
    let targetPath = path.resolve(rootPath, moduleName, "target/classes/front");

    fork(path.resolve(rootPath, '.node/node/yarn/dist/bin/yarn.js'), [
        "run",
        "serve"
    ], {
        cwd: modulePath
    })

    promises.push(getWatcher(modulePath, targetPath, moduleName));

}

Promise.all(promises).then(function() {
    console.log("All modules built, starting Vue main dev server...");
    fork(path.resolve(rootPath, '.node/node/yarn/dist/bin/yarn.js'), [
        "run",
        "serve"
    ], {
        cwd: __dirname
    })
});