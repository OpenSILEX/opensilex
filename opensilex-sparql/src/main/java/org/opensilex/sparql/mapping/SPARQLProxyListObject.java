//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.URI;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 *
 * @author vincent
 */
public class SPARQLProxyListObject<T extends SPARQLResourceModel> extends SPARQLProxyList<T> {

    protected final Node uriGraph;

    public SPARQLProxyListObject(SPARQLClassObjectMapperIndex repository, Node graph, URI uri, Node uriGraph, Property property, Class<T> genericType, boolean isReverseRelation, String lang, SPARQLService service) {
        super(repository, graph, uri, property, genericType, isReverseRelation, lang, service);
        this.uriGraph = uriGraph;
    }

    @Override
    protected List<T> loadData() throws Exception {
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(genericType);

        Node graphNode = service.getDefaultGraph(genericType);
        if (graph != null) {
            graphNode = graph;
        }
        Node nodeURI = SPARQLDeserializers.nodeURI(uri);
        List<T> list = service.search(graphNode, genericType, lang, (SelectBuilder select) -> {
            if (graph != null && isReverseRelation) {
                select.addGraph(graph, makeVar(mapper.getURIFieldName()), property, nodeURI);
            } else if (uriGraph != null && !isReverseRelation) {
                select.addGraph(uriGraph, nodeURI, property, makeVar(mapper.getURIFieldName()));
            } else {
                if (isReverseRelation) {
                    select.addWhere(makeVar(mapper.getURIFieldName()), property, nodeURI);
                } else {
                    select.addWhere(nodeURI, property, makeVar(mapper.getURIFieldName()));
                }
            }
        });

        return list;
    }

    @Override
    public int getSize() throws Exception {
        SPARQLClassObjectMapper<T> mapper = mapperIndex.getForClass(genericType);

        Node nodeURI = SPARQLDeserializers.nodeURI(uri);
        Node graphNode = service.getDefaultGraph(genericType);
        if (graph != null) {
            graphNode = graph;
        }
        return service.count(graphNode, genericType, lang, (SelectBuilder select) -> {
            if (isReverseRelation) {
                select.addWhere(makeVar(mapper.getURIFieldName()), property, nodeURI);
            } else {
                select.addWhere(nodeURI, property, makeVar(mapper.getURIFieldName()));
            }
        },null);
    }

}
