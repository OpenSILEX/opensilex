//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.graphimport.ontology.ClassHierarchy;
import org.opensilex.graphimport.ontology.Labels;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.jena.arq.querybuilder.Converters.makeVar;

/**
 * Reads the {@link ClassCatalog} in one query.
 *
 * @author Arnaud Charleroy
 */
public class ClassCatalogReader {

    private static final ExprFactory EXPR = SPARQLQueryHelper.getExprFactory();
    private static final Var CLASS = makeVar("class");
    private static final Var ROOT = makeVar("root");
    private static final Var PARENT = makeVar("parent");
    private static final Var LABEL = makeVar("label");
    private static final Var LANGUAGE = makeVar("language");

    private final SPARQLService sparql;

    public ClassCatalogReader(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ClassCatalog read(Collection<URI> rootClasses) throws SPARQLException {
        Set<URI> roots = rootClasses.stream().map(Uris::expand).collect(Collectors.toSet());
        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(CLASS).addVar(PARENT).addVar(LABEL)
                .addVar(EXPR.lang(LABEL), LANGUAGE)
                .addWhere(CLASS, Ontology.subClassAny, ROOT)
                .addFilter(EXPR.isIRI(CLASS))
                .addOptional(new WhereBuilder()
                        .addWhere(CLASS, RDFS.subClassOf, PARENT)
                        .addWhere(PARENT, RDF.type, OWL2.Class)
                        .addFilter(EXPR.isIRI(PARENT)))
                .addOptional(CLASS, RDFS.label, LABEL);
        select.addWhereValueVar(ROOT, roots.stream().map(root -> NodeFactory.createURI(root.toString())).toArray());

        ClassHierarchy hierarchy = new ClassHierarchy();
        Map<URI, Map<String, String>> translations = new HashMap<>();
        for (SPARQLResult result : sparql.executeSelectQuery(select)) {
            URI classUri = Uris.expand(result.getStringValue(CLASS.getVarName()));
            Map<String, String> labels = translations.computeIfAbsent(classUri, key -> new HashMap<>());
            String parent = result.getStringValue(PARENT.getVarName());
            if (!StringUtils.isEmpty(parent)) {
                hierarchy.link(classUri, Uris.expand(parent));
            }
            String label = result.getStringValue(LABEL.getVarName());
            if (!StringUtils.isEmpty(label)) {
                labels.putIfAbsent(StringUtils.defaultString(result.getStringValue(LANGUAGE.getVarName())), label);
            }
        }
        Map<URI, Labels> labels = new HashMap<>();
        translations.forEach((classUri, byLanguage) -> labels.put(classUri, new Labels(byLanguage)));
        return new ClassCatalog(roots, labels, hierarchy);
    }
}
