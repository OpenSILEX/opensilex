//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
class SPARQLProxyListData<T> extends SPARQLProxyList<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLProxyListData.class);

    public SPARQLProxyListData(SPARQLClassObjectMapperIndex repository, Node graph, URI uri, Property property, Class<T> genericType, boolean isReverseRelation, String lang, SPARQLService service) throws SPARQLDeserializerNotFoundException {
        super(repository, graph, uri, property, genericType, isReverseRelation, lang, service);
    }

    @Override
    protected List<T> loadData() throws Exception {
        SelectBuilder select = new SelectBuilder();

        Var value = makeVar("value");
        select.addVar(value);

        if (graph != null) {
            if (isReverseRelation) {
                select.addGraph(graph, value, property, SPARQLDeserializers.nodeURI(uri));
            } else {
                select.addGraph(graph, SPARQLDeserializers.nodeURI(uri), property, value);
            }
        } else {
            if (isReverseRelation) {
                select.addWhere(value, property, SPARQLDeserializers.nodeURI(uri));
            } else {
                select.addWhere(SPARQLDeserializers.nodeURI(uri), property, value);
            }
        }

        List<T> results = new ArrayList<>();
        SPARQLDeserializer<T> deserializer = SPARQLDeserializers.getForClass(genericType);

        service.executeSelectQuery(select, (SPARQLResult result) -> {
            String strValue = result.getStringValue("value");
            try {
                results.add(deserializer.fromString(strValue));
            } catch (Exception ex) {
                LOGGER.warn("Error while parsing SPARQL result, result will be ignored: " + strValue, ex);
            }
        });

        return results;
    }

    @Override
    public int getSize() throws Exception {
        SelectBuilder select = new SelectBuilder();

        Var value = makeVar("value");
        select.addVar("(COUNT(DISTINCT ?value))", makeVar("count"));

        if (graph != null) {
            if (isReverseRelation) {
                select.addGraph(graph, value, property, SPARQLDeserializers.nodeURI(uri));
            } else {
                select.addGraph(graph, SPARQLDeserializers.nodeURI(uri), property, value);
            }
        } else {
            if (isReverseRelation) {
                select.addWhere(value, property, SPARQLDeserializers.nodeURI(uri));
            } else {
                select.addWhere(SPARQLDeserializers.nodeURI(uri), property, value);
            }
        }

        List<SPARQLResult> resultSet = service.executeSelectQuery(select);

        if (resultSet.size() == 1) {
            return Integer.valueOf(resultSet.get(0).getStringValue("count"));
        } else {
            return this.loadData().size();
        }
    }

}
