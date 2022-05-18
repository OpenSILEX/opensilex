/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.dev;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.cli.MainCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevModule extends OpenSilexModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(DevModule.class);

    public final static String CONFIG_FILE_PATH = "./src/main/resources/config/opensilex.yml";

    private static OpenSilex devInstance = null;

    /**
     *
     * @param baseDirectory OpenSILEX base directory (used for retrieve config and load modules)
     * @param customArgs custom arguments. If null or empty nothing happened, else all args from custom args are used
     * @return an OpenSilex instance
     */
    public static OpenSilex getOpenSilexDev(Path baseDirectory, Map<String,String> customArgs) throws Exception {
        if (devInstance == null) {

            Map<String, String> args = new HashMap<>();
            args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
            args.put(OpenSilex.DEBUG_ARG_KEY, "true");

            if (baseDirectory == null) {
                baseDirectory = OpenSilex.getDefaultBaseDirectory();
            }
            args.put(OpenSilex.BASE_DIR_ARG_KEY, baseDirectory.toFile().getCanonicalPath());
            args.put(OpenSilex.CONFIG_FILE_ARG_KEY, getConfig(baseDirectory));

            // overload args with custom args if provided
            if (!MapUtils.isEmpty(customArgs)) {
                args.putAll(customArgs);
            }

            LOGGER.debug("Create OpenSilex instance for development tools");
            devInstance = OpenSilex.createInstance(args);
        }
        return devInstance;
    }

    /**
     * @param args commands arguments
     * @param baseDirectory OpenSILEX base directory (used for retrieve config and load modules)
     * @param customArgs custom arguments. If null or empty nothing happened, else all args from custom args are used
     */
    public static void run(Path baseDirectory, String[] args, Map<String,String> customArgs) throws Exception {
        OpenSilex instance = getOpenSilexDev(baseDirectory, customArgs);

        // If no arguments assume help is requested
        if (args.length == 0) {
            args = new String[]{"--help"};
        }

        MainCommand.getCLI(args, instance).execute(args);
    }

    private static String getConfig(Path baseDirectory) {
        return baseDirectory.resolve(DevModule.CONFIG_FILE_PATH).toFile().getAbsolutePath();
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
