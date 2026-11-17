//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology.literal;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * A value read from a cell, or the reason it could not be.
 *
 * @author Arnaud Charleroy
 */
record ReadResult<T>(T value, ReportMessage problem) {

    static <T> ReadResult<T> of(T value) {
        return new ReadResult<>(value, null);
    }

    static <T> ReadResult<T> refused(ReportMessage problem) {
        return new ReadResult<>(null, problem);
    }

    boolean isRefused() {
        return problem != null;
    }
}
