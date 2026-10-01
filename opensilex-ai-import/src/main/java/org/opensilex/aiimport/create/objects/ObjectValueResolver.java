//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.objects;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.E_Str;
import org.apache.jena.sparql.expr.E_StrLowerCase;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Finds the resource a cell names, among the instances of the class a property points to.
 * <p>
 * The platform's importer takes a URI for every property whose value is a resource, and never a
 * name; a workbook writes names. The name is looked up here, by exact label, case aside — the same
 * rule every other name of the file is resolved by — and a name that matches two resources is
 * refused rather than guessed.
 * <p>
 * A cell that already holds an absolute URI is passed as it is: the importer checks it exists.
 *
 * @author Arnaud Charleroy
 */
public class ObjectValueResolver {

    /**
     * What a name turned into: a URI, nothing, or more than one candidate.
     */
    public record Resolution(URI uri, int candidates) {

        public boolean isFound() {
            return uri != null;
        }

        public boolean isAmbiguous() {
            return candidates > 1;
        }
    }

    private final SPARQLService sparql;
    private final Map<String, Resolution> cache = new HashMap<>();

    public ObjectValueResolver(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     * @param range the class the resource must belong to, subclasses included
     * @param graph the graph to look in — an experiment, for the objects of that experiment — or
     *              {@code null} for any
     */
    public Resolution resolve(String value, URI range, URI graph) throws SPARQLException {
        if (value == null || value.isBlank()) {
            return new Resolution(null, 0);
        }
        String trimmed = value.trim();
        if (isAbsoluteUri(trimmed)) {
            return new Resolution(URI.create(trimmed), 1);
        }
        String key = range + "|" + graph + "|" + trimmed.toLowerCase(Locale.ROOT);
        Resolution known = cache.get(key);
        if (known != null) {
            return known;
        }
        Resolution found = lookUp(trimmed, range, graph);
        cache.put(key, found);
        return found;
    }

    private Resolution lookUp(String name, URI range, URI graph) throws SPARQLException {
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var labelVar = makeVar("label");
        Var typeVar = makeVar("type");

        SelectBuilder select = new SelectBuilder().addVar(uriVar).setDistinct(true);
        if (graph == null) {
            select.addWhere(uriVar, RDFS.label, labelVar);
            select.addWhere(uriVar, RDF.type, typeVar);
        } else {
            Node graphNode = SPARQLDeserializers.nodeURI(graph);
            select.addGraph(graphNode, uriVar, RDFS.label, labelVar);
            select.addGraph(graphNode, uriVar, RDF.type, typeVar);
        }
        if (range != null) {
            select.addWhere(typeVar, new P_ZeroOrMore1(new P_Link(RDFS.subClassOf.asNode())),
                    SPARQLDeserializers.nodeURI(range));
        }
        select.addFilter(new E_Equals(new E_StrLowerCase(new E_Str(new ExprVar(labelVar))),
                new NodeValueString(name.toLowerCase(Locale.ROOT))));
        select.setLimit(2);

        List<URI> uris = new ArrayList<>();
        for (SPARQLResult result : sparql.executeSelectQuery(select)) {
            uris.add(URI.create(SPARQLDeserializers.getExpandedURI(
                    result.getStringValue(SPARQLResourceModel.URI_FIELD))));
        }
        return new Resolution(uris.size() == 1 ? uris.get(0) : null, uris.size());
    }

    static boolean isAbsoluteUri(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
