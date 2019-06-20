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
public class SPARQLProxyListObject<T> extends SPARQLProxyList<T> {

    public SPARQLProxyListObject(URI uri, Property property, Class<T> genericType, SPARQLService service) {
        super(uri, property, genericType, service);
    }

    @Override
    protected List<T> loadData() throws Exception {
        SPARQLClassDescriptor<T> descriptor = SPARQLClassDescriptor.getForClass(genericType);
        
        List<T> results = service.search(genericType, (SelectBuilder select) -> {
            select.addWhere(uri, property, descriptor.getURIFieldName());
        });

        return results;
    }

    
}
