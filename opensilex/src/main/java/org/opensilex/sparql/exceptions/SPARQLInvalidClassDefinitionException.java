//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.exceptions;

/**
 *
 * @author vincent
 */
public class SPARQLInvalidClassDefinitionException extends SPARQLException {

    private final Class<?> objectClass;

    public SPARQLInvalidClassDefinitionException(Class<?> objectClass, String message) {
        super(message);
        this.objectClass = objectClass;
    }

    public SPARQLInvalidClassDefinitionException(Class<?> objectClass, String message, Throwable cause) {
        super(message, cause);
        this.objectClass = objectClass;
    }

    public String getMessage() {
        return "Invalid SPARQL object class: " + objectClass.getCanonicalName() + " - " + super.getMessage();
    }
}
