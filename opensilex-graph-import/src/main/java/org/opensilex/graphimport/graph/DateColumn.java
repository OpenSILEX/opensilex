//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.opensilex.graphimport.binding.ColumnReading;
import org.opensilex.graphimport.ontology.literal.DateOrder;

import java.net.URI;

/**
 * A column of dates, and how its cells are read: what the user said, else what its values show.
 *
 * @param sheet    the sheet, as the import names it
 * @param column   the column, from 0
 * @param header   its header as written
 * @param datatype {@code xsd:date} or {@code xsd:dateTime}
 * @param detected the order of day and month its values show
 * @param chosen   what the user said
 * @author Arnaud Charleroy
 */
public record DateColumn(String sheet, int column, String header, URI datatype, DateOrder detected,
                         ColumnReading chosen) {

    /** The order the cells are read with. */
    public DateOrder effectiveOrder() {
        return chosen.dateOrder() == null || chosen.dateOrder() == DateOrder.UNDECIDED ? detected : chosen.dateOrder();
    }
}
