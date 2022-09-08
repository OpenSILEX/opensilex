package org.opensilex.fs.s3;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.fs.service.FileStorageService;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class S3FileStorageConnectionTest {

    private static OpenSilex openSilex;
    private static S3FileStorageConnection connection;

    private static final Path LITTLE_IMG = Paths.get("src", "test", "resources", "files","greenhouse.jpg");
    private static final Path MIDDLE_IMG =  Paths.get("src", "test", "resources", "files","hemispheric.jpg");
    private static final Path BIG_IMG =  Paths.get("src", "test", "resources", "files","vine.jpg");


    @BeforeClass
    public static void beforeClass() throws Exception {

        // define an OpenSILEX config with use grids as filesystem
        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
        args.put(OpenSilex.CONFIG_FILE_ARG_KEY, MIDDLE_IMG.toAbsolutePath().toString());

        // initialize OpenSILEX with this config
        openSilex = OpenSilex.createInstance(args);
        FileStorageService fs = openSilex.getServiceInstance(FileStorageService.DEFAULT_FS_SERVICE, FileStorageService.class);

        // bypass protected access of FileStorageService.getConnection method
        // use getDeclaredMethod in order to access protected method
        Method getConnectionMethod = FileStorageService.class.getDeclaredMethod("getConnection", String.class);
        getConnectionMethod.setAccessible(true);
        connection = (S3FileStorageConnection) getConnectionMethod.invoke(fs,"s3");
    }

    @AfterClass
    public static void afterClass() throws Exception{
        if(openSilex != null){
            openSilex.shutdown();
        }
    }

    private static Path[] getPaths(){
        return new Path[]{LITTLE_IMG, MIDDLE_IMG, BIG_IMG};
    }

    @Test
    public void writeFile() throws IOException {
        for(Path filePath : getPaths()){

            // build storage path, read original content and push file
            Path storagePath = Paths.get("datafile", UUID.randomUUID().toString());
            byte[] originalContent = Files.readAllBytes(filePath);
            connection.writeFile(storagePath,filePath.toFile());

            // compare file content and check existence
            byte[] contentFromFs = connection.readFileAsByteArray(storagePath);
            Assert.assertArrayEquals(originalContent,contentFromFs);
            Assert.assertTrue(connection.exist(storagePath));

            // delete file and check it no longer exist
            connection.delete(storagePath);
            Assert.assertFalse(connection.exist(storagePath));
        }
    }

    @Test
    public void WriteFileContent() throws IOException {
        for(Path filePath : getPaths()){

            // build storage path, read original content and push file
            Path storagePath = Paths.get("datafile", UUID.randomUUID().toString());
            byte[] originalContent = Files.readAllBytes(filePath);
            connection.writeFile(storagePath,new String(originalContent, StandardCharsets.UTF_8));

            // compare file content and check existence
            byte[] contentFromFs = connection.readFileAsByteArray(storagePath);
            Assert.assertArrayEquals(originalContent,contentFromFs);
            Assert.assertTrue(connection.exist(storagePath));

            // delete file and check it no longer exist
            connection.delete(storagePath);
            Assert.assertFalse(connection.exist(storagePath));
        }
    }

    @Test
    public void createDirectories() throws IOException {

        // create directory and check existence
        Path directoryPath = Paths.get("opensilex","datafile","dir1");
        connection.createDirectories(directoryPath);
        Assert.assertTrue(connection.exist(directoryPath));

        // delete directory and check it no longer exist
        connection.delete(directoryPath);
        Assert.assertFalse(connection.exist(directoryPath));

    }

}