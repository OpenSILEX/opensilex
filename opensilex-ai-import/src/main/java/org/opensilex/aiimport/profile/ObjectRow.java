//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One observed object as a row of the workbook describes it, before it exists in the instance.
 * <p>
 * Names only — germplasm, treatment, facility — resolved later against the report and the
 * experiment, like every other name the module reads. The position is kept as written; it becomes
 * a move event, dated, because that is how the platform records where an object is.
 *
 * @author Arnaud Charleroy
 */
public class ObjectRow {

    private final String sheet;
    private final int row;
    private final String name;
    private String germplasm;
    private String factorLevel;
    private String facility;
    private String x;
    private String y;

    /**
     * Every cell of the row, by header: what a column mapped by the user to a property of the type
     * is read from. The fields above are what the profile itself made of the row, and stand when no
     * column is mapped to the same target.
     */
    private final Map<String, String> cells = new LinkedHashMap<>();

    public ObjectRow(String sheet, int row, String name) {
        this.sheet = sheet;
        this.row = row;
        this.name = name;
    }

    public String getSheet() {
        return sheet;
    }

    /**
     * The row number as the spreadsheet shows it.
     */
    public int getRow() {
        return row;
    }

    public String getName() {
        return name;
    }

    public String getGermplasm() {
        return germplasm;
    }

    public ObjectRow setGermplasm(String germplasm) {
        this.germplasm = blankToNull(germplasm);
        return this;
    }

    public String getFactorLevel() {
        return factorLevel;
    }

    public ObjectRow setFactorLevel(String factorLevel) {
        this.factorLevel = blankToNull(factorLevel);
        return this;
    }

    public String getFacility() {
        return facility;
    }

    public ObjectRow setFacility(String facility) {
        this.facility = blankToNull(facility);
        return this;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public ObjectRow setPosition(String x, String y) {
        this.x = blankToNull(x);
        this.y = blankToNull(y);
        return this;
    }

    public boolean hasPosition() {
        return x != null || y != null;
    }

    public Map<String, String> getCells() {
        return cells;
    }

    /**
     * Records the row's cells under the sheet's headers; an empty header names no cell.
     */
    public ObjectRow setCells(List<String> headers, List<String> values) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header != null && !header.isEmpty()) {
                cells.put(header, i < values.size() && values.get(i) != null ? values.get(i).trim() : "");
            }
        }
        return this;
    }

    /**
     * @return the cell under that header, or {@code null} when the cell is empty or absent
     */
    public String cell(String header) {
        return blankToNull(cells.get(header));
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
