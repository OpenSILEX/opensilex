package org.opensilex.dev;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;

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

        Path currentDirectory = Paths.get(System.getProperty("user.dir"));

        ProcessBuilder sharedBuilder = new ProcessBuilder(
                currentDirectory.resolve("../.ng/node/node").toFile().getCanonicalPath(),
                currentDirectory.resolve("../opensilex-front-app/angular/dev-tools/build-shared.js").toFile()
                        .getCanonicalPath());
        sharedBuilder.directory(currentDirectory.resolve("../opensilex-front-app/angular/dev-tools").toFile());
        sharedBuilder.inheritIO();
        Process sharedBuilderProcess = sharedBuilder.start();

        ProcessBuilder pluginBuilder = new ProcessBuilder(
                currentDirectory.resolve("../.ng/node/node").toFile().getCanonicalPath(),
                currentDirectory.resolve("../opensilex-front-app/angular/dev-tools/build-plugins.js").toFile()
                        .getCanonicalPath(),
                "--module=opensilex-front-ue");
        pluginBuilder.directory(currentDirectory.resolve("../opensilex-front-app/angular/dev-tools").toFile());
        pluginBuilder.inheritIO();
        Process pluginBuilderProcess = pluginBuilder.start();

        ProcessBuilder devServer = new ProcessBuilder(
                currentDirectory.resolve("../.ng/node/node").toFile().getCanonicalPath(),
                currentDirectory.resolve("../node_modules/.bin/ng").toFile().getCanonicalPath(), "serve",
                "--poll=2000");
        devServer.directory(currentDirectory.resolve("../opensilex-front-app/angular").toFile());
        devServer.inheritIO();
        Process devServerProcess = devServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                sharedBuilderProcess.destroy();
                pluginBuilderProcess.destroy();
                devServerProcess.destroy();
            }
        });
        
        String classpathStr = System.getProperty("java.class.path");
        System.out.print(classpathStr);
        MainCommand.main(new String[]{"server", "start",
            "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID});

    }
}
