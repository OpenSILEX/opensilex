package org.opensilex.sparql.exceptions;

import java.net.URI;
import java.util.Collection;

public class SPARQLInvalidUriListException extends SPARQLException {

    private final Collection<URI> uris;
    protected String field;

    public SPARQLInvalidUriListException(String message, Collection<URI> uris) {
        super(message + uris.toString());
        this.uris = uris;
    }

    public SPARQLInvalidUriListException(String message, Collection<URI> uris, String field) {
       this(message,uris);
       setField(field);
    }

    public Collection<URI> getUris() {
        return uris;
    }

    public String getStrUris() {
        if (uris.isEmpty()) {
            return null;
        } else {
            return uris.toString();
        }
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
