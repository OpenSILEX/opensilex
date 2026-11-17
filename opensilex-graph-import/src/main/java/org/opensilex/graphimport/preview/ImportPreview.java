//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.measure.DataSheetSummary;
import org.opensilex.graphimport.validate.GraphProblem;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * What an import would do to the instance, worked out once and read by the screens a page at a time:
 * the resources compared, the columns of each sheet, the map of the sheets, and each cell's state.
 *
 * @author Arnaud Charleroy
 */
public final class ImportPreview {

    private final List<NodeComparison> nodes;
    private final PreviewCounts counts;
    private final Map<String, List<PreviewColumn>> columns;
    private final SheetMap map;
    private final int problemTotal;
    /** What each sheet of data gives: data are read apart from the resources. */
    private List<DataSheetSummary> data = List.of();
    private final Map<String, NavigableMap<Integer, Integer>> nodeByRow = new LinkedHashMap<>();
    private final Map<CellRef, ValueComparison> valueByCell = new HashMap<>();
    private final Map<CellRef, GraphProblem.Severity> severityByCell = new HashMap<>();

    /**
     * @param columns      the columns of each sheet, in the workbook's order
     * @param problemTotal how many problems the file has
     */
    public ImportPreview(List<NodeComparison> nodes, Map<String, List<PreviewColumn>> columns, SheetMap map,
                         int problemTotal) {
        this.nodes = List.copyOf(nodes);
        this.counts = PreviewCounts.of(this.nodes);
        this.columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
        this.map = map;
        this.problemTotal = problemTotal;
        for (NodeComparison node : this.nodes) {
            node.node().getRows().forEach(row ->
                    nodeByRow.computeIfAbsent(row.sheet(), sheet -> new TreeMap<>())
                            .putIfAbsent(row.row(), node.index()));
            node.values().forEach(value -> valueByCell.put(value.value().cell(), value));
            for (GraphProblem problem : node.problems()) {
                problem.cells().forEach(cell -> severityByCell.merge(cell, problem.severity(),
                        (one, other) -> one == GraphProblem.Severity.ERROR ? one : other));
            }
        }
    }

    /** The same preview, with what the sheets of data give. */
    public ImportPreview withData(List<DataSheetSummary> sheets) {
        data = List.copyOf(sheets);
        return this;
    }

    public List<DataSheetSummary> getData() {
        return data;
    }

    public List<NodeComparison> getNodes() {
        return nodes;
    }

    public Optional<NodeComparison> node(int index) {
        return index >= 0 && index < nodes.size() ? Optional.of(nodes.get(index)) : Optional.empty();
    }

    public PreviewCounts getCounts() {
        return counts;
    }

    /** The sheets that gave the graph rows, and those left out, in the workbook's order. */
    public Map<String, List<PreviewColumn>> getColumns() {
        return columns;
    }

    public SheetMap getMap() {
        return map;
    }

    public int getProblemTotal() {
        return problemTotal;
    }

    /** The rows of a sheet that are a resource, by row, with that resource's index; empty for a sheet left out. */
    public NavigableMap<Integer, Integer> rowsOf(String sheet) {
        return Collections.unmodifiableNavigableMap(nodeByRow.getOrDefault(sheet, new TreeMap<>()));
    }

    /** The value a cell gives, compared; empty for a cell that gives none. */
    public Optional<ValueComparison> valueAt(CellRef cell) {
        return Optional.ofNullable(valueByCell.get(cell));
    }

    /** The worst problem on a cell; empty when it has none. */
    public Optional<GraphProblem.Severity> severityAt(CellRef cell) {
        return Optional.ofNullable(severityByCell.get(cell));
    }
}
