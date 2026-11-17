//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import java.net.URI;
import java.util.Set;

/**
 * A property a class accepts, as its restrictions describe it.
 *
 * @param uri         the property, as a full URI
 * @param kind        literal or link
 * @param range       the datatype of a literal, or the class of a linked node
 * @param cardinality how many values a node may hold
 * @param declaredOn  the classes whose restrictions gave the cardinality: the class itself, or the
 *                    nearest ancestors that restrict the property
 * @param labels      the property's labels
 * @author Arnaud Charleroy
 */
public record PropertyView(
        URI uri,
        PropertyKind kind,
        URI range,
        Cardinality cardinality,
        Set<URI> declaredOn,
        Labels labels) {

    public PropertyView {
        declaredOn = Set.copyOf(declaredOn);
    }
}
