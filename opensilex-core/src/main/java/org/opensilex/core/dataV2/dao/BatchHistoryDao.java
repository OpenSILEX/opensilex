package org.opensilex.core.dataV2.dao;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.opensilex.core.dataV2.model.BatchHistoryModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<Bson> getBsonFilters(BatchHistorySearchFilter filter) {
        List<Bson> result = new ArrayList<>();

        if (filter.getBatchId() != null) {
            result.add(Filters.regex(BatchHistoryModel.BATCH_ID_FIELD, filter.getBatchId(), "i"));
        }

        if (filter.getUserName() != null) {
            result.add(Filters.regex(BatchHistoryModel.USERNAME, filter.getUserName(), "i"));
        }
        return result;
    }

}
