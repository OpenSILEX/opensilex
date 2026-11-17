//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * How many problems of one kind a column, or the rows of a sheet, have: one wrong column gives one
 * problem per row, so the counts are kept whole when the list of problems is cut.
 *
 * @param sheet    the sheet, as the import names it
 * @param column   the column, from 0; null for problems about whole rows
 * @param severity whether they stop the import
 * @param count    how many there are
 * @param example  the first of them, to show what they are
 * @author Arnaud Charleroy
 */
public record ProblemCount(String sheet, Integer column, GraphProblem.Severity severity, int count,
                           ReportMessage example) {
}
