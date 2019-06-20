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
import org.opensilex.utils.deserializer.DeserializerNotFoundException;
import org.opensilex.utils.deserializer.Deserializers;

/**
 *
 * @author vincent
 */
class SPARQLProxyListData<T> extends SPARQLProxyList<T> {

    public SPARQLProxyListData(URI uri, Property property, Class<T> genericType, SPARQLService service) throws DeserializerNotFoundException {
        super(uri, property, genericType, service);
    }

    @Override
    protected List<T> loadData() throws Exception {
        SelectBuilder select = new SelectBuilder();

        select.addVar("value");
        select.addWhere(uri, property, "value");

        List<T> results = new ArrayList<>();
        Deserializer<T> deserializer = Deserializers.getForClass(genericType);

        service.executeSelectQuery(select, (SPARQLResult result) -> {
            try {
                String strValue = result.getStringValue("value");
                results.add(deserializer.fromString(strValue));
            } catch (Exception ex) {
                // TODO warn
            }
        });

        return results;
    }

}
