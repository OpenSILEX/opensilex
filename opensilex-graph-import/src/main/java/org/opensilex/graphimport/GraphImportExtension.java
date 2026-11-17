//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport;

import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.validate.GraphValidator;
import org.opensilex.graphimport.write.ResourceWriter;

import java.util.List;

/**
 * What a module brings to the experimental import assistant. The assistant reads every module that
 * implements it, its own included: a new kind of evidence or a new class of instances to recognise
 * needs no change in the assistant.
 *
 * @author Arnaud Charleroy
 */
public interface GraphImportExtension {

    /** Sources of evidence about the property a column holds. */
    default List<EvidenceSource> getEvidenceSources() {
        return List.of();
    }

    /** Classes whose existing instances may be recognised by name, with their reading rules. */
    default List<InstanceScope> getInstanceScopes() {
        return List.of();
    }

    /** Checks of the graph of an import, its rows merged. */
    default List<GraphValidator> getGraphValidators() {
        return List.of();
    }

    /** Writers of the resources of some classes, for the creation step, in the order they run. */
    default List<ResourceWriter> getResourceWriters() {
        return List.of();
    }
}
