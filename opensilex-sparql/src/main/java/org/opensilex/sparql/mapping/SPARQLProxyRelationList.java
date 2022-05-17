/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLStatement;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author vidalmor
 */
@SuppressWarnings("rawtypes")
class SPARQLProxyRelationList extends SPARQLProxy<List> {

    private final Set<String> propertiesToIgnore;
    private final URI uri;

    public SPARQLProxyRelationList(SPARQLClassObjectMapperIndex repository, Node graph, URI uri, Set<Property> propertiesToIgnore, String lang, SPARQLService service) {
        super(repository, graph, List.class, lang, service);
        this.uri = uri;
        this.propertiesToIgnore = propertiesToIgnore.stream().map(prop -> prop.getURI()).collect(Collectors.toSet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SPARQLModelRelation> getInstance() {
        return super.getInstance();
    }

    @Override
    protected List loadData() throws Exception {
        List<SPARQLStatement> results = this.service.describe(graph, uri);

        List<SPARQLModelRelation> list = new ArrayList<>(results.size());

        for (SPARQLStatement statement : results) {
            if (!propertiesToIgnore.contains(statement.getPredicate())) {
                SPARQLModelRelation relation = new SPARQLModelRelation();

                relation.setProperty(Ontology.property(statement.getPredicate()));

                boolean isReverse = SPARQLDeserializers.compareURIs(uri.toString(),statement.getObject());
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
