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
public class SPARQLException extends Exception {

    public SPARQLException() {
        super();
    }

    public SPARQLException(String message) {
        super(message);
    }

    public SPARQLException(String message, Throwable cause) {
        super(message, cause);
    }

    public SPARQLException(Throwable cause) {
        super(cause);
    }

}
