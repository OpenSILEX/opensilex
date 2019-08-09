/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.deserializer;

/**
 *
 * @author vincent
 */
public class DeserializerNotFoundException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 7068977181450600027L;

	public DeserializerNotFoundException(Class<?> clazz) {
        super("Deserializer not found for class: " + clazz.getCanonicalName());
    }

}
