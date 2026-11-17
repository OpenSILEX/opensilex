//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.jena.arq.querybuilder.Converters.makeVar;

/**
 * Reads the {@link ClassView} of a class from the triplestore, in four queries whatever the class.
 * <p>
 * The ontology store is not used on purpose. Asking it for a class with an ancestor writes the
 * inherited restrictions into its shared cache, where the ancestor's restriction replaces the class's
 * own; its loader also reads an exact cardinality as a maximum only. Both stores, the one of the tests
 * and the one of production, would then give different answers. Reading the triplestore gives the
 * same answer on both.
 *
 * @author Arnaud Charleroy
 */
public class ClassViewReader {

    private static final ExprFactory EXPR = SPARQLQueryHelper.getExprFactory();

    private static final Var CHILD = makeVar("child");
    private static final Var PARENT = makeVar("parent");
    private static final Var DOMAIN = makeVar("domain");
    private static final Var RESTRICTION = makeVar("restriction");
    private static final Var PROPERTY = makeVar("property");
    private static final Var PROPERTY_TYPE = makeVar("propertyType");
    private static final Var ON_CLASS = makeVar("onClass");
    private static final Var ON_DATA_RANGE = makeVar("onDataRange");
    private static final Var EXACT = makeVar("exact");
    private static final Var MIN = makeVar("min");
    private static final Var MAX = makeVar("max");
    private static final Var EXACT_UNQUALIFIED = makeVar("exactUnqualified");
    private static final Var MIN_UNQUALIFIED = makeVar("minUnqualified");
    private static final Var MAX_UNQUALIFIED = makeVar("maxUnqualified");
    private static final Var RESOURCE = makeVar("resource");
    private static final Var LABEL = makeVar("label");
    private static final Var LANGUAGE = makeVar("language");

    private final SPARQLService sparql;

    public ClassViewReader(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     * @param classUri the class, as a full or a prefixed URI
     * @throws SPARQLInvalidURIException when the URI names no {@code owl:Class}
     */
    public ClassView read(URI classUri) throws SPARQLException {
        URI expanded = Uris.expand(classUri.toString());
        Node classNode = NodeFactory.createURI(expanded.toString());
        if (!sparql.executeAskQuery(new AskBuilder().addWhere(classNode, RDF.type, OWL2.Class))) {
            throw new SPARQLInvalidURIException("Not an owl:Class: ", expanded);
        }

        ClassHierarchy hierarchy = readHierarchy(classNode);
        Set<URI> scope = new LinkedHashSet<>(hierarchy.ancestorsOf(expanded));
        scope.add(expanded);
        List<RestrictionRow> restrictions = readRestrictions(scope);

        Set<URI> labelled = new LinkedHashSet<>(scope);
        restrictions.forEach(row -> labelled.add(row.property()));
        return ClassView.assemble(expanded, hierarchy, restrictions, readLabels(labelled));
    }

    private ClassHierarchy readHierarchy(Node classNode) throws SPARQLException {
        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(CHILD)
                .addVar(PARENT)
                .addWhere(classNode, Ontology.subClassAny, CHILD)
                .addWhere(CHILD, RDFS.subClassOf, PARENT)
                .addWhere(PARENT, RDF.type, OWL2.Class)
                .addFilter(EXPR.isIRI(PARENT));

        ClassHierarchy hierarchy = new ClassHierarchy();
        for (SPARQLResult result : sparql.executeSelectQuery(select)) {
            hierarchy.link(uri(result, CHILD), uri(result, PARENT));
        }
        return hierarchy;
    }

    private List<RestrictionRow> readRestrictions(Collection<URI> domains) throws SPARQLException {
        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(DOMAIN).addVar(RESTRICTION).addVar(PROPERTY).addVar(PROPERTY_TYPE)
                .addVar(ON_CLASS).addVar(ON_DATA_RANGE)
                .addVar(EXACT).addVar(MIN).addVar(MAX)
                .addVar(EXACT_UNQUALIFIED).addVar(MIN_UNQUALIFIED).addVar(MAX_UNQUALIFIED)
                .addWhere(DOMAIN, RDFS.subClassOf, RESTRICTION)
                .addWhere(RESTRICTION, RDF.type, OWL2.Restriction)
                .addWhere(RESTRICTION, OWL2.onProperty, PROPERTY)
                .addFilter(EXPR.isIRI(PROPERTY))
                .addOptional(RESTRICTION, OWL2.onClass, ON_CLASS)
                .addOptional(RESTRICTION, OWL2.onDataRange, ON_DATA_RANGE)
                .addOptional(RESTRICTION, OWL2.qualifiedCardinality, EXACT)
                .addOptional(RESTRICTION, OWL2.minQualifiedCardinality, MIN)
                .addOptional(RESTRICTION, OWL2.maxQualifiedCardinality, MAX)
                .addOptional(RESTRICTION, OWL2.cardinality, EXACT_UNQUALIFIED)
                .addOptional(RESTRICTION, OWL2.minCardinality, MIN_UNQUALIFIED)
                .addOptional(RESTRICTION, OWL2.maxCardinality, MAX_UNQUALIFIED)
                .addOptional(new WhereBuilder()
                        .addWhere(PROPERTY, RDF.type, PROPERTY_TYPE)
                        .addFilter(EXPR.in(PROPERTY_TYPE, OWL2.DatatypeProperty.asNode(),
                                OWL2.ObjectProperty.asNode())));
        select.addWhereValueVar(DOMAIN, nodes(domains));

        List<RestrictionRow> rows = new ArrayList<>();
        for (SPARQLResult result : sparql.executeSelectQuery(select)) {
            URI onClass = optionalUri(result, ON_CLASS);
            URI onDataRange = optionalUri(result, ON_DATA_RANGE);
            Cardinality cardinality = Cardinality
                    .of(integer(result, EXACT), integer(result, MIN), integer(result, MAX))
                    .intersect(Cardinality.of(integer(result, EXACT_UNQUALIFIED), integer(result, MIN_UNQUALIFIED),
                            integer(result, MAX_UNQUALIFIED)));
            rows.add(PlatformReadings.read(new RestrictionRow(
                    uri(result, DOMAIN),
                    uri(result, PROPERTY),
                    kind(onClass, onDataRange, optionalUri(result, PROPERTY_TYPE)),
                    onDataRange != null ? onDataRange : onClass,
                    cardinality)));
        }
        return rows;
    }

    private Map<URI, Labels> readLabels(Collection<URI> resources) throws SPARQLException {
        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(RESOURCE)
                .addVar(LABEL)
                .addVar(EXPR.lang(LABEL), LANGUAGE)
                .addWhere(RESOURCE, RDFS.label, LABEL);
        select.addWhereValueVar(RESOURCE, nodes(resources));

        Map<URI, Map<String, String>> byResource = new HashMap<>();
        for (SPARQLResult result : sparql.executeSelectQuery(select)) {
            String language = StringUtils.defaultString(result.getStringValue(LANGUAGE.getVarName()));
            byResource.computeIfAbsent(uri(result, RESOURCE), key -> new HashMap<>())
                    .putIfAbsent(language, result.getStringValue(LABEL.getVarName()));
        }
        Map<URI, Labels> labels = new HashMap<>();
        byResource.forEach((resource, translations) -> labels.put(resource, new Labels(translations)));
        return labels;
    }

    /**
     * Literal or link: the restriction's range says it, else the property's declared type. A
     * restriction with only {@code owl:someValuesFrom} and an untyped property tells nothing.
     */
    private static PropertyKind kind(URI onClass, URI onDataRange, URI propertyType) {
        if (onDataRange != null) {
            return PropertyKind.DATA;
        }
        if (onClass != null) {
            return PropertyKind.OBJECT;
        }
        if (propertyType == null) {
            return null;
        }
        return propertyType.toString().equals(OWL2.DatatypeProperty.getURI()) ? PropertyKind.DATA : PropertyKind.OBJECT;
    }

    private static Object[] nodes(Collection<URI> uris) {
        return uris.stream().map(uri -> NodeFactory.createURI(uri.toString())).toArray();
    }

    private static URI uri(SPARQLResult result, Var var) {
        return Uris.expand(result.getStringValue(var.getVarName()));
    }

    private static URI optionalUri(SPARQLResult result, Var var) {
        String value = result.getStringValue(var.getVarName());
        return StringUtils.isEmpty(value) ? null : Uris.expand(value);
    }

    private static Integer integer(SPARQLResult result, Var var) {
        String value = result.getStringValue(var.getVarName());
        return StringUtils.isEmpty(value) ? null : Integer.valueOf(value.trim());
    }
}
