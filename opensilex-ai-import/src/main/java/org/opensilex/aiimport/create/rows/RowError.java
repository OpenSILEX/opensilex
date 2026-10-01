//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.rows;

import org.opensilex.aiimport.create.UnresolvedRow;
import org.opensilex.aiimport.report.ReportMessage;

/**
 * One thing wrong with one row of the user's workbook, said where they will look for it.
 * <p>
 * Whatever found the problem — the platform's CSV importers, its data import, or this module's own
 * resolution — the error is brought back to the sheet, the row number the spreadsheet shows, and
 * the column header as the file writes it. The CSV the platform actually validated is an
 * intermediate the user never sees; an error pointing at its line 57 would send them looking for
 * a line that does not exist in their file.
 *
 * @author Arnaud Charleroy
 */
public class RowError {

    /**
     * The families the interface counts and filters on. Deliberately few: the platform reports
     * about twenty kinds of problem, and a user needs to know whether a value is missing, wrong,
     * unknown or already there — not which internal check noticed it.
     */
    public enum Kind {
        MISSING_VALUE,
        INVALID_VALUE,
        INVALID_DATATYPE,
        INVALID_DATE,
        INVALID_URI,
        UNKNOWN_REFERENCE,
        ALREADY_EXISTS,
        DUPLICATE_IN_FILE,
        DUPLICATE_IN_INSTANCE,
        INVALID_ROW,
        UNRESOLVED,
        /**
         * About the file as a whole rather than one row — a limit reached, a header the platform
         * refused. Carried with no sheet and row 0.
         */
        FILE
    }

    private final String sheet;
    private final int row;
    private final String column;
    private final String value;
    private final Kind kind;
    private final ReportMessage message;

    public RowError(String sheet, int row, String column, String value, Kind kind,
                    ReportMessage message) {
        this.sheet = sheet;
        this.row = row;
        this.column = column;
        this.value = value;
        this.kind = kind;
        this.message = message;
    }

    /**
     * An error about the whole file, not tied to a row.
     */
    public static RowError ofFile(ReportMessage message) {
        return new RowError(null, 0, null, null, Kind.FILE, message);
    }

    /**
     * The module's own refusals — a target or a variable that did not resolve — told in the same
     * terms as the platform's, so the grid shows both the same way.
     */
    public static RowError fromUnresolved(UnresolvedRow unresolved) {
        return new RowError(unresolved.getSheet(), unresolved.getRowNumber(),
                unresolved.getColumn(), unresolved.getValue(), Kind.UNRESOLVED,
                ReportMessage.of(unresolved.getReasonKey(),
                                "Row " + unresolved.getRowNumber() + " of " + unresolved.getSheet()
                                        + ": '" + unresolved.getValue() + "' could not be placed.")
                        .with("value", unresolved.getValue()));
    }

    public String getSheet() {
        return sheet;
    }

    public int getRow() {
        return row;
    }

    public String getColumn() {
        return column;
    }

    public String getValue() {
        return value;
    }

    public Kind getKind() {
        return kind;
    }

    public ReportMessage getMessage() {
        return message;
    }

    public boolean isAboutTheFile() {
        return kind == Kind.FILE || sheet == null;
    }
}
