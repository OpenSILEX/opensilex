/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.opensilex.sparql.SPARQLResult;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.deserializer.DeserializerNotFoundException;
import org.opensilex.sparql.deserializer.Deserializers;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;

/**
 *
 * @author vincent
 */
class SPARQLProxyListData<T> extends SPARQLProxyList<T> {

    public SPARQLProxyListData(URI uri, Property property, Class<T> genericType, boolean isReverseRelation, SPARQLService service) throws DeserializerNotFoundException {
        super(uri, property, genericType, isReverseRelation, service);
    }

    @Override
    protected List<T> loadData() throws Exception {
        SelectBuilder select = new SelectBuilder();

        Var value = makeVar("value");
        select.addVar(value);
        if (isReverseRelation) {
            select.addWhere(value, property, Ontology.nodeURI(uri));
        } else {
            select.addWhere(Ontology.nodeURI(uri), property, value);
        }

        List<T> results = new ArrayList<>();
        SPARQLDeserializer<T> deserializer = Deserializers.getForClass(genericType);

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
