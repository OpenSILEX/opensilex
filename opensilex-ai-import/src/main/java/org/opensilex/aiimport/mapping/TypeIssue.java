//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.mapping;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * A value that will not go in as it stands, and what to do about it.
 * <p>
 * Nothing is rewritten on the user's behalf: the suggestion is shown, and the correction is theirs
 * to make. A silently converted measurement is a measurement nobody can trace.
 *
 * @author Arnaud Charleroy
 */
public class TypeIssue {

    private String sheet;
    private String column;

    /**
     * Row number as it appears in the spreadsheet, so the user can go and look.
     */
    private int rowNumber;

    private String value;

    /**
     * What is wrong, in one sentence.
     */
    private ReportMessage problem;

    /**
     * What would fix it, in one sentence.
     */
    private ReportMessage suggestion;

    public String getSheet() {
        return sheet;
    }

    public TypeIssue setSheet(String sheet) {
        this.sheet = sheet;
        return this;
    }

    public String getColumn() {
        return column;
    }

    public TypeIssue setColumn(String column) {
        this.column = column;
        return this;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public TypeIssue setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
        return this;
    }

    public String getValue() {
        return value;
    }

    public TypeIssue setValue(String value) {
        this.value = value;
        return this;
    }

    public String getProblem() {
        return problem == null ? null : problem.getEnglish();
    }

    public ReportMessage getProblemMessage() {
        return problem;
    }

    public TypeIssue setProblem(ReportMessage problem) {
        this.problem = problem;
        return this;
    }

    public TypeIssue setProblem(String problem) {
        this.problem = problem == null ? null : ReportMessage.plain(problem);
        return this;
    }

    public String getSuggestion() {
        return suggestion == null ? null : suggestion.getEnglish();
    }

    public ReportMessage getSuggestionMessage() {
        return suggestion;
    }

    public TypeIssue setSuggestion(ReportMessage suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    public TypeIssue setSuggestion(String suggestion) {
        this.suggestion = suggestion == null ? null : ReportMessage.plain(suggestion);
        return this;
    }
}
