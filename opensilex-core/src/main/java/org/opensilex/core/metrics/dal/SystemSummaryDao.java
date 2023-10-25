package org.opensilex.core.metrics.dal;

import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;

public class SystemSummaryDao extends MongoReadWriteDao<SystemSummaryModel, SystemSummarySearchFilter> {

    public SystemSummaryDao(MongoDBService mongodb) {
        super(mongodb, SystemSummaryModel.class, "metrics", "system");
    }

}
