//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.*;
import java.util.*;
import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.*;
import org.opensilex.sparql.*;

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

    public SPARQLProxyList(Node graph, URI uri, Property property, Class<T> genericType, boolean isReverseRelation, SPARQLService service) {
        super(graph, List.class, service);
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
