//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology.literal;

import org.opensilex.aiimport.report.ReportMessage;

import java.net.URI;

/**
 * Why a cell was refused, as messages the interface translates: the key goes under
 * {@code GraphImport.report.}, the parameters fill it in.
 *
 * @author Arnaud Charleroy
 */
final class Problems {

    private static final String VALUE = "value";

    private Problems() {
    }

    static ReportMessage notANumber(String value) {
        return ReportMessage.of("literal.notANumber", "\"" + value + "\" is not a number.").with(VALUE, value);
    }

    static ReportMessage twoSeparators(String value) {
        return ReportMessage.of("literal.twoSeparators",
                "\"" + value + "\" has both a comma and a dot: which one separates the decimals is unclear.")
                .with(VALUE, value);
    }

    static ReportMessage notAnInteger(String value) {
        return ReportMessage.of("literal.notAnInteger", "\"" + value + "\" is not a whole number.").with(VALUE, value);
    }

    static ReportMessage notABoolean(String value) {
        return ReportMessage.of("literal.notABoolean", "\"" + value + "\" is neither yes nor no.").with(VALUE, value);
    }

    static ReportMessage notADate(String value) {
        return ReportMessage.of("literal.notADate", "\"" + value + "\" is not a date.").with(VALUE, value);
    }

    static ReportMessage ambiguousDate(String value, String dayFirst, String monthFirst) {
        return ReportMessage.of("literal.ambiguousDate",
                "\"" + value + "\" may be " + dayFirst + " or " + monthFirst + ": choose the order of the column.")
                .with(VALUE, value).with("dayFirst", dayFirst).with("monthFirst", monthFirst);
    }

    static ReportMessage missingTime(String value) {
        return ReportMessage.of("literal.missingTime", "\"" + value + "\" has a date but no time.").with(VALUE, value);
    }

    static ReportMessage missingZone(String value) {
        return ReportMessage.of("literal.missingZone",
                "\"" + value + "\" has no time zone: choose the zone of the column.").with(VALUE, value);
    }

    static ReportMessage notAUri(String value) {
        return ReportMessage.of("literal.notAUri", "\"" + value + "\" is not a URI.").with(VALUE, value);
    }

    static ReportMessage invalid(String value, URI datatype) {
        return ReportMessage.of("literal.invalid", "\"" + value + "\" is not a valid " + datatype + ".")
                .with(VALUE, value).with("datatype", datatype);
    }
}
