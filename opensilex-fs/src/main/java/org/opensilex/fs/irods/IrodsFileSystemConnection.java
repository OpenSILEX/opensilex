package org.opensilex.fs.irods;

import org.apache.commons.lang3.NotImplementedException;
import org.opensilex.fs.local.LocalFileSystemConnection;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author rcolin
 */
public class IrodsFileSystemConnection extends BaseService implements FileStorageConnection {

    public IrodsFileSystemConnection() {
        super(null);
    }

    private LocalFileSystemConnection localStorageConnection;

    private final static Logger LOGGER = LoggerFactory.getLogger(IrodsFileSystemConnection.class);

    private final static String IRODS_GET_CMD = "iget";

    /**
     * Option used in order to force IRODS to write the content of a source file into the destination file when using IGET
     */
    private final static String IRODS_GET_FORCE_WRITE = "-f";

    /**
     * Temp folder used for storing IRODS incoming file(s), the files are created and then deleted after been read.
     */
    private Path IRODS_TMP_DIRECTORY;

    private Path lastTmpFilePath;

    @Override
    public void setup() throws Exception {
        IRODS_TMP_DIRECTORY = Files.createTempDirectory("opensilex_irods");
        localStorageConnection = new LocalFileSystemConnection();

        // TODO add IRODS init method in order to check connection
    }

    @Override
    public void shutdown() throws Exception {
        Files.deleteIfExists(IRODS_TMP_DIRECTORY);
    }

    private void checkErrorFromProcess(Process process) throws IOException {

        InputStream errorStream = process.getErrorStream();
        try {
            byte[] errorBytes = errorStream.readAllBytes();
            if (errorBytes != null && errorBytes.length > 0) {
                errorStream.close();
                if (process.isAlive()) {
                    process.destroy();
                }
                throw new IOException(new String(errorBytes, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            errorStream.close();
            throw e;
        }
    }

    @Override
    public String readFile(Path filePath) throws IOException {
        Path tmpFile = getPhysicalPath(filePath);
        String fileContent = localStorageConnection.readFile(tmpFile);
        Files.delete(tmpFile);
        return fileContent;
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        Path tmpFile = getPhysicalPath(filePath);
        byte[] fileContent = localStorageConnection.readFileAsByteArray(tmpFile);
        Files.delete(tmpFile);
        return fileContent;
    }

    public Path getPhysicalPath(Path filePath) throws IOException {

        Process irodsProcess = null;
        Path tmpFile = null;

        if (lastTmpFilePath != null) {
            Files.deleteIfExists(lastTmpFilePath);
        }
        try {
            tmpFile = Files.createTempFile(IRODS_TMP_DIRECTORY, null, null);

            irodsProcess = new ProcessBuilder().command(IRODS_GET_CMD,
                    filePath.toString(),
                    tmpFile.toString(),
                    IRODS_GET_FORCE_WRITE).start();

            // redirect eventual errors to an IOException
            checkErrorFromProcess(irodsProcess);
            return tmpFile;

        } catch (Exception e) {
            if (irodsProcess != null && irodsProcess.isAlive()) {
                irodsProcess.destroy();
            }
            if (tmpFile != null) {
                Files.deleteIfExists(tmpFile);
            }
            throw e;
        }
    }

    @Override
    public void writeFile(Path filePath, String content) throws IOException {
        throw new NotImplementedException("Not implemented yet");
    }

    private final static String IRODS_IPUT_CMD = "iput";

    @Override
    public void writeFile(Path dest, File file) throws IOException {

        Process irodsProcess = null;

        try {
            irodsProcess = new ProcessBuilder().command(
                    IRODS_IPUT_CMD,
                    file.getPath(),
                    dest.toString()
            ).start();

            // redirect eventual errors to logger
            checkErrorFromProcess(irodsProcess);

        } catch (Exception e) {

            if (irodsProcess != null && irodsProcess.isAlive()) {
                irodsProcess.destroy();
            }
            throw new IOException(e);
        }
    }

    private final static String IRODS_MKDIR_CMD = "imkdir";

    @Override
    public void createDirectories(Path directoryPath) throws IOException {
        Process irodsProcess = null;

        try {
            irodsProcess = new ProcessBuilder().command(
                    IRODS_IPUT_CMD,
                    directoryPath.toString()
            ).start();

            // redirect eventual errors to logger
            checkErrorFromProcess(irodsProcess);

        } catch (Exception e) {

            if (irodsProcess != null && irodsProcess.isAlive()) {
                irodsProcess.destroy();
            }
            throw new IOException(e);
        }
    }

    @Override
    public Path createFile(Path filePath) throws IOException {
        throw new NotImplementedException("Not implemented yet");
    }

    private final static String IRODS_LS_CMD = "ils";

    @Override
    public boolean exist(Path filePath) throws IOException {

        Process irodsProcess = null;
        try {
            irodsProcess = new ProcessBuilder().command(
                    IRODS_LS_CMD,
                    filePath.toString()
            ).start();

            // redirect eventual errors to logger
            checkErrorFromProcess(irodsProcess);
            return true;

        } catch (Exception e) {
            if (irodsProcess != null && irodsProcess.isAlive()) {
                irodsProcess.destroy();
            }
            return false;
        }
    }

    private final static String IRODS_RM_CMD = "irm";

    @Override
    public void delete(Path filePath) throws IOException {
        Process irodsProcess = null;
        try {
            irodsProcess = new ProcessBuilder().command(
                    IRODS_RM_CMD,
                    filePath.toString()
            ).start();

            // redirect eventual errors to logger
            checkErrorFromProcess(irodsProcess);

        } catch (Exception e) {
            if (irodsProcess != null && irodsProcess.isAlive()) {
                irodsProcess.destroy();
            }
        }
    }

}
