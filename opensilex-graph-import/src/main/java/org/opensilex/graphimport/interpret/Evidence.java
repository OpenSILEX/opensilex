//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * One reason to believe, or to doubt, a suggestion: shown to the user as it is, with its weight.
 *
 * @param source the id of the source that found it
 * @param weight how much it counts: positive for, negative against
 * @param reason the sentence the user reads, translatable
 * @author Arnaud Charleroy
 */
public record Evidence(String source, double weight, ReportMessage reason) {
}
