//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology.literal;

import java.time.ZoneOffset;

/**
 * What a column, or its workbook, tells about how to read its cells.
 *
 * @param date1904  true when the workbook counts its date serials from 1904
 * @param dateOrder the order of day and month in dates written with slashes
 * @param zone      the offset of dates and times written without one; null when nobody chose it, in
 *                  which case such a value is refused rather than placed in an arbitrary zone
 * @author Arnaud Charleroy
 */
public record ReadingContext(boolean date1904, DateOrder dateOrder, ZoneOffset zone) {

    /** A column nothing is known about. */
    public static final ReadingContext UNKNOWN = new ReadingContext(false, DateOrder.UNDECIDED, null);
}
