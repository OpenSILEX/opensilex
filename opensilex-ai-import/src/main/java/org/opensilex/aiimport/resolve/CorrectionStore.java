//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The misspellings this instance has been taught, kept in the triplestore.
 * <p>
 * Each one is written twice, on purpose. The resource itself gets a {@code skos:hiddenLabel} —
 * the SKOS property defined for misspelt variants, searchable but never displayed as a name — so
 * any SPARQL client can use what was learned, not just this module. And a small PROV-O record says
 * who taught it, when, and for which kind of resource, because a correction that silently acts on
 * other people's imports must be traceable and revocable.
 * <p>
 * Everything lives in one graph of its own. Nothing is written into a resource's graph, so the
 * rest of OpenSILEX is untouched, and forgetting a correction is deleting a few triples here.
 * A correction whose resource has since been deleted is simply not read back.
 *
 * @author Arnaud Charleroy
 */
public class CorrectionStore {

    private static final String PROV = "http://www.w3.org/ns/prov#";
    private static final Node PROV_ENTITY = NodeFactory.createURI(PROV + "Entity");
    private static final Node PROV_ATTRIBUTED_TO = NodeFactory.createURI(PROV + "wasAttributedTo");
    private static final Node PROV_GENERATED_AT = NodeFactory.createURI(PROV + "generatedAtTime");

    private final SPARQLService sparql;
    private final URI graph;

    /**
     * @param graph the graph holding the corrections, derived from the instance's base URI by the
     *              caller so the store needs no configuration of its own
     */
    public CorrectionStore(SPARQLService sparql, URI graph) {
        this.sparql = sparql;
        this.graph = graph;
    }

    /**
     * @return the graph of an instance whose resources are named under {@code baseUri}
     */
    public static URI graphFor(URI baseUri) {
        String base = baseUri.toString();
        return URI.create(base + (base.endsWith("/") ? "" : "/") + "set/ai-import/corrections");
    }

    /**
     * Every correction whose resource still exists, by category and normalised misspelling.
     */
    public Corrections load() throws Exception {
        Node graphNode = NodeFactory.createURI(graph.toString());
        SelectBuilder select = new SelectBuilder()
                .addVar("?c").addVar("?target").addVar("?label").addVar("?category")
                .addVar("?title").addVar("?author").addVar("?created")
                .addGraph(graphNode, new SelectBuilder()
                        .addWhere("?c", RDF.type.asNode(), PROV_ENTITY)
                        .addWhere("?c", DCTerms.subject.asNode(), "?target")
                        .addWhere("?c", SKOS.hiddenLabel.asNode(), "?label")
                        .addWhere("?c", DCTerms.type.asNode(), "?category")
                        .addOptional("?c", DCTerms.title.asNode(), "?title")
                        .addOptional("?c", DCTerms.creator.asNode(), "?author")
                        .addOptional("?c", PROV_GENERATED_AT, "?created"))
                // A deleted resource loses its type; its corrections are left unread rather than
                // resolving names to something that no longer exists.
                .addWhere("?target", RDF.type.asNode(), "?anyType");

        Corrections corrections = new Corrections();
        for (SPARQLResult row : sparql.executeSelectQuery(select, null)) {
            Optional<ReportCategory> category = ReportCategory.fromKey(row.getStringValue("category"));
            if (!category.isPresent()) {
                continue;
            }
            String created = row.getStringValue("created");
            corrections.add(new LearnedCorrection(
                    URI.create(row.getStringValue("c")),
                    category.get(),
                    row.getStringValue("label"),
                    URI.create(row.getStringValue("target")),
                    row.getStringValue("title"),
                    row.getStringValue("author"),
                    created == null || created.isEmpty() ? null : OffsetDateTime.parse(created)));
        }
        return corrections;
    }

    /**
     * Teaches the instance that {@code label} means {@code target} for this kind of resource.
     * <p>
     * A misspelling can only mean one thing: a previous correction of the same spelling in the
     * same category is replaced, not kept alongside.
     */
    public LearnedCorrection remember(ReportCategory category, String label, URI target,
                                      String targetName, URI authorUri, String authorName)
            throws Exception {
        Optional<LearnedCorrection> previous = load().lookup(category, label);
        if (previous.isPresent()) {
            forget(previous.get());
        }

        URI uri = URI.create(graph + "/" + UUID.randomUUID());
        OffsetDateTime now = OffsetDateTime.now();
        String g = uri(graph);
        String c = uri(uri);
        String t = uri(target);
        String l = literal(label);

        StringBuilder data = new StringBuilder()
                .append(t).append(' ').append(uri(SKOS.hiddenLabel.getURI())).append(' ').append(l).append(" .\n")
                .append(c).append(' ').append(uri(RDF.type.getURI())).append(' ').append(uri(PROV + "Entity")).append(" .\n")
                .append(c).append(' ').append(uri(DCTerms.subject.getURI())).append(' ').append(t).append(" .\n")
                .append(c).append(' ').append(uri(SKOS.hiddenLabel.getURI())).append(' ').append(l).append(" .\n")
                .append(c).append(' ').append(uri(DCTerms.type.getURI())).append(' ').append(literal(category.getKey())).append(" .\n")
                .append(c).append(' ').append(uri(PROV + "generatedAtTime")).append(' ')
                .append(FmtUtils.stringForNode(NodeFactory.createLiteral(now.toString(), XSDDatatype.XSDdateTime))).append(" .\n");
        if (targetName != null) {
            data.append(c).append(' ').append(uri(DCTerms.title.getURI())).append(' ').append(literal(targetName)).append(" .\n");
        }
        if (authorUri != null) {
            data.append(c).append(' ').append(uri(PROV + "wasAttributedTo")).append(' ').append(uri(authorUri)).append(" .\n");
        }
        if (authorName != null) {
            data.append(c).append(' ').append(uri(DCTerms.creator.getURI())).append(' ').append(literal(authorName)).append(" .\n");
        }

        sparql.executeUpdateQuery("INSERT DATA { GRAPH " + g + " {\n" + data + "} }");
        return new LearnedCorrection(uri, category, label, target, targetName, authorName, now);
    }

    /**
     * Removes a correction: its record and the hidden label it put on the resource.
     */
    public void forget(LearnedCorrection correction) throws Exception {
        String g = uri(graph);
        sparql.executeUpdateQuery(
                "DELETE WHERE { GRAPH " + g + " { " + uri(correction.getUri()) + " ?p ?o } } ;\n"
                        + "DELETE DATA { GRAPH " + g + " { " + uri(correction.getTarget()) + " "
                        + uri(SKOS.hiddenLabel.getURI()) + " " + literal(correction.getLabel())
                        + " } }");
    }

    /**
     * Through the platform's URI deserializer, which expands a prefixed URI ({@code test:id/…}) to
     * the IRI it stands for: the platform hands URIs out in their short form, and written as such a
     * correction would sit on a resource that does not exist, and never be read back.
     */
    private static String uri(Object uri) {
        Node node = SPARQLDeserializers.nodeURI(uri.toString());
        return FmtUtils.stringForNode(node != null ? node : NodeFactory.createURI(uri.toString()));
    }

    /**
     * Serialised by Jena, never concatenated by hand: a quote or a backslash in a spreadsheet cell
     * must not be able to end the literal and start a statement.
     */
    private static String literal(String value) {
        return FmtUtils.stringForNode(NodeFactory.createLiteral(value));
    }

    /**
     * The corrections of one analysis, looked up by category and by normalised spelling, so a
     * correction taught for "Chardonay" also covers "CHARDONAY" and "chardonay ".
     */
    public static class Corrections {

        private final Map<ReportCategory, Map<String, LearnedCorrection>> byCategory =
                new EnumMap<>(ReportCategory.class);

        public void add(LearnedCorrection correction) {
            byCategory.computeIfAbsent(correction.getCategory(), key -> new HashMap<>())
                    .put(HeaderMatcher.normalize(correction.getLabel()), correction);
        }

        public Optional<LearnedCorrection> lookup(ReportCategory category, String fileValue) {
            Map<String, LearnedCorrection> known = byCategory.get(category);
            return known == null
                    ? Optional.empty()
                    : Optional.ofNullable(known.get(HeaderMatcher.normalize(fileValue)));
        }

        public boolean isEmpty() {
            return byCategory.values().stream().allMatch(Map::isEmpty);
        }
    }
}
