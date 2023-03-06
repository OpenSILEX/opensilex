/** *****************************************************************************
 * ExportDocumentFile.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 ***************************************************************************** */
package org.opensilex.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.document.dal.DocumentDAO;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.fs.FileStorageConfig;
import org.opensilex.fs.FileStorageModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.utils.ListWithPagination;

/**
 * Export documents files and metadata from local fs to GRIDFS
 * @author Arnaud Charleroy
 *
 */
public class ExportDocumentFilesFromLocalFSToGRIDFS implements OpenSilexModuleUpdate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportDocumentFilesFromLocalFSToGRIDFS.class);
    public static final String EXPORT_DOCUMENT_DIRNAME = "exportOpenSILEXDocuments";

    private OpenSilex opensilex;

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override

    public String getDescription() {
        return "Export documents files and metadata to temp directory";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {

        Objects.requireNonNull(opensilex);

        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);

        SPARQLService sparql = factory.provide();

        FileStorageService fs = null;

        MongoDBService nosql = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        LOGGER.info("Creating temp dir");
        try {
            FileStorageConfig moduleConfig = opensilex.getModuleConfig(FileStorageModule.class, FileStorageConfig.class);
            fs = moduleConfig.fs();
            fs.startup(); 
        } catch (Exception ex) {
            LOGGER.error("Can't start file system "+ ex.getMessage(), ex);
            throw new OpensilexModuleUpdateException( ex.getMessage(), ex);
        }

        String tmpDirsLocation = System.getProperty("java.io.tmpdir");

        // your directory
        File tmpDirsLocationFile = new File(tmpDirsLocation);
        File[] matchingDirectories = tmpDirsLocationFile.listFiles((File dir, String name) -> name.contains(EXPORT_DOCUMENT_DIRNAME));

        for (File directoryToBeDeleted : matchingDirectories) {
            boolean deleteDirectory = deleteDirectory(directoryToBeDeleted);
            LOGGER.info(directoryToBeDeleted.toPath().toString() + " dir deleted - " + deleteDirectory);
        }

        Path exportDocumentsDirPath = null;
        Path documentPaths = null;
        Path descriptionPaths = null;
        try {
            exportDocumentsDirPath = Files.createTempDirectory(EXPORT_DOCUMENT_DIRNAME).toFile().toPath();
            documentPaths = Paths.get(exportDocumentsDirPath.toString(), "documents");
            descriptionPaths = Paths.get(exportDocumentsDirPath.toString(), "description");

            Files.createDirectories(documentPaths);
            Files.createDirectories(descriptionPaths);

        } catch (IOException ex) {
            LOGGER.error("Can't write temp dir " + ex.getMessage(), ex);
        }

        if (exportDocumentsDirPath == null || documentPaths == null || descriptionPaths == null) {
            LOGGER.error("Can't write temp dirs ");
            System.exit(1);
        } else {
            LOGGER.info(exportDocumentsDirPath.toString() + "temp dir setted");
        }
        
        LOGGER.info("Creating temp dir");

        LOGGER.info(exportDocumentsDirPath.toString() + " created");

        DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);

        try {
            LOGGER.info("Finding document");
            AccountModel accountModel = new AccountModel();
            accountModel.setAdmin(Boolean.TRUE);

            ListWithPagination<DocumentModel> search = documentDAO.search(accountModel, null, null, null, null, null, null, null, null, null, 0, 10000);

            LOGGER.info("Number of documents");

            List<DocumentModel> list = search.getList();

            LOGGER.info("Number of documents :" + list.size());

            int writtenFile = 0;
            int writtenMetadata = 0;
            int workingDocument = 0;

            for (DocumentModel documentModel : list) {
                workingDocument++;

                URI documentUri = new URI(URIDeserializer.getExpandedURI(documentModel.getUri()));

                LOGGER.debug("Work on document n°" + workingDocument + " / " + list.size());

                LOGGER.debug("URI document : " + documentUri);

                String encodeHexString = Hex.encodeHexString(documentUri.toString().getBytes(StandardCharsets.UTF_8));

                ObjectMapper objectMapper = new ObjectMapper();
                Path metadataPath = Paths.get(descriptionPaths.toString(), encodeHexString + ".json");
                objectMapper.writeValue(metadataPath.toFile(), documentModel);

                LOGGER.debug(metadataPath.toString() + "metadata created");
                writtenMetadata++;
                
                if(documentModel.getSource() == null){
                    try{
                        byte[] file = documentDAO.getFile(documentUri);
                        // if file has not been found in file system
                        if(file != null ){
                            File theFile = new File(documentPaths.toFile(), encodeHexString); 
                            theFile.createNewFile();

                            LOGGER.debug(theFile.toPath().toString() + "file created");
                            writtenFile++;
                            FileUtils.writeByteArrayToFile(theFile, file);

                            LOGGER.info("write" + theFile.toPath().toString() + "file");
                        }else{
                            LOGGER.error(documentUri + " has been not found in the file system");
                        }
                    }catch(NotFoundURIException e){
                         LOGGER.error(documentUri + " has been not found in the file system", e);
                    }
                }
               
            }

            LOGGER.info("Number of documents files written => " + writtenFile + " and metadata => " + writtenMetadata);

            LOGGER.info("In directory :" + exportDocumentsDirPath.toString());

        } catch (Exception ex) {

            LOGGER.error("Error during search document", ex);

        }

        LOGGER.info("org.opensilex.migration.ExportDocumentFile.execute()");

    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
