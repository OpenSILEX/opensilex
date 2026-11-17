//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The class a resource holds once its rows are merged: the most specific of the classes its sheets
 * give it, every other one being an ancestor of it.
 *
 * @author Arnaud Charleroy
 */
public final class NodeClasses {

    private NodeClasses() {
    }

    /** Empty when two of its classes are unrelated. */
    public static Optional<URI> mostSpecific(GraphNode node, ClassCatalog catalog) {
        for (URI candidate : node.getClasses()) {
            if (node.getClasses().stream().allMatch(other -> catalog.isA(candidate, other))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** The property as the resource's class sees it; null when its classes conflict or it does not accept it. */
    public static PropertyView property(GraphNode node, URI property, InterpretationServices services)
            throws Exception {
        Optional<URI> classUri = mostSpecific(node, services.catalog());
        return classUri.isEmpty() ? null : services.views().get(classUri.get()).getProperty(property);
    }

    /** The labels of the classes, in the user's language, joined. */
    public static String labels(Collection<URI> classes, InterpretationServices services) {
        return classes.stream()
                .map(classUri -> services.catalog().labelOf(classUri, services.language()))
                .collect(Collectors.joining(", "));
    }

    /** The label of a property in the user's language, else its local name. */
    public static String label(PropertyView property, InterpretationServices services) {
        String label = property.labels().in(services.language());
        return label != null ? label : ClassCatalog.localName(property.uri());
    }
}
