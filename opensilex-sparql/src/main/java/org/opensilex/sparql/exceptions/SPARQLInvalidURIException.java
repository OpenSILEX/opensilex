//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.exceptions;

import java.net.URI;

/**
 *
 * @author vincent
 */
public class SPARQLInvalidURIException extends SPARQLException {

    private final URI uri;

    public SPARQLInvalidURIException(URI uri) {
        this("Invalid URI: ", uri);
    }

    public SPARQLInvalidURIException(String message, URI uri) {
        super(message + uri);
        this.uri = uri;
    }

    public String getUri() {
        if (uri == null) {
            return null;
        }
        return uri.toString();
    }

}
