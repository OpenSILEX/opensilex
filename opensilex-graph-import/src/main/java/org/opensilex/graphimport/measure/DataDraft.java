//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.measure;

import org.opensilex.graphimport.graph.CellRef;

import java.net.URI;

/**
 * One datum a sheet of data gives: a value of a variable, measured on a target at a date.
 *
 * @param cell      the cell of the value
 * @param target    the resource the value was measured on, when it exists already
 * @param targetKey the row of the file it was measured on, when that row is still to create
 * @param date      the date, as the platform reads it: a day ({@code 2026-05-03}) or a date and time with
 *                  its offset
 * @param variable  the variable measured
 * @param value     the value, read for the variable's datatype
 * @author Arnaud Charleroy
 */
public record DataDraft(CellRef cell, URI target, String targetKey, String date, URI variable, String value) {

    /** Whether the target is a row of the file still to create, so the datum waits for it. */
    public boolean waits() {
        return target == null && targetKey != null;
    }
}
