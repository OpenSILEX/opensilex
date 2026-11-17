//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.report.ReportMessage;

/**
 * The sentences evidence and notes are shown with: the key goes under {@code GraphImport.report.}, the
 * parameters fill it in, the English text is the fallback.
 *
 * @author Arnaud Charleroy
 */
public final class Reasons {

    private static final String COUNT = "count";
    private static final String TOTAL = "total";
    private static final String CLASS = "class";
    private static final String HEADER = "header";
    private static final String LABEL = "label";

    private Reasons() {
    }

    //#region evidence about a column's property

    public static ReportMessage header(String property) {
        return ReportMessage.of("evidence.header", "The header is the property " + property + ".")
                .with("property", property);
    }

    public static ReportMessage alias(String header) {
        return ReportMessage.of("evidence.alias", "You chose this property for \"" + header + "\" before.")
                .with(HEADER, header);
    }

    public static ReportMessage label(String label) {
        return ReportMessage.of("evidence.label", "The header is the label \"" + label + "\".").with(LABEL, label);
    }

    public static ReportMessage commonWord(String header, String label) {
        return ReportMessage.of("evidence.commonWord", "\"" + header + "\" usually holds the " + label + ".")
                .with(HEADER, header).with(LABEL, label);
    }

    public static ReportMessage closeLabel(String label) {
        return ReportMessage.of("evidence.closeLabel", "The header is close to the label \"" + label + "\".")
                .with(LABEL, label);
    }

    public static ReportMessage valuesRead(int count, int total, String datatype) {
        return ReportMessage.of("evidence.valuesRead", count + " of " + total + " values read as " + datatype + ".")
                .with(COUNT, count).with(TOTAL, total).with("datatype", datatype);
    }

    public static ReportMessage valuesNotRead(int count, int total, String datatype) {
        return ReportMessage.of("evidence.valuesNotRead",
                        "Only " + count + " of " + total + " values read as " + datatype + ".")
                .with(COUNT, count).with(TOTAL, total).with("datatype", datatype);
    }

    public static ReportMessage instances(int count, int total, String classLabel) {
        return ReportMessage.of("evidence.instances",
                        count + " of " + total + " values name an existing " + classLabel + ".")
                .with(COUNT, count).with(TOTAL, total).with(CLASS, classLabel);
    }

    public static ReportMessage sheetIdentifiers(int count, int total, String sheet) {
        return ReportMessage.of("evidence.sheetIdentifiers",
                        count + " of " + total + " values are identifiers of the sheet " + sheet + ".")
                .with(COUNT, count).with(TOTAL, total).with("sheet", sheet);
    }

    //#endregion

    //#region evidence about a sheet's identifier column

    public static ReportMessage identifierHeader(String header) {
        return ReportMessage.of("evidence.identifierHeader", "\"" + header + "\" usually identifies the rows.")
                .with(HEADER, header);
    }

    public static ReportMessage identifierUris() {
        return ReportMessage.of("evidence.identifierUris", "The values are URIs.");
    }

    public static ReportMessage uniqueValues() {
        return ReportMessage.of("evidence.uniqueValues", "Every row has its own value.");
    }

    public static ReportMessage uniqueButIncomplete() {
        return ReportMessage.of("evidence.uniqueButIncomplete", "No value repeats, but some rows have none.");
    }

    public static ReportMessage firstColumn() {
        return ReportMessage.of("evidence.firstColumn", "It is the first column.");
    }

    //#endregion

    //#region evidence about a sheet's class

    public static ReportMessage typeColumn(String classLabel) {
        return ReportMessage.of("evidence.typeColumn", "The type column gives " + classLabel + ".")
                .with(CLASS, classLabel);
    }

    public static ReportMessage sheetName(String sheet, String classLabel) {
        return ReportMessage.of("evidence.sheetName", "The sheet " + sheet + " is named after " + classLabel + ".")
                .with("sheet", sheet).with(CLASS, classLabel);
    }

    public static ReportMessage headerCoverage(int count, int total, String classLabel) {
        return ReportMessage.of("evidence.headerCoverage",
                        count + " of " + total + " headers are properties of " + classLabel + ".")
                .with(COUNT, count).with(TOTAL, total).with(CLASS, classLabel);
    }

    public static ReportMessage existingRows(int count, int total, String classLabel) {
        return ReportMessage.of("evidence.existingRows",
                        count + " of " + total + " identifiers name an existing " + classLabel + ".")
                .with(COUNT, count).with(TOTAL, total).with(CLASS, classLabel);
    }

    //#endregion

    //#region notes

    public static ReportMessage unknownPrefix(String prefix) {
        return ReportMessage.of("note.unknownPrefix", "The prefix \"" + prefix + "\" is unknown.")
                .with("prefix", prefix);
    }

    public static ReportMessage notAccepted(String property, String classLabel) {
        return ReportMessage.of("note.notAccepted", classLabel + " does not accept the property " + property + ".")
                .with("property", property).with(CLASS, classLabel);
    }

    public static ReportMessage unresolvedTypes(String values) {
        return ReportMessage.of("note.unresolvedTypes", "These types are not classes: " + values + ".")
                .with("values", values);
    }

    public static ReportMessage duplicateSingle(String property) {
        return ReportMessage.of("note.duplicateSingle",
                        "Another column gives " + property + ", which holds one value only: choose one.")
                .with("property", property);
    }

    public static ReportMessage notATable() {
        return ReportMessage.of("note.notATable", "This sheet is not a table.");
    }

    public static ReportMessage noExperiment() {
        return ReportMessage.of("note.noExperiment",
                "Choose an experiment to recognise its scientific objects.");
    }

    //#endregion
}
