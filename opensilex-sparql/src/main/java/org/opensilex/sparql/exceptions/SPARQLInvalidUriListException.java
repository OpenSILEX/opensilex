package org.opensilex.sparql.exceptions;

import java.net.URI;
import java.util.Collection;

public class SPARQLInvalidUriListException extends SPARQLException {

    private final Collection<URI> uris;

    public SPARQLInvalidUriListException(Collection<URI> uris) {
        this("Invalid URIs: ", uris);
    }

    public SPARQLInvalidUriListException(String message, Collection<URI> uris) {
        super(message + uris.toString());
        this.uris = uris;
    }

    public String getUris() {
        if (uris.isEmpty()) {
            return null;
        } else {
            return uris.toString();
        }

    }
}
