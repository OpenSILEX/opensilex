//******************************************************************************
//                          ProvenanceDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

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
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.nosql.exceptions.NoSQLBadPersistenceManagerException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.utils.ListWithPagination;
        
/**
 * Provenance DAO
 * @author Alice Boizet
 */
public class ProvenanceDAO {
    
    protected final DataNucleusService nosql;    
    
    public ProvenanceDAO(DataNucleusService nosql) {
        this.nosql = nosql;
    }  

    public ProvenanceModel create(ProvenanceModel provenance) throws NamingException, URISyntaxException, Exception {
        nosql.prepareInstanceCreation(provenance);
        return provenance;
    }

    public boolean provenanceExists(URI uri) throws NamingException, IOException{  
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            try (JDOQLTypedQuery<ProvenanceModel> tq = persistenceManager.newJDOQLTypedQuery(ProvenanceModel.class)) {
                QProvenanceModel cand = QProvenanceModel.candidate();
                BooleanExpression expr = null;
                expr = cand.uri.eq(uri);
                Object res = tq.filter(expr).executeUnique();
                
                return res != null;
            }
        }
    }
    
    public boolean provenanceListExists(Collection<String> uriList) throws NamingException, IOException{  
        //if at least one provenance doesn't exist, return false;
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            Query q = persistenceManager.newQuery(ProvenanceModel.class);
            q.setFilter(" :uris.contains(uri)");
            q.setParameters(uriList);
            List<ProvenanceModel> results = q.executeList();
            return results.size() == uriList.size();            
        }
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
        
        //prepare filter
         String filter = "";
            if (label != null) {
                filter = filter + "label == '" + label + "' && "; 
            }

            if (agentURI != null) {
                filter = filter + "agents.contains(agent) && (agent.uri == '" 
                        + agentURI.toString() + "') && ";
            }

            if (agentType != null) {
                filter = filter + "agents.contains(agent2) && (agent2.type == '" 
                        + agentType.toString() + "') && ";
            }

            if (experiment != null) {
                filter = filter + "(experiments.contains(exp) && exp.uri == '" + experiment.toString() + "') && "; 
            }
            
            if (activityType != null) {
                filter = filter + "(activity.contains(act) && act.type == '" + activityType.toString() + "') && "; 
            }

            if (!"".equals(filter)) {
                filter = filter.substring(0, filter.length()-3);
            }
        
        try (PersistenceManager pm = nosql.getPersistentConnectionManager()) {
            Map params = new HashMap();
            int total = nosql.countResults(pm, ProvenanceModel.class, filter, params);
            List<ProvenanceModel> results = nosql.searchWithPagination(pm, ProvenanceModel.class, filter, params, page, pageSize, total);

            List<ProvenanceModel> provenances = new ArrayList();
            for (ProvenanceModel res:results) {
                ProvenanceModel prov = new ProvenanceModel();
                prov.setUri(res.uri);
                prov.setLabel(res.label);
                prov.setComment(res.comment);
                prov.setExperiments(res.experiments);
                prov.setActivity(res.activity);
                prov.setAgents(res.agents);

                provenances.add(prov);
            }

            return new ListWithPagination<>(provenances, page, pageSize, total);        
        }       
    }
        
    public void prepareURI(ProvenanceModel model) throws Exception{
        String[] URICompose = new String[1];
        URICompose[0] = model.getLabel();
        model.setURICompose(URICompose);
    }

    public void delete(URI uri) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException {
        nosql.delete(new ProvenanceModel(), uri);
    }

    public ProvenanceModel update(ProvenanceModel model) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException {
        nosql.update(model);
        return model;
    }

    public ProvenanceModel get(URI uri) throws NamingException, IOException {
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            try (JDOQLTypedQuery<ProvenanceModel> tq = persistenceManager.newJDOQLTypedQuery(ProvenanceModel.class)) {
                QProvenanceModel cand = QProvenanceModel.candidate();
                BooleanExpression expr = cand.uri.eq(uri);
                ProvenanceModel provenance = tq.filter(expr).executeUnique();                
                return provenance;
            }
        }
    }
}
