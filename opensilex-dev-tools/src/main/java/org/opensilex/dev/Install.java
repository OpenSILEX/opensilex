//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.opensilex.OpenSilex;
import org.opensilex.rest.RestModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class Install {

    private final static Logger LOGGER = LoggerFactory.getLogger(OpenSilex.class);

    private static OpenSilex opensilex;

    public static void main(String[] args) throws Exception {
        install(false, null);
    }

    public static void install(boolean deleteFirst, String baseDirectory) throws Exception {

        Map<String, String> args = new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
//                put(OpenSilex.DEBUG_ARG_KEY, "true");
            }
        };

        if (baseDirectory != null) {
            args.put(OpenSilex.BASE_DIR_ARG_KEY, baseDirectory);
            args.put(OpenSilex.CONFIG_FILE_ARG_KEY, getConfig(baseDirectory));
        } else {
            args.put(OpenSilex.CONFIG_FILE_ARG_KEY, getConfig(System.getProperty("user.dir")));
        }

        OpenSilex.setup(args);

        opensilex = OpenSilex.getInstance();

        LOGGER.info("Initialize Modules");
        opensilex.install(deleteFirst);

        LOGGER.info("Create Super Admin");
        RestModule.createDefaultSuperAdmin();
    }

    private static String getConfig(String baseDirectory) {
        return Paths.get(baseDirectory).resolve(DevModule.CONFIG_FILE_PATH).toFile().getAbsolutePath();
    }

}
