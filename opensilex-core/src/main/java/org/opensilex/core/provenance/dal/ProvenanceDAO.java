//******************************************************************************
//                          ProvenanceDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.mongodb.client.MongoCollection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.query.BooleanExpression;
import javax.naming.NamingException;
import org.bson.Document;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.nosql.exceptions.NoSQLBadPersistenceManagerException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.utils.ListWithPagination;
        
/**
 * Provenance DAO
 * @author Alice Boizet
 */
public class ProvenanceDAO {
    
   protected final MongoDBService nosql; 
    
    public ProvenanceDAO(MongoDBService nosql) {
        this.nosql = nosql;
    }  

    public ProvenanceModel create(ProvenanceModel provenance) throws NamingException, URISyntaxException, Exception {
        ProvenanceModel prov = nosql.create(provenance, ProvenanceModel.class, "provenance");
        return prov;
    }
    
    public ProvenanceModel get(URI uri) throws NamingException, IOException {
        ProvenanceModel provenance = nosql.findByURI(ProvenanceModel.class, "provenance", uri);
        return provenance;
    }
    
    public ListWithPagination<ProvenanceModel> search(
            String label, 
            URI experiment, 
            URI activityType, 
            URI agentType, 
            URI agentURI,
            Integer page,
            Integer pageSize
    ) throws NamingException, IOException, Exception {
        
        Document filter = new Document();
        if (label != null) {
            filter.put("label", label);
        }
        
        if (experiment != null) {
            filter.put("experiments", experiment);
        }

        if (activityType != null) {
            filter.put("activity.type", activityType);
        }
        
        if (agentType != null) {
            filter.put("agents.type", agentType);
        }
        
        if (agentURI != null) {
            filter.put("agents.uri", agentURI);
        }      
        
        ListWithPagination<ProvenanceModel> provenances = nosql.searchWithPagination(ProvenanceModel.class, "provenance", filter, page, pageSize);
        return provenances;        
           
    }
    
    
//
//    public boolean provenanceExists(URI uri) throws NamingException, IOException{  
//        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
//            try (JDOQLTypedQuery<ProvenanceModel> tq = persistenceManager.newJDOQLTypedQuery(ProvenanceModel.class)) {
//                QProvenanceModel cand = QProvenanceModel.candidate();
//                BooleanExpression expr = null;
//                expr = cand.uri.eq(uri);
//                Object res = tq.filter(expr).executeUnique();
//                
//                return res != null;
//            }
//        }
//    }
//    
//    public boolean provenanceListExists(Collection<String> uriList) throws NamingException, IOException{  
//        //if at least one provenance doesn't exist, return false;
//        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
//            Query q = persistenceManager.newQuery(ProvenanceModel.class);
//            q.setFilter(" :uris.contains(uri)");
//            q.setParameters(uriList);
//            List<ProvenanceModel> results = q.executeList();
//            return results.size() == uriList.size();            
//        }
//    }
//
    
//        
//    public void prepareURI(ProvenanceModel model) throws Exception{
//        String[] URICompose = new String[1];
//        URICompose[0] = model.getLabel();
//        model.setURICompose(URICompose);
//    }
//
//    public void delete(URI uri) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException {
//        nosql.delete(new ProvenanceModel(), uri);
//    }
//
//    public ProvenanceModel update(ProvenanceModel model) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException {
//        nosql.update(model);
//        return model;
//    }
//

}
