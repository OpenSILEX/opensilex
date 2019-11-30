/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.net.*;
import java.util.*;
import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.utils.*;

/**
 *
 * @author vidalmor
 */
@SuppressWarnings("rawtypes")
public class SPARQLProxyRelationList extends SPARQLProxy<List> {

    private final Set<String> propertiesToIgnore;
    private final URI uri;

    public SPARQLProxyRelationList(Node graph, URI uri, Set<String> propertiesToIgnore, SPARQLService service) {
        super(graph, List.class, service);
        this.uri = uri;
        this.propertiesToIgnore = propertiesToIgnore;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SPARQLModelRelation> getInstance() {
        return super.getInstance();
    }

    @Override
    protected List loadData() throws Exception {
        List<SPARQLStatement> results = this.service.describe(uri);

        List<SPARQLModelRelation> list = new ArrayList<>();
        
        for (SPARQLStatement statement : results) {
            if (!propertiesToIgnore.contains(statement.getPredicate())) {
                SPARQLModelRelation relation = new SPARQLModelRelation();

                relation.setProperty(Ontology.property(statement.getPredicate()));

                boolean isReverse = uri.toString().equals(statement.getObject());
                relation.setReverse(isReverse);

                if (isReverse) {
                    relation.setValue(statement.getSubject());
                } else {
                    relation.setValue(statement.getObject());
                }
                
                if (statement.getContext() != null) {
                    relation.setGraph(new URI(statement.getContext()));
                }
                
                list.add(relation);
            }

        };

        return list;
    }

}
