//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.URI;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vincent
 */
@SuppressWarnings("rawtypes")
abstract public class SPARQLProxyList<T> extends SPARQLProxy<List> {

    protected final URI uri;
    protected final Property property;
    protected final Class<T> genericType;
    protected final boolean isReverseRelation;

    public SPARQLProxyList(Node graph, URI uri, Property property, Class<T> genericType, boolean isReverseRelation, String lang, SPARQLService service) {
        super(graph, List.class, lang, service);
        this.property = property;
        this.uri = uri;
        this.genericType = genericType;
        this.isReverseRelation = isReverseRelation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getInstance() {
        return super.getInstance();
    }

}
