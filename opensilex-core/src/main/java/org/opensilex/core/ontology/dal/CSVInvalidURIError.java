/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;

/**
 *
 * @author vmigot
 */
public class CSVInvalidURIError {

    private final int rowIndex;

    private final int colIndex;

    private final String header;

    private final String value;

    public CSVInvalidURIError(int rowIndex, int colIndex, String header, String value) {
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.header = header;
        this.value = value;
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

    public String getValue() {
        return value;
    }

}
