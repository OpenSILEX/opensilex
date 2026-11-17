//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.interpret.InterpretationServices;

import java.util.List;

/**
 * Checks the graph of an import, its rows merged, before anything is compared with the instance. A
 * module adds a validator through {@link org.opensilex.graphimport.GraphImportExtension}.
 *
 * @author Arnaud Charleroy
 */
public interface GraphValidator {

    /** A stable name, which orders the validators so that the same file always gives the same report. */
    String id();

    /**
     * @param services the classes and their views, for the user's language
     * @return what is wrong, in any order
     */
    List<GraphProblem> validate(ImportGraph graph, InterpretationServices services) throws Exception;
}
