//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import java.net.URI;

/**
 * One OWL restriction as read from the triplestore, before the restrictions of a class and of its
 * ancestors are combined.
 *
 * @param domain      the class the restriction hangs from
 * @param property    the restricted property
 * @param kind        literal or link: from {@code owl:onDataRange} or {@code owl:onClass}, else from the
 *                    property's own type; null when nothing tells
 * @param range       {@code owl:onDataRange} or {@code owl:onClass}, null when the restriction has neither
 * @param cardinality the bounds this restriction alone sets
 * @author Arnaud Charleroy
 */
public record RestrictionRow(URI domain, URI property, PropertyKind kind, URI range, Cardinality cardinality) {
}
