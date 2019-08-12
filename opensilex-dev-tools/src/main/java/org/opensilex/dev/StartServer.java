package org.opensilex.dev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
        ProcessBuilder pb = new ProcessBuilder(
                currentDirectory.resolve("../.ng/node").toFile().getCanonicalPath(),
                 currentDirectory.resolve("../opensilex-front-ng/src/main/angular/.bin/ng").toFile().getCanonicalPath(),
                "serve"
        );
        pb.directory(currentDirectory.resolve("../opensilex-front-ng/src/main/angular").toFile());
        pb.inheritIO();
        pb.start();

        MainCommand.main(new String[]{
            "server",
            "start",
            "--profile=dev"
        });

    }
}
