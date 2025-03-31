package org.opensilex.core.data.bll;

import org.apache.tika.Tika;
import org.opensilex.core.data.dal.DataFileDaoV2;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.fs.source.FileSource;
import org.opensilex.fs.operation.checksum.FileCheckSumOperation;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.opensilex.core.data.dal.DataFileDaoV2.FS_FILE_PREFIX;

public class DataFileLogic {

    private final DataFileDaoV2 dao;
    private final AccountModel user;
    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;

    public DataFileLogic(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user) {
        this.dao = new DataFileDaoV2(nosql, sparql);
        this.sparql = sparql;
        this.nosql = nosql;
        this.user = user;
        this.fs = fs;
    }

//    public void index(@NotNull DataFileModel model) throws Exception {
//        // Determine the full path
//        // Case1 : Full path -> use this one
//        // Case2: Resolve path according fs connection rootDirectory
//        Path filePath = Paths.get(model.getRecord().getPath());
//
//        // try to index this file from the absolute path
//        if(! filePath.isAbsolute()) {
//            filePath = fs.getAbsolutePath(FS_FILE_PREFIX, filePath);
//        }
//
//        if(! fs.exist(FS_FILE_PREFIX, filePath)){
//            throw new FileNotFoundException(filePath.toString());
//        }
//    }

    public void upload(@NotNull DataFileModel model, @NotNull File file) throws Exception {
        model.setPublisher(user.getUri());

        nosql.generateUniqueUriIfNullOrValidateCurrent(model, true, DataFileDaoV2.FILE_PREFIX, DataFileDaoV2.COLLECTION_NAME);

        // determine data-connection from config
        var connection = fs.getConnection(FS_FILE_PREFIX);
        FileSource fileSource = connection.getFileSource();

        // set size, hash and type
        byte[] hash = new FileCheckSumOperation<>("SHA3-512", connection)
                .execute(file.toPath());

        final String filename = Base64.getEncoder().encodeToString(model.getUri().toString().getBytes());
        Path filePath = Paths.get(FS_FILE_PREFIX, filename);

        String type = new Tika().detect(file);

        // create file entry
        model.getRecord()
            .setId(model.getUri().toString())
            .setPath(filePath.toString())
            .setFileSource(fileSource)
            .setHash(new String(hash))
            .setType(type);

        try{
            nosql.getServiceV2().withSession(session -> {
                dao.create(session, model);
                fs.writeFile(FS_FILE_PREFIX, filePath, file);
            });
        } catch(Exception e){
            fs.deleteIfExists(FS_FILE_PREFIX, filePath);
            throw e;
        }
    }
}
