//******************************************************************************
//                          DataAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.nosql.exceptions;

import java.util.Collection;

/**
 *
 * @author Alice Boizet
 */
public class NoSQLInvalidUriListException extends Exception {

    public NoSQLInvalidUriListException(Collection<?> uris) {
        this("Invalid URIs: ", uris);
    }

    public NoSQLInvalidUriListException(String message, Collection<?> uris) {
        super(message + uris);
    }
}
