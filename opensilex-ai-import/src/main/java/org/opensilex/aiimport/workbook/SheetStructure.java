//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One sheet of an uploaded workbook, with its header row and every data row as raw text.
 * <p>
 * This is the in-memory model. Bounded projections are built from it: {@link #sample(int)} for the
 * user interface and for the language model prompt.
 *
 * @author Arnaud Charleroy
 */
public class SheetStructure {

    private String name;
    private int index;
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
    private boolean tabular = true;

    /**
     * Free text of a sheet that carries prose rather than a table, such as a ReadMe. Only filled
     * for sheets detected as non-tabular.
     */
    private String text;

    public String getName() {
        return name;
    }

    public SheetStructure setName(String name) {
        this.name = name;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public SheetStructure setIndex(int index) {
        this.index = index;
        return this;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public SheetStructure setHeaders(List<String> headers) {
        this.headers = headers;
        return this;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public SheetStructure setRows(List<List<String>> rows) {
        this.rows = rows;
        return this;
    }

    public boolean isTabular() {
        return tabular;
    }

    public SheetStructure setTabular(boolean tabular) {
        this.tabular = tabular;
        return this;
    }

    public String getText() {
        return text;
    }

    public SheetStructure setText(String text) {
        this.text = text;
        return this;
    }

    public int getDataRowCount() {
        return rows.size();
    }

    /**
     * @return at most {@code maxRows} data rows, for display or for the prompt
     */
    public List<List<String>> sample(int maxRows) {
        if (maxRows <= 0 || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows.subList(0, Math.min(maxRows, rows.size())));
    }

    public int indexOfHeader(String header) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).equalsIgnoreCase(header)) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasHeader(String header) {
        return indexOfHeader(header) >= 0;
    }

    /**
     * @return the cell at {@code header} in {@code row}, or an empty string when the column is
     * absent or the row is shorter than the header row
     */
    public String cell(List<String> row, String header) {
        int columnIndex = indexOfHeader(header);
        if (columnIndex < 0 || columnIndex >= row.size()) {
            return "";
        }
        return row.get(columnIndex);
    }

    /**
     * @return every distinct non-empty value of a column, in order of first appearance
     */
    public List<String> distinctValues(String header) {
        List<String> values = new ArrayList<>();
        int columnIndex = indexOfHeader(header);
        if (columnIndex < 0) {
            return values;
        }
        for (List<String> row : rows) {
            if (columnIndex >= row.size()) {
                continue;
            }
            String value = row.get(columnIndex);
            if (!value.isEmpty() && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }
}
