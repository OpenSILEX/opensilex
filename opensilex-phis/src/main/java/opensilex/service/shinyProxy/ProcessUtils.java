/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.shinyProxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author charlero
 */
public class ProcessUtils {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(ProcessUtils.class);

      /**
      * 
      * @param command
      * @return 
      */
    public static Process runCommand(String command) {
        Process p = null;
        try {
            LOGGER.debug(command);
            p = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return p;
    }

    
    /**
      * 
      * @param command
      * @return 
      */
    public static Process runCommandArray(String[] command) {
        Process p = null;
        try {
            LOGGER.debug(Arrays.toString(command));
            p = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return p;
    }
    /**
     * 
     * @param proc
     * @return 
     */
    public static boolean readCommandResult(Process proc) {
        String s;
        boolean result = false;
        try {
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }

            // read the output from the command
            while ((s = stdOut.readLine()) != null) {
//                if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
//                    result = Boolean.parseBoolean(s);
//                }
                result = true;
                LOGGER.debug("stdOut : " + s);
            }
         
            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                LOGGER.error("stdError : " + s);
                result = false;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return result;
    }
    
    /**
     *
     * @param proccessArgs
     * @param directoryPath
     * @return
     */
    public static Process executeProcess(String[] proccessArgs, File directoryPath) {
        ProcessBuilder processBuilder;
        Process process;
        try {
            processBuilder = new ProcessBuilder(proccessArgs);
            if(directoryPath != null){
                processBuilder.directory(directoryPath);
            }
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
    public boolean runTaskList(List<Callable<Process>> callables, boolean parralelism) {
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
    
    public static String[]  convertListStringToArray(List<String> list){
        return list.stream().toArray(String[]::new);
    }
    
    
}
