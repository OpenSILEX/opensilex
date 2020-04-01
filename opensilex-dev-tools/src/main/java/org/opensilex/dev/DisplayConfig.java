/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.dev;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;

/**
 *
 * @author vince
 */
public class DisplayConfig {

    public static void main(String[] args) throws Exception {
        start(Paths.get(System.getProperty("user.dir")));
    }

    public static void start(Path baseDirectory) throws Exception {

        String configFile = baseDirectory.resolve(DevModule.CONFIG_FILE_PATH).toFile().getCanonicalPath();
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile);
            }
        });

        MainCommand.run(new String[]{
            //            "system",
            //            "full-config"
            "sparql",
            "shacl-enable"
        });

    }
}
