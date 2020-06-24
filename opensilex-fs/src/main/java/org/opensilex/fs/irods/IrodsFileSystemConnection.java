package org.opensilex.fs.irods;

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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * @author rcolin
 */
public class IrodsFileSystemConnection extends BaseService implements FileStorageConnection {

    public IrodsFileSystemConnection() {
        super(null);
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(IrodsFileSystemConnection.class);

    private final static String IRODS_GET_CMD = "iget";

    /**
     * Option used in order to force IRODS to write the content of a source file into the destination file when using IGET
     */
    private final static String IRODS_GET_FORCE_WRITE = "-f";

    /**
     * Temp folder used for storing IRODS incoming file(s), the files are created and then deleted after been read.
     */
    private Path tmpDirectory;

    @Override
    public void startup() throws Exception {
        tmpDirectory = Files.createTempDirectory("opensilex_irods");
        tmpDirectory.toFile().deleteOnExit();
        // TODO add IRODS init method in order to check connection
    }

    @Override
    public void shutdown() throws Exception {
        Files.deleteIfExists(tmpDirectory);
    }

    private void irodsCommand(String... args) throws IOException {
        Process irodsProcess = null;
        try {
            irodsProcess = new ProcessBuilder().command(args).start();

            // redirect eventual errors to an IOException
            checkErrorFromProcess(irodsProcess);

        } finally {
            if (irodsProcess != null && irodsProcess.isAlive()) {
                irodsProcess.destroy();
            }
        }
    }

    private void checkErrorFromProcess(Process process) throws IOException {
        InputStream errorStream = process.getErrorStream();
        try {
            byte[] errorBytes =  IOUtils.toByteArray(errorStream);
            if (errorBytes != null && errorBytes.length > 0) {
                errorStream.close();
                if (process.isAlive()) {
                    process.destroy();
                }
                throw new IOException(new String(errorBytes, StandardCharsets.UTF_8));
            }
        } finally {
            errorStream.close();
        }
    }

    @Override
    public String readFile(Path filePath) throws IOException {
        byte[] bytes = readFileAsByteArray(filePath);
        return new String(bytes,StandardCharsets.UTF_8);
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        Path tmpFile = createLocalTempFile(filePath);
        byte[] fileContent = Files.readAllBytes(tmpFile);
        Files.delete(tmpFile);
        return fileContent;
    }

    private Path createLocalTempFile(Path filePath) throws IOException {
        Path tmpFile = null;

        try {
            tmpFile = Files.createTempFile(tmpDirectory, null, null);

            irodsCommand(IRODS_GET_CMD,
                    filePath.toString(),
                    tmpFile.toString(),
                    IRODS_GET_FORCE_WRITE
            );

            return tmpFile;

        } catch (IOException e) {
            if (tmpFile != null) {
                Files.deleteIfExists(tmpFile);
            }
            throw e;
        }
    }

    @Override
    public void writeFile(Path dest, String content) throws IOException {
        File tmpFile = null;
        try {
            tmpFile = Files.createTempFile(tmpDirectory, null, null).toFile();
            FileUtils.writeStringToFile(tmpFile, content, StandardCharsets.UTF_8);
            writeFile(dest, tmpFile);
        } finally {
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    private final static String IRODS_IPUT_CMD = "iput";

    @Override
    public void writeFile(Path dest, File file) throws IOException {
        irodsCommand(
                IRODS_IPUT_CMD,
                file.getPath(),
                dest.toString()
        );
    }

    private final static String IRODS_MKDIR_CMD = "imkdir";

    @Override
    public void createDirectories(Path directoryPath) throws IOException {
        irodsCommand(
                IRODS_MKDIR_CMD,
                directoryPath.toString()
        );
    }

    private final static String IRODS_LS_CMD = "ils";

    @Override
    public boolean exist(Path filePath) throws IOException {
        try {
            irodsCommand(
                    IRODS_LS_CMD,
                    filePath.toString()
            );
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private final static String IRODS_RM_CMD = "irm";

    @Override
    public void delete(Path filePath) throws IOException {
        irodsCommand(
                IRODS_RM_CMD,
                filePath.toString()
        );
    }

}
