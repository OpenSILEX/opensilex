//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.graphimport.ontology.HeaderReading;

import java.net.URI;

/**
 * What a column holds.
 *
 * @param index      its position, from 0
 * @param header     its header as written
 * @param reading    what the header says by its form
 * @param identifier true when it identifies the rows of its sheet
 * @param typeColumn true when it gives the class of each row rather than a property
 * @param property   the property it holds; undecided for a type column
 * @author Arnaud Charleroy
 */
public record ColumnInterpretation(
        int index,
        String header,
        HeaderReading reading,
        boolean identifier,
        boolean typeColumn,
        Choice<URI> property) {

    public ColumnInterpretation withProperty(Choice<URI> newProperty) {
        return new ColumnInterpretation(index, header, reading, identifier, typeColumn, newProperty);
    }
}
