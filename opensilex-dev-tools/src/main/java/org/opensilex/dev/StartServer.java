//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.opensilex.*;
import org.opensilex.cli.*;

/**
 *
 * @author vincent
 */
public class StartServer {

    public static void main(String[] args) throws IOException {

        Path currentDirectory = Paths.get(System.getProperty("user.dir"));
        String configFile = currentDirectory.resolve("./src/main/resources/config/opensilex.local.yml").toFile().getCanonicalPath();
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile);
                put(OpenSilex.DEBUG_ARG_KEY, "true");
            }
        });

        MainCommand.run(new String[]{
            "server",
            "start"
        });

    }
}
