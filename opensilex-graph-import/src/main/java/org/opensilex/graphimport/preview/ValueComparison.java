//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.graphimport.graph.GraphValue;

import java.net.URI;
import java.util.List;

/**
 * A value of the file, compared with the resource it goes to.
 *
 * @param property the property, as a full URI
 * @param value    the value and the cell it comes from
 * @param state    what the import would do with it
 * @param target   for a link, the resource it points to, as a full URI; null for a literal, or for a row
 *                 of the file that would be created
 * @param held     what the resource holds instead, for a value in conflict; empty otherwise
 * @author Arnaud Charleroy
 */
public record ValueComparison(URI property, GraphValue value, ValueState state, URI target, List<String> held) {

    public ValueComparison {
        held = List.copyOf(held);
    }
}
