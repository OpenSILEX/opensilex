/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.exceptions;

import org.apache.jena.rdf.model.Property;

/**
 *
 * @author vincent
 */
public class SPARQLEntityNotFoundException extends Exception {
 
	private static final long serialVersionUID = 6747954788930812513L;

	public SPARQLEntityNotFoundException(Class<?> objectClass, Property property, Object propertyValue) {
        super("SPARQL object of type: " + objectClass.getCanonicalName() 
                + " not found with " + property.getLocalName() 
                + " = " + propertyValue.toString());
    }
}
