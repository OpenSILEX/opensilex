//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * The sentences problems are shown with: the key goes under {@code GraphImport.report.}, the
 * parameters fill it in, the English text is the fallback.
 *
 * @author Arnaud Charleroy
 */
final class ProblemMessages {

    private static final String VALUE = "value";
    private static final String PROPERTY = "property";
    private static final String COUNT = "count";

    /** The key of {@link #requiredMissing}, which the preview settles once it knows what exists. */
    static final String REQUIRED_MISSING = "problem.requiredMissing";

    private ProblemMessages() {
    }

    static ReportMessage linkUnfound(String value) {
        return ReportMessage.of("problem.linkUnfound",
                        "\"" + value + "\" names nothing known: no row of the file, no instance you may see.")
                .with(VALUE, value);
    }

    static ReportMessage linkAmbiguous(String value, int count) {
        return ReportMessage.of("problem.linkAmbiguous",
                        "\"" + value + "\" names " + count + " existing instances: which one is meant is unclear.")
                .with(VALUE, value).with(COUNT, count);
    }

    static ReportMessage linkWrongClass(String value, String classes, String expected) {
        return ReportMessage.of("problem.linkWrongClass",
                        "\"" + value + "\" is a row of the file of class " + classes + ", where " + expected
                                + " is expected.")
                .with(VALUE, value).with("classes", classes).with("expected", expected);
    }

    static ReportMessage classConflict(String classes) {
        return ReportMessage.of("problem.classConflict",
                "The sheets give this resource unrelated classes: " + classes + ".").with("classes", classes);
    }

    static ReportMessage tooManyValues(String property, int max, int count) {
        return ReportMessage.of("problem.tooManyValues",
                        count + " different values for " + property + ", which takes at most " + max + ".")
                .with(PROPERTY, property).with("max", max).with(COUNT, count);
    }

    static ReportMessage requiredMissing(String property, int min, int count) {
        return ReportMessage.of(REQUIRED_MISSING,
                        property + " needs at least " + min + " value(s), the file gives " + count
                                + ": required unless the resource already exists.")
                .with(PROPERTY, property).with("min", min).with(COUNT, count);
    }
}
