//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template.csvw;

/**
 * A document that is not a template this module can read, with the reason: refused rather than
 * half understood.
 *
 * @author Arnaud Charleroy
 */
public class CsvwException extends Exception {

    public CsvwException(String reason) {
        super(reason);
    }

    public CsvwException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
