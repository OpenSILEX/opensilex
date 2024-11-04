package org.opensilex.sparql.exceptions;

import java.net.URI;
import java.util.Collection;

public class SPARQLInvalidUriListException extends SPARQLException {

    private final Collection<?> uris;
    protected String field;

    public SPARQLInvalidUriListException(String message, Collection<?> uris) {
        super(message + uris.toString());
        this.uris = uris;
    }

    public SPARQLInvalidUriListException(String message, Collection<?> uris, String field) {
       this(message,uris);
       setField(field);
    }

    public Collection<?> getUris() {
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
