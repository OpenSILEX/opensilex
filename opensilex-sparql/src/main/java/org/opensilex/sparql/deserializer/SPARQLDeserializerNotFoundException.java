//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.net.URI;

/**
 *
 * @author vincent
 */
public class SPARQLDeserializerNotFoundException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 7068977181450600027L;

    public SPARQLDeserializerNotFoundException(Class<?> clazz) {
        super("Deserializer not found for class: " + clazz.getCanonicalName());
    }

    public SPARQLDeserializerNotFoundException(URI datatypeURI) {
        super("Deserializer not found for datatype: " + datatypeURI.toString());
    }

}
