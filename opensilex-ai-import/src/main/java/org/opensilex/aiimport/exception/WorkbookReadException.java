//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.exception;

/**
 * Raised when an uploaded file cannot be opened as a spreadsheet.
 *
 * @author Arnaud Charleroy
 */
public class WorkbookReadException extends Exception {

    private final String fileName;

    public WorkbookReadException(String fileName, Throwable cause) {
        super("Could not read " + fileName + " as a spreadsheet", cause);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
