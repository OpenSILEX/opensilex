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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.jdo.query.BooleanExpression;
import javax.naming.NamingException;
import org.opensilex.core.provenance.api.ProvenanceCreationDTO;
import org.opensilex.nosql.service.NoSQLService;
/**
 *
 * @author boizetal
 */
public class ProvenanceDAO {
    
    protected final NoSQLService nosql;    
    
    public ProvenanceDAO(NoSQLService nosql) {
        this.nosql = nosql;
    }  

    public URI create(ProvenanceCreationDTO provDTO) throws NamingException, URISyntaxException {
        ProvenanceModel provenance = new ProvenanceModel();
        URI uri = generateProvenanceUri();
        provenance.setUri(uri);
        provenance.setLabel(provDTO.getLabel());
        provenance.setComment(provDTO.getComment());
        if (provDTO.getExperiments() != null) {
            provenance.setExperiments(provDTO.getExperiments());
        }        
        //ProvMetadata metadata = new ProvMetadata(provDTO.getMetadata().get("operator").toString());
        provenance.setAgent(provDTO.getAgent());
        nosql.create(provenance);

        return uri;
    }
    
    public boolean exist(URI uri) throws NamingException, IOException{
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
    
    public List<ProvenanceModel> search(String label, URI sensor, URI operator) throws NamingException, IOException {
        
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            
            try (JDOQLTypedQuery<ProvenanceModel> tq = persistenceManager.newJDOQLTypedQuery(ProvenanceModel.class)) {
                QProvenanceModel cand = QProvenanceModel.candidate();
                BooleanExpression expr = null;
                
                if (label != null) {
                    expr = cand.label.eq(label);
                    if (sensor != null) {
                        expr = expr.and(cand.agent.sensingDevice.containsKey(sensor.toString()));
                        if (operator != null) {
                            expr = expr.and(cand.agent.operator.containsValue(operator.toString()));
                        }
                    }
                } else {
                    if (sensor != null) {
                        expr = cand.agent.sensingDevice.containsKey(sensor.toString());
                        if (operator != null) {
                            expr = expr.and(cand.agent.operator.containsValue(operator.toString()));
                        }
                    }else {
                        if (operator != null) {
                            expr = cand.agent.operator.containsValue(operator.toString());
                        }                        
                    }
                }
                
                List<ProvenanceModel> results = null;
                if (expr != null) {
                    results = tq.filter(expr)
                        .executeList();
                } else {
                    results = tq.executeList();                    
                }
                
                List<ProvenanceModel> provenances = new ArrayList<>();

                for (ProvenanceModel res:results) {
                    ProvenanceModel prov = new ProvenanceModel();
                    prov.setUri(res.uri);
                    prov.setLabel(res.label);
                    prov.setComment(res.getComment());
                    prov.setExperiments(res.experiments);
                    prov.setAgent(res.agent);

                    provenances.add(prov);
                }
                
                return provenances;
            }
        }       
    }
    
    private URI generateProvenanceUri() throws URISyntaxException {
        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();
        URI uri = null;

        try {
            uri = new URI("http://opensilex.dev/" + "provenance/" + Long.toString(timeStampMillis));
            return uri;
            
        } catch (URISyntaxException e) {
            
        }
        
        return uri;
    }
    
}
