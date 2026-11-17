//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import java.net.URI;

/**
 * What a column of a sheet gives the graph, for the grid of the preview.
 *
 * @param index         its position, from 0
 * @param header        its header as written
 * @param role          what it gives
 * @param property      the property it holds, as a full URI; null when it holds none
 * @param propertyLabel the property's label in the user's language; null when it holds none
 * @author Arnaud Charleroy
 */
public record PreviewColumn(int index, String header, Role role, URI property, String propertyLabel) {

    /** What a column gives the graph. */
    public enum Role {
        /** It identifies the rows; it may also give a value. */
        IDENTIFIER,
        /** It gives the class of each row. */
        TYPE,
        /** It gives literal values. */
        VALUE,
        /** It gives links. */
        LINK,
        /** It gives nothing: its property is not chosen, or the class of its rows does not accept it. */
        LEFT_OUT
    }
}
