package org.opensilex.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ProcessExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutor.class);
    private final int processTimeoutSeconds;

    public ProcessExecutor(int processTimeoutSeconds) {
        this.processTimeoutSeconds = processTimeoutSeconds;
    }

    public ProcessExecutor() {
        this(600);
    }

    private byte[] readProcessStream(Process process, InputStream stream) throws IOException {

        try {
            // Read stream inside a byte[].
            // This way is OK since expected stream size is not too big for memory
            byte[] streamData = IOUtils.toByteArray(stream);

            if (streamData == null || streamData.length == 0) {
                return new byte[0];
            }

            // Check if the process output was read and process ended properly
            // Ensure process termination within a delay
            boolean exitOk = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
            if (!exitOk) {
                LOGGER.error("Command failed to properly exit");
                throw new IOException("Command failed to properly exist");
            }

            return streamData;

            // Error handling: I/O, timeout, interruption
        } catch (InterruptedException e) {
            throw new IOException(e);
        }catch (IOException e){
            LOGGER.error("Process interrupted during command execution: {} {}", e, process.info().command().orElse(""));
            throw e;
        }finally {
            if(process.isAlive()){
                process.destroy();
            }
        }
    }


    /**
     * Get and read the process standard error
     *
     * @param process An alive process from which stderr is read
     * @throws IOException if I/O error occurs during process output read
     * @see Process#getErrorStream()
     */
    public void throwIfStderr(@NotNull Process process) throws IOException {
        byte[] bytes = readStderr(process);
        if (bytes.length > 0) {
            throw new IOException(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    public void throwIfStderr(@NotNull String args) throws IOException {
        Process process = new ProcessBuilder(args).start();
        throwIfStderr(process);
    }

    /**
     *
     * @param process
     * @return
     * @throws IOException
     */
    public byte[] readStderr(Process process) throws IOException {
        try (InputStream errorStream = process.getErrorStream()) {
            return readProcessStream(process, errorStream);
        }
    }

    /**
     *
     * @param process
     * @return
     * @throws IOException
     */
    public byte[] readStdout(Process process) throws IOException {
        try (InputStream inputStream = process.getInputStream()) {
            return readProcessStream(process, inputStream);
        }
    }

    /**
     *
     * @param process
     * @return
     * @throws IOException
     */
    public String readStdoutString(Process process) throws IOException {
        byte[] bytes = readStdout(process);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     *
     * @param args
     * @return
     * @throws IOException
     */
    public byte[] execute(List<String> args) throws IOException {
        return execute(args.toArray(new String[0]));
    }

    /**
     *
     * @param args
     * @return
     * @throws IOException
     */
    public byte[] execute(String... args) throws IOException {
        return execute(true, args);
    }

    /**
     *
     * @param readStdout
     * @param args
     * @return
     * @throws IOException
     */
    public byte[] execute(boolean readStdout, String... args) throws IOException {

        Objects.requireNonNull(args);
        Process process = null;

        try {
            process = new ProcessBuilder().command(args).start();

            // Read stdout from process and check stderr
            throwIfStderr(process);
            if (readStdout) {
                return readStdout(process);
            }

        } finally {
            // Check that process is ended
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
        return null;
    }

}
