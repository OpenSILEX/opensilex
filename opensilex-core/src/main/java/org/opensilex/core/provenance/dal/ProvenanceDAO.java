//******************************************************************************
//                          ProvenanceDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
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
public class ProvenanceDAO {
    
    public static final String PROVENANCE_COLLECTION_NAME = "provenance";
    public static final String PROVENANCE_PREFIX = "provenance";
    protected final MongoDBService nosql; 
    protected final SPARQLService sparql;
    
    public ProvenanceDAO(MongoDBService nosql, SPARQLService sparql) {
        this.nosql = nosql;
        this.sparql = sparql;
    }  

    public ProvenanceModel create(ProvenanceModel provenance) throws Exception {

        nosql.getDatabase().getCollection(PROVENANCE_COLLECTION_NAME).createIndex(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
        nosql.create(provenance, ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, PROVENANCE_PREFIX);
        return provenance;
    }
    
    public ProvenanceModel get(URI uri) throws NoSQLInvalidURIException {
        return nosql.findByURI(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, uri);
    }
    
    public int count(
            Set<URI> uris,
            String name, 
            String description,
            URI activityType,
            URI activityUri,
            URI agentType, 
            URI agentURI
    ) throws Exception {
        
        Document filter = searchFilter(uris, name, description, activityType, activityUri, agentType, agentURI);
        return nosql.count(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, filter );
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
        
        if (uris != null && !uris.isEmpty()) {
            Document inFilter = new Document(); 
            inFilter.put("$in", uris);
            filter.put(MongoModel.URI_FIELD, inFilter);
        }
        
        if (name != null) {
            Document regexFilter = new Document();
            regexFilter.put("$regex", ".*" + Pattern.quote(name) + ".*" );
            // Case ignore
            regexFilter.put("$options", "i" );

            //regexFilter.put("$options", "i");
            filter.put("name", regexFilter);
        }
        
        if (description != null) {
            Document regexFilter = new Document();
            regexFilter.put("$regex", ".*" + Pattern.quote(description) + ".*" );
            // Case ignore
            regexFilter.put("$options", "i" );

            //regexFilter.put("$options", "i");
            filter.put("description", regexFilter);
        }
        
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
    
    public ListWithPagination<ProvenanceModel> search(
            Set<URI> uris,
            String name, 
            String description,
            URI activityType,
            URI activityUri,
            URI agentType, 
            URI agentURI,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize
    ) throws Exception {
        
        Document filter = searchFilter(uris, name, description, activityType, activityUri, agentType, agentURI);
        return nosql.searchWithPagination(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, filter, orderByList, page, pageSize);
    }
    
    public void delete(URI uri) throws NoSQLInvalidURIException {
        nosql.delete(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, uri);
    }
    
    public ProvenanceModel update(ProvenanceModel model) throws NoSQLInvalidURIException {
        nosql.update(model, ProvenanceModel.class, PROVENANCE_COLLECTION_NAME);
        return model;
    }

    public boolean provenanceExists(URI uri){
        return nosql.uriExists(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, uri);
    }
    
    public boolean provenanceListExists(Set<URI> uris) {
        Document filter = new Document(MongoModel.URI_FIELD, new Document("$in",uris));
        Set<URI> foundedURIs = nosql.distinct(MongoModel.URI_FIELD, URI.class, PROVENANCE_COLLECTION_NAME, filter);
        return (foundedURIs.size() == uris.size());
    }
    
    public Set<URI> getExistingProvenanceURIs(Set<URI> uris){
        Document filter = new Document(MongoModel.URI_FIELD, new Document("$in",uris));
        return nosql.distinct(MongoModel.URI_FIELD, URI.class, PROVENANCE_COLLECTION_NAME, filter);
    }
    
    public Set<URI> getNotExistingProvenanceURIs(Set<URI> uris){
        Set<URI> existingURIs = getExistingProvenanceURIs(uris);
        uris.removeAll(existingURIs);
        return uris;
    }
    
    public List<ProvenanceModel> getListByURIs(List<URI> uris) {
        return nosql.findByURIs(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME,uris);
    }
    
    public Set<URI> getProvenancesURIsByAgents(List<URI> agents) {
        Document filter = new Document("agents.uri", new Document("$in",agents));
        return nosql.distinct(MongoModel.URI_FIELD, URI.class, PROVENANCE_COLLECTION_NAME, filter);
    }

}
