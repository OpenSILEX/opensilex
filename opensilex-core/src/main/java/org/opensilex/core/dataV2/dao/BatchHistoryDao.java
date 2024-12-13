package org.opensilex.core.dataV2.dao;

import org.opensilex.core.dataV2.model.BatchHistoryModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

/**
 * BatchHistoryDao used to handle {@link BatchHistoryModel}
 *
 * @author MKOURDI
 */
public class BatchHistoryDao extends MongoReadWriteDao<BatchHistoryModel, BatchHistorySearchFilter> {

    public static final String BATCH_HISTORY_COLLECTION_NAME = "batchHistory";
    public static final String BATCH_HISTORY_PREFIX = "batchHistory";

    public BatchHistoryDao(MongoDBServiceV2 mongodb) {
        super(mongodb, BatchHistoryModel.class, BATCH_HISTORY_COLLECTION_NAME, BATCH_HISTORY_PREFIX);
    }

}
