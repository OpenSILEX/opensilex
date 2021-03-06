//******************************************************************************
//                          DataTypeException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

import java.net.URI;

/**
 *
 * @author Alice Boizet
 */
public class DataTypeException extends Exception {
    protected final URI variable;
    protected final Object value;
    protected final URI dataType;
    protected final Integer dataIndex; 

    public DataTypeException(URI variable, Object value, URI dataType) {
        super("The dataType defined on the variable "+ variable.toString() + " is " + dataType.toString() + " and doesn't correspond to the value format: " + value.toString());
        this.variable = variable;
        this.value = value;         
        this.dataType = dataType;
        this.dataIndex = null;  
    }
    
    public DataTypeException(URI variable, Object value, URI dataType, int dataIndex) {
        super("The dataType defined on the variable "+ variable.toString() + " is " + dataType.toString() + " and doesn't correspond to the value format: " + value.toString());
        this.variable = variable;
        this.value = value;         
        this.dataType = dataType;
        this.dataIndex = dataIndex; 
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
}
