package org.opensilex.core.germplasm.dal;

import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.metadata.MetaDataDaoV2;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

public class GermplasmMetadataDAO extends MetaDataDaoV2<GermplasmMetadataModel> {
    public GermplasmMetadataDAO(MongoDBServiceV2 mongodb, String collectionName) {
        super(mongodb, GermplasmMetadataModel.class, collectionName);
    }

    public GermplasmMetadataDAO(MongoDBService mongodb, String collectionName) {
        super(mongodb, GermplasmMetadataModel.class, collectionName);
    }
}
