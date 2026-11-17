//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.graphimport.preview.NodeComparison;
import org.opensilex.graphimport.preview.ValueComparison;
import org.opensilex.graphimport.preview.ValueState;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The values of a resource to create, as a writer reads them: the literals as the file gives them, read
 * for their datatype, and the links as the resources they point to.
 *
 * @author Arnaud Charleroy
 */
public final class NodeValues {

    private final NodeComparison node;

    private NodeValues(NodeComparison node) {
        this.node = node;
    }

    public static NodeValues of(NodeComparison node) {
        return new NodeValues(node);
    }

    /** The properties the resource has a value for, in the order of the file. */
    public Set<URI> properties() {
        Set<URI> properties = new LinkedHashSet<>();
        written().forEach(value -> properties.add(value.property()));
        return properties;
    }

    /** The first literal of the property, in the form its datatype is written in. */
    public Optional<String> literal(URI property) {
        return literals(property).stream().findFirst();
    }

    public List<String> literals(URI property) {
        List<String> literals = new ArrayList<>();
        written().stream()
                .filter(value -> value.property().equals(property) && !value.value().isLink())
                .forEach(value -> literals.add(value.value().literal().lexical()));
        return literals;
    }

    /** The resources that exist already the property points to. */
    public List<URI> targets(URI property) {
        return written().stream()
                .filter(value -> value.property().equals(property) && value.target() != null)
                .map(ValueComparison::target)
                .toList();
    }

    /** The keys of the rows of the file still to create the property points to. */
    public List<String> rows(URI property) {
        return written().stream()
                .filter(value -> value.property().equals(property) && value.target() == null && value.value().isLink()
                        && value.value().link().nodeKey() != null)
                .map(value -> value.value().link().nodeKey())
                .toList();
    }

    /** The keys of the rows still to create the resource points to, whatever the property. */
    public List<String> rowsPointedTo() {
        return written().stream()
                .filter(value -> value.target() == null && value.value().isLink() && value.value().link().nodeKey() != null)
                .map(value -> value.value().link().nodeKey())
                .distinct()
                .toList();
    }

    /** The values the resource would be created with: those that read, new to the instance. */
    private List<ValueComparison> written() {
        return node.values().stream().filter(value -> value.state() == ValueState.NEW).toList();
    }
}
