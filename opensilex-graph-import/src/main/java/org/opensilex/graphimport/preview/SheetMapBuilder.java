//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.graph.GraphLinks;
import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.SheetInterpretation;
import org.opensilex.graphimport.ontology.PropertyView;
import org.opensilex.graphimport.validate.NodeClasses;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws the map of the sheets from the graph: which sheets describe the same resources, which point to
 * rows of which, which point to existing resources. The links are those of {@link GraphLinks}, gathered
 * by property label: a link to rows is drawn to the first sheet they are in, the others being reached
 * through the shared resources.
 *
 * @author Arnaud Charleroy
 */
final class SheetMapBuilder {

    private final InterpretationServices services;

    SheetMapBuilder(InterpretationServices services) {
        this.services = services;
    }

    /** A link before it is counted. */
    private record LinkKey(String from, String to, SheetMap.Kind kind, String property) {
    }

    SheetMap build(Interpretation interpretation, ImportGraph graph) {
        Map<String, Set<String>> resourcesBySheet = new HashMap<>();
        for (GraphNode node : graph.nodes()) {
            for (CellRef row : node.getRows()) {
                resourcesBySheet.computeIfAbsent(row.sheet(), key -> new LinkedHashSet<>()).add(node.getKey());
            }
        }
        Map<String, SheetInterpretation> bySheet = new HashMap<>();
        List<SheetMap.Sheet> sheets = new ArrayList<>();
        for (SheetInterpretation sheet : interpretation.sheets()) {
            URI classUri = sheet.sheetClass().chosen();
            sheets.add(new SheetMap.Sheet(sheet.name(),
                    classUri == null ? null : services.catalog().labelOf(classUri, services.language()),
                    sheet.rowCount(), resourcesBySheet.getOrDefault(sheet.name(), Set.of()).size()));
            bySheet.put(sheet.name(), sheet);
        }

        GraphLinks links = GraphLinks.of(graph, interpretation);
        Map<LinkKey, Integer> counts = new LinkedHashMap<>();
        links.shared().forEach(pair ->
                counts.merge(new LinkKey(pair.from(), pair.to(), SheetMap.Kind.SAME_RESOURCE, null), pair.count(),
                        Integer::sum));
        for (GraphLinks.ColumnLink link : links.columnLinks()) {
            String label = propertyLabel(bySheet.get(link.sheet()), link.property());
            if (link.kind() == GraphLinks.Kind.ROW) {
                counts.merge(new LinkKey(link.sheet(), link.toSheet(), SheetMap.Kind.ROW, label), link.count(),
                        Integer::sum);
            } else if (link.kind() == GraphLinks.Kind.INSTANCE) {
                String to = link.toClass() == null ? label : services.catalog().labelOf(link.toClass(),
                        services.language());
                counts.merge(new LinkKey(link.sheet(), to, SheetMap.Kind.INSTANCE, label), link.count(),
                        Integer::sum);
            }
        }
        List<SheetMap.Link> mapLinks = new ArrayList<>();
        counts.forEach((key, count) ->
                mapLinks.add(new SheetMap.Link(key.from(), key.to(), key.kind(), key.property(), count)));
        return new SheetMap(sheets, mapLinks);
    }

    /** The label of the property as the sheet's class names it, else its local name. */
    private String propertyLabel(SheetInterpretation sheet, URI property) {
        PropertyView view = sheet == null || sheet.classView() == null ? null : sheet.classView().getProperty(property);
        return view == null ? property.toString() : NodeClasses.label(view, services);
    }
}
