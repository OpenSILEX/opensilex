//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.exceptions;

import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author vincent
 */
public class SPARQLMapperNotFoundException extends SPARQLException {

    private Class<?> objectClass;
    private Resource resource;

    public SPARQLMapperNotFoundException(Class<?> objectClass) {
        this.objectClass = objectClass;
    }


    public SPARQLMapperNotFoundException(Resource resource) {
        this.resource = resource;
    }

    public String getMessage() {
        if (objectClass != null) {
             return "No SPARQL Mapper found for class: " + objectClass.getCanonicalName();
        } else if (resource != null) {
             return "No SPARQL Mapper found for resource: " + resource.toString();
        } else {
            return "No SPARQL Mapper found for null";
        }
       
    }
}
