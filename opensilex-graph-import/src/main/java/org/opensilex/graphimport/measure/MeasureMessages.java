//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.measure;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * What the reading of a sheet of data tells the user, translatable under {@code GraphImport.report.measure.}.
 *
 * @author Arnaud Charleroy
 */
final class MeasureMessages {

    private MeasureMessages() {
    }

    static ReportMessage targetUnfound(String name) {
        return ReportMessage.of("measure.targetUnfound", "\"" + name + "\" is no scientific object of the file or of "
                + "the experiment.").with("name", name);
    }

    static ReportMessage targetAmbiguous(String name, int count) {
        return ReportMessage.of("measure.targetAmbiguous", "\"" + name + "\" names " + count + " scientific objects.")
                .with("name", name).with("count", count);
    }

    static ReportMessage noDate() {
        return ReportMessage.of("measure.noDate", "This row gives no date: its values give no datum.");
    }

    static ReportMessage variableUnfound(String name) {
        return ReportMessage.of("measure.variableUnfound", "\"" + name + "\" names no variable you may see.")
                .with("name", name);
    }

    static ReportMessage duplicate() {
        return ReportMessage.of("measure.duplicate",
                "Another cell gives a value of the same variable, for the same target, at the same date.");
    }
}
