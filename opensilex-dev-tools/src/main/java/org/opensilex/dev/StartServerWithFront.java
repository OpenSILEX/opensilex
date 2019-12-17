//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.opensilex.*;
import org.opensilex.cli.*;
import org.opensilex.front.FrontModule;
import org.opensilex.front.api.FrontAPI;
import org.opensilex.OpenSilexModule;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class StartServerWithFront {

    private final static Logger LOGGER = LoggerFactory.getLogger(StartServerWithFront.class);

    private static Path currentDirectory;
    private static String nodeBin = "node";
    private static CountDownLatch countDownLatch;

    public static void main(String[] args) throws IOException, Exception {

        if (isWindows()) {
            nodeBin += ".exe";
        }
        // Define current directory to launch node.js processes
        currentDirectory = Paths.get(System.getProperty("user.dir"));
        String configFile = currentDirectory.resolve("./src/main/resources/config/opensilex.yml").toFile().getCanonicalPath();
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile);
                put(OpenSilex.DEBUG_ARG_KEY, "true");
            }
        });

        Map<String, Process> moduleProcesses = new HashMap<>();
        Set<String> moduleToBuild = new HashSet<>();
        OpenSilex opensilex = OpenSilex.getInstance();
        for (OpenSilexModule module : opensilex.getModules()) {
            String projectId = ClassUtils.getProjectIdFromClass(module.getClass());
            if (module.fileExists(FrontAPI.getModuleFrontLibFilePath(projectId))) {
                if (!moduleToBuild.contains(projectId)) {
                    moduleToBuild.add(projectId);
                }
            }
        }

        LOGGER.debug("build front modules");
        countDownLatch = new CountDownLatch(moduleToBuild.size());

        for (String projectId : moduleToBuild) {
            moduleProcesses.put(projectId, createFrontModuleBuilder(projectId));
        }

        countDownLatch.await();
        LOGGER.debug("start front server");
        Process frontProcess = createFrontServer();

        // Add hook to clean node.js processes on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                frontProcess.destroy();
                for (Process process : moduleProcesses.values()) {
                    process.destroy();
                }
            }
        });

        LOGGER.debug("start back server");
        MainCommand.run(new String[]{
            "server",
            "start"
        });

    }

    private static Process createFrontServer() throws Exception {
        Path targetDirectory = currentDirectory.resolve("../opensilex-front/target/classes/front");
        if (!targetDirectory.toFile().exists()) {
            Files.createDirectories(targetDirectory);
        }

        Path moduleDirectory = currentDirectory.resolve("../opensilex-front/front");
        createConfigMonitor(moduleDirectory, targetDirectory);

        List<String> args = new ArrayList<>();
        args.add(currentDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        args.add(currentDirectory.resolve("../.node/node/yarn/dist/bin/yarn.js").toFile().getCanonicalPath());
        args.add("run");
        args.add("serve");
        ProcessBuilder frontBuilder = new ProcessBuilder(args);
        frontBuilder.directory(currentDirectory.resolve("../opensilex-front/front").toFile());
        frontBuilder.inheritIO();

        return frontBuilder.start();
    }

    private static Process createFrontModuleBuilder(String moduleId) throws Exception {
        List<String> args = new ArrayList<>();
        args.add(currentDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        args.add(currentDirectory.resolve("../.node/node/yarn/dist/bin/yarn.js").toFile().getCanonicalPath());
        args.add("run");
        args.add("serve");
        ProcessBuilder frontBuilder = new ProcessBuilder(args);

        String modulePath = moduleId;
        if (moduleId.equals("phis2ws")) {
            modulePath = "phis-ws/phis2-ws";
        }

        Path moduleDirectory = currentDirectory.resolve("../" + modulePath + "/front");
        frontBuilder.directory(moduleDirectory.toFile());
        frontBuilder.inheritIO();

        Path targetDirectory = currentDirectory.resolve("../" + modulePath + "/target/classes/front");
        if (!targetDirectory.toFile().exists()) {
            Files.createDirectories(targetDirectory);
        }
        String filename = moduleId + ".umd.min.js";

        FileAlterationObserver observer = new FileAlterationObserver(moduleDirectory.resolve("dist").toFile().getCanonicalPath());
        FileAlterationMonitor monitor = new FileAlterationMonitor(200);
        FileAlterationListener listener = new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                if (file.getName().equals(filename)) {
                    LOGGER.debug("File created: " + file.getName());
                    try {
                        FileUtils.copyFile(moduleDirectory.resolve("dist/" + filename).toFile(), targetDirectory.resolve(filename).toFile());
                    } catch (IOException ex) {
                        LOGGER.error("Error while copying lib file: " + filename, ex);
                    }
                    countDownLatch.countDown();
                }
            }

            @Override
            public void onFileDelete(File file) {
            }

            @Override
            public void onFileChange(File file) {
                if (file.getName().equals(filename)) {
                    LOGGER.debug("File changed: " + file.getName());
                    try {
                        FileUtils.copyFile(moduleDirectory.resolve("dist/" + filename).toFile(), targetDirectory.resolve(filename).toFile());
                        String pseudoScript = "export default { \"last-dev-update\": " + System.currentTimeMillis() + "};";
                        PrintWriter prw = new PrintWriter(currentDirectory.resolve("../opensilex-front/front/src/opensilex.dev.ts").toFile());
                        prw.println(pseudoScript);
                        prw.close();
                    } catch (IOException ex) {
                        LOGGER.error("Error while copying lib file: " + filename, ex);
                    }
                }
            }
        };
        observer.addListener(listener);
        monitor.addObserver(observer);
        monitor.start();

        createConfigMonitor(moduleDirectory, targetDirectory);
        return frontBuilder.start();
    }

    public static void createConfigMonitor(Path moduleDirectory, Path targetDirectory) throws Exception {
        File configFile = moduleDirectory.resolve(FrontModule.FRONT_CONFIG_FILE).toFile();
        if (configFile.exists()) {
            copyConfig(configFile, targetDirectory);
            FileAlterationObserver configObserver = new FileAlterationObserver(configFile.getCanonicalPath());
            FileAlterationMonitor configMonitor = new FileAlterationMonitor(200);

            FileAlterationListener configListener = new FileAlterationListenerAdaptor() {
                @Override
                public void onFileCreate(File file) {
                    LOGGER.debug("File created: " + file.getName());
                    copyConfig(configFile, targetDirectory);
                }

                @Override
                public void onFileDelete(File file) {
                }

                @Override
                public void onFileChange(File file) {
                    LOGGER.debug("File changed: " + file.getName());
                    copyConfig(configFile, targetDirectory);
                }

            };
            configObserver.addListener(configListener);
            configMonitor.addObserver(configObserver);
            configMonitor.start();
        }
    }

    private static void copyConfig(File configFile, Path targetDirectory) {
        try {
            FileUtils.copyFile(configFile, targetDirectory.resolve(FrontModule.FRONT_CONFIG_FILE).toFile());
            String pseudoScript = "export default { \"last-dev-update\": " + System.currentTimeMillis() + "};";
            PrintWriter prw = new PrintWriter(currentDirectory.resolve("../opensilex-front/front/src/opensilex.dev.ts").toFile());
            prw.println(pseudoScript);
            prw.close();
        } catch (IOException ex) {
            LOGGER.error("Error while copying config file: " + configFile.getName(), ex);
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
