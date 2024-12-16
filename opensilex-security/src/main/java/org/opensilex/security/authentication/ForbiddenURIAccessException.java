/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.authentication;

import java.net.URI;
import java.util.Collection;

/**
 *
 * @author vince
 */
public class ForbiddenURIAccessException extends Exception {

    public ForbiddenURIAccessException(URI uri) {
        super("Forbidden access to URI: " + uri);
    }

    public ForbiddenURIAccessException(URI uri, String message) {
        super("[ Forbidden access to URI list : " + uri.toString() + "] " + message);
    }

    public ForbiddenURIAccessException(Collection<?> uris, String message) {
        super("[ Forbidden access to URI list : " + uris.toString() + "] " + message);
    }
}
