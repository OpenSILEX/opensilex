//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;

/**
 *
 * @author vincent
 */
public class ResetSHACL {

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
            "sparql",
            "shacl-enable"
        });

    }
}
