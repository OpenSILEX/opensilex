//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import java.util.ArrayList;
import java.util.List;

/**
 * A column of the workbook that looks like an observed variable, with whatever identifying
 * information the profile could recover for it.
 *
 * @author Arnaud Charleroy
 */
public class VariableCandidate {

    /**
     * The column header as written in the file, e.g. {@code Bai_Suc_g}.
     */
    private String columnKey;

    /**
     * A longer human label when the file provides one, e.g. from a variable catalogue sheet.
     */
    private String label;

    /**
     * An external ontology identifier when the file provides one, e.g. {@code CO_356:1000217}.
     * This is the most reliable matching key available.
     */
    private String externalId;

    /**
     * The trait observed, when the file names it. Becomes an entity and a characteristic, which
     * the file does not separate — MIAPPE says "plant height", OpenSILEX wants "plant" and
     * "height" — so the split is proposed and confirmed rather than deduced.
     */
    private VariableComponent trait;

    /**
     * How it was measured, when the file names it.
     */
    private VariableComponent method;

    /**
     * The scale it was measured on, which becomes the unit.
     */
    private VariableComponent unit;

    /**
     * Sheets in which the column appears.
     */
    private List<String> sheets = new ArrayList<>();

    public VariableCandidate() {
    }

    public VariableCandidate(String columnKey) {
        this.columnKey = columnKey;
    }

    public String getColumnKey() {
        return columnKey;
    }

    public VariableCandidate setColumnKey(String columnKey) {
        this.columnKey = columnKey;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public VariableCandidate setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getExternalId() {
        return externalId;
    }

    public VariableCandidate setExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public VariableComponent getTrait() {
        return trait;
    }

    public VariableCandidate setTrait(VariableComponent trait) {
        this.trait = trait;
        return this;
    }

    public VariableComponent getMethod() {
        return method;
    }

    public VariableCandidate setMethod(VariableComponent method) {
        this.method = method;
        return this;
    }

    public VariableComponent getUnit() {
        return unit;
    }

    public VariableCandidate setUnit(VariableComponent unit) {
        this.unit = unit;
        return this;
    }

    /**
     * @return true when the file describes what the variable is made of, so that creating it is a
     *         matter of confirming a decomposition rather than inventing one
     */
    public boolean hasComponents() {
        return trait != null && !trait.isEmpty();
    }

    public List<String> getSheets() {
        return sheets;
    }

    public VariableCandidate setSheets(List<String> sheets) {
        this.sheets = sheets;
        return this;
    }

    public VariableCandidate addSheet(String sheet) {
        if (!sheets.contains(sheet)) {
            sheets.add(sheet);
        }
        return this;
    }

    /**
     * @return the best name to search the instance with: the catalogue label when there is one,
     * otherwise the raw column header
     */
    public String getSearchName() {
        return label != null && !label.isEmpty() ? label : columnKey;
    }
}
