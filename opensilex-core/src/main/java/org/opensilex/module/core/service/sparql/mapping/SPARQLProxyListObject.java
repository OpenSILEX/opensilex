/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql.mapping;

import java.net.URI;
import java.util.List;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.opensilex.module.core.service.sparql.SPARQLService;
import org.opensilex.utils.ontology.Ontology;

/**
 *
 * @author vincent
 */
public class SPARQLProxyListObject<T> extends SPARQLProxyList<T> {

    public SPARQLProxyListObject(URI uri, Property property, Class<T> genericType, boolean isReverseRelation, SPARQLService service) {
        super(uri, property, genericType, isReverseRelation, service);
        
    }

    @Override
    protected List<T> loadData() throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(genericType);
        
        Node nodeURI = Ontology.nodeURI(uri);
        List<T> results = service.search(genericType, (SelectBuilder select) -> {
            if (isReverseRelation) {
                select.addWhere(makeVar(sparqlObjectMapper.getURIFieldName()), property, nodeURI);
            } else {
                select.addWhere(nodeURI, property, makeVar(sparqlObjectMapper.getURIFieldName()));
            }
            
        });

        return results;
    }

    
}
