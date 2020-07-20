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
public class CSVDuplicateURIError extends CSVCell {

    private int previousRow;

    public CSVDuplicateURIError(CSVCell cell, int previousRow) {
        super(cell);
        this.previousRow = previousRow;
    }

    public int getPreviousRow() {
        return previousRow;
    }

}
