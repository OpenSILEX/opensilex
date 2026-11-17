//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * Why a resource is in conflict with the instance as a whole: the key goes under
 * {@code GraphImport.report.}, the parameters fill it in, the English text is the fallback.
 *
 * @author Arnaud Charleroy
 */
final class PreviewMessages {

    private static final String CLASSES = "classes";

    private PreviewMessages() {
    }

    static ReportMessage ambiguousMatch(String name, int count) {
        return ReportMessage.of("preview.ambiguousMatch",
                        "\"" + name + "\" names " + count + " existing resources of its class: which one the file "
                                + "completes is unclear.")
                .with("name", name).with("count", count);
    }

    static ReportMessage classMismatch(String uri, String classes, String expected) {
        return ReportMessage.of("preview.classMismatch",
                        "The existing resource " + uri + " is of class " + classes + ", where the file says " + expected
                                + ".")
                .with("uri", uri).with(CLASSES, classes).with("expected", expected);
    }

    static ReportMessage fileClasses(String classes) {
        return ReportMessage.of("preview.fileClasses",
                "The sheets give this resource unrelated classes: " + classes + ".").with(CLASSES, classes);
    }
}
