package org.opensilex.dev;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;
import org.opensilex.front.app.FrontAppExtension;
import org.opensilex.front.app.FrontAppModule;
import org.opensilex.utils.ClassInfo;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author vincent
 */
public class StartServer {

    public static void main(String[] args) throws IOException {

        // Start server in development mode
        MainCommand.main(new String[]{"server", "start",
            "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID});

        // Define current directory to launch node.js processes
        Path currentDirectory = Paths.get(System.getProperty("user.dir"));

        // Start process to automatically rebuild Angular shared library
        ProcessBuilder sharedBuilder = new ProcessBuilder(
                currentDirectory.resolve("../.ng/node/node").toFile().getCanonicalPath(),
                currentDirectory.resolve("../opensilex-front-app/angular/dev-tools/build-shared.js").toFile()
                        .getCanonicalPath());
        sharedBuilder.directory(currentDirectory.resolve("../opensilex-front-app/angular/dev-tools").toFile());
        sharedBuilder.inheritIO();
        Process sharedBuilderProcess = sharedBuilder.start();

        // Start process to automatically rebuild Angular plugin modules
        List<String> appPluginBuilderArgs = new ArrayList<>();
        appPluginBuilderArgs.add(currentDirectory.resolve("../.ng/node/node").toFile().getCanonicalPath());
        appPluginBuilderArgs.add(currentDirectory.resolve("../opensilex-front-app/angular/dev-tools/build-plugins.js").toFile().getCanonicalPath());

        Set<String> modulesPluginBuilderArgs = new HashSet<>();
        OpenSilex.getInstance().getModulesImplementingInterface(FrontAppExtension.class).forEach((FrontAppExtension extension) -> {
            if (!(extension instanceof FrontAppModule)) {
                modulesPluginBuilderArgs.add("--module=" + ClassInfo.getProjectIdFromClass(extension.getClass()));
            }
        });
        appPluginBuilderArgs.addAll(modulesPluginBuilderArgs);

        ProcessBuilder pluginBuilder = new ProcessBuilder(appPluginBuilderArgs);

        pluginBuilder.directory(currentDirectory.resolve("../opensilex-front-app/angular/dev-tools").toFile());
        pluginBuilder.inheritIO();
        Process pluginBuilderProcess = pluginBuilder.start();

        // Start process to run Angular development server on port 4200
        ProcessBuilder devServer = new ProcessBuilder(
                currentDirectory.resolve("../.ng/node/node").toFile().getCanonicalPath(),
                currentDirectory.resolve("../node_modules/.bin/ng").toFile().getCanonicalPath(), "serve",
                "--poll=2000");
        devServer.directory(currentDirectory.resolve("../opensilex-front-app/angular").toFile());
        devServer.inheritIO();
        Process devServerProcess = devServer.start();

        // Add hook to clean node.js processes on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                sharedBuilderProcess.destroy();
                pluginBuilderProcess.destroy();
                devServerProcess.destroy();
            }
        });
    }
}
