/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.exceptions;

import java.net.URI;

/**
 *
 * @author vince
 */
public class SPARQLMultipleObjectException extends SPARQLException {

    public SPARQLMultipleObjectException(URI uri, String request) {
        super("Multiple objects for the same URI: " + uri.toString() + "\nSPARQL request : \n" + request);
    }
}
