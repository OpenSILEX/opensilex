//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.graph.GraphValue;
import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.graph.LinkTarget;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cells that give no value: a literal the datatype refuses, a link nothing answers to, or that names
 * several instances, or a row of the wrong class.
 *
 * @author Arnaud Charleroy
 */
public class CellValueValidator implements GraphValidator {

    public static final String ID = "cells";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<GraphProblem> validate(ImportGraph graph, InterpretationServices services) throws Exception {
        Map<String, GraphNode> nodes = graph.byKey();
        List<GraphProblem> problems = new ArrayList<>();
        for (GraphNode node : graph.nodes()) {
            for (Map.Entry<URI, List<GraphValue>> values : node.getValues().entrySet()) {
                for (GraphValue value : values.getValue()) {
                    if (!value.isUsable()) {
                        problems.add(problem(node, values.getKey(), value, nodes, services));
                    }
                }
            }
        }
        return problems;
    }

    private static GraphProblem problem(GraphNode node, URI property, GraphValue value, Map<String, GraphNode> nodes,
                                        InterpretationServices services) throws Exception {
        if (!value.isLink()) {
            return GraphProblem.ofValue(GraphProblem.Severity.ERROR, value.literal().problem(), node, value.cell(),
                    value.raw(), List.of());
        }
        LinkTarget link = value.link();
        ReportMessage message = switch (link.status()) {
            case AMBIGUOUS -> ProblemMessages.linkAmbiguous(value.raw(), link.instances().size());
            case WRONG_CLASS -> ProblemMessages.linkWrongClass(value.raw(),
                    NodeClasses.labels(nodes.get(link.nodeKey()).getClasses(), services),
                    expected(node, property, services));
            default -> ProblemMessages.linkUnfound(value.raw());
        };
        return GraphProblem.ofValue(GraphProblem.Severity.ERROR, message, node, value.cell(), value.raw(),
                link.suggestions());
    }

    private static String expected(GraphNode node, URI property, InterpretationServices services) throws Exception {
        PropertyView view = NodeClasses.property(node, property, services);
        if (view == null || view.range() == null) {
            return "?";
        }
        return services.catalog().labelOf(view.range(), services.language());
    }
}
