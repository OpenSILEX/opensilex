//******************************************************************************
//                          GridFSConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.fs.gridfs;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 * Gridfs filesystem class
 *
 * @author Arnaud Charleroy
 */
@ServiceDefaultDefinition(config = GridFSConfig.class)
public class GridFSConnection extends BaseService implements FileStorageConnection {

    private GridFSBucket gridFSBucket = null;

    public GridFSConnection(GridFSConfig config) {
        super(config);
    }

    public GridFSConnection() {
        super(null);
    }

    @Override
    public void startup() throws Exception {
        GridFSConfig implementedConfig = this.getImplementedConfig();
        MongoClient mongo = MongoDBService.getMongoDBClient(implementedConfig);
        MongoDatabase database = mongo.getDatabase(implementedConfig.database());
        gridFSBucket = GridFSBuckets.create(database);
    }

    public GridFSConfig getImplementedConfig() {
        return (GridFSConfig) this.getConfig();
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
            byte[] bytesToWriteTo = new byte[fileLength];
            downloadStream.read(bytesToWriteTo);
            return bytesToWriteTo;
        }

    }

    @Override
    public String readFile(Path filePath) throws IOException {
        Bson query = Filters.eq("metadata.path", filePath.toString());
        GridFSFindIterable find = gridFSBucket.find(query);
        MongoCursor<GridFSFile> cursor = find.cursor();
        GridFSFile gridFSFile = cursor.next();
        ObjectId fileId = gridFSFile.getObjectId();
        try ( GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(fileId)) {
            int fileLength = (int) downloadStream.getGridFSFile().getLength();
            byte[] bytesToWriteTo = new byte[fileLength];
            downloadStream.read(bytesToWriteTo);
            return new String(bytesToWriteTo, StandardCharsets.UTF_8);
        }
    }

    @Override
    public void writeFile(Path filePath, String content, URI fileURI) throws IOException {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(1048576)
                .metadata(new Document("path", filePath.toString()));
        try ( GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(fileURI.toString(), options)) {
            uploadStream.write(data);
        }
    }

    @Override
    public void writeFile(Path filePath, File file, URI fileURI) throws IOException {
        try ( InputStream streamToUploadFrom = new FileInputStream(file)) {
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(1048576)
                    .metadata(new Document("path", filePath.toString()));
            gridFSBucket.uploadFromStream(fileURI.toString(), streamToUploadFrom, options);
        }
    }

    @Override
    public void createDirectories(Path directoryPath) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
