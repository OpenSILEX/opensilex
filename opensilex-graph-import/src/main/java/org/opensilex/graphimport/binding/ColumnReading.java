//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.binding;

import org.opensilex.graphimport.ontology.literal.DateOrder;

import java.time.ZoneOffset;

/**
 * How the user said the dates of a column are written, where the cells cannot tell.
 *
 * @param dateOrder the order of day and month; null to keep the order the values show
 * @param zone      the offset of the times written without one; null to refuse them
 * @author Arnaud Charleroy
 */
public record ColumnReading(DateOrder dateOrder, ZoneOffset zone) {

    /** Nothing said: the values decide. */
    public static final ColumnReading NONE = new ColumnReading(null, null);

    /** True when it says nothing the values would not. */
    public boolean isNone() {
        return (dateOrder == null || dateOrder == DateOrder.UNDECIDED) && zone == null;
    }
}
