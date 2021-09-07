/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.authentication;

import java.net.URI;

/**
 *
 * @author vince
 */
public class ForbiddenURIAccessException extends Exception {

    private final URI uri;

    public ForbiddenURIAccessException(URI uri) {
        super("Forbidden access to URI: " + uri);
        this.uri = uri;
    }

    public ForbiddenURIAccessException(URI uri,String message) {
        super(message);
        this.uri = uri;
    }

    public String getUri() {
        if (uri == null) {
            return null;
        }
        return uri.toString();
    }

}
