//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import java.time.LocalDate;

/**
 * A single spreadsheet cell after normalisation: the raw text as typed by the observer, plus the
 * interpreted value when one could be derived.
 *
 * @author Arnaud Charleroy
 */
public class CellValue {

    private final String raw;
    private final Double number;
    private final LocalDate date;
    private final boolean missing;

    private CellValue(String raw, Double number, LocalDate date, boolean missing) {
        this.raw = raw;
        this.number = number;
        this.date = date;
        this.missing = missing;
    }

    public static CellValue missing(String raw) {
        return new CellValue(raw, null, null, true);
    }

    public static CellValue text(String raw) {
        return new CellValue(raw, null, null, false);
    }

    public static CellValue number(String raw, double value) {
        return new CellValue(raw, value, null, false);
    }

    public static CellValue date(String raw, LocalDate value) {
        return new CellValue(raw, null, value, false);
    }

    public String getRaw() {
        return raw;
    }

    public Double getNumber() {
        return number;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isMissing() {
        return missing;
    }

    @Override
    public String toString() {
        return raw == null ? "" : raw;
    }
}
