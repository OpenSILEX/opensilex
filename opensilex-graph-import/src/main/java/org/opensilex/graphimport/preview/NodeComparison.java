//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.validate.GraphProblem;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * A resource of the import, compared with the instance.
 *
 * @param index      its place in the import, from 0, which the screens use to ask for it
 * @param node       the resource and the rows it comes from
 * @param classUri   the most specific of its classes; null when its sheets give unrelated ones
 * @param state      what the import would do to it
 * @param existing   the existing resource it is, as a full URI; null when none is
 * @param candidates the existing resources its name designates, when it designates several
 * @param reason     why it is in conflict as a whole; null otherwise
 * @param values     its values, compared one by one
 * @param missing    the labels of the properties it needs and has no value for, the instance's included
 * @param problems   the problems of the file about it, a missing value excepted
 * @param toFix      true when the file must be corrected before it could be written: an error, an
 *                   ambiguous link, or a required value a new resource lacks
 * @author Arnaud Charleroy
 */
public record NodeComparison(int index, GraphNode node, URI classUri, NodeState state, URI existing,
                             Set<URI> candidates, ReportMessage reason, List<ValueComparison> values,
                             List<String> missing, List<GraphProblem> problems, boolean toFix) {

    public NodeComparison {
        candidates = Set.copyOf(candidates);
        values = List.copyOf(values);
        missing = List.copyOf(missing);
        problems = List.copyOf(problems);
    }
}
