package org.opensilex.core.provenance.dal;

import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

public class ProvenanceDaoV2 extends MongoReadWriteDao<ProvenanceModel, MongoSearchFilter> {
    //TODO

    public ProvenanceDaoV2(MongoDBServiceV2 mongodb, Class<ProvenanceModel> modelClass, String collectionName, String createUriPath) {
        super(mongodb, modelClass, collectionName, createUriPath);
    }
}
