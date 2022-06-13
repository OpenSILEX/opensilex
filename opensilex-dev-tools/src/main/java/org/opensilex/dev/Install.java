//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.opensilex.OpenSilex;
import org.opensilex.security.SecurityModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class Install {

    private final static Logger LOGGER = LoggerFactory.getLogger(Install.class);

    public static void main(String[] args) throws Exception {
        install(false, null);
    }

    public static void install(boolean deleteFirst, Path baseDirectory) throws Exception {

        Map<String, String> customArgs = new HashMap<>();
        customArgs.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.INTERNAL_OPERATIONS_PROFILE_ID);

        OpenSilex opensilex = DevModule.getOpenSilexDev(baseDirectory, customArgs);

        LOGGER.info("Initialize Modules");
        opensilex.install(deleteFirst);

        LOGGER.info("Create Super Admin");
        opensilex.getModuleByClass(SecurityModule.class).createDefaultSuperAdmin();
    }

}
