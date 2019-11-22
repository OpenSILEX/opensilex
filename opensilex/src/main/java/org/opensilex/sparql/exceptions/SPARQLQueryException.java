//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.exceptions;

import org.apache.jena.query.Query;

/**
 *
 * @author vincent
 */
public class SPARQLQueryException extends SPARQLException {

    public SPARQLQueryException(String message, Query query) {
        super(message + "\n" + query.toString());
    }

}
