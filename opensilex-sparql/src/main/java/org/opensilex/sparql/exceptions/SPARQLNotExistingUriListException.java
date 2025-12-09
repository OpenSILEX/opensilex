package org.opensilex.sparql.exceptions;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Collection;

public class SPARQLNotExistingUriListException extends SPARQLException {

    private final Collection<URI> uris;

    protected String field;


    public SPARQLNotExistingUriListException(String message, Collection<URI> uris) {
        super(StringUtils.isEmpty(message) ? "not existing URIs" + uris.toString(): message);
        this.uris = uris;
    }

    public SPARQLNotExistingUriListException(String message, Collection<URI> uris, String field){
        this(message,uris);
        setField(field);
    }

    public Collection<URI> getUris() {
        return uris;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
