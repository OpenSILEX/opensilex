package org.opensilex.core.metrics.dal;

import com.mongodb.client.model.Filters;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;

import java.util.List;

public class ExperimentSummaryDao extends MongoReadWriteDao<ExperimentSummaryModel, ExperimentSummarySearchFilter> {

    public ExperimentSummaryDao(MongoDBService mongodb) {
        super(mongodb, ExperimentSummaryModel.class, "metrics", "experiment");
    }

    @Override
    public List<Bson> getBsonFilters(ExperimentSummarySearchFilter searchQuery) {
        List<Bson> filters =  super.getBsonFilters(searchQuery);

        if (!CollectionUtils.isEmpty(searchQuery.getExperiments())) {
            filters.add(Filters.in(ExperimentSummaryModel.EXPERIMENT_FIELD, searchQuery.getExperiments()));
        }
        if (searchQuery.getStart() != null) {
            filters.add(Filters.gte(ExperimentSummaryModel.CREATION_DATE_FIELD, searchQuery.getStart()));
        }
        if (searchQuery.getEnd() != null) {
            filters.add(Filters.lt(ExperimentSummaryModel.CREATION_DATE_FIELD, searchQuery.getEnd()));
        }

        return filters;
    }
}
