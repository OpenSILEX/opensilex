/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.net.URI;
import java.util.List;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.SPARQLService;

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

    public SPARQLProxyList(URI uri, Property property, Class<T> genericType, boolean isReverseRelation, SPARQLService service) {
        super(List.class, service);
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
