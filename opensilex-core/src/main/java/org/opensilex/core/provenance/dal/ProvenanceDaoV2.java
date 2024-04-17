package org.opensilex.core.provenance.dal;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProvenanceDaoV2 extends MongoReadWriteDao<ProvenanceModel, ProvenanceSearchFilter> {

    public static final String PROVENANCE_COLLECTION_NAME = "provenance";
    public static final String PROVENANCE_PREFIX = "provenance";

    public ProvenanceDaoV2(MongoDBServiceV2 mongodb) {
        super(mongodb, ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, PROVENANCE_PREFIX);
    }

    @Override
    public List<Bson> getBsonFilters(ProvenanceSearchFilter filter) {
        List<Bson> result = new ArrayList<>();

        if (filter.name != null) {
            Document regexFilter = new Document();
            regexFilter.put("$regex", ".*" + Pattern.quote(filter.name) + ".*" );
            // Case ignore
            regexFilter.put("$options", "i" );

            result.add(new Document("name", regexFilter));
        }

        if (filter.description != null) {
            Document regexFilter = new Document();
            regexFilter.put("$regex", ".*" + Pattern.quote(filter.description) + ".*" );
            // Case ignore
            regexFilter.put("$options", "i" );

            result.add(new Document("description", regexFilter));
        }

        if (filter.activityType != null) {
            result.add(new Document("activity.rdfType", filter.activityType));
        }

        if (filter.activityUri != null) {
            result.add(new Document("activity.uri", filter.activityUri));
        }

        if (filter.agentType != null) {
            result.add(new Document("agents.rdfType", filter.agentType));
        }

        if (filter.agentURI != null) {
            result.add(new Document("agents.uri", filter.agentURI));
        }
        return result;
    }

}
