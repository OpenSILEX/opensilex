//******************************************************************************
//                                ShinyProxyProcess.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 sept. 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/charlero/GIT/GITHUB/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.shinyProxy;

import opensilex.service.model.ScientificAppDescription;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import opensilex.service.PropertiesFileManager;
import opensilex.service.dao.DocumentMongoDAO;
import opensilex.service.dao.DocumentRdf4jDAO;
import opensilex.service.dao.ScientificAppDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.Document;
import opensilex.service.model.User;
import org.slf4j.LoggerFactory;

/**
 *
 * @author charlero
 */
public class ShinyProxyProcess {

    final static Logger LOGGER = LoggerFactory.getLogger(ShinyProxyProcess.class);

    static Process SHINYPROXY_PROCESS;
    public static Boolean SHINYPROX_UPDATE_APP_STATE = false;
    public static Boolean SHINYPROX_RUN_STATE = false;
    public static Path SHINYPROXY_JAR_DIRECTORY;
    public static Path SHINYPROXY_JAR_FILE;
    public static Path SHINYPROXY_JAR_CONFIG;
    public static Path SHINYPROXY_DOCKER_FILES;
    public static String SHINYPROXY_WEB_JAR_FILE;
    static String INTERNAL_SHINYPROXY_CONFIG_FILENAME = "shinyproxy_config";
    public final static String SHINYPROXY_APP_DOCTYPE = "http://www.opensilex.org/vocabulary/oeso#ShinyAppPackage";
    public static ArrayList<ScientificAppDescription> SHINYPROXY_APPS_LIST;

    /**
     *
     */
    public ShinyProxyProcess() {
        // Initialize directory variables
        setConstantsVariables();
    }

    public void updateApplicationsListAndImages() {
        SHINYPROX_UPDATE_APP_STATE = true;
        LOGGER.info("Listing shiny apps ... ");
        ScientificAppDAO scientificAppDAO = new ScientificAppDAO();
        SHINYPROXY_APPS_LIST = scientificAppDAO.find(null,null);
        LOGGER.info("Build images ...");
        createDockerDirAndFiles(SHINYPROXY_APPS_LIST);
        createBuildImageProcess(SHINYPROXY_APPS_LIST);
        SHINYPROX_UPDATE_APP_STATE = false;
    }

    /**
     *
     */
    public void run() {
        LOGGER.info("Validating jar executable...");
        boolean validShinyProxyInstallation = downloadShinyProxyExecutable();
        
        LOGGER.info("Creating config file ...");
        Map<String, Object> parsedYAMLFile = createConfigMapFromConfigFile();
        boolean  isConfigFileWritten = PropertiesFileManager.writeYAMLFile(parsedYAMLFile, SHINYPROXY_JAR_CONFIG.toString());

        LOGGER.info("Valid config" + validShinyProxyInstallation + " " + isConfigFileWritten);
        if (validShinyProxyInstallation && isConfigFileWritten) {
            LOGGER.info("Run shinyproxy");
            String[] proccessArgs = {"java", "-jar", SHINYPROXY_JAR_FILE.toString()};
            SHINYPROXY_PROCESS = executeShinyProcess(proccessArgs, SHINYPROXY_JAR_DIRECTORY.toFile());
            SHINYPROX_RUN_STATE = true;
        } else {
            LOGGER.error("Shinyproxy is not running");
        }
    }

    public void stop() {
        if (SHINYPROXY_PROCESS != null) {
            SHINYPROXY_PROCESS.destroy();
            try {
                SHINYPROXY_PROCESS.waitFor();
            } catch (InterruptedException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
        SHINYPROXY_PROCESS = null;
        SHINYPROX_RUN_STATE = false;
    }

    private void setConstantsVariables() {
        final String shinyJarFile = PropertiesFileManager.getConfigFileProperty("data_analysis_config", "shinyproxy.jarFilePath");
        SHINYPROXY_JAR_FILE = Paths.get(shinyJarFile);
        SHINYPROXY_JAR_DIRECTORY = SHINYPROXY_JAR_FILE.getParent();
        SHINYPROXY_JAR_CONFIG = Paths.get(SHINYPROXY_JAR_DIRECTORY.toString(), File.separator + "application.yml");
        SHINYPROXY_DOCKER_FILES = Paths.get(SHINYPROXY_JAR_DIRECTORY.toString(), File.separator + "docker");
        SHINYPROXY_WEB_JAR_FILE = PropertiesFileManager.getConfigFileProperty("data_analysis_config", "shinyproxy.webFilePath");
        SHINYPROXY_APPS_LIST = new ArrayList<>();
    }

    /**
     *
     */
    public void reload() {
        CompletableFuture<String> completableFuture
                = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> {
            updateApplicationsListAndImages();
            stop();
            run();
            completableFuture.complete("Shiny Proxy Reload Complete");
            return null;
        });

    }

    private Map<String, Object> createConfigMapFromConfigFile() {
        File shinyJarConfigPathFile = SHINYPROXY_JAR_CONFIG.toFile();
        if (shinyJarConfigPathFile.exists()) {
            shinyJarConfigPathFile.delete();
            try {
                shinyJarConfigPathFile.createNewFile();
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
        
        Map<String, Object> parsedYAMLFile = PropertiesFileManager.
                parseYAMLConfigFile(INTERNAL_SHINYPROXY_CONFIG_FILENAME);
        LOGGER.info("Using default " + INTERNAL_SHINYPROXY_CONFIG_FILENAME + " config file");
        
        Map<String, Object> proxy = (Map) parsedYAMLFile.get("proxy");
        final String shinyHost = PropertiesFileManager.
                getConfigFileProperty("data_analysis_config", "shinyproxy.host");
        final String shinyPort = PropertiesFileManager.
                getConfigFileProperty("data_analysis_config", "shinyproxy.port");
        proxy.put("bind-address", shinyHost);
        proxy.put("port", Integer.parseInt(shinyPort));
        ArrayList<Map> specs = new ArrayList<>();
        for (ScientificAppDescription shinyAppDescription : SHINYPROXY_APPS_LIST) {
            if (shinyAppDescription.getExtractDockerFilesState()) {
                specs.add(shinyAppDescription.convertToYamlFormatMap());
            }
        }
        proxy.put("specs", specs);

        return parsedYAMLFile;
    }

    /**
     * 
     * @return 
     */
    private boolean downloadShinyProxyExecutable(){
        boolean validShinyProxyExecutable = true;
        File shinyJarFileFile = SHINYPROXY_JAR_FILE.toFile();
        // retreive executable
        if (!shinyJarFileFile.exists()) {
            LOGGER.error("shinyproxy.jarFile doesn't exists");
            validShinyProxyExecutable = false;
            LOGGER.info("Try to  download shinyproxy.jarFile ....");
            try {
                ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(SHINYPROXY_WEB_JAR_FILE).openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(SHINYPROXY_JAR_FILE.toString());
                fileOutputStream.getChannel()
                        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                if (!shinyJarFileFile.exists()) {
                    LOGGER.info("shinyproxy.jarFile can't be downloaded");
                    System.exit(1);
                } else {
                    LOGGER.info("shinyproxy.jarFile downloaded");
                    validShinyProxyExecutable = true;
                }
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        } else {
            LOGGER.info("shinyproxy.jarFile found");
        }
        return validShinyProxyExecutable;
    }

    /**
     * 
     * @param proccessArgs
     * @param directoryPath
     * @return 
     */
    private Process executeProcess(String[] proccessArgs, File directoryPath) {
        ProcessBuilder processBuilder;
        Process process;
        try {
            processBuilder = new ProcessBuilder(proccessArgs);
            processBuilder.directory(directoryPath);
            processBuilder.inheritIO();

            process = processBuilder.start();
            BufferedReader stdoutReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                LOGGER.info(line);
            }

            BufferedReader stderrReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            while ((line = stderrReader.readLine()) != null) {
                LOGGER.error(line);
            }
            try {
                process.waitFor();
            } catch (InterruptedException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            return process;
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 
     * @param proccessArgs
     * @param directoryPath
     * @return 
     */
    private Process executeShinyProcess(String[] proccessArgs, File directoryPath) {
        ProcessBuilder processBuilder;
        Process process = null;
        boolean resultState = true;
        try {
            processBuilder = new ProcessBuilder(proccessArgs);
            processBuilder.directory(directoryPath);
            processBuilder.inheritIO();

            process = processBuilder.start();
            BufferedReader stdoutReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                LOGGER.info(line);
            }

            BufferedReader stderrReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            while ((line = stderrReader.readLine()) != null) {
                LOGGER.error(line);
            }
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        System.out.println("opensilex.service.shinyProxy.ShinyProxyProcess.executeProcess()" + resultState);

        return process;
    }


    /**
     * 
     * @param shinyProxyAppList 
     */
    private void createDockerDirAndFiles(ArrayList<ScientificAppDescription> shinyProxyAppList) {
        for (ScientificAppDescription shinyAppDescription : shinyProxyAppList) {
            DocumentMongoDAO documentMongoDAO = new DocumentMongoDAO();
            File document = documentMongoDAO.getDocument(shinyAppDescription.getDocumentUri());
            // Create appDirectory
            File shinyDockerPath = Paths.get(SHINYPROXY_DOCKER_FILES.toString(), File.separator, shinyAppDescription.getId()).toFile();
            shinyDockerPath.mkdirs();
            shinyAppDescription.setExtractDockerFilesState(
                    unzipFile(document.toString(), shinyDockerPath.toString())
            );
        }
    }

    /**
     * 
     * @param zipFile
     * @param extractFolder
     * @return 
     */
    private static boolean unzipFile(String zipFile, String extractFolder) {
        try {
            int BUFFER = 2048;
            File file = new File(zipFile);

            ZipFile zip = new ZipFile(file);
            String newPath = extractFolder;

            new File(newPath).mkdir();
            Enumeration zipFileEntries = zip.entries();

            // Process each entry
            while (zipFileEntries.hasMoreElements()) {
                // grab a zip file entry
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();

                File destFile = new File(newPath, currentEntry);
                //destFile = new File(newPath, destFile.getName());
                File destinationParent = destFile.getParentFile();

                // create the parent directory structure if needed
                destinationParent.mkdirs();

                if (!entry.isDirectory()) {
                    BufferedInputStream is = new BufferedInputStream(zip
                            .getInputStream(entry));
                    int currentByte;
                    // establish buffer for writing file
                    byte data[] = new byte[BUFFER];

                    // write the current file to disk
                    FileOutputStream fos = new FileOutputStream(destFile);
                    BufferedOutputStream dest = new BufferedOutputStream(fos,
                            BUFFER);

                    // read and write until last byte is encountered
                    while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, currentByte);
                    }
                    dest.flush();
                    dest.close();
                    is.close();

                }

            }
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    /**
     * 
     * @param shinyProxyAppList 
     */
    private void createBuildImageProcess(ArrayList<ScientificAppDescription> shinyProxyAppList) {
        ExecutorService executor = Executors.newWorkStealingPool();
        List<Callable<Process>> callables = new ArrayList();
        for (ScientificAppDescription shinyAppDescription : shinyProxyAppList) {
            if (shinyAppDescription.getExtractDockerFilesState()) {
                Callable<Process> callableObj = () -> {
                    File shinyDockerPath = Paths.get(SHINYPROXY_DOCKER_FILES.toString(), File.separator, shinyAppDescription.getId()).toFile();
                    String[] proccessArgs = {"docker",
                        "build",
                        ".",
                        "-t",
                        shinyAppDescription.container_image,};
                    return executeProcess(proccessArgs, shinyDockerPath);
                };
                callables.add(callableObj);
            }
        }
        try {
            executor.invokeAll(callables)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .forEach(System.out::println);
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
