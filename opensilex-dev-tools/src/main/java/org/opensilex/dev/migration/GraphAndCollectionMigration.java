package org.opensilex.dev.migration;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilex;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.logs.dal.LogsDAO;
import org.opensilex.mobile.dal.FormDAO;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author rcolin
 */
public class GraphAndCollectionMigration implements OpenSilexModuleUpdate {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphAndCollectionMigration.class);

    private OpenSilex opensilex;

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return "Rename SPARQL graph and mongo collections";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public void execute() throws Exception {

        Objects.requireNonNull(opensilex);

        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        SPARQLConfig sparqlConfig = opensilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);

        MongoDBService mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        MongoDBConfig mongoDBConfig = opensilex.loadConfigPath("big-data.mongodb.config", MongoDBConfig.class);

        try {
            sparql.startTransaction();
            mongodb.startTransaction();

            executeSparqlMigration(sparql, sparqlConfig);
            executeMongoMigration(mongodb, mongoDBConfig);

            sparql.commitTransaction();
            mongodb.commitTransaction();
        } catch (Exception e) {
            try {
                sparql.rollbackTransaction();
                mongodb.rollbackTransaction();
                throw e;
            } catch (Exception e2) {
                LOGGER.error("Error on rollback {}", e2.getMessage());
                throw e2;
            }
        }
    }

    private String getTemplatedMoveQuery(SPARQLConfig sparqlConfig) throws IOException {

        // read file which contains templated SPARQL query
        String filePath = Paths.get("migration", "sparql_graph_rename_template.rq").toString();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);

        if (inputStream == null) {
            throw new IllegalArgumentException("SPARQL query file not found! " + filePath);
        }

        // read file content and replace template by config baseURI
        String renameQuery = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        if (renameQuery.isEmpty()) {
            throw new IllegalArgumentException("Empty SPARQL query file " + filePath);
        }

        if (StringUtils.isEmpty(sparqlConfig.baseURI())) {
            throw new IllegalArgumentException("Empty baseURI property for sparql config");
        }

        String baseUri;
        if (sparqlConfig.baseURI().endsWith("/")) {
            // remove last character
            baseUri = StringUtils.chop(sparqlConfig.baseURI());
        } else {
            baseUri = sparqlConfig.baseURI();
        }

        return renameQuery.replace("__OPENSILEX_BASE_URI_TO_REPLACE__", baseUri);
    }

    private void executeSparqlMigration(SPARQLService sparql, SPARQLConfig sparqlConfig) throws IOException, SPARQLException {

        String templatedMoveQuery = getTemplatedMoveQuery(sparqlConfig);
        LOGGER.debug("Executing SPARQL UPDATE query : {}{}", System.getProperty("line.separator"), templatedMoveQuery);

        sparql.executeUpdateQuery(templatedMoveQuery);
        LOGGER.debug("SPARQL graph migration [OK]");
    }

    private void executeMongoMigration(MongoDBService mongodb, MongoDBConfig mongoDBConfig) {

        String database = mongoDBConfig.database();
        if (StringUtils.isEmpty(database)) {
            throw new IllegalArgumentException("Empty database property for mongodb config");
        }

        // build the Map of old -> new collection names (germplasm/device attributes), log and form
        Map<String, String> oldToNewCollectionNames = new HashMap<>();
        oldToNewCollectionNames.put("germplasmAttributes", GermplasmDAO.ATTRIBUTES_COLLECTION_NAME);
        oldToNewCollectionNames.put("devicesAttributes", DeviceDAO.ATTRIBUTES_COLLECTION_NAME);
        oldToNewCollectionNames.put("forms", FormDAO.FORM_COLLECTION_NAME);
        oldToNewCollectionNames.put("logs", LogsDAO.LOGS_COLLECTION_NAME);
        oldToNewCollectionNames.put("Moves", MoveEventDAO.MOVE_COLLECTION_NAME);

        /* iterate over mongodb collections in order to ensure to rename only old collection which already exists.
         Unfortunately there isn't a better way to do it efficiently since 3.0 MongoDB Driver
         (https://stackoverflow.com/questions/31909247/mongodb-3-java-check-if-collection-exists) */

        for (String collectionName : mongodb.getDatabase().listCollectionNames()) {

            if (oldToNewCollectionNames.containsKey(collectionName)) {
                String newName = oldToNewCollectionNames.get(collectionName);

                MongoNamespace newNameSpace = new MongoNamespace(mongodb.getDatabase().getName(), newName);
                MongoCollection<?> oldCollection = mongodb.getDatabase().getCollection(collectionName);

                oldCollection.renameCollection(newNameSpace);
                LOGGER.debug("[{}] Mongo collection migration : {} -> {} [OK]", database, collectionName, newName);
            }
        }
    }

}
