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
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.deserializer.*;
import org.opensilex.sparql.utils.*;


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
