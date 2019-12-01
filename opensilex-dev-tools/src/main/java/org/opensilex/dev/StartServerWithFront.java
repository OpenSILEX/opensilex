//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

//import org.opensilex.angular.AngularExtension;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.opensilex.*;
import org.opensilex.cli.*;

/**
 *
 * @author vincent
 */
public class StartServerWithFront {

    public static void main(String[] args) throws IOException {

        String nodeBin = "node";
        if (isWindows()) {
            nodeBin += ".exe";
        }
        // Define current directory to launch node.js processes
        Path currentDirectory = Paths.get(System.getProperty("user.dir"));
        String configFile = currentDirectory.resolve("./src/main/resources/config/opensilex.yml").toFile().getCanonicalPath();
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile);
            }
        });

        // Start process to automatically rebuild front modular app
        List<String> appPluginBuilderArgs = new ArrayList<>();
        appPluginBuilderArgs.add(currentDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        appPluginBuilderArgs.add(currentDirectory.resolve("../opensilex-front/front/build-debug.js").toFile().getCanonicalPath());

        Set<String> modulesPluginBuilderArgs = new HashSet<>();
//        OpenSilex.getInstance().getModulesImplementingInterface(AngularExtension.class).forEach((AngularExtension extension) -> {
//            String moduleProjectId = ClassInfo.getProjectIdFromClass(extension.getClass());
//            if (!moduleProjectId.isEmpty()) {
//                modulesPluginBuilderArgs.add("--module=" + moduleProjectId);
//            }
//        });
        appPluginBuilderArgs.addAll(modulesPluginBuilderArgs);

        ProcessBuilder pluginBuilder = new ProcessBuilder(appPluginBuilderArgs);

        pluginBuilder.directory(currentDirectory.resolve("../opensilex-front/front").toFile());
        pluginBuilder.inheritIO();
        Process pluginBuilderProcess = pluginBuilder.start();

        // Add hook to clean node.js processes on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                pluginBuilderProcess.destroy();
            }
        });

        MainCommand.run(new String[]{
            "server",
            "start"
        });

    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
