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
public class NoSQLAlreadyExistingUriException extends Exception {

    private final URI uri;

    public NoSQLAlreadyExistingUriException(URI uri) {
        super("URI already exists: " + uri.toString());
        this.uri = uri;
    }

    public String getUri() {
        if (uri == null) {
            return null;
        }
        return uri.toString();
    }
}
