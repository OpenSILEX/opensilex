//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.preview.NodeComparison;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Writes to the instance the resources of some classes that the preview shows as to create, through the
 * platform's own classes, so that the platform's rules apply. A module adds one through
 * {@link org.opensilex.graphimport.GraphImportExtension}.
 * <p>
 * A writer never leaves a value out silently: a node holding a property it cannot write is refused by
 * {@link #whyNot}, and stays to create.
 *
 * @author Arnaud Charleroy
 */
public interface ResourceWriter {

    /** The step of the creation this writer makes, stable, used by the screens and the API. */
    String step();

    /** The classes whose resources, subclasses included, this writer creates. */
    Set<URI> rootClasses();

    /** The platform credential a user needs to create these resources, unless an administrator. */
    String credentialId();

    /** The steps whose resources must exist before these can be created. */
    List<String> after();

    /**
     * Why this node cannot be written by this writer, whatever the instance holds; empty when it can.
     * Only what the node holds is looked at, not the instance.
     */
    Optional<ReportMessage> whyNot(NodeComparison node, WriteContext context);

    /**
     * Checks the nodes the way the platform would, without writing anything.
     *
     * @return what would stop the creation; empty when it would go through
     */
    List<WriteProblem> check(List<NodeComparison> nodes, WriteContext context) throws Exception;

    /**
     * Among the nodes ready to create, those the instance already holds though the preview cannot tell,
     * for resources it cannot find by name; they count as done. None, by default.
     */
    default Set<NodeComparison> existing(List<NodeComparison> nodes, WriteContext context) throws Exception {
        return Set.of();
    }

    /**
     * Writes the nodes, all or none.
     *
     * @return the URIs of the resources created
     */
    List<URI> write(List<NodeComparison> nodes, WriteContext context) throws Exception;
}
