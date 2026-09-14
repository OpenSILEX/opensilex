//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.mapping;

import org.opensilex.aiimport.report.ReportMessage;

import org.opensilex.aiimport.resolve.ResolutionStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * One column of the file, and everything known about it: what it stands for, which resource it
 * matches, what the cells actually hold, and where the two disagree.
 * <p>
 * Keyed by column name and role rather than by sheet. A variable observed at five phenological
 * stages is one variable: its role, its matched resource and its expected type do not change
 * between sheets, only the offending cells do, and those carry their own sheet. Keeping one entry
 * per sheet meant repeating the same five facts five times, to the user and to the language model
 * alike.
 *
 * @author Arnaud Charleroy
 */
public class ColumnMapping {

    /**
     * The sheets this column appears in, in reading order.
     */
    private List<String> sheets = new ArrayList<>();

    private String column;

    private ColumnRole role = ColumnRole.UNKNOWN;

    /**
     * The matching resource, when the column names one and it was found. Comes from the resolver.
     */
    private URI resolvedUri;
    private String resolvedName;
    private ResolutionStatus resolutionStatus;

    /**
     * The datatype the matched variable expects, when there is one.
     */
    private String expectedDatatype;

    /**
     * What the cells actually hold.
     */
    private ValueKind observedKind = ValueKind.EMPTY;

    private int valueCount;
    private int missingCount;

    /**
     * A few distinct values, so the user recognises the column at a glance.
     */
    private List<String> sampleValues = new ArrayList<>();

    /**
     * Cells that will not go in as they stand.
     */
    private List<TypeIssue> issues = new ArrayList<>();

    /**
     * What to do about the column as a whole, when the issues share one cause.
     */
    private ReportMessage suggestion;

    public List<String> getSheets() {
        return sheets;
    }

    public ColumnMapping setSheets(List<String> sheets) {
        this.sheets = sheets;
        return this;
    }

    public ColumnMapping addSheet(String sheet) {
        if (!sheets.contains(sheet)) {
            sheets.add(sheet);
        }
        return this;
    }

    public String getColumn() {
        return column;
    }

    public ColumnMapping setColumn(String column) {
        this.column = column;
        return this;
    }

    public ColumnRole getRole() {
        return role;
    }

    public ColumnMapping setRole(ColumnRole role) {
        this.role = role;
        return this;
    }

    public URI getResolvedUri() {
        return resolvedUri;
    }

    public ColumnMapping setResolvedUri(URI resolvedUri) {
        this.resolvedUri = resolvedUri;
        return this;
    }

    public String getResolvedName() {
        return resolvedName;
    }

    public ColumnMapping setResolvedName(String resolvedName) {
        this.resolvedName = resolvedName;
        return this;
    }

    public ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }

    public ColumnMapping setResolutionStatus(ResolutionStatus resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
        return this;
    }

    public String getExpectedDatatype() {
        return expectedDatatype;
    }

    public ColumnMapping setExpectedDatatype(String expectedDatatype) {
        this.expectedDatatype = expectedDatatype;
        return this;
    }

    public ValueKind getObservedKind() {
        return observedKind;
    }

    public ColumnMapping setObservedKind(ValueKind observedKind) {
        this.observedKind = observedKind;
        return this;
    }

    public int getValueCount() {
        return valueCount;
    }

    public ColumnMapping setValueCount(int valueCount) {
        this.valueCount = valueCount;
        return this;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public ColumnMapping setMissingCount(int missingCount) {
        this.missingCount = missingCount;
        return this;
    }

    public List<String> getSampleValues() {
        return sampleValues;
    }

    public ColumnMapping setSampleValues(List<String> sampleValues) {
        this.sampleValues = sampleValues;
        return this;
    }

    public List<TypeIssue> getIssues() {
        return issues;
    }

    public ColumnMapping setIssues(List<TypeIssue> issues) {
        this.issues = issues;
        return this;
    }

    public String getSuggestion() {
        return suggestion == null ? null : suggestion.getEnglish();
    }

    public ReportMessage getSuggestionMessage() {
        return suggestion;
    }

    public ColumnMapping setSuggestion(ReportMessage suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    public ColumnMapping setSuggestion(String suggestion) {
        this.suggestion = suggestion == null ? null : ReportMessage.plain(suggestion);
        return this;
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * @return true when this column needs the user's attention: something to fix, or a role the
     * profile could not determine
     */
    public boolean needsAttention() {
        return hasIssues() || suggestion != null || role == ColumnRole.UNKNOWN;
    }
}
