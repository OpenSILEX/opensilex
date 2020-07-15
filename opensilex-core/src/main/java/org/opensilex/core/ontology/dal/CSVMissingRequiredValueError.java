/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

/**
 *
 * @author vmigot
 */
public class CSVMissingRequiredValueError {

    private final int rowIndex;

    private final int colIndex;

    private final String header;

    public CSVMissingRequiredValueError(int rowIndex, int colIndex, String header) {
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.header = header;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getColIndex() {
        return colIndex;
    }

    public String getHeader() {
        return header;
    }

}
