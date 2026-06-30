package org.opensilex.migration.one_point_five_ALL;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.opensilex.OpenSilex;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmMetadataModel;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

public class GermplasmAttributeUpdateRightsMigration implements OpenSilexModuleUpdate {
    private OpenSilex opensilex;
    private final Logger logger;

    public GermplasmAttributeUpdateRightsMigration() {
        this(LoggerFactory.getLogger(GermplasmAttributeUpdateRightsMigration.class));
    }

    public GermplasmAttributeUpdateRightsMigration(Logger logger) {
        this.logger = logger;
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return "Add germplasm rights information to germplasm attribute collection (publisher, isPublic, groups)";
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        var factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        var sparql = factory.provide();
        var mongo = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

        try {
            new SparqlMongoTransaction(sparql, mongo.getServiceV2()).execute(session -> {
                executeWithSession(sparql, mongo.getServiceV2(), session);
                return null;
            });
        } catch (Exception e) {
            logger.error("Error during germplasm attribute rights migration", e);
            throw new OpensilexModuleUpdateException(this, e);
        }
    }

    public void executeWithSession(SPARQLService sparql, MongoDBServiceV2 mongo, ClientSession session) throws Exception {
        var attributeCollection = mongo.getDatabase().getCollection(GermplasmDAO.ATTRIBUTES_COLLECTION_NAME);

        var uris = attributeCollection.distinct(GermplasmMetadataModel.URI_FIELD, String.class)
                .map(URI::create)
                .into(new ArrayList<>());
        logger.info("Retrieved " + uris.size() + " germplasm attribute documents to update");
        List<GermplasmModel> germplasms = sparql.getListByURIs(GermplasmModel.class, uris, null);

        logger.debug("Sleeping to avoid stressing RDF4J...");
        Thread.sleep(10000);
        List<WriteModel<Document>> ops = new ArrayList<>();

        for (GermplasmModel g : germplasms) {
            var uri = SPARQLDeserializers.getExpandedURI(g.getUri());

            var update = new Document();
            update.put(GermplasmMetadataModel.IS_PUBLIC_FIELD,
                    Optional.ofNullable(g.getIsPublic()).orElse(true));

            update.put(GermplasmMetadataModel.GROUPS_FIELD,
                    g.getGroups().stream()
                            .map(gr -> SPARQLDeserializers.getExpandedURI(gr.getUri()))
                            .toList()
            );

            if (g.getPublisher() != null) {
                update.put(GermplasmMetadataModel.PUBLISHER_FIELD,
                        SPARQLDeserializers.getExpandedURI(g.getPublisher()));
            }

            ops.add(new UpdateOneModel<>(
                    Filters.eq(GermplasmMetadataModel.URI_FIELD, uri),
                    Updates.combine(new Document("$set", update))
            ));
        }

        attributeCollection.bulkWrite(session, ops);
        //TODO ive left the old way in case you say bulk write is garbage for some reason
        /*for (var i = 0; i < germplasms.size(); i += 1) {
            var germplasm = germplasms.get(i);
            var uri = SPARQLDeserializers.getExpandedURI(germplasm.getUri());
            var updateDocument = new Document(Map.of(
                    GermplasmMetadataModel.IS_PUBLIC_FIELD, Optional.ofNullable(germplasm.getIsPublic()).orElse(true),
                    GermplasmMetadataModel.GROUPS_FIELD, germplasm.getGroups().stream().map(group -> SPARQLDeserializers.getExpandedURI(group.getUri())).toList()
            ));
            if (germplasm.getPublisher() != null) {
                updateDocument.put(GermplasmMetadataModel.PUBLISHER_FIELD, SPARQLDeserializers.getExpandedURI(germplasm.getPublisher()));
            }
            logger.info("Updating " + uri + " with values " + updateDocument.toJson());
            logger.debug(String.format("Progress : %s / %s (%.1f %%)", i, germplasms.size(), 100.f * (float) i / (float) germplasms.size()));
            attributeCollection.updateMany(
                    session,
                    new Document(GermplasmMetadataModel.URI_FIELD, uri),
                    new Document("$set", updateDocument)
            );
        }*/
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }
}
