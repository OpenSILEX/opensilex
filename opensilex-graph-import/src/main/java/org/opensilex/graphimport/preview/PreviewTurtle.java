//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.graph.GraphValue;
import org.opensilex.graphimport.graph.LinkTarget;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.graphimport.ontology.literal.LiteralReading;

import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes what an import describes, its rows merged, as Turtle: every resource with its classes and the
 * values the file gives it, whether the instance holds them already or not. Nothing is written to the
 * instance.
 * <p>
 * A resource keeps the URI it exists under, or the one the file writes; any other gets an indicative
 * URI under {@link #INDICATIVE_BASE}, the same for the same file, which the platform would replace
 * with its own. A cell that gives no value, and a link whose target is unclear, are left out and
 * counted in the header.
 *
 * @author Arnaud Charleroy
 */
public class PreviewTurtle {

    /** Where the URIs of resources that do not exist yet are made up. */
    public static final String INDICATIVE_BASE = "urn:graph-import:";

    private static final String LITERAL = RDFS.Literal.getURI();

    private final ValueFactory values = SimpleValueFactory.getInstance();

    /**
     * @param prefixes the platform's prefixes, by name
     */
    public String write(ImportPreview preview, Map<String, String> prefixes) {
        Model model = new LinkedHashModel();
        new TreeMap<>(prefixes).forEach(model::setNamespace);
        model.setNamespace("rdf", RDF.NAMESPACE);
        model.setNamespace("rdfs", RDFS.getURI());
        model.setNamespace("xsd", XSD.NAMESPACE);

        Map<String, IRI> subjects = new HashMap<>();
        for (NodeComparison node : preview.getNodes()) {
            subjects.put(node.node().getKey(), subject(node));
        }
        int leftOut = 0;
        for (NodeComparison node : preview.getNodes()) {
            IRI subject = subjects.get(node.node().getKey());
            for (URI classUri : node.classUri() == null ? node.node().getClasses() : List.of(node.classUri())) {
                model.add(subject, RDF.TYPE, values.createIRI(Uris.expand(classUri).toString()));
            }
            for (ValueComparison value : node.values()) {
                Value object = object(value, subjects);
                if (object == null) {
                    leftOut++;
                } else {
                    model.add(subject, values.createIRI(Uris.expand(value.property()).toString()), object);
                }
            }
        }

        StringWriter out = new StringWriter();
        out.write("# What the import describes once its rows are merged. Nothing was written to the instance.\n");
        out.write("# Resources that do not exist yet have an indicative URI under " + INDICATIVE_BASE
                + ": the platform gives them their own when they are created.\n");
        out.write("# Values left out, the cell giving none or the link being unclear: " + leftOut + ".\n\n");
        Rio.write(model, out, RDFFormat.TURTLE);
        return out.toString();
    }

    private IRI subject(NodeComparison node) {
        GraphNode graphNode = node.node();
        if (node.existing() != null && node.reason() == null) {
            return values.createIRI(node.existing().toString());
        }
        if (graphNode.getUri() != null) {
            return values.createIRI(Uris.expand(graphNode.getUri()).toString());
        }
        if (graphNode.isIdentified()) {
            return values.createIRI(INDICATIVE_BASE + "resource:" + encode(graphNode.getKey()));
        }
        CellRef row = graphNode.getRows().get(0);
        return values.createIRI(INDICATIVE_BASE + "row:" + encode(row.sheet()) + ":" + row.row());
    }

    private Value object(ValueComparison value, Map<String, IRI> subjects) {
        if (value.state() == ValueState.UNUSABLE || value.state() == ValueState.AMBIGUOUS) {
            return null;
        }
        GraphValue cell = value.value();
        if (cell.isLink()) {
            if (value.target() != null) {
                return values.createIRI(value.target().toString());
            }
            LinkTarget link = cell.link();
            return link.nodeKey() == null ? null : subjects.get(link.nodeKey());
        }
        LiteralReading literal = cell.literal();
        if (cell.language() != null) {
            return values.createLiteral(literal.lexical(), cell.language());
        }
        if (LITERAL.equals(literal.datatype().toString())) {
            return values.createLiteral(literal.lexical());
        }
        return values.createLiteral(literal.lexical(), values.createIRI(literal.datatype().toString()));
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
