//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

/**
 * Whether a cell of a column is read as a literal or as a link to another node.
 *
 * @author Arnaud Charleroy
 */
public enum PropertyKind {
    /** A literal of the property's datatype. */
    DATA,
    /** A link to a node of the property's range class. */
    OBJECT
}
