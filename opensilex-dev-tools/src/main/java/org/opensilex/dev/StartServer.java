package org.opensilex.dev;

import java.io.IOException;
import java.nio.file.Path;
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

        Path currentDirectory = Path.of(System.getProperty("user.dir"));
        
        ProcessBuilder pluginBuilder = new ProcessBuilder(
                currentDirectory.resolve("../.ng/node/node").toFile().getCanonicalPath(),
                currentDirectory.resolve("../opensilex-front-ng/src/main/angular/dev-tools/build-plugins.js").toFile().getCanonicalPath(),
                "--module=opensilex-front-ue"
        );
        pluginBuilder.directory(currentDirectory.resolve("../opensilex-front-ng/src/main/angular/dev-tools").toFile());
        pluginBuilder.inheritIO();
        pluginBuilder.start();

        ProcessBuilder devServer = new ProcessBuilder(
                currentDirectory.resolve("../.ng/node/node").toFile().getCanonicalPath(),
                currentDirectory.resolve("../node_modules/.bin/ng").toFile().getCanonicalPath(),
                "serve"
        );
        devServer.directory(currentDirectory.resolve("../opensilex-front-ng/src/main/angular").toFile());
        devServer.inheritIO();
        devServer.start();

        MainCommand.main(new String[]{
            "server",
            "start",
            "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID
        });

    }
}
