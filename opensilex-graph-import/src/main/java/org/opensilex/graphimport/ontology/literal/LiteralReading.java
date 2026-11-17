//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology.literal;

import org.opensilex.aiimport.report.ReportMessage;

import java.net.URI;

/**
 * A cell read as a literal: its value in the datatype's canonical form, nothing for an empty cell,
 * or the reason it was refused.
 *
 * @param lexical  the value as the triplestore would store it; null when empty or refused
 * @param datatype the datatype it was read for
 * @param problem  why it was refused, as a translatable message; null otherwise
 * @author Arnaud Charleroy
 */
public record LiteralReading(String lexical, URI datatype, ReportMessage problem) {

    static LiteralReading value(String lexical, URI datatype) {
        return new LiteralReading(lexical, datatype, null);
    }

    static LiteralReading empty(URI datatype) {
        return new LiteralReading(null, datatype, null);
    }

    static LiteralReading refused(URI datatype, ReportMessage problem) {
        return new LiteralReading(null, datatype, problem);
    }

    public boolean isEmpty() {
        return lexical == null && problem == null;
    }

    public boolean isRefused() {
        return problem != null;
    }
}
