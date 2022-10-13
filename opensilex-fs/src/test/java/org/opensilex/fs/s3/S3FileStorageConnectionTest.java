package org.opensilex.fs.s3;

import org.junit.*;
import org.opensilex.OpenSilex;
import org.opensilex.fs.service.FileStorageService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
@Ignore("Need real S3 config for run")
public class S3FileStorageConnectionTest {

    private static OpenSilex openSilex;
    protected static FileStorageService fs;
    protected S3FileStorageConnection connection;

    /**
     * Path to test config. You should edit with correct endpoint/region and bucket before running this test.
     * Moreover, you should ensure that authentication is OK.
     * <p>
     * By default, the {@link S3FileStorageConnection}
     * will use a {@link software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider}.
     */
    private static final Path CONFIG_PATH = Paths.get("src", "test", "resources", "s3", "opensilex_s3_fs_config.yml");
    private static final Path LITTLE_IMG = Paths.get("src", "test", "resources", "files", "greenhouse.jpg");
    private static final Path MIDDLE_IMG = Paths.get("src", "test", "resources", "files", "hemispheric.jpg");
    private static final Path BIG_IMG = Paths.get("src", "test", "resources", "files", "vine.jpg");

    public S3FileStorageConnectionTest() throws ReflectiveOperationException {
        connection = getConnection(fs,"connection1");
    }

    @BeforeClass
    public static void beforeClass() throws Exception {

        // define an OpenSILEX config with use grids as filesystem
        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
        args.put(OpenSilex.CONFIG_FILE_ARG_KEY, CONFIG_PATH.toAbsolutePath().toString());

        // initialize OpenSILEX with this config
        openSilex = OpenSilex.createInstance(args);
        fs = openSilex.getServiceInstance(FileStorageService.DEFAULT_FS_SERVICE, FileStorageService.class);
    }

    protected static S3FileStorageConnection getConnection(FileStorageService fs, String key) throws ReflectiveOperationException {

        // bypass protected access of FileStorageService.getConnection method
        // use getDeclaredMethod in order to access protected method
        Method getConnectionMethod = FileStorageService.class.getDeclaredMethod("getConnection", String.class);
        getConnectionMethod.setAccessible(true);
        return (S3FileStorageConnection) getConnectionMethod.invoke(fs, key);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (openSilex != null) {
            openSilex.shutdown();
        }
    }

    private static Path[] getPaths() {
        return new Path[]{LITTLE_IMG, MIDDLE_IMG, BIG_IMG};
    }

    @Test
    @Ignore
    public void writeFile() throws IOException {
        for (Path filePath : getPaths()) {

            // build storage path, read original content and push file
            Path storagePath = Paths.get("file", UUID.randomUUID().toString());
            byte[] originalContent = Files.readAllBytes(filePath);
            connection.writeFile(storagePath, filePath.toFile());

            // compare file content and check existence
            byte[] contentFromFs = connection.readFileAsByteArray(storagePath);
            Assert.assertArrayEquals(originalContent, contentFromFs);
            Assert.assertTrue(connection.exist(storagePath));

            // delete file and check it no longer exist
            connection.delete(storagePath);
            Assert.assertFalse(connection.exist(storagePath));
        }
    }

    @Test
    @Ignore
    public void WriteFileContent() throws IOException {
        for (Path filePath : getPaths()) {

            // build storage path, read original content and push file
            Path storagePath = Paths.get("file", UUID.randomUUID().toString());
            byte[] originalContent = Files.readAllBytes(filePath);
            connection.writeFile(storagePath, originalContent);

            // compare file content and check existence
            byte[] contentFromFs = connection.readFileAsByteArray(storagePath);
            Assert.assertArrayEquals(originalContent, contentFromFs);
            Assert.assertTrue(connection.exist(storagePath));

            // delete file and check it no longer exist
            connection.delete(storagePath);
            Assert.assertFalse(connection.exist(storagePath));
        }
    }

    @Test
    @Ignore
    public void createDirectories() throws IOException {

        // create directory and check existence
        Path directoryPath = Paths.get("opensilex", "file", "dir1");
        connection.createDirectories(directoryPath);
        Assert.assertTrue(connection.exist(directoryPath));

        // delete directory and check it no longer exist
        connection.delete(directoryPath);
        Assert.assertFalse(connection.exist(directoryPath));
    }

}