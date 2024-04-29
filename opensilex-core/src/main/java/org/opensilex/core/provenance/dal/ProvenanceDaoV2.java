package org.opensilex.core.provenance.dal;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.opensilex.core.data.dal.DataProvenanceModel.*;

public class ProvenanceDaoV2 extends MongoReadWriteDao<ProvenanceModel, ProvenanceSearchFilter> {

    public static final String PROVENANCE_COLLECTION_NAME = "provenance";
    public static final String PROVENANCE_PREFIX = "provenance";

    public ProvenanceDaoV2(MongoDBServiceV2 mongodb) {
        super(mongodb, ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, PROVENANCE_PREFIX);
    }

    public static Map<Bson, IndexOptions> getIndexes() {

        Map<Bson, IndexOptions> indexes = new HashMap<>();
        indexes.put(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
        return indexes;
    }

    @Override
    public List<Bson> getBsonFilters(ProvenanceSearchFilter filter) {
        List<Bson> result = new ArrayList<>();

        if (filter.getName() != null) {
            Document regexFilter = new Document();
            regexFilter.put("$regex", ".*" + Pattern.quote(filter.getName()) + ".*" );
            // Case ignore
            regexFilter.put("$options", "i" );

            result.add(new Document("name", regexFilter));
        }

        if (filter.getDescription() != null) {
            Document regexFilter = new Document();
            regexFilter.put("$regex", ".*" + Pattern.quote(filter.getDescription()) + ".*" );
            // Case ignore
            regexFilter.put("$options", "i" );

            result.add(new Document("description", regexFilter));
        }

        if (filter.getActivityType() != null) {
            result.add(new Document("activity.rdfType", filter.getActivityType()));
        }

        if (filter.getActivityUri() != null) {
            result.add(new Document("activity.uri", filter.getActivityUri()));
        }

        if (filter.getAgentType() != null) {
            result.add(new Document("agents.rdfType", filter.getAgentType()));
        }

        if (filter.getAgentURI() != null) {
            result.add(new Document("agents.uri", filter.getAgentURI()));
        }
        return result;
    }

}
