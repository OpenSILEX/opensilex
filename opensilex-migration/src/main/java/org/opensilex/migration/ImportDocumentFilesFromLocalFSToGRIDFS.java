/** *****************************************************************************
 * ImportDocumentFilesFromLocalFSToGRIDFS.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 ***************************************************************************** */
package org.opensilex.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.apache.commons.codec.binary.Hex;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.document.dal.DocumentDAO;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.fs.FileStorageConfig;
import org.opensilex.fs.FileStorageModule;

import org.opensilex.fs.service.FileStorageService;
import static org.opensilex.migration.ExportDocumentFilesFromLocalFSToGRIDFS.EXPORT_DOCUMENT_DIRNAME;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Import documents files and metadata from local fs to GRIDFS
 * @author Arnaud Charleroy
 *
 */
public class ImportDocumentFilesFromLocalFSToGRIDFS implements OpenSilexModuleUpdate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDocumentFilesFromLocalFSToGRIDFS.class);

    private OpenSilex opensilex;

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override

    public String getDescription() {
        return "Import documents files and metadata from temp directory to gridfs";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {

        Objects.requireNonNull(opensilex);

        FileStorageService fs = null;

        MongoDBService nosql = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

        try {
            FileStorageConfig moduleConfig = opensilex.getModuleConfig(FileStorageModule.class, FileStorageConfig.class);
            fs = moduleConfig.fs();
        } catch (OpenSilexModuleNotFoundException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

       
        try {
            if (fs == null) {
                LOGGER.error("Can't initialize filesystem");
                System.exit(1);
            }
            fs.startup();
        } catch (Exception ex) {
            LOGGER.error("Can't startup");
            throw new OpensilexModuleUpdateException( ex.getMessage(), ex);
        }
        
        String tmpDirsLocation = System.getProperty("java.io.tmpdir");

        // your directory
        File tmpDirsLocationFile = new File(tmpDirsLocation);
        File[] matchingDirectories = tmpDirsLocationFile.listFiles((File dir, String name) -> name.contains(EXPORT_DOCUMENT_DIRNAME));

        if (matchingDirectories.length > 1) {
            LOGGER.error("Multiple directories found, process will be stopped");
            System.exit(1);
        } else if (matchingDirectories.length == 0) {
            LOGGER.error("No directories found, process will be stopped");
            System.exit(1);
        }

        File exportDocumentsDirPath = matchingDirectories[0];

        Path documentPaths = Paths.get(exportDocumentsDirPath.toString(), "documents");
        Path descriptionPaths = Paths.get(exportDocumentsDirPath.toString(), "description");

        LOGGER.info("Finding document");
        File[] descriptionsFiles = descriptionPaths.toFile().listFiles();
        LOGGER.info("Number of documents :" + descriptionsFiles.length); 
        
        int workingDocument = 0;
        int importedFile = 0;
        int notImported = 0;
        Map<String, URI> notImportedFile = new HashMap<>();

        try {
            for (File descriptionsFile : descriptionsFiles) {
                workingDocument++;
                LOGGER.debug("Work on document n°" + workingDocument + " / " + descriptionsFiles.length);

                ObjectMapper objectMapper = new ObjectMapper();
                DocumentModel documentModel = objectMapper.readValue(descriptionsFile, DocumentModel.class);
                URI documentUri = new URI(SPARQLDeserializers.getExpandedURI(documentModel.getUri()));
                LOGGER.debug("URI document : " + documentUri);

                String encodeHexString = Hex.encodeHexString(documentUri.toString().getBytes(StandardCharsets.UTF_8));
                Path documentFile = Paths.get(documentPaths.toString(), encodeHexString);
                LOGGER.debug("DocumentFile : " + documentFile);
                
                boolean existingFileInFileSystem = Files.exists(documentFile);
                LOGGER.debug("ExistingFileInFileSystem : " + existingFileInFileSystem);
                
                if (existingFileInFileSystem) {
                    try {
                        fs.writeFile(DocumentDAO.FS_DOCUMENT_PREFIX, documentUri, documentFile.toFile());
                        importedFile++;
                        LOGGER.debug("Not Existing document " + documentFile.toString() + " was created");
                    } catch (com.mongodb.MongoWriteException e) {
                        notImported++;
                        LOGGER.debug("Existing document " + documentFile.toString() + " not created", e);
                        notImportedFile.put(documentModel.getTitle(), documentUri);
                    }
                } else {
                    notImported++;
                    notImportedFile.put(documentModel.getTitle(), documentUri);
                    LOGGER.debug("Not Existing document in local file system");
                }

            }
            LOGGER.info("Number of documents files imported => " + importedFile + " \n Number of documents files not imported  - List \n " + notImported +" details : " + notImportedFile.toString());

        } catch (IOException ex) {
            throw new OpensilexModuleUpdateException(ex.getMessage(), ex);
        } catch (URISyntaxException ex) {
            LOGGER.debug(ex.getMessage(), ex);
        }

    }

    /**
     * Delete directory recursively
     * @param directoryToBeDeleted
     * @return 
     */
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
