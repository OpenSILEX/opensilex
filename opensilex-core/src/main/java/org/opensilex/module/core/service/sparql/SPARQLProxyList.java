/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Property;
import org.opensilex.utils.deserializer.Deserializer;
import org.opensilex.utils.deserializer.Deserializers;

/**
 *
 * @author vincent
 */
@SuppressWarnings("rawtypes")
abstract public class SPARQLProxyList<T> extends SPARQLProxy<List> {

    protected final URI uri;
    protected final Property property;
    protected final Class<T> genericType;

    public SPARQLProxyList(URI uri, Property property, Class<T> genericType, SPARQLService service) {
        super(List.class, service);
        this.property = property;
        this.uri = uri;
        this.genericType = genericType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getInstance() {
        return super.getInstance();
    }

}
