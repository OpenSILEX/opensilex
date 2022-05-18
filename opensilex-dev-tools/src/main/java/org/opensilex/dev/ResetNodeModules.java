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
import org.opensilex.*;
import org.opensilex.OpenSilexModule;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class ResetNodeModules {

    private final static Logger LOGGER = LoggerFactory.getLogger(ResetNodeModules.class);

    public static void main(String[] args) throws Exception {
        start(OpenSilex.getDefaultBaseDirectory(), false);
    }

    private static String nodeBin = "node";
    private static CountDownLatch countDownLatch;

    public static void start(Path baseDirectory, boolean reinstall) throws Exception {

        if (DevModule.isWindows()) {
            nodeBin += ".exe";
        }

        Map<String, String> customArgs = new HashMap<>();
        customArgs.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.INTERNAL_OPERATIONS_PROFILE_ID);

        OpenSilex opensilex = DevModule.getOpenSilexDev(baseDirectory, customArgs);

        Set<String> moduleToReinstall = new HashSet<>();
        LOGGER.debug("Purge base node_modules folder");
        Path rootNodeModulesPath = baseDirectory.resolve("../node_modules");
        File rootNodeDir = rootNodeModulesPath.toFile();
        if (rootNodeDir.exists() && rootNodeDir.isDirectory()) {
            Files.walk(rootNodeModulesPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        }

        Path rfPath = baseDirectory.resolve("../yarn.lock");
        File rf = rfPath.toFile();
        if (rf.exists() && rf.isFile()) {
            FileUtils.deleteQuietly(rf);
        }

        yarnCleanCache(baseDirectory);

        for (OpenSilexModule module : opensilex.getModules()) {
            String projectId = ClassUtils.getProjectIdFromClass(module.getClass());
            LOGGER.debug("Purge front node_modules folder for: " + projectId);
            String modulePath = projectId;

            Path nodeModulesPath = baseDirectory.resolve("..").resolve(modulePath).resolve("front/node_modules");
            File nodeDir = nodeModulesPath.toFile();
            if (nodeDir.exists() && nodeDir.isDirectory()) {
                Files.walk(nodeModulesPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            Path fPath = baseDirectory.resolve("..").resolve(modulePath).resolve("front/yarn.lock");
            File f = fPath.toFile();
            if (f.exists() && f.isFile()) {
                FileUtils.deleteQuietly(f);
            }

            fPath = baseDirectory.resolve("..").resolve(modulePath).resolve("front/package.json");
            f = fPath.toFile();
            if (f.exists() && f.isFile()) {
                moduleToReinstall.add(modulePath);
            }
        }

        if (reinstall) {
            LOGGER.debug("Re-install front modules dependencies");
            countDownLatch = new CountDownLatch(moduleToReinstall.size() + 1);

            createYarnInstallProcess(baseDirectory, baseDirectory.resolve("../"));

            for (String modulePath : moduleToReinstall) {
                Path moduleDirectory = baseDirectory.resolve("../" + modulePath + "/front");
                createYarnInstallProcess(baseDirectory, moduleDirectory);
            }

            countDownLatch.await();
        }
    }

    private static void yarnCleanCache(Path baseDirectory) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>();
        args.add(baseDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        args.add(baseDirectory.resolve("../.node/node/yarn/dist/bin/yarn.js").toFile().getCanonicalPath());
        args.add("cache");
        args.add("clean");
        ProcessBuilder yarnCleanCacheProcess = new ProcessBuilder(args);

        yarnCleanCacheProcess.inheritIO();

        Process process = yarnCleanCacheProcess.start();
        process.waitFor();

    }

    private static void createYarnInstallProcess(Path baseDirectory, Path moduleDirectory) throws IOException {
        List<String> args = new ArrayList<>();
        args.add(baseDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        args.add(baseDirectory.resolve("../.node/node/yarn/dist/bin/yarn.js").toFile().getCanonicalPath());
        args.add("install");
        ProcessBuilder nodeModulesBuilder = new ProcessBuilder(args);

        nodeModulesBuilder.directory(moduleDirectory.toFile());
        nodeModulesBuilder.inheritIO();

        new Thread() {
            @Override
            public void run() {
                try {
                    Process process = nodeModulesBuilder.start();
                    process.waitFor();
                } catch (Exception ex) {
                    LOGGER.error("Yarn install process interrupted", ex);
                } finally {
                    countDownLatch.countDown();
                }

            }

        }.start();

    }
}
