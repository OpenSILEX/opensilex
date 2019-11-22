//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.exceptions;

import java.lang.reflect.Field;

/**
 *
 * @author vincent
 */
public class SPARQLUnknownFieldException extends SPARQLException {

    public SPARQLUnknownFieldException(Field f) {
        super("Unknown field: " + ((f == null) ? "null" : f.getName()));
    }

    public SPARQLUnknownFieldException(Class<?> objectClass, String fieldName, Exception ex) {
        super("Field '" + fieldName + "' not found in class: " + objectClass.getCanonicalName(), ex);
    }

}
