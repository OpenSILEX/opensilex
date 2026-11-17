//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.validate.GraphProblem;
import org.opensilex.graphimport.validate.OwlRestrictionGraphValidator;

import java.util.List;

/**
 * Works out the preview of an import from its graph: the resources compared with the instance, the
 * columns of each sheet, the map of the sheets. Nothing is written.
 *
 * @author Arnaud Charleroy
 */
public class PreviewBuilder {

    private final InterpretationServices services;

    public PreviewBuilder(InterpretationServices services) {
        this.services = services;
    }

    /**
     * @param problems every problem the validators found on the graph; those about a required value are
     *                 replaced by what the comparison finds, and not counted
     */
    public ImportPreview build(Interpretation interpretation, ImportGraph graph, List<GraphProblem> problems)
            throws Exception {
        int counted = (int) problems.stream()
                .filter(problem -> !OwlRestrictionGraphValidator.REQUIRED_MISSING.equals(problem.message().getKey()))
                .count();
        return new ImportPreview(new InstanceComparator(services).compare(graph, problems),
                PreviewColumns.of(interpretation, services), new SheetMapBuilder(services).build(interpretation, graph),
                counted);
    }
}
