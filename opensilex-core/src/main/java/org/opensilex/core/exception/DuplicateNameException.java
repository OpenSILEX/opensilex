/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.exception;

/**
 *
 * @author boizetal
 */
public class DuplicateNameException extends Exception {
    String name; 

    public DuplicateNameException(String name) {
        super("Duplicate name "+ name);
        this.name = name;
    }
    
}
