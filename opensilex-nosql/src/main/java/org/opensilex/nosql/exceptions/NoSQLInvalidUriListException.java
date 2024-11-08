//******************************************************************************
//                          DataAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.nosql.exceptions;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Alice Boizet
 */
public class NoSQLInvalidUriListException extends Exception {

    private final Collection<?> uris;

    public NoSQLInvalidUriListException(Collection<?> uris) {
        this("Invalid URIs: ", uris);
    }

    public NoSQLInvalidUriListException(String message, Collection<?> uris) {
        super(message + uris);
        this.uris = uris;
    }

    public Collection<?> getUris() {
        return uris;
    }
}
