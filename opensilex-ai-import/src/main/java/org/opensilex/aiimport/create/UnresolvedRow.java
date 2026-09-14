//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

/**
 * An observation that cannot be written, and why.
 * <p>
 * Named down to the sheet and the row as the spreadsheet numbers it, because the user will go and
 * look. A count alone would say something is wrong without saying where.
 *
 * @author Arnaud Charleroy
 */
public class UnresolvedRow {

    private final String sheet;
    private final int rowNumber;
    private final String column;

    /**
     * Translation key naming what could not be resolved.
     */
    private final String reasonKey;

    /**
     * The value that resolved to nothing, quoted back so the user recognises it.
     */
    private final String value;

    public UnresolvedRow(String sheet, int rowNumber, String column, String reasonKey, String value) {
        this.sheet = sheet;
        this.rowNumber = rowNumber;
        this.column = column;
        this.reasonKey = reasonKey;
        this.value = value;
    }

    public String getSheet() {
        return sheet;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getColumn() {
        return column;
    }

    public String getReasonKey() {
        return reasonKey;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return sheet + " row " + rowNumber + " (" + column + " = " + value + ")";
    }
}
