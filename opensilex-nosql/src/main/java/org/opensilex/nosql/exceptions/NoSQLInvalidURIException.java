/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.exceptions;

import java.net.URI;

/**
 *
 * @author sammy
 */
public class NoSQLInvalidURIException extends Exception{
    private final URI uri;

    public NoSQLInvalidURIException(URI uri) {
        this("Invalid URI: ", uri);
    }

    public NoSQLInvalidURIException(String message, URI uri) {
        super(message + uri);
        this.uri = uri;
    }

    public String getUri() {
        if (uri == null) {
            return null;
        }
        return uri.toString();
    }

}
