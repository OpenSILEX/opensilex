//******************************************************************************
//                          ProvenanceDaoV2.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Maximilian Hart
 */
public class ProvenanceDaoV2 extends MongoReadWriteDao<ProvenanceModel, ProvenanceSearchFilter> {

    public static final String PROVENANCE_COLLECTION_NAME = "provenance";
    public static final String PROVENANCE_PREFIX = "provenance";

    public ProvenanceDaoV2(MongoDBServiceV2 mongodb) {
        super(mongodb, ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, PROVENANCE_PREFIX);
    }

    public static Map<Bson, IndexOptions> getIndexes() {

        Map<Bson, IndexOptions> indexes = new HashMap<>();
        indexes.put(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
        indexes.put(Indexes.ascending(ProvenanceModel.NAME_FIELD), null);
        indexes.put(Indexes.ascending(ProvenanceModel.ACTIVITY_TYPE_FIELD), null);
        indexes.put(Indexes.ascending(ProvenanceModel.AGENTS_TYPE_FIELD), null);
        return indexes;
    }

    @Override
    public List<Bson> getBsonFilters(ProvenanceSearchFilter filter) {
        List<Bson> result = new ArrayList<>();

        if (filter.getName() != null) {
            result.add(Filters.regex(ProvenanceModel.NAME_FIELD, filter.getName(), "i"));
        }

        if (filter.getDescription() != null) {
            result.add(Filters.regex(ProvenanceModel.DESCRIPTION_FIELD, filter.getDescription(), "i"));
        }

        if (filter.getActivityType() != null) {
            result.add(Filters.eq(ProvenanceModel.ACTIVITY_TYPE_FIELD, filter.getActivityType()));
        }

        if (filter.getActivityUri() != null) {
            result.add(Filters.eq(ProvenanceModel.ACTIVITY_URI_FIELD, filter.getActivityUri()));
        }

        if (filter.getAgentType() != null) {
            result.add(Filters.eq(ProvenanceModel.AGENTS_TYPE_FIELD, filter.getAgentType()));
        }

        if (filter.getAgentURIs() != null && !filter.getAgentURIs().isEmpty()) {
            result.add(Filters.in(ProvenanceModel.AGENTS_URI_FIELD, filter.getAgentURIs().stream().map(SPARQLDeserializers::getExpandedURI).toList()));
        }

        return result;
    }

}
