//******************************************************************************
//                          DataTypeException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

import org.opensilex.sparql.csv.CSVCell;

import java.net.URI;

/**
 *
 * @author Alice Boizet
 */
public class CSVDataTypeException extends Exception {
    private final URI variable;
    private final Object value;
    private final URI dataType;
    private final Integer dataIndex; 
    private final CSVCell csvCell;

    
     public CSVDataTypeException(URI variable, Object value, URI dataType, int dataIndex) {
        super("The dataType defined on the variable "+ variable.toString() + " is " + dataType.toString() + " and doesn't correspond to the value format: " + value.toString());
        this.variable = variable;
        this.value = value;         
        this.dataType = dataType;
        this.dataIndex = dataIndex; 
        this.csvCell = null;
    }
     public CSVDataTypeException(URI variable, Object value, URI dataType, int dataIndex, CSVCell csvCell) {
        super("The dataType defined on the variable "+ variable.toString() + " is " + dataType.toString() + " and doesn't correspond to the value format: " + value.toString());
        this.variable = variable;
        this.value = value;         
        this.dataType = dataType;
        this.dataIndex = dataIndex; 
        this.csvCell = csvCell;
    }

    public URI getVariable() {
        return variable;
    }

    public Object getValue() {
        return value;
    }    

    public URI getDataType() {
        return dataType;
    }

    public Integer getDataIndex() {
        return dataIndex;
    }

    public CSVCell getCsvCell() {
        return csvCell;
    }
    
    
}
