//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.URI;
import java.util.List;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;



/**
 *
 * @author vincent
 */
public class SPARQLProxyListObject<T extends SPARQLResourceModel> extends SPARQLProxyList<T> {

    public SPARQLProxyListObject(Node graph, URI uri, Property property, Class<T> genericType, boolean isReverseRelation, SPARQLService service) {
        super(graph, uri, property, genericType, isReverseRelation, service);
        
    }

    @Override
    protected List<T> loadData() throws Exception {
        SPARQLClassObjectMapper<T> sparqlObjectMapper = SPARQLClassObjectMapper.getForClass(genericType);
        
        Node nodeURI = SPARQLDeserializers.nodeURI(uri);
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
