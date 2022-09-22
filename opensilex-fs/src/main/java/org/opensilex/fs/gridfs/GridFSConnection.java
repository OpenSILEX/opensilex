//******************************************************************************
// GridFSConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.fs.gridfs;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.*;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * \* Gridfs filesystem class
 *
 * \* @author Arnaud Charleroy
 *
 */
@ServiceDefaultDefinition(config = GridFSConfig.class)

public class GridFSConnection extends BaseService implements FileStorageConnection {

    /**
     * Inner MongoClient used for gridfs connection.
     * Moreover, this client must be closed when the GridFSConnection service is shutdown in order to avoid
     * resource leak
     */
    private MongoClient mongoClient;
    private GridFSBucket gridFSBucket = null;
    private static final String BUCKETS_COLLECTION_NAME = "fs";
    private static final String FILES_COLLECTION_NAME = BUCKETS_COLLECTION_NAME + ".files";
    private static final String METADATA_PATH = "metadata.path";
    private static final String PATH = "path";

    public static final int CHUNK_SIZE = 1048576;

    public static final Logger LOGGER =  LoggerFactory.getLogger(GridFSConnection.class);
    
    public GridFSConnection(GridFSConfig config) { 
        super(config);
    }

    @Override
    public void startup() throws Exception {
        GridFSConfig implementedConfig = this.getImplementedConfig();
        mongoClient = MongoDBService.buildMongoDBClient(implementedConfig);
        createFileSystemCollections(implementedConfig);
    }

    private void createFilesIndexes(MongoDatabase database){
        MongoCollection<?> dataCollection = database.getCollection(FILES_COLLECTION_NAME);

        IndexOptions unicityOptions = new IndexOptions().unique(true);
        dataCollection.createIndex(Indexes.ascending("filename"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending(METADATA_PATH ), unicityOptions);
    }
    
    public GridFSConfig getImplementedConfig() {
        return (GridFSConfig) this.getConfig(); 
    }

    private void createFileSystemCollections(MongoDBConfig config){
        MongoDatabase database = mongoClient.getDatabase(config.database());
        gridFSBucket = GridFSBuckets.create(database);
        createFilesIndexes(database);
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {

        GridFSFindIterable find = findByPath(filePath);

        // check if a file exists
        try(MongoCursor<GridFSFile> cursor = find.cursor()){

            if(!cursor.hasNext()){
                throw new FileNotFoundException(filePath.toAbsolutePath().toString());
            }

            // get description of the file
            GridFSFile gridFSFile = cursor.next();
            ObjectId fileId = gridFSFile.getObjectId();

            // download it
            try ( GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(fileId)) {
                int fileLength = (int) downloadStream.getGridFSFile().getLength();
                int chunkSize = downloadStream.getGridFSFile().getChunkSize();
                byte[] bytesToWriteTo = new byte[fileLength];
                int bytesRead = 0;
                while (bytesRead < fileLength) {
                    bytesRead += downloadStream.read(bytesToWriteTo, bytesRead, chunkSize);
                }

                return bytesToWriteTo;
            }
        }

    }


    @Override
    public void writeFile(Path filePath, String content) throws IOException {

        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        GridFSUploadOptions options = getWriteOptions(filePath);

        try ( GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(filePath.getFileName().toString(), options)) {

            // file content is wrote after GridFSUploadStream.close() method
            uploadStream.write(data);
        } 
    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {

        try ( InputStream streamToUploadFrom = Files.newInputStream(file.toPath())) {
            GridFSUploadOptions options = getWriteOptions(filePath);
            gridFSBucket.uploadFromStream(filePath.getFileName().toString(), streamToUploadFrom, options);
        }
    }

    @Override
    public void createDirectories(Path directoryPath) {
        GridFSConfig implementedConfig = this.getImplementedConfig();
        createFileSystemCollections(implementedConfig);
    }

    /**
     *
     * @param filePath Path to the file to write
     * @return upload options with "metadata.path" set
     *
     * @apiNote This method use the same value for the metadata path as {@link #findByPath(Path)}
     * This ensure that read/write always use the same path identifier
     */
    private GridFSUploadOptions getWriteOptions(Path filePath){
        return new GridFSUploadOptions()
                .chunkSizeBytes(CHUNK_SIZE)
                .metadata(new Document(PATH, filePath.toString()));
    }

    /**
     *
     * @param filePath Path to the file to find
     * @return a {@link GridFSFindIterable} by using an equality filter between
     * {@link #METADATA_PATH} and filePath String representation
     *
     * @apiNote This method use the same value for the metadata path as {@link #getWriteOptions(Path)}
     * This ensure that read/write always use the same path identifier
     */
    private GridFSFindIterable findByPath(Path filePath){
        Bson query = Filters.eq(METADATA_PATH, filePath.toString());
        return gridFSBucket.find(query);
    }

    @Override
    public boolean exist(Path filePath) throws IOException {

        GridFSFindIterable find = findByPath(filePath);

        try(MongoCursor<GridFSFile> cursor = find.cursor()){
            return cursor.hasNext();
        }
    }

    @Override 
    public void delete(Path filePath) throws IOException {

        GridFSFindIterable find = findByPath(filePath);

        try(MongoCursor<GridFSFile> cursor = find.cursor()){
            if(! cursor.hasNext()){
                throw new FileNotFoundException(filePath.toAbsolutePath().toString());
            }
            GridFSFile gridFSFile = cursor.next();
            gridFSBucket.delete(gridFSFile.getObjectId());
        }

    }

    @Override
    public Path getAbsolutePath(Path filePath){
       return filePath; 
    }

    @Override
    public void shutdown() {

        // ensure that the client is well closed
        if(mongoClient != null){
            mongoClient.close();
        }
    }
}
