/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.shinyProxy;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author charlero
 */
public class DockerUtils {

    public static final int TIMEOUT = 20;

    private final static Logger LOGGER = LoggerFactory.getLogger(DockerUtils.class);

    public DockerUtils() {
    }

    public boolean isDockerServiceIsRunning() {
        return !checkDockerIsRunning();
    }

    /**
     *
     * @param containerId
     */
    public static void startDockerContainer(String containerId) {
        if (!checkContainerAlive(containerId)) {
            ProcessUtils.runCommand("docker start " + containerId);
            LOGGER.info("Trying to start " + containerId + " container");
        } else {
            LOGGER.info(containerId + " is up-and-running.");
        }
    }

    /**
     *
     * @param containerId
     */
    public static void stopDockerContainer(String containerId) {
        if (checkContainerAlive(containerId)) {
            System.out.println("Trying to stop " + containerId + " container");
            String[] proccessArgs = {
                "docker",
                "stop",
                containerId};
            ProcessUtils.executeProcess(proccessArgs, null);
            LOGGER.info("Docker is stopped.");
        } else {
            LOGGER.info("Docker container is not working.");
        }
    }

    /**
     *
     * @param containerId
     * @return
     */
    public static boolean checkContainerAlive(String containerId) {
        LOGGER.debug("Checking the container " + containerId);
        Process p = ProcessUtils.runCommand("docker inspect -f {{.State.Running}} " + containerId);
        return ProcessUtils.readCommandResult(p);
    }

    /**
     *
     * @param imageName
     * @param tagName
     * @return
     */
    public static boolean checkDockerImageExist(String imageName, String tagName) {

        if (imageName == null) {
            return false;
        }
        List<String> cmdArgs = Stream.of(
                "docker",
                "image",
                "inspect")
                .collect(Collectors.toList());
        LOGGER.debug("Checking the docker image " + imageName);

        if (tagName != null) {
            cmdArgs.add(imageName + ":" + tagName);
        } else {
            cmdArgs.add(imageName);
        }

        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }

    /**
     *
     * @param containerName
     * @return
     */
    public static boolean checkDockerContainerExist(String containerName) {

        if (containerName == null) {
            return false;
        }
        List<String> cmdArgs = Stream.of(
                "docker",
                "container",
                "inspect",
                containerName)
                .collect(Collectors.toList());
        LOGGER.debug("Checking the docker container " + containerName);
        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }

    /**
     *
     * @param networkName
     * @return
     */
    public static boolean checkDockerNetworkExist(String networkName) {

        if (networkName == null) {
            return false;
        }
        List<String> cmdArgs = Stream.of("docker",
                "network",
                "inspect",
                networkName)
                .collect(Collectors.toList());
        LOGGER.debug("Checking the docker network " + networkName);
        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }
    
     /**
     *
     * @param containerName
     * @return
     */
    public static boolean stopRemoveContainer(String containerName) {

        if (containerName == null) {
            return false;
        }
        return (stopContainer(containerName) && removeContainer(containerName));
    }
    
     /**
     *
     * @param containerName
     * @return
     */
    public static boolean stopContainer(String containerName) {

        if (containerName == null) {
            return false;
        }
        List<String> cmdArgs = Stream.of(
                "docker",
                "stop", 
                containerName)
                .collect(Collectors.toList());
        LOGGER.debug("Stopping the docker container " + containerName);
        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }
    
      /**
     *
     * @param containerName
     * @return
     */
    public static boolean removeContainer(String containerName) {

        if (containerName == null) {
            return false;
        }
        List<String> cmdArgs = Stream.of(
                "docker",
                "rm", 
                containerName)
                .collect(Collectors.toList());
        LOGGER.debug("Removing  the docker container " + containerName);
        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }
    
      /**
     *
     * @param imageName
     * @return
     */
    public static boolean temoveImage(String imageName) {

        if (imageName == null) {
            return false;
        }
        List<String> cmdArgs = Stream.of(
                "docker",
                "rm", 
                imageName)
                .collect(Collectors.toList());
        LOGGER.debug("Removing  the docker image " + imageName);
        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }
    
    /**
     *
     * @param runArgs
     * @return
     */
    public static boolean runContainer(List<String> runArgs) {

        if (runArgs == null || runArgs.isEmpty()) {
            return false;
        }
        List<String> cmdArgs = Stream.of(
                "docker",
                "run")
                .collect(Collectors.toList());

        cmdArgs.addAll(runArgs);

        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }

    /**
     *
     * @param DockerFilePath
     * @param imageName
     * @return
     */
    public static boolean buildImage(Path DockerFilePath, String imageName) {

        if (imageName == null || DockerFilePath == null || DockerFilePath.toString().isEmpty() || imageName.isEmpty()) {
            return false;
        }
        List<String> cmdArgs = Stream.of(
                "docker",
                "build",
                DockerFilePath.toString(),
                "-t",
                imageName)
                .collect(Collectors.toList());

        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }

    /**
     *
     * @param networkName
     * @return
     */
    public static boolean createNetwork(String networkName) {

        if (networkName == null || networkName.isEmpty()) {
            return false;
        }
        List<String> cmdArgs = Stream.of(
                "docker",
                "network",
                "create",
                networkName)
                .collect(Collectors.toList());

        Process p = ProcessUtils.runCommandArray(ProcessUtils.convertListStringToArray(cmdArgs));
        return ProcessUtils.readCommandResult(p);
    }

    /**
     *
     * @return
     */
    public static boolean checkDockerIsRunning() {
        LOGGER.debug("Is docker running");
        Process p = ProcessUtils.runCommand("docker ps");
        return ProcessUtils.readCommandResult(p);
    }

}
