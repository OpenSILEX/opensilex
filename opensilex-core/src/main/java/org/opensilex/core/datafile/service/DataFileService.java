/*
 *  *************************************************************************************
 *  DataFileService.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2023
 * Contact :  user@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.core.datafile.service;

import com.mongodb.client.ClientSession;
import org.opensilex.core.datafile.dal.DataFileDAO;
import org.opensilex.core.datafile.dal.DataFileModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Base64;

public class DataFileService {

    private final MongoDBService mongodb;
    private final FileStorageService fs;
    private final DataFileDAO dataFileDAO;

    public DataFileService(MongoDBService mongodb, SPARQLService sparql, FileStorageService fs) {
        this.mongodb = mongodb;
        dataFileDAO = new DataFileDAO(mongodb, sparql);
        this.fs = fs;
    }

    public void insertFile(DataFileModel model, File file) throws URISyntaxException, IOException {

        mongodb.generateUniqueUriIfNullOrValidateCurrent(model, true, dataFileDAO.getCreatePrefix(), dataFileDAO.getCollection());

        final String filename = Base64.getEncoder().encodeToString(model.getUri().toString().getBytes());
        java.nio.file.Path filePath = Paths.get(dataFileDAO.getCreatePrefix(), filename);
        model.setPath(filePath.toString());

        ClientSession mongoSession = mongodb.newSession();
        try {
            mongoSession.startTransaction();
            dataFileDAO.create(model, mongoSession);
            fs.writeFile(dataFileDAO.getCreatePrefix(), filePath, file);
            mongoSession.commitTransaction();
        } catch (Exception e) {
            mongoSession.abortTransaction();
            fs.deleteIfExists(dataFileDAO.getCreatePrefix(), filePath);
            throw e;
        }


    }

}
