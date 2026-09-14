//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import java.time.LocalDate;

/**
 * One observation read from the file: a value, for a variable, on an object, at a date.
 * <p>
 * Still expressed in the file's own words — an object name rather than a URI, a column key rather
 * than a variable URI. Turning those into URIs is the resolver's job, so that nothing here can
 * carry an identifier the instance never confirmed.
 *
 * @author Arnaud Charleroy
 */
public class DataPoint {

    /**
     * What the observation was made on. A data sheet names either a plot or the field itself, and
     * the two resolve against different things.
     */
    public enum TargetKind {

        SCIENTIFIC_OBJECT,

        /**
         * A place rather than a plot — weather is measured at the field, not on a micro plot.
         */
        FACILITY
    }

    private String sheet;

    /**
     * Row number as the user sees it in the spreadsheet, header row included, so that an error
     * message can point at something they can find.
     */
    private int rowNumber;

    /**
     * Name of the observed object, e.g. a unit plot identifier.
     */
    private String objectName;

    private TargetKind targetKind = TargetKind.SCIENTIFIC_OBJECT;

    private LocalDate date;

    /**
     * The column header, which the resolver matches to a variable.
     */
    private String variableKey;

    private String rawValue;

    /**
     * Who observed, when the file says so. Carried on the provenance, not on the value.
     */
    private String observer;

    public String getSheet() {
        return sheet;
    }

    public DataPoint setSheet(String sheet) {
        this.sheet = sheet;
        return this;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public DataPoint setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
        return this;
    }

    public String getObjectName() {
        return objectName;
    }

    public DataPoint setObjectName(String objectName) {
        this.objectName = objectName;
        return this;
    }

    public TargetKind getTargetKind() {
        return targetKind;
    }

    public DataPoint setTargetKind(TargetKind targetKind) {
        this.targetKind = targetKind;
        return this;
    }

    public LocalDate getDate() {
        return date;
    }

    public DataPoint setDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public String getVariableKey() {
        return variableKey;
    }

    public DataPoint setVariableKey(String variableKey) {
        this.variableKey = variableKey;
        return this;
    }

    public String getRawValue() {
        return rawValue;
    }

    public DataPoint setRawValue(String rawValue) {
        this.rawValue = rawValue;
        return this;
    }

    public String getObserver() {
        return observer;
    }

    public DataPoint setObserver(String observer) {
        this.observer = observer;
        return this;
    }
}
