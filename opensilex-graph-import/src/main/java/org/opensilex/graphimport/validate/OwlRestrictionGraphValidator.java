//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.graph.GraphValue;
import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The OWL restrictions of the ontology, checked on each resource once its rows are merged, so that a
 * value a sheet lacks may come from another sheet:
 * <ul>
 *     <li>the classes the sheets give a resource must descend from one another;</li>
 *     <li>a property takes at most its maximum of different values: two sheets giving one single-valued
 *     property two different values is a conflict, shown on both cells, the same value twice is not;</li>
 *     <li>a property takes at least its minimum, which only warns: the resource may already exist and
 *     hold the value, which the preview checks.</li>
 * </ul>
 * Values are compared once read, so {@code 1,5} and {@code 1.5} are one value. Language-tagged values
 * count apart, as OWL counts them: {@code label@fr} and {@code label@en} are two values of a property
 * that takes one.
 *
 * @author Arnaud Charleroy
 */
public class OwlRestrictionGraphValidator implements GraphValidator {

    public static final String ID = "owl";

    /**
     * The key of the warning for a required value the file lacks: the preview replaces it with what it
     * finds, the value being held already by the existing resource or missing from a new one.
     */
    public static final String REQUIRED_MISSING = ProblemMessages.REQUIRED_MISSING;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<GraphProblem> validate(ImportGraph graph, InterpretationServices services) throws Exception {
        List<GraphProblem> problems = new ArrayList<>();
        for (GraphNode node : graph.nodes()) {
            Optional<URI> classUri = NodeClasses.mostSpecific(node, services.catalog());
            if (classUri.isEmpty()) {
                problems.add(classConflict(node, services));
            } else {
                checkCardinalities(node, services.views().get(classUri.get()), services, problems);
            }
        }
        return problems;
    }

    private static GraphProblem classConflict(GraphNode node, InterpretationServices services) {
        List<CellRef> cells = new ArrayList<>();
        node.getClasses().forEach(classUri -> cells.addAll(node.rowsOfClass(classUri)));
        return GraphProblem.ofCells(GraphProblem.Severity.ERROR,
                ProblemMessages.classConflict(NodeClasses.labels(node.getClasses(), services)), node, cells, true);
    }

    private static void checkCardinalities(GraphNode node, ClassView view, InterpretationServices services,
                                           List<GraphProblem> problems) {
        Map<URI, List<GraphValue>> values = node.getValues();
        for (Map.Entry<URI, List<GraphValue>> entry : values.entrySet()) {
            PropertyView property = view.getProperty(entry.getKey());
            Integer max = property == null ? null : property.cardinality().max();
            Set<String> distinct = distinct(entry.getValue());
            if (max != null && distinct.size() > max) {
                List<CellRef> cells = entry.getValue().stream()
                        .filter(GraphValue::isUsable)
                        .map(GraphValue::cell)
                        .toList();
                problems.add(GraphProblem.ofCells(GraphProblem.Severity.ERROR,
                        ProblemMessages.tooManyValues(NodeClasses.label(property, services), max, distinct.size()),
                        node, cells, false));
            }
        }
        for (PropertyView property : view.getProperties()) {
            int min = property.cardinality().min();
            int count = distinct(values.getOrDefault(property.uri(), List.of())).size();
            if (count < min) {
                problems.add(GraphProblem.ofCells(GraphProblem.Severity.WARNING,
                        ProblemMessages.requiredMissing(NodeClasses.label(property, services), min, count), node,
                        node.getRows(), true));
            }
        }
    }

    /** The different values a property holds, the cells that give none left out. */
    private static Set<String> distinct(List<GraphValue> values) {
        Set<String> identities = new HashSet<>();
        for (GraphValue value : values) {
            String identity = value.identity();
            if (identity != null) {
                identities.add(identity);
            }
        }
        return identities;
    }
}
