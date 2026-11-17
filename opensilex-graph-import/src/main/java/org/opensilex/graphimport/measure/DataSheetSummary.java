//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.measure;

/**
 * What a sheet of data gives, counted.
 *
 * @param sheet        the sheet
 * @param ready        the data that could be created now
 * @param waiting      the data whose target is a row of the file still to create
 * @param notAvailable the cells written as not available ({@code NA}, {@code n/a}): no datum
 * @param refused      the cells that give no datum because of a problem
 * @author Arnaud Charleroy
 */
public record DataSheetSummary(String sheet, int ready, int waiting, int notAvailable, int refused) {
}
