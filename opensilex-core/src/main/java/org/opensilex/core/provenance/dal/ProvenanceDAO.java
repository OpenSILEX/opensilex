//******************************************************************************
//                          ProvenanceDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Provenance DAO
 * @author Alice Boizet
 */
public class ProvenanceDAO extends MongoReadWriteDao<ProvenanceModel,ProvenanceSearchFilter> {
    
    public static final String PROVENANCE_COLLECTION_NAME = "provenance";
    public static final String PROVENANCE_PREFIX = "provenance";

    public ProvenanceDAO(MongoDBService mongodb) {
        super(mongodb, ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, PROVENANCE_PREFIX);
    }

    @Override
    public List<Bson> getBsonFilters(ProvenanceSearchFilter filter) {
        List<Bson> filters = super.getBsonFilters(filter);

        if (!CollectionUtils.isEmpty(filter.getUris())) {
            filters.add(Filters.in(MongoModel.URI_FIELD, filter.getUris()));
        }
        if(!StringUtils.isEmpty(filter.getName())){
            filters.add(Filters.regex(ProvenanceModel.NAME_FIELD, ".*" + filter.getName() + ".*", "i"));
        }
        if(!StringUtils.isEmpty(filter.getDescription())){
            filters.add(Filters.regex(ProvenanceModel.NAME_FIELD, ".*" + filter.getDescription() + ".*", "i"));
        }

        return filters;
    }

    public Document searchFilter(
            Set<URI> uris,
            String name, 
            String description,
            URI activityType,
            URI activityUri,
            URI agentType, 
            URI agentURI
    ) throws Exception {

        Document filter = new Document();

        if (activityType != null) {
            filter.put("activity.rdfType", activityType);
        }
        
        if (activityUri != null) {
            filter.put("activity.uri", activityUri);
        }
        
        if (agentType != null) {
            filter.put("agents.rdfType", agentType);
        }
        
        if (agentURI != null) {
            filter.put("agents.uri", agentURI);
        }  
        return filter;
    
    }


    public Set<URI> getProvenancesURIsByAgents(List<URI> agents) {
        Document filter = new Document("agents.uri", new Document("$in",agents));
        return nosql.distinct(MongoModel.URI_FIELD, URI.class, PROVENANCE_COLLECTION_NAME, filter);
    }

    @Override
    public ListWithPagination<ProvenanceModel> search(ProvenanceSearchFilter filter) throws MongoException {
        return super.search(filter);
    }
}
