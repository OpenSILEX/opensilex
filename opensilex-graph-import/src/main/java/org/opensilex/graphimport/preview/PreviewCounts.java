//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import java.util.List;

/**
 * How many resources the import would create, complete, leave as they are, or cannot settle.
 *
 * @param created   resources nothing in the instance answers to
 * @param completed existing resources the file gives new values
 * @param unchanged existing resources that hold every value already
 * @param conflict  resources the file and the instance disagree on
 * @param toFix     resources the file must be corrected for, whatever their state
 * @author Arnaud Charleroy
 */
public record PreviewCounts(int created, int completed, int unchanged, int conflict, int toFix) {

    public static PreviewCounts of(List<NodeComparison> nodes) {
        int[] byState = new int[NodeState.values().length];
        int toFix = 0;
        for (NodeComparison node : nodes) {
            byState[node.state().ordinal()]++;
            if (node.toFix()) {
                toFix++;
            }
        }
        return new PreviewCounts(byState[NodeState.CREATED.ordinal()], byState[NodeState.COMPLETED.ordinal()],
                byState[NodeState.UNCHANGED.ordinal()], byState[NodeState.CONFLICT.ordinal()], toFix);
    }

    public int total() {
        return created + completed + unchanged + conflict;
    }
}
