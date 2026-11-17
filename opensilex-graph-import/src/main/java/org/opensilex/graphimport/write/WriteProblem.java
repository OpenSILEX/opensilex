//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.preview.NodeComparison;

/**
 * Why a resource cannot be created, or why the platform would refuse it.
 *
 * @param node    the index of the resource in the preview; -1 for a problem of the whole step
 * @param name    its name, as the files give it
 * @param message what is wrong, translatable
 * @author Arnaud Charleroy
 */
public record WriteProblem(int node, String name, ReportMessage message) {

    public static WriteProblem of(NodeComparison node, ReportMessage message) {
        return new WriteProblem(node.index(), node.node().getName(), message);
    }

    public static WriteProblem ofStep(ReportMessage message) {
        return new WriteProblem(-1, null, message);
    }
}
