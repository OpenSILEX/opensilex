//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.mapping;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.workbook.CellValue;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps every column of the file onto an OpenSILEX concept, and checks what the cells hold against
 * what the matching variable expects.
 * <p>
 * Entirely deterministic: the profile says what a column stands for, the resolver says what it
 * matches, and the cells say what they contain. The assistant then explains the result and offers
 * to help fix it — it never decides the mapping, because a mapping that changes between two runs
 * cannot be reviewed.
 *
 * @author Arnaud Charleroy
 */
public class MappingService {

    private static final String SUGGEST = "AiImport.report.suggestion.";
    private static final String PROBLEM = "AiImport.report.problem.";
    private static final String FIX = "AiImport.report.fix.";

    /**
     * How many offending cells are listed per column. Enough to see the pattern; a column where
     * every row is wrong does not need every row quoted.
     */
    private static final int MAX_ISSUES_PER_COLUMN = 10;

    private static final int MAX_SAMPLE_VALUES = 5;

    public List<ColumnMapping> map(WorkbookStructure workbook, ImportProfile profile,
                                   ResolutionReport report) {
        // Keyed by column name and role: the same header in two sheets is the same thing, unless
        // the profile says the role differs, in which case the two are genuinely distinct.
        Map<String, ColumnMapping> byColumnAndRole = new LinkedHashMap<>();
        Map<String, List<String>> valuesByKey = new LinkedHashMap<>();
        Map<String, List<SheetStructure>> sheetsByKey = new LinkedHashMap<>();

        for (SheetStructure sheet : workbook.getSheets()) {
            if (!sheet.isTabular()) {
                continue;
            }
            for (String header : sheet.getHeaders()) {
                if (header.isEmpty()) {
                    continue;
                }
                ColumnRole role = profile.roleOf(workbook, sheet.getName(), header);
                String key = header.toLowerCase() + "/" + role.name();

                byColumnAndRole
                        .computeIfAbsent(key, ignored -> new ColumnMapping()
                                .setColumn(header)
                                .setRole(role))
                        .addSheet(sheet.getName());
                valuesByKey.computeIfAbsent(key, ignored -> new ArrayList<>())
                        .addAll(valuesOf(sheet, header));
                sheetsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(sheet);
            }
        }

        List<ColumnMapping> mappings = new ArrayList<>(byColumnAndRole.size());
        for (Map.Entry<String, ColumnMapping> entry : byColumnAndRole.entrySet()) {
            ColumnMapping mapping = entry.getValue();
            summariseValues(mapping, valuesByKey.get(entry.getKey()), workbook.isDate1904());

            if (mapping.getRole() == ColumnRole.VARIABLE) {
                attachVariable(mapping, report, mapping.getColumn());
                checkAgainstExpectedType(mapping, sheetsByKey.get(entry.getKey()),
                        workbook.isDate1904());
            }
            mappings.add(mapping);
        }
        return mappings;
    }

    /**
     * Reads the kind of the column and its counts from every cell it has across every sheet.
     */
    private void summariseValues(ColumnMapping mapping, List<String> values, boolean date1904) {
        int missing = 0;
        List<String> present = new ArrayList<>();
        for (String value : values) {
            if (ExcelValueParser.isMissing(ExcelValueParser.clean(value))) {
                missing++;
            } else {
                present.add(value);
            }
        }
        mapping.setValueCount(present.size())
                .setMissingCount(missing)
                .setObservedKind(inferKind(mapping.getColumn(), present, date1904))
                .setSampleValues(sample(present));
    }

    //#region what the column matches

    private void attachVariable(ColumnMapping mapping, ResolutionReport report, String header) {
        for (ResolvedItem item : report.getVariables()) {
            if (!header.equalsIgnoreCase(item.getSourceValue())) {
                continue;
            }
            mapping.setResolutionStatus(item.getStatus());
            if (item.getMatches().size() == 1) {
                ResourceReference match = item.getMatches().get(0);
                mapping.setResolvedUri(match.getUri())
                        .setResolvedName(match.getName())
                        .setExpectedDatatype(match.getDatatype());
            }
            return;
        }
    }

    //#endregion

    //#region what the cells hold

    private List<String> valuesOf(SheetStructure sheet, String header) {
        int columnIndex = sheet.indexOfHeader(header);
        List<String> values = new ArrayList<>();
        if (columnIndex < 0) {
            return values;
        }
        for (List<String> row : sheet.getRows()) {
            values.add(columnIndex < row.size() ? row.get(columnIndex) : "");
        }
        return values;
    }

    /**
     * Reads the kind of the column from its cells. A column of whole numbers reads as an integer,
     * one that mixes whole and fractional numbers as a decimal, and a column that mixes numbers
     * with words as mixed, which no datatype accepts.
     */
    private ValueKind inferKind(String header, List<String> values, boolean date1904) {
        if (values.isEmpty()) {
            return ValueKind.EMPTY;
        }

        boolean anyText = false;
        boolean anyDecimal = false;
        boolean anyInteger = false;
        boolean anyDate = false;
        boolean anyBoolean = false;

        for (String raw : values) {
            CellValue value = ExcelValueParser.parse(header, raw, date1904);
            if (value.getDate() != null || isIsoDate(raw)) {
                anyDate = true;
            } else if (value.getNumber() != null) {
                if (value.getNumber() == Math.rint(value.getNumber())) {
                    anyInteger = true;
                } else {
                    anyDecimal = true;
                }
            } else if (isBoolean(raw)) {
                anyBoolean = true;
            } else {
                anyText = true;
            }
        }

        int kinds = (anyText ? 1 : 0) + (anyDate ? 1 : 0) + (anyBoolean ? 1 : 0)
                + ((anyDecimal || anyInteger) ? 1 : 0);
        if (kinds > 1) {
            return ValueKind.MIXED;
        }
        if (anyDate) {
            return ValueKind.DATE;
        }
        if (anyBoolean) {
            return ValueKind.BOOLEAN;
        }
        if (anyDecimal) {
            return ValueKind.DECIMAL;
        }
        if (anyInteger) {
            return ValueKind.INTEGER;
        }
        return ValueKind.TEXT;
    }

    private boolean isIsoDate(String raw) {
        try {
            LocalDate.parse(ExcelValueParser.clean(raw));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isBoolean(String raw) {
        String cleaned = ExcelValueParser.clean(raw).toLowerCase();
        return cleaned.equals("true") || cleaned.equals("false")
                || cleaned.equals("vrai") || cleaned.equals("faux");
    }

    private List<String> sample(List<String> values) {
        List<String> distinct = new ArrayList<>();
        for (String value : values) {
            if (!distinct.contains(value)) {
                distinct.add(value);
            }
            if (distinct.size() == MAX_SAMPLE_VALUES) {
                break;
            }
        }
        return distinct;
    }

    //#endregion

    //#region where the two disagree

    private void checkAgainstExpectedType(ColumnMapping mapping, List<SheetStructure> sheets,
                                          boolean date1904) {
        if (mapping.getResolutionStatus() == ResolutionStatus.MISSING
                || mapping.getResolutionStatus() == null) {
            proposeDatatypeForANewVariable(mapping);
            return;
        }
        String expected = mapping.getExpectedDatatype();
        if (expected == null) {
            // Matched, but the instance declares no datatype for it, so there is nothing to check
            // against. Saying so is more useful than staying silent.
            mapping.setSuggestion(ReportMessage.of(SUGGEST + "noDeclaredDatatype",
                    "The matching variable declares no data type, so the values cannot be checked. "
                            + "Consider setting one on the variable."));
            return;
        }

        if (mapping.getObservedKind() == ValueKind.MIXED) {
            mapping.setSuggestion(ReportMessage.of(SUGGEST + "mixedValues",
                    "This column mixes several kinds of value, which no single data type accepts. "
                            + "Split it, or record the odd values in a comment column."));
        } else if (!accepts(expected, mapping.getObservedKind())) {
            mapping.setSuggestion(describeMismatch(expected, mapping.getObservedKind()));
        }

        for (SheetStructure sheet : sheets) {
            collectOffendingCells(mapping, sheet, mapping.getColumn(), expected, date1904);
        }
    }

    /**
     * A column with no matching variable still tells us what it holds, which is what the user needs
     * in order to create it correctly.
     */
    private void proposeDatatypeForANewVariable(ColumnMapping mapping) {
        String suggested = mapping.getObservedKind().getDatatypeUri();
        if (suggested == null) {
            mapping.setSuggestion(mapping.getObservedKind() == ValueKind.EMPTY
                    ? ReportMessage.of(SUGGEST + "emptyColumn",
                            "This column is empty, so nothing can be deduced about it.")
                    : ReportMessage.of(SUGGEST + "mixedBeforeCreating",
                            "This column mixes several kinds of value; decide what it measures "
                                    + "before creating a variable for it."));
            return;
        }
        mapping.setSuggestion(ReportMessage.of(SUGGEST + "createWithDatatype",
                        "No variable matches this column. Its values look like "
                                + label(mapping.getObservedKind()) + ", so create it with the data "
                                + "type " + shortName(suggested) + ".")
                .with("kind", label(mapping.getObservedKind()))
                .with("datatype", shortName(suggested)));
    }

    private void collectOffendingCells(ColumnMapping mapping, SheetStructure sheet, String header,
                                       String expected, boolean date1904) {
        int columnIndex = sheet.indexOfHeader(header);
        if (columnIndex < 0) {
            return;
        }
        for (int i = 0; i < sheet.getRows().size(); i++) {
            if (mapping.getIssues().size() >= MAX_ISSUES_PER_COLUMN) {
                return;
            }
            List<String> row = sheet.getRows().get(i);
            String raw = columnIndex < row.size() ? row.get(columnIndex) : "";
            String cleaned = ExcelValueParser.clean(raw);
            if (ExcelValueParser.isMissing(cleaned)) {
                continue;
            }
            CellValue value = ExcelValueParser.parse(header, raw, date1904);
            ReportMessage problem = problemWith(expected, cleaned, value);
            if (problem == null) {
                continue;
            }
            mapping.getIssues().add(new TypeIssue()
                    .setSheet(sheet.getName())
                    .setColumn(header)
                    // The header is row 1, so the first data row is row 2.
                    .setRowNumber(i + 2)
                    .setValue(cleaned)
                    .setProblem(problem)
                    .setSuggestion(suggestionFor(expected, cleaned, value)));
        }
    }

    private ReportMessage problemWith(String expected, String cleaned, CellValue value) {
        if (isDecimal(expected)) {
            return value.getNumber() == null ? notA("number", "not a number") : null;
        }
        if (isInteger(expected)) {
            if (value.getNumber() == null) {
                return notA("number", "not a number");
            }
            return value.getNumber() == Math.rint(value.getNumber())
                    ? null
                    : notA("wholeNumber", "not a whole number");
        }
        if (isDate(expected)) {
            return value.getDate() != null || isIsoDate(cleaned) ? null : notA("date", "not a date");
        }
        if (isBooleanType(expected)) {
            return isBoolean(cleaned) ? null : notA("boolean", "not a true or false value");
        }
        // A string accepts anything.
        return null;
    }

    private ReportMessage notA(String what, String english) {
        return ReportMessage.of(PROBLEM + what, english);
    }

    private ReportMessage suggestionFor(String expected, String cleaned, CellValue value) {
        if (isInteger(expected) && value.getNumber() != null) {
            return ReportMessage.of(FIX + "roundOrChangeDatatype",
                    "Round it, or change the variable's data type to decimal.");
        }
        if ((isDecimal(expected) || isInteger(expected)) && value.getNumber() == null) {
            return ReportMessage.of(FIX + "useANumber",
                    "Replace it with a number, or move it to a comment column and mark the cell "
                            + "NA.");
        }
        if (isDate(expected)) {
            return ReportMessage.of(FIX + "useADate",
                    "Write it as a date, or check the spreadsheet's date format.");
        }
        if (isBooleanType(expected)) {
            return ReportMessage.of(FIX + "useTrueOrFalse", "Use true or false.");
        }
        return null;
    }

    private ReportMessage describeMismatch(String expected, ValueKind observed) {
        if (isInteger(expected) && observed == ValueKind.DECIMAL) {
            return ReportMessage.of(SUGGEST + "wholeVersusDecimal",
                    "The variable expects whole numbers but this column holds decimals. Either "
                            + "change the variable's data type to decimal, or round the values.");
        }
        if ((isInteger(expected) || isDecimal(expected)) && observed == ValueKind.TEXT) {
            return ReportMessage.of(SUGGEST + "numberVersusText",
                    "The variable expects numbers but this column holds text. Check whether it is "
                            + "the right variable, or whether the values are coded.");
        }
        if (isDate(expected) && observed != ValueKind.DATE) {
            return ReportMessage.of(SUGGEST + "dateExpected",
                    "The variable expects dates but this column does not hold any.");
        }
        return ReportMessage.of(SUGGEST + "kindMismatch",
                        "The variable expects " + shortName(expected) + " but this column holds "
                                + label(observed) + ".")
                .with("expected", shortName(expected))
                .with("observed", label(observed));
    }

    //#endregion

    //#region datatype helpers

    private boolean accepts(String expected, ValueKind observed) {
        if (observed == ValueKind.EMPTY) {
            return true;
        }
        if (isString(expected)) {
            return true;
        }
        if (isDecimal(expected)) {
            return observed.isNumeric();
        }
        if (isInteger(expected)) {
            return observed == ValueKind.INTEGER;
        }
        if (isDate(expected)) {
            return observed == ValueKind.DATE;
        }
        if (isBooleanType(expected)) {
            return observed == ValueKind.BOOLEAN;
        }
        return true;
    }

    private boolean isDecimal(String datatype) {
        return XSDDatatype.XSDdecimal.getURI().equals(datatype)
                || XSDDatatype.XSDdouble.getURI().equals(datatype)
                || XSDDatatype.XSDfloat.getURI().equals(datatype);
    }

    private boolean isInteger(String datatype) {
        return XSDDatatype.XSDinteger.getURI().equals(datatype)
                || XSDDatatype.XSDint.getURI().equals(datatype)
                || XSDDatatype.XSDlong.getURI().equals(datatype);
    }

    private boolean isDate(String datatype) {
        return XSDDatatype.XSDdate.getURI().equals(datatype)
                || XSDDatatype.XSDdateTime.getURI().equals(datatype);
    }

    private boolean isBooleanType(String datatype) {
        return XSDDatatype.XSDboolean.getURI().equals(datatype);
    }

    private boolean isString(String datatype) {
        return XSDDatatype.XSDstring.getURI().equals(datatype);
    }

    /**
     * @return the local part of a datatype URI, e.g. {@code decimal}
     */
    private String shortName(String datatype) {
        if (datatype == null) {
            return "an unspecified type";
        }
        int hash = datatype.lastIndexOf('#');
        return hash >= 0 ? datatype.substring(hash + 1) : datatype;
    }

    private String label(ValueKind kind) {
        switch (kind) {
            case INTEGER:
                return "whole numbers";
            case DECIMAL:
                return "decimal numbers";
            case DATE:
                return "dates";
            case BOOLEAN:
                return "true or false values";
            case TEXT:
                return "text";
            case MIXED:
                return "several kinds of value";
            default:
                return "no value";
        }
    }

    //#endregion
}
