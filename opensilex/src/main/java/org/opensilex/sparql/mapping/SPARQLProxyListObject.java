//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.*;
import java.util.*;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.utils.*;


/**
 *
 * @author vincent
 */
public class SPARQLProxyListObject<T extends SPARQLModel> extends SPARQLProxyList<T> {

    public SPARQLProxyListObject(Node graph, URI uri, Property property, Class<T> genericType, boolean isReverseRelation, SPARQLService service) {
        super(graph, uri, property, genericType, isReverseRelation, service);
        
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
