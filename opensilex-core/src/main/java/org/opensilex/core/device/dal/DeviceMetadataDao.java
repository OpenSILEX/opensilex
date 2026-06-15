package org.opensilex.core.device.dal;

import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.metadata.MetaDataDaoV2;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

public class DeviceMetadataDao extends MetaDataDaoV2<MetaDataModel> {
    public DeviceMetadataDao(MongoDBServiceV2 mongodb, String collectionName) {
        super(mongodb, MetaDataModel.class, collectionName);
    }

    public DeviceMetadataDao(MongoDBService mongodb, String collectionName) {
        super(mongodb, MetaDataModel.class, collectionName);
    }
}
