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
public class CSVURINotFoundError {

    private final int rowIndex;

    private final int colIndex;

    private final String header;

    private final URI classURI;

    private final URI objectURI;

    public CSVURINotFoundError(int rowIndex, int colIndex, String header, URI classURI, URI objectURI) {
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.header = header;
        this.classURI = classURI;
        this.objectURI = objectURI;
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

    public URI getClassURI() {
        return classURI;
    }

    public URI getObjectURI() {
        return objectURI;
    }

}
