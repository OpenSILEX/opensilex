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
import org.opensilex.front.FrontModule;
import org.opensilex.front.api.FrontAPI;
import org.opensilex.OpenSilexModule;
import org.opensilex.server.ServerConfig;
import org.opensilex.server.ServerModule;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class StartServerWithFront {

    private final static Logger LOGGER = LoggerFactory.getLogger(StartServerWithFront.class);

    private static Path baseDirectory;
    private static String nodeBin = "node";
    private static CountDownLatch countDownLatch;

    public static void main(String[] args) throws Exception {
        start(OpenSilex.getDefaultBaseDirectory());
    }

    public static void start(Path baseDirectory) throws Exception {

        if (DevModule.isWindows()) {
            nodeBin += ".exe";
        }

        StartServerWithFront.baseDirectory = baseDirectory;

        OpenSilex opensilex = DevModule.getOpenSilexDev(baseDirectory,null);

        Map<String, Process> moduleProcesses = new HashMap<>();
        Set<String> moduleToBuild = new HashSet<>();

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
        String pathPrefix = opensilex.getModuleConfig(ServerModule.class, ServerConfig.class).pathPrefix();
        Process frontProcess = createFrontServer(pathPrefix);

        // Add hook to clean node.js processes on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                frontProcess.destroy();
                for (Process process : moduleProcesses.values()) {
                    process.destroy();
                }
                setPseudoScript(0);
            }
        });

        LOGGER.debug("start back server");

        DevModule.run(null,new String[]{
            "server",
            "start"
        },null);

    }

    private static Process createFrontServer(String pathPrefix) throws Exception {
        Path targetDirectory = baseDirectory.resolve("../opensilex-front/target/classes/front");
        if (!targetDirectory.toFile().exists()) {
            Files.createDirectories(targetDirectory);
        }

        Path moduleDirectory = baseDirectory.resolve("../opensilex-front/front");
        createConfigMonitor(moduleDirectory, targetDirectory);
        createThemeMonitor(moduleDirectory, targetDirectory);

        List<String> args = new ArrayList<>();
        args.add(baseDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        args.add(baseDirectory.resolve("../.node/node/yarn/dist/bin/yarn.js").toFile().getCanonicalPath());
        args.add("run");
        args.add("serve");
        ProcessBuilder frontBuilder = new ProcessBuilder(args);
        frontBuilder.directory(baseDirectory.resolve("../opensilex-front/front").toFile());
        frontBuilder.inheritIO();
        return frontBuilder.start();
    }

    private static Process createFrontModuleBuilder(String moduleId) throws Exception {
        List<String> args = new ArrayList<>();
        args.add(baseDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        args.add(baseDirectory.resolve("../.node/node/yarn/dist/bin/yarn.js").toFile().getCanonicalPath());
        args.add("run");
        args.add("serve");
        ProcessBuilder frontBuilder = new ProcessBuilder(args);

        String modulePath = moduleId;

        Path moduleDirectory = baseDirectory.resolve("../" + modulePath + "/front");
        frontBuilder.directory(moduleDirectory.toFile());
        frontBuilder.inheritIO();

        Path targetDirectory = baseDirectory.resolve("../" + modulePath + "/target/classes/front");
        if (!targetDirectory.toFile().exists()) {
            Files.createDirectories(targetDirectory);
        }

        createThemeMonitor(moduleDirectory, targetDirectory);

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
                        triggerHotReload();
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

    private static void createConfigMonitor(Path moduleDirectory, Path targetDirectory) throws Exception {
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
            triggerHotReload();
        } catch (IOException ex) {
            LOGGER.error("Error while copying config file: " + configFile.getName(), ex);
        }
    }

    private static void createThemeMonitor(Path moduleDirectory, Path targetDirectory) throws Exception {
        File themeFolder = moduleDirectory.resolve("theme").toFile();
        Path themeDestFolder = targetDirectory.resolve("theme");

        if (themeFolder.exists() && themeFolder.isDirectory()) {
            initDevTheme(themeFolder, themeDestFolder.toFile());
            FileAlterationObserver configObserver = new FileAlterationObserver(themeFolder.getCanonicalPath());
            FileAlterationMonitor configMonitor = new FileAlterationMonitor(200);

            FileAlterationListener configListener = new FileAlterationListenerAdaptor() {

                @Override
                public void onFileCreate(File file) {
                    try {
                        String srcFilePath = file.getCanonicalPath();
                        String srcBasePath = themeFolder.getCanonicalPath();
                        if (srcFilePath.startsWith(srcBasePath)) {
                            String srcFileRelativePath = srcFilePath.substring(srcBasePath.length() + 1);
                            File destFile = themeDestFolder.resolve(srcFileRelativePath).toFile();
                            FileUtils.copyFile(file, destFile);
                            triggerHotReload();

                        }
                    } catch (IOException ex) {
                        LOGGER.error("Error while updating theme file", ex);
                    }
                }

                @Override
                public void onFileDelete(File file) {
                    try {
                        String srcFilePath = file.getCanonicalPath();
                        String srcBasePath = themeFolder.getCanonicalPath();
                        if (srcFilePath.startsWith(srcBasePath)) {
                            String srcFileRelativePath = srcFilePath.substring(srcBasePath.length() + 1);
                            File destFile = themeDestFolder.resolve(srcFileRelativePath).toFile();

                            FileUtils.deleteDirectory(destFile);
                            triggerHotReload();

                        }
                    } catch (IOException ex) {
                        LOGGER.error("Error while deleting theme file", ex);
                    }
                }

                @Override
                public void onFileChange(File file) {
                    this.onFileCreate(file);
                }

            };
            configObserver.addListener(configListener);
            configMonitor.addObserver(configObserver);
            configMonitor.start();
        }
    }

    private static void initDevTheme(File themeFolder, File targetDirectory) {

        try {
            if (targetDirectory.exists()) {
                FileUtils.deleteDirectory(targetDirectory);
            }
        } catch (IOException ex) {
            LOGGER.warn("Error while deleting theme directory: " + themeFolder.getAbsolutePath(), ex);
        }

        try {
            FileUtils.forceMkdir(targetDirectory);
            FileUtils.copyDirectory(themeFolder, targetDirectory);
            triggerHotReload();

        } catch (IOException ex) {
            LOGGER.error("Error while copying theme directory: " + themeFolder.getAbsolutePath(), ex);
        }

    }

    private static void triggerHotReload() {
        setPseudoScript(System.currentTimeMillis());
    }

    private static void setPseudoScript(long timeMs) {
        PrintWriter prw = null;
        try {
            String pseudoScript = "export default { \"last-dev-update\": " + timeMs + "};";
            prw = new PrintWriter(baseDirectory.resolve("../opensilex-front/front/src/opensilex.dev.ts").toFile());
            prw.println(pseudoScript);
            prw.close();
        } catch (FileNotFoundException ex) {
            LOGGER.error("Error while trying to trigger vue js hot reload", ex);
        } finally {
            prw.close();
        }
    }
}
