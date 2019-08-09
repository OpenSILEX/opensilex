/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.exceptions;

import org.apache.jena.query.Query;

/**
 *
 * @author vincent
 */
public class SPARQLQueryException extends SPARQLException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 6683322669764585493L;

	public SPARQLQueryException(String message, Query query) {
        super(message + "\n" + query.toString());
    }

}
