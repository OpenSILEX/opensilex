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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    private GridFSBucket gridFSBucket = null;
    public static String BUCKETS_COLLECTION_NAME = "fs";
    public static String FILES_COLLECTION_NAME = BUCKETS_COLLECTION_NAME + ".files";
    public static String CHUNKS_COLLECTION_NAME = BUCKETS_COLLECTION_NAME + ".chunks";

    public static final int CHUNK_SIZE = 1048576;

    public static Logger LOGGER =  LoggerFactory.getLogger(GridFSConnection.class);
    
    public GridFSConnection(GridFSConfig config) { 
        super(config);
        createFileSystemCollections(config); 

    }

    public GridFSConnection(MongoDBConfig config) { 
        super(config);
        createFileSystemCollections(config); 
    }

    @Override
    public void startup() throws Exception { 
        GridFSConfig implementedConfig = this.getImplementedConfig();
        createFileSystemCollections(implementedConfig);
    }

    private void createFilesIndexes(MongoDatabase database){
        MongoCollection dataCollection = database.getCollection(FILES_COLLECTION_NAME);

        IndexOptions unicityOptions = new IndexOptions().unique(true);
        dataCollection.createIndex(Indexes.ascending("filename"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("metadata.path" ), unicityOptions);
    }
    
    public GridFSConfig getImplementedConfig() {
        return (GridFSConfig) this.getConfig(); 
    }
    
    private void createFileSystemCollections(MongoDBConfig config){

        try(MongoClient mongo = MongoDBService.buildMongoDBClient(config)){
            MongoDatabase database = mongo.getDatabase(config.database());
            gridFSBucket = GridFSBuckets.create(database);
            createFilesIndexes(database);
        }
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {

        Bson query = Filters.eq("metadata.path", filePath.toString());

        GridFSFindIterable find = gridFSBucket.find(query);
        MongoCursor<GridFSFile> cursor = find.cursor();

        GridFSFile gridFSFile = cursor.next();

        ObjectId fileId = gridFSFile.getObjectId();

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


    @Override
    public void writeFile(Path filePath, String content) throws IOException {

        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(CHUNK_SIZE)
                .metadata(new Document("path", filePath.toString()));

        try ( GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(null, options)) {
            uploadStream.write(data);
        } 
    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {

        try ( InputStream streamToUploadFrom = new FileInputStream(file)) {

            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(CHUNK_SIZE)
                    .metadata(new Document("path", filePath.toString()));

            gridFSBucket.uploadFromStream(filePath.getParent().toString(), streamToUploadFrom, options);
        }catch(Exception ex){
            LOGGER.error(ex.getMessage(),ex);
        }
    }

    @Override
    public void createDirectories(Path directoryPath) throws IOException {
        
        GridFSConfig implementedConfig = this.getImplementedConfig();
        createFileSystemCollections(implementedConfig);
    }

    @Override
    public boolean exist(Path filePath) throws IOException {

        Bson query = Filters.eq("metadata.path", filePath.toUri());

        GridFSFindIterable find = gridFSBucket.find(query);
        MongoCursor<GridFSFile> cursor = find.cursor();

        return cursor.hasNext();
    }

    @Override 
    public void delete(Path filePath) throws IOException {

        Bson query = Filters.eq("metadata.path", filePath.toUri());

        GridFSFindIterable find = gridFSBucket.find(query);
        MongoCursor<GridFSFile> cursor = find.cursor();

        GridFSFile gridFSFile = cursor.next();

        gridFSBucket.delete(gridFSFile.getObjectId());

    }

    @Override
    public Path getAbsolutePath(Path filePath) throws IOException { 
       return filePath; 
    }
}
