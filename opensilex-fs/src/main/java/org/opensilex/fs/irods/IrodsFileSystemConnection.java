package org.opensilex.fs.irods;

import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.service.BaseService;
import org.opensilex.utils.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 * @author rcolin
 */
@ServiceDefaultDefinition(config = IrodsFileSystemConfig.class)
public class IrodsFileSystemConnection extends BaseService implements Service, FileStorageConnection {

    private final ProcessExecutor processExecutor;

    public IrodsFileSystemConnection(IrodsFileSystemConfig config) {
        super(config);
        this.processExecutor = new ProcessExecutor(config.readTimeoutSeconds());
    }

    public IrodsFileSystemConfig getImplementedConfig() {
        return (IrodsFileSystemConfig) this.getConfig();
    }

    public Path getStorageBasePath() {
        return Paths.get(getImplementedConfig().basePath());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(IrodsFileSystemConnection.class);

    private static final String IRODS_GET_CMD = "iget";
    private static final String IRODS_IPUT_CMD = "iput";
    private static final String IRODS_MKDIR_CMD = "imkdir";

    /**
     * Option used in order to force IRODS to write the content of a source file into the destination file when using IGET
     */
    private static final String IRODS_FORCE_OPTION = "-f";


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
        if (tmpDirectory != null) {
            Files.deleteIfExists(tmpDirectory);
        }
    }

    public Path getAbsolutePath(Path filePath) throws IOException {
        if (filePath.isAbsolute()) {
            return filePath;
        } else {
            return this.getStorageBasePath().resolve(filePath).toAbsolutePath();
        }
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        filePath = getAbsolutePath(filePath);
        Path tmpFile = createLocalTempFile(filePath);
        byte[] fileContent = Files.readAllBytes(tmpFile);
        Files.delete(tmpFile);
        return fileContent;
    }

    private Path createLocalTempFile(Path filePath) throws IOException {
        Path tmpFile = null;

        try {
            tmpFile = Files.createTempFile(tmpDirectory, null, null);

            processExecutor.execute(IRODS_GET_CMD,
                    filePath.toString(),
                    tmpFile.toString(),
                    IRODS_FORCE_OPTION
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
    public void writeFile(Path dest, byte[] content) throws IOException {
        Path filePath = getAbsolutePath(dest);

        Path tmpPath = null;
        try {
            tmpPath = Files.createTempFile(tmpDirectory, null, null);
            Files.write(tmpPath, content, StandardOpenOption.WRITE);
            writeFile(filePath, tmpPath.toFile());
        } finally {
            if (tmpPath != null) {
                File tmpFile = tmpPath.toFile();
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
            }
        }

    }


    @Override
    public void writeFile(Path dest, File file) throws IOException {
        Path filePath = getAbsolutePath(dest);

        try {
            createDirectories(filePath.getParent());
        } catch (Exception e) {
            LOGGER.debug(e.getMessage());
        }

        processExecutor.execute(
                IRODS_IPUT_CMD,
                file.getPath(),
                filePath.toString()
        );

    }

    /**
     * Option used in order to force IRODS make parent directories as needed
     */
    private static final String IRODS_MKDIR_PARENT = "-p";

    @Override
    public void createDirectories(Path directoryPath) throws IOException {
        processExecutor.execute(
                IRODS_MKDIR_CMD,
                directoryPath.toString(),
                IRODS_MKDIR_PARENT
        );
    }

    private static final String IRODS_LS_CMD = "ils";

    @Override
    public boolean exist(Path filePath) throws IOException {
        try {
            processExecutor.execute(
                    IRODS_LS_CMD,
                    filePath.toString()
            );
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private static final String IRODS_RM_CMD = "irm";

    @Override
    public void delete(Path filePath) throws IOException {
        filePath = getAbsolutePath(filePath);
        processExecutor.execute(
                IRODS_RM_CMD,
                filePath.toString()
        );
    }

}
