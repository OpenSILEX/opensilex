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
    public List<Bson> getBsonFilters(ProvenanceSearchFilter searchQuery) {
        List<Bson> filters = super.getBsonFilters(searchQuery);

        if (!CollectionUtils.isEmpty(searchQuery.getUris())) {
            filters.add(Filters.in(MongoModel.URI_FIELD, searchQuery.getUris()));
        }
        if(!StringUtils.isEmpty(searchQuery.getName())){
            filters.add(Filters.regex(ProvenanceModel.NAME_FIELD, ".*" + searchQuery.getName() + ".*", "i"));
        }
        if(!StringUtils.isEmpty(searchQuery.getDescription())){
            filters.add(Filters.regex(ProvenanceModel.NAME_FIELD, ".*" + searchQuery.getDescription() + ".*", "i"));
        }
        if(searchQuery.getActivityUri() != null){
            filters.add(Filters.eq(ProvenanceModel.ACTIVITY_FIELD+"."+ActivityModel.URI_FIELD, searchQuery.getActivityUri()));
        }
        if(searchQuery.getActivityType() != null){
            filters.add(Filters.eq(ProvenanceModel.ACTIVITY_FIELD+"."+ActivityModel.TYPE_FIELD, searchQuery.getActivityType()));
        }
        if(! CollectionUtils.isEmpty(searchQuery.getAgents())){
            filters.add(Filters.in(ProvenanceModel.AGENTS_FIELD+"."+ActivityModel.URI_FIELD, searchQuery.getAgents()));
        }
        if(searchQuery.getAgentType() != null){
            filters.add(Filters.eq(ProvenanceModel.AGENTS_FIELD+"."+AgentModel.TYPE_FIELD, searchQuery.getAgentType()));
        }
        return filters;
    }

}
