package org.opensilex.fs.gridfs;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.junit.*;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.fs.service.FileStorageService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rcolin
 */
public class GridFSConnectionTest extends AbstractMongoIntegrationTest {

    private static OpenSilex openSilex;
    private static GridFSConnection gridFSConnection;
    private static GridFSBucket gridFSBucket;

    private static final Path gridFsConfigPath = Paths.get("src", "test", "resources", "fs","gridfs","opensilex_gridfs_config.yml");
    private static final Path filePath = Paths.get("src", "test", "resources", "fs","gridfs", "opensilex_logo.png");

    @BeforeClass
    public static void beforeClass() throws Exception {

        // define an OpenSILEX config with use grids as filesystem
        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
        args.put(OpenSilex.CONFIG_FILE_ARG_KEY,gridFsConfigPath.toAbsolutePath().toString());

        // initialize OpenSILEX with this config
        openSilex = OpenSilex.createInstance(args);
        FileStorageService fs = openSilex.getServiceInstance(FileStorageService.DEFAULT_FS_SERVICE, FileStorageService.class);

        // bypass protected access of FileStorageService.getConnection method
        // use getDeclaredMethod in order to access protected method
        Method getConnectionMethod = FileStorageService.class.getDeclaredMethod("getConnection", String.class);
        getConnectionMethod.setAccessible(true);
        gridFSConnection = (GridFSConnection) getConnectionMethod.invoke(fs,"gridfs");

        // bypass private access of GridFSConnection.gridFSBucket
        Field bucketField = GridFSConnection.class.getDeclaredField("gridFSBucket");
        bucketField.setAccessible(true);
        gridFSBucket = (GridFSBucket) bucketField.get(gridFSConnection);
    }

    @AfterClass
    public static void afterClass() throws Exception{
        gridFSConnection.shutdown();

        // bypass private access of static field GridFSConnection.mongoClient
        Field mongoClientField = GridFSConnection.class.getDeclaredField("mongoClient");
        mongoClientField.setAccessible(true);

        // ensure that the client has been closed, if so then access to any method should throw an IllegalStateException
        MongoClient mongoClient = (MongoClient) mongoClientField.get(gridFSConnection);
        Assert.assertThrows(IllegalStateException.class, mongoClient::startSession);

        if(openSilex != null){
            openSilex.shutdown();
        }
    }

    /**
     * Ensure that the bucket is created before each test
     */
    @Before
    public void before() throws Exception {
        gridFSConnection.startup();
        Assert.assertNotNull(gridFSBucket);
        Assert.assertEquals("fs", gridFSBucket.getBucketName());
    }

    /**
     * Ensure that the bucket is dropped after each test
     */
    @After
    public void after() {
        gridFSBucket.drop();
    }

    @Test
    public void getImplementedConfig(){
        GridFSConfig config = gridFSConnection.getImplementedConfig();
        Assert.assertNotNull(config);
        Assert.assertEquals(MONGO_DATABASE,config.database());
        Assert.assertEquals(MONGO_HOST,config.host());
        Assert.assertEquals(MONGO_PORT,config.port());
    }

    private void writeFileForTest() throws IOException {
        gridFSConnection.writeFile(filePath,filePath.toFile());
    }

    @Test
    public void readFileAsByteArray() throws IOException {
        writeFileForTest();

        // ensure that content integrity is OK
        byte[] expectedContent = Files.readAllBytes(filePath);
        byte[] fileContent = gridFSConnection.readFileAsByteArray(filePath);
        Assert.assertArrayEquals(expectedContent, fileContent);
    }

    @Test
    public void writeFile() throws IOException {
        writeFileForTest();

        // do a search on each file and ensure that a file has been created
        GridFSFindIterable find = gridFSBucket.find();
        try(MongoCursor<GridFSFile> cursor = find.cursor()){
            Assert.assertTrue(cursor.hasNext());
        }
    }

    @Test
    public void writeFileContent() throws IOException {
        byte[] content = Files.readAllBytes(filePath);
        gridFSConnection.writeFile(filePath,content);

        // do a search on each file and ensure that a file has been created
        GridFSFindIterable find = gridFSBucket.find();
        try(MongoCursor<GridFSFile> cursor = find.cursor()){
            Assert.assertTrue(cursor.hasNext());
        }
    }


    @Test
    public void exist() throws IOException {
        Assert.assertFalse(gridFSConnection.exist(filePath));
        writeFileForTest();
        Assert.assertTrue(gridFSConnection.exist(filePath));
    }

    @Test
    public void delete() throws IOException {
        writeFileForTest();
        gridFSConnection.delete(filePath);
        Assert.assertFalse(gridFSConnection.exist(filePath));
    }

}