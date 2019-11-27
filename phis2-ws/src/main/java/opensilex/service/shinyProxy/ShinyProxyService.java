//******************************************************************************
//                                ShinyProxyService.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 sept. 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import opensilex.service.dao.ScientificAppDAO;
import org.slf4j.LoggerFactory;

/**
 * ShinyProxyService
 * Manage the link with ShinyProxy Service
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ShinyProxyService {

    final static Logger LOGGER = LoggerFactory.getLogger(ShinyProxyService.class);

    public static Boolean SHINYPROXY_UPDATE_APP_STATE = false;
    public static Boolean SHINYPROX_RUN_STATE = false;
    public static Path SHINYPROXY_CONFIG_DIRECTORY;
    public static Path SHINYPROXY_CONFIG_FILE;
    final private static String SHINYPROXY_INTERNAL_DOCKERFILE_IMAGE = "shinyProxy/Dockerfile";
    public static Path SHINYPROXY_DOCKERFILE_IMAGE;
    public static String SHINYPROXY_NETWORK_ID = "shiny-proxy-network";

    public static Path SHINYPROXY_DOCKER_FILES;
    public static String SHINYPROXY_WEB_JAR_FILE;
    final private static String SHINYPROXY_DOCKER_IMAGE = "opensilex/shinyproxy";

    final private static String INTERNAL_SHINYPROXY_CONFIG_FILEPATH = "shinyProxy/shinyproxy_config";
    public final static String SHINYPROXY_APP_DOCTYPE = "http://www.opensilex.org/vocabulary/oeso#ShinyAppPackage";
    public static ArrayList<ScientificAppDescription> SHINYPROXY_APPS_LIST;

    /**
     *
     */
    public ShinyProxyService() {
        // Initialize directory variables
        setConstantsVariables();
    }

    public void updateApplicationsListAndImages() {
        SHINYPROXY_UPDATE_APP_STATE = true;
        LOGGER.info("Listing shiny apps ... ");
        ScientificAppDAO scientificAppDAO = new ScientificAppDAO();
        SHINYPROXY_APPS_LIST = scientificAppDAO.find(null, null);
        LOGGER.info("Build images ...");
        createDockerDirAndFiles(SHINYPROXY_APPS_LIST);
        createWebApplicationsBuildImageProcess(SHINYPROXY_APPS_LIST);
        SHINYPROXY_UPDATE_APP_STATE = false;
    }

    /**
     *
     */
    public void run() {
        boolean validShinyProxyInstallation = false;
        boolean isConfigFileWritten = false;

        LOGGER.info("Creating config file ...");
        Map<String, Object> parsedYAMLFile = createConfigMapFromConfigFile();
        isConfigFileWritten = PropertiesFileManager.writeYAMLFile(
                parsedYAMLFile,
                SHINYPROXY_CONFIG_FILE.toString()
        );

        LOGGER.info("Valid configuration file" + isConfigFileWritten);
        if (isConfigFileWritten) {
            LOGGER.info("Build shinyproxy image with application.yml");
            validShinyProxyInstallation = buildShinyProxyDockerImage();
            LOGGER.info("Shinyproxy image is build");
        } else {
            LOGGER.error("Shinyproxy was not build");
        }
        if (isConfigFileWritten && validShinyProxyInstallation) {
            boolean runningShinyProxy = runProxyDockerImage();
            SHINYPROX_RUN_STATE = true;
            if (runningShinyProxy) {
                LOGGER.info("Shinyproxy is running");
            }

        }
    }

    public void stop() {
        List<Callable<Process>> callables = new ArrayList();
        callables.add(stopShinyProxyContainerServiceProcess());
        callables.add(removeShinyProxyContainerServiceProcess());
        runListTask(callables, false);
        SHINYPROX_RUN_STATE = false;
    }

    private void setConstantsVariables() {
        final String configFilePath = PropertiesFileManager.getConfigFileProperty("data_analysis_config", "shinyproxy.configFilePath");
        SHINYPROXY_CONFIG_DIRECTORY = Paths.get(configFilePath);
        SHINYPROXY_DOCKERFILE_IMAGE = Paths.get(SHINYPROXY_CONFIG_DIRECTORY.toString(), File.separator + "Dockerfile");
        SHINYPROXY_CONFIG_FILE = Paths.get(SHINYPROXY_CONFIG_DIRECTORY.toString(), File.separator + "application.yml");
        SHINYPROXY_DOCKER_FILES = Paths.get(SHINYPROXY_CONFIG_DIRECTORY.toString(), File.separator + "docker");
        SHINYPROXY_APPS_LIST = new ArrayList<>();
    }

   /**
    * Reload ShinyProxy Service
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
        File shinyJarConfigPathFile = SHINYPROXY_CONFIG_FILE.toFile();
        if (shinyJarConfigPathFile.exists()) {
            shinyJarConfigPathFile.delete();
            try {
                shinyJarConfigPathFile.createNewFile();
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }

        Map<String, Object> parsedYAMLFile = PropertiesFileManager.
                parseYAMLConfigFile(INTERNAL_SHINYPROXY_CONFIG_FILEPATH);
        LOGGER.info("Using default " + INTERNAL_SHINYPROXY_CONFIG_FILEPATH + " config file");

        Map<String, Object> proxy = (Map) parsedYAMLFile.get("proxy");
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
     * Build Shiny proxy docker Image
     * @return process
     */
    private boolean buildShinyProxyDockerImage() {

        Path copiedDockerFilePath = SHINYPROXY_DOCKERFILE_IMAGE;
        Path internalDockerFilePath = Paths.get(
                getClass().getClassLoader().
                        getResource(SHINYPROXY_INTERNAL_DOCKERFILE_IMAGE).
                        getFile()
        );
        try {
            Files.copy(
                    internalDockerFilePath,
                    copiedDockerFilePath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        List<Callable<Process>> callables = new ArrayList();
        callables.add(buildShinyNetworkServiceProcess());
        callables.add(buildShinyProxyContainerServiceProcess());
        return runListTask(callables, false);
    }
    
     private Callable<Process> buildShinyNetworkServiceProcess(){
        return () -> {
            String[] networkProccessArgs = {
                "docker",
                "network",
                "create",
                SHINYPROXY_NETWORK_ID};
            return executeProcess(networkProccessArgs, SHINYPROXY_CONFIG_DIRECTORY.toFile());
        };
    }
    
    private Callable<Process> buildShinyProxyContainerServiceProcess(){
        return () -> {
            String[] proccessArgs = {
                "docker",
                "build",
                ".",
                "-t",
                SHINYPROXY_DOCKER_IMAGE};
            return executeProcess(proccessArgs, SHINYPROXY_CONFIG_DIRECTORY.toFile());
        };
    }
    
    
    private Callable<Process> stopShinyProxyContainerServiceProcess(){
        return () -> {
            String[] proccessArgs = {
                "docker",
                "stop",
                "shiny_proxy"};
            return executeProcess(proccessArgs, SHINYPROXY_CONFIG_DIRECTORY.toFile());
        };
    }
    
    
    private  Callable<Process> removeShinyProxyContainerServiceProcess(){
        return () -> {
            String[] proccessArgs = {
                "docker",
                "rm",
                "shiny_proxy"};
            return executeProcess(proccessArgs, SHINYPROXY_CONFIG_DIRECTORY.toFile());
        };
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
     * Run a list of task in asynchronous or synchronous way
     * @param callables process
     * @param parralelism if true run asynchronous tasks
     *                    if false run synchronous tasks
     * @return 
     */
    private boolean runListTask(List<Callable<Process>> callables, boolean parralelism) {
        ExecutorService executor;
        if (parralelism) {
            executor = Executors.newWorkStealingPool();
        } else {
            executor = Executors.newSingleThreadExecutor();
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
            return false;
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return true;
    }

    /**
     * Create a directory to save informations to run dockerFile and scientific applications
     * @param shinyProxyAppList
     */
    private void createDockerDirAndFiles(ArrayList<ScientificAppDescription> shinyProxyAppList) {
        for (ScientificAppDescription shinyAppDescription : shinyProxyAppList) {
            DocumentMongoDAO documentMongoDAO = new DocumentMongoDAO();
            File document = documentMongoDAO.getDocument(shinyAppDescription.getDocumentUri());
            // Create appDirectory
            File shinyDockerPath = Paths.get(
                    SHINYPROXY_DOCKER_FILES.toString(),
                    File.separator, shinyAppDescription.getId()
            ).toFile();
            shinyDockerPath.mkdirs();
            shinyAppDescription.setExtractDockerFilesState(
                    unzipFile(document.toString(), shinyDockerPath.toString())
            );
        }
    }

    /**
     * Unzip an zip archive
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
     * Create docker image from application definition
     * @param shinyProxyAppList Application definition
     */
    private void createWebApplicationsBuildImageProcess(ArrayList<ScientificAppDescription> shinyProxyAppList) {
        List<Callable<Process>> callables = new ArrayList();
        for (ScientificAppDescription shinyAppDescription : shinyProxyAppList) {
            if (shinyAppDescription.getExtractDockerFilesState()) {
                Callable<Process> callableObj = () -> {
                    File shinyDockerPath = Paths.get(
                            SHINYPROXY_DOCKER_FILES.toString(),
                            File.separator, shinyAppDescription.getId()
                    ).toFile();
                    String[] proccessArgs = {
                        "docker",
                        "build",
                        ".",
                        "-t",
                        shinyAppDescription.containerImageName};
                    return executeProcess(proccessArgs, shinyDockerPath);
                };
                callables.add(callableObj);
            }
        }
       runListTask(callables,true);
    }

    /**
     * Run shiny Proxy Service 
     * @return 
     */
    private boolean runProxyDockerImage() {
        final String shinyproxyPort = PropertiesFileManager.getConfigFileProperty("data_analysis_config", "shinyproxy.port");
        List<Callable<Process>> callables = new ArrayList();
        Callable<Process> runCallableObject = () -> {
            String[] proccessArgs = {
                "docker",
                "run",
                "-d",
                "-v",
                "/var/run/docker.sock:/var/run/docker.sock",
                "--net", SHINYPROXY_NETWORK_ID,
                "--name", "shiny_proxy",
                "-p",
                shinyproxyPort + ":8080",
                SHINYPROXY_DOCKER_IMAGE
            };
            return executeProcess(proccessArgs, SHINYPROXY_CONFIG_DIRECTORY.toFile());
        };
        callables.add(runCallableObject);
        return runListTask(callables, false);
    }
}
