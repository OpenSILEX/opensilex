/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.authentication;

import java.net.URI;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;

/**
 *
 * @author vince
 */
public class NotFoundURIException extends SPARQLInvalidURIException {

    public NotFoundURIException(URI uri) {
        super("URI not found : ",  uri);
    }
    
    public NotFoundURIException(String message, URI uri) {
        super(message, uri);
    }
}
