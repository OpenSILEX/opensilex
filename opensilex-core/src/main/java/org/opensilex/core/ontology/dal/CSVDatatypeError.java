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
public class CSVDatatypeError extends CSVCell {

    private final URI datatype;

    public CSVDatatypeError(CSVCell cell, URI datatype) {
        super(cell);
        this.datatype = datatype;
    }

    public URI getDatatype() {
        return datatype;
    }

}
