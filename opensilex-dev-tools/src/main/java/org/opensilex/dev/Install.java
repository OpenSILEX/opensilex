//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;

/**
 *
 * @author vincent
 */
public class Install {
    
        public static void main(String[] args) throws IOException {
        
        File configFile = new File(StartServer.class.getClassLoader().getResource("./config/opensilex.yml").getPath());
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile.getAbsolutePath());
            }
        });

        // download files && insert them
        
        MainCommand.run(new String[]{
            "server",
            "start"
        });

    }
}
