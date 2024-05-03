/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.exceptions;

import java.net.URI;

/**
 *
 * @author vince
 */
public class NotFoundURIException extends NotFoundException {

    public NotFoundURIException(URI uri) {
        this("URI not found : ", uri);
    }
    
    public NotFoundURIException(String message, URI uri) {
        this(message, "URI not found", uri);
    }

    public NotFoundURIException(String message, String title, URI uri) {
        super(message+' '+uri, title);
    }
}
