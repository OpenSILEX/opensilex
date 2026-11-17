//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * A piece of evidence about one target: a property for a column, a class for a sheet, a column for
 * the identifier of a sheet.
 *
 * @param target   what the evidence is about
 * @param evidence the evidence
 * @param <T>      the kind of target
 * @author Arnaud Charleroy
 */
public record Clue<T>(T target, Evidence evidence) {

    public static <T> Clue<T> of(T target, String source, double weight, ReportMessage reason) {
        return new Clue<>(target, new Evidence(source, weight, reason));
    }
}
