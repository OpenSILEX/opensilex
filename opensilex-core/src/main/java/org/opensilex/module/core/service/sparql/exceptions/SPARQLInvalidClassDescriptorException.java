/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql.exceptions;

/**
 *
 * @author vincent
 */
public class SPARQLInvalidClassDescriptorException extends SPARQLException {

    private final Class<?> objectClass;
    
    public SPARQLInvalidClassDescriptorException(Class<?> objectClass, String message) {
        super(message);
        this.objectClass = objectClass;
    }
        
    public SPARQLInvalidClassDescriptorException(Class<?> objectClass, String message, Throwable cause) {
        super(message, cause);
        this.objectClass = objectClass;
    }
    
    public String getMessage() {
        return "Invalid SPARQL object class: " + objectClass.getCanonicalName() + " - " + super.getMessage();
    }
}
