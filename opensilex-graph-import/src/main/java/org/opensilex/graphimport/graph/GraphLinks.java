//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.SheetInterpretation;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How the sheets of an import hang together once their rows are merged: where the links of each column
 * land, and which sheets describe the same resources. The screens draw it; they cannot work it out,
 * since where a link lands depends on the cells.
 *
 * @param columnLinks where the cells of each link column point, counted
 * @param shared      the pairs of sheets whose rows are the same resources, counted
 * @author Arnaud Charleroy
 */
public record GraphLinks(List<ColumnLink> columnLinks, List<SharedResources> shared) {

    public GraphLinks {
        columnLinks = List.copyOf(columnLinks);
        shared = List.copyOf(shared);
    }

    /** Where a link cell points. */
    public enum Kind {
        /** A row of the file. */
        ROW,
        /** A resource that exists already. */
        INSTANCE,
        /** Nothing settled: no answer, several, or a row of a class that does not fit. */
        UNRESOLVED
    }

    /**
     * Cells of one column pointing to one place.
     *
     * @param sheet    the sheet of the column
     * @param column   the column, from 0
     * @param property the property of the column
     * @param kind     where they point
     * @param toSheet  for {@link Kind#ROW}, the first sheet, in the workbook's order, where the rows they
     *                 name are; the other sheets of those rows are reached through the shared resources
     * @param toClass  for {@link Kind#INSTANCE}, the range of the property; null when it has none
     * @param count    how many cells
     */
    public record ColumnLink(String sheet, int column, URI property, Kind kind, String toSheet, URI toClass,
                             int count) {
    }

    /**
     * Two sheets whose rows share identifiers, and so are the same resources.
     *
     * @param from  the sheet that comes first in the workbook
     * @param to    the other sheet
     * @param count how many resources they share
     */
    public record SharedResources(String from, String to, int count) {
    }

    /** A link before it is counted. */
    private record Key(String sheet, int column, URI property, Kind kind, String toSheet, URI toClass) {
    }

    public static GraphLinks of(ImportGraph graph, Interpretation interpretation) {
        Map<String, SheetInterpretation> sheets = new HashMap<>();
        interpretation.sheets().forEach(sheet -> sheets.put(sheet.name(), sheet));
        Map<String, String> firstSheet = new HashMap<>();
        Map<List<String>, Integer> pairs = new LinkedHashMap<>();
        for (GraphNode node : graph.nodes()) {
            Set<String> own = new LinkedHashSet<>();
            node.getRows().forEach(row -> own.add(row.sheet()));
            if (!own.isEmpty()) {
                firstSheet.put(node.getKey(), own.iterator().next());
            }
            List<String> ordered = new ArrayList<>(own);
            for (int first = 0; first < ordered.size(); first++) {
                for (int second = first + 1; second < ordered.size(); second++) {
                    pairs.merge(List.of(ordered.get(first), ordered.get(second)), 1, Integer::sum);
                }
            }
        }

        Map<Key, Integer> counts = new LinkedHashMap<>();
        for (GraphNode node : graph.nodes()) {
            for (Map.Entry<URI, List<GraphValue>> entry : node.getValues().entrySet()) {
                for (GraphValue value : entry.getValue()) {
                    if (value.isLink()) {
                        counts.merge(keyOf(value, entry.getKey(), sheets, firstSheet), 1, Integer::sum);
                    }
                }
            }
        }
        List<ColumnLink> columnLinks = new ArrayList<>();
        counts.forEach((key, count) -> columnLinks.add(new ColumnLink(key.sheet(), key.column(), key.property(),
                key.kind(), key.toSheet(), key.toClass(), count)));
        Map<String, Integer> position = new HashMap<>();
        for (int index = 0; index < interpretation.sheets().size(); index++) {
            position.put(interpretation.sheets().get(index).name(), index);
        }
        columnLinks.sort(Comparator.comparing((ColumnLink link) -> position.getOrDefault(link.sheet(), Integer.MAX_VALUE))
                .thenComparingInt(ColumnLink::column)
                .thenComparing(ColumnLink::kind));
        List<SharedResources> shared = new ArrayList<>();
        pairs.forEach((pair, count) -> shared.add(new SharedResources(pair.get(0), pair.get(1), count)));
        return new GraphLinks(columnLinks, shared);
    }

    private static Key keyOf(GraphValue value, URI property, Map<String, SheetInterpretation> sheets,
                             Map<String, String> firstSheet) {
        CellRef cell = value.cell();
        LinkTarget link = value.link();
        if (!value.isUsable()) {
            return new Key(cell.sheet(), cell.column(), property, Kind.UNRESOLVED, null, null);
        }
        if (link.nodeKey() != null && firstSheet.containsKey(link.nodeKey())) {
            return new Key(cell.sheet(), cell.column(), property, Kind.ROW, firstSheet.get(link.nodeKey()), null);
        }
        return new Key(cell.sheet(), cell.column(), property, Kind.INSTANCE, null, rangeOf(sheets.get(cell.sheet()),
                property));
    }

    /** The range the sheet's class gives the property, which the link was resolved against. */
    private static URI rangeOf(SheetInterpretation sheet, URI property) {
        if (sheet == null || sheet.classView() == null) {
            return null;
        }
        PropertyView view = sheet.classView().getProperty(property);
        return view == null ? null : view.range();
    }
}
