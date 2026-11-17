//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.opensilex.aiimport.report.ReportMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The resources an import describes, merged across sheets, before anything is compared with the
 * instance.
 *
 * @param nodes the resources, in the order their first row comes in the workbook
 * @param notes       what was left out of the graph and why: a sheet without class, undecided columns,
 *                    rows without identifier
 * @param dateColumns the columns of dates, with how their cells were read
 * @author Arnaud Charleroy
 */
public record ImportGraph(List<GraphNode> nodes, List<ReportMessage> notes, List<DateColumn> dateColumns) {

    public ImportGraph {
        nodes = List.copyOf(nodes);
        notes = List.copyOf(notes);
        dateColumns = List.copyOf(dateColumns);
    }

    /** The resources by key. */
    public Map<String, GraphNode> byKey() {
        Map<String, GraphNode> byKey = new LinkedHashMap<>();
        nodes.forEach(node -> byKey.put(node.getKey(), node));
        return byKey;
    }

    /** How many values the resources hold, links included. */
    public int valueCount() {
        return nodes.stream().mapToInt(node -> node.getValues().values().stream().mapToInt(List::size).sum()).sum();
    }

    /** How many of those values are links. */
    public int linkCount() {
        return (int) nodes.stream()
                .flatMap(node -> node.getValues().values().stream())
                .flatMap(List::stream)
                .filter(GraphValue::isLink)
                .count();
    }
}
