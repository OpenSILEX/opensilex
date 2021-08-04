//******************************************************************************
//                          DuplicateNameException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

/**
 *
 * @author Alice Boizet
 */
public class DuplicateNameException extends Exception {
    String name; 

    public DuplicateNameException(String name) {
        super("Duplicate name "+ name);
        this.name = name;
    }
    
}
