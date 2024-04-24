package org.opensilex.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import org.apache.jena.atlas.json.JSON;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.OpenSilex;
import org.opensilex.core.device.api.DeviceAPI;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class DeviceAttributeModelRefactorMigration extends DatabaseMigrationModuleUpdate {
    @Override
    public String getDescription() {
        return "Updates any device attribute models who have an attribute field to attributes (with a s)";
    }

    @Override
    protected boolean applyOnMongodb(MongoDBService mongo, MongoDBConfig mongoDBConfig){
        return true;
    }

    @Override
    protected void mongoOperation(MongoDBService mongo, MongoDBConfig mongoDBConfig) {
        // Get device attributes collection name
        String collectionName = DeviceAPI.METADATA_COLLECTION_NAME;
        MongoDatabase db = mongo.getDatabase();
        MongoCollection<MetaDataModel> collection = db.getCollection(collectionName, MetaDataModel.class);

        // Creating the BSON filter and aggregation pipeline for the update.
        // The created query will be the following :
        //
        // db.deviceAttribute.updateMany({
        //     attribute: { $exists: true }
        // }, { $rename: { "attribute": "attributes" } } );
        //
        // Simply rename any documents that have an attribute field to attributes
        Bson filter = Filters.exists("attribute");

        Bson update = new Document().append("$rename", new Document().append("attribute", "attributes"));

        // Query execution
        UpdateResult result = collection.updateMany(filter, update);

    }
}
