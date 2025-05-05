package org.opensilex.core.data.dal.batchHistory;

import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BatchHistoryDao used to handle {@link BatchHistoryModel}
 *
 * @author MKOURDI
 */
public class BatchHistoryDao extends MongoReadWriteDao<BatchHistoryModel, BatchHistorySearchFilter> {

    public static final String BATCH_HISTORY_COLLECTION_NAME = "batchHistory";
    public static final String BATCH_HISTORY_PREFIX = "batchHistory";
    public static final String CASE_INSENSITIVE_OPTION = "i";

    public BatchHistoryDao(MongoDBServiceV2 mongodb) {
        super(mongodb, BatchHistoryModel.class, BATCH_HISTORY_COLLECTION_NAME, BATCH_HISTORY_PREFIX);
    }

    @Override
    public List<Bson> getBsonFilters(BatchHistorySearchFilter filter) {
        List<Bson> result = super.getBsonFilters(filter);

        if (filter.getStartDate() != null) {
            result.add(Filters.gte(BatchHistoryModel.PUBLICATION_DATE_FIELD, filter.getStartDate()));
        }

        if (filter.getEndDate() != null) {
            result.add(Filters.lte(BatchHistoryModel.PUBLICATION_DATE_FIELD, filter.getEndDate()));
        }

        return result;
    }

}
