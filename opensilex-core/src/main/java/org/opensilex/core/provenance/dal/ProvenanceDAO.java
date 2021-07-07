//******************************************************************************
//                          ProvenanceDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.naming.NamingException;
import org.bson.Document;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.nosql.exceptions.NoSQLBadPersistenceManagerException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
        
/**
 * Provenance DAO
 * @author Alice Boizet
 */
public class ProvenanceDAO {
    
    public static final String PROVENANCE_COLLECTION_NAME = "provenance";
    public static final String PROVENANCE_PREFIX = "id/provenance";
    protected final MongoDBService nosql; 
    protected final SPARQLService sparql;
    
    public ProvenanceDAO(MongoDBService nosql, SPARQLService sparql) {
        this.nosql = nosql;
        this.sparql = sparql;
    }  

    public ProvenanceModel create(ProvenanceModel provenance) throws Exception {

        nosql.getDatabase().getCollection(PROVENANCE_COLLECTION_NAME).createIndex(Indexes.ascending("uri"), new IndexOptions().unique(true));
        nosql.create(provenance, ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, PROVENANCE_PREFIX);
        return provenance;
    }
    
    public ProvenanceModel get(URI uri) throws NoSQLInvalidURIException {
        ProvenanceModel provenance = nosql.findByURI(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, uri);
        return provenance;
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
    ) throws NamingException, IOException, Exception {
        
        Document filter = new Document();
        
        if (uris != null && !uris.isEmpty()) {
            Document inFilter = new Document(); 
            inFilter.put("$in", uris);
            filter.put("uri", inFilter);
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
        
        ListWithPagination<ProvenanceModel> provenances = nosql.searchWithPagination(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, filter, orderByList, page, pageSize);
        return provenances;        
           
    }
    
    public void delete(URI uri) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException {
        nosql.delete(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, uri);
    }
    
    public ProvenanceModel update(ProvenanceModel model) throws NoSQLInvalidURIException {
        nosql.update(model, ProvenanceModel.class, PROVENANCE_COLLECTION_NAME);
        return model;
    }

    public boolean provenanceExists(URI uri) throws NamingException, IOException{  
        return nosql.uriExists(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME, uri);
    }
    
    public boolean provenanceListExists(Set<URI> uris) throws NamingException, IOException {  
        Document filter = new Document("uri", new Document("$in",uris));
        Set foundedURIs = nosql.distinct("uri", URI.class, PROVENANCE_COLLECTION_NAME, filter);
        return (foundedURIs.size() == uris.size());
    }
    
    public Set<URI> getExistingProvenanceURIs(Set<URI> uris) throws NamingException, IOException {  
        Document filter = new Document("uri", new Document("$in",uris));
        Set foundedURIs = nosql.distinct("uri", URI.class, PROVENANCE_COLLECTION_NAME, filter);
        return foundedURIs;
    }
    
    public Set<URI> getNotExistingProvenanceURIs(Set<URI> uris) throws NamingException, IOException {
        Set<URI> existingURIs = getExistingProvenanceURIs(uris);
        uris.removeAll(existingURIs);
        return uris;
    }
    
    public List<ProvenanceModel> getListByURIs(List<URI> uris) throws NamingException, IOException, NoSQLInvalidUriListException{  
        List<ProvenanceModel> findByURIs = nosql.findByURIs(ProvenanceModel.class, PROVENANCE_COLLECTION_NAME,uris);
        return findByURIs;
    }
    
    public Set<URI> getProvenancesURIsByAgents(List<URI> agents) {
        Document filter = new Document("agents.uri", new Document("$in",agents));
        Set uris = nosql.distinct("uri", URI.class, PROVENANCE_COLLECTION_NAME, filter);
        return uris;
    }
    
    public ListWithPagination<ProvenanceModel> getProvenancesByDevice(UserModel user, URI uri, String label, List<OrderBy> orderByList, int page, int pageSize, String collectionName) throws Exception {
        DataDAO dataDAO = new DataDAO(nosql, null, null);
        Document filter = dataDAO.searchByDeviceFilter(uri, user, null, null, null, null, null, null, null, null, null);
        Set<URI> provenancesURIs = nosql.distinct("provenance.uri", URI.class, collectionName, filter);
        
        return search(provenancesURIs, label, collectionName, uri, uri, uri, uri, orderByList, Integer.SIZE, Integer.SIZE);
    }
}
