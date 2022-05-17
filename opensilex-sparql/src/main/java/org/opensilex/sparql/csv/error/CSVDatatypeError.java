/*******************************************************************************
 *                         CSVDatatypeError.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.csv.error;

import org.opensilex.sparql.csv.CSVCell;

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
