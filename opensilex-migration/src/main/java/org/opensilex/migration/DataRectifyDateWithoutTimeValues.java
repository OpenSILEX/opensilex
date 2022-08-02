/*******************************************************************************
 *                         DataRectifyDateWithoutTimeValues.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 13/07/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.XSD;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.OpenSilex;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Updates the data entries referring to a variable of type "date" (without time). This is necessary for making the
 * export and display of those data entries work correctly, if the data was inserted on a version older than 1.0.0-rc+5.
 *
 * @author Valentin RIGOLLE
 */
public class DataRectifyDateWithoutTimeValues implements OpenSilexModuleUpdate {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataRectifyDateWithoutTimeValues.class);
    private OpenSilex opensilex;

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return "Updates the data entries where the variable is of type date (without time), so that it is correctly recognized by the system";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        Objects.requireNonNull(opensilex);

        // Get mongo and sparql services
        MongoDBService mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        // Get data collection name
        String dataCollectionName = DataDAO.DATA_COLLECTION_NAME;

        mongodb.startTransaction();

        try {
            // First, search all variables which have the "date" type
            List<String> dateVariableList = sparql.search(VariableModel.class, null, selectBuilder -> {
                Var uriVar = makeVar(VariableModel.URI_FIELD);

                selectBuilder.addWhere(uriVar, Oeso.hasDataType.asNode(), XSD.date.asNode());
            }).stream().map(variableModel -> SPARQLDeserializers.getExpandedURI(variableModel.getUri())).collect(Collectors.toList());

            LOGGER.debug("Variables of type \"date\": {}", dateVariableList);

            long localOffsetMillis = ZoneId.systemDefault()
                    .getRules()
                    .getOffset(Instant.now())
                    .getTotalSeconds() * 1000L;

            MongoDatabase db = mongodb.getDatabase();
            MongoCollection<DataModel> collection = db.getCollection(dataCollectionName, DataModel.class);

            // Creating the BSON filter and aggregation pipeline for the update.
            // The created query will be the following :
            //
            // db.data.updateMany({
            //     $and: [
            //         variable: {$in: [<list of variable URIs>]},
            //         value: {$type: "date"}
            //     ]
            // }, [{
            //     $set: {
            //         "value": {$add: ["$value", 2 * 60 * 60000]}
            //     }
            // }]);
            //
            // The query only updates data entries for which :
            // - the variable is one of the variables with a "date" type
            // - the value is actually a date (to perform the addition)
            // The aggregation pipeline updates the value field, by incrementing it by the local time offset.
            Bson filter = Filters.and(
                    Filters.in(DataModel.VARIABLE_FIELD, dateVariableList),
                    Filters.type(DataModel.VALUE_FIELD, BsonType.DATE_TIME)
            );

            List<Bson> update = Collections.singletonList(
                    new Document()
                            .append("$set", new Document()
                                    .append(DataModel.VALUE_FIELD, new Document("$add", Arrays.asList("$" + DataModel.VALUE_FIELD, localOffsetMillis))))
            );

            // Debug display
            String filterString = JSON.parse(filter.toBsonDocument().toString()).toString();
            String updateString = JSON.parse(update.get(0).toBsonDocument().toString()).toString();
            LOGGER.debug("Filter document:\n{}", filterString);
            LOGGER.debug("Update pipeline:\n{}", updateString);

            // Query execution
            UpdateResult result = collection.updateMany(filter, update);

            LOGGER.debug("Number of matches: {}", result.getMatchedCount());
            LOGGER.debug("Number of updated documents: {}", result.getModifiedCount());

            mongodb.commitTransaction();
        } catch (Exception e1) {
            try {
                mongodb.rollbackTransaction();
            } catch (Exception e2) {
                throw new OpensilexModuleUpdateException(e2.getMessage(), e2);
            }
            throw new OpensilexModuleUpdateException(e1.getMessage(), e1);
        }
    }
}
