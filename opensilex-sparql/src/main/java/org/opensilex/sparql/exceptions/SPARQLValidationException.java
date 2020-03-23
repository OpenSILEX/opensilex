/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.exceptions;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author vince
 */
public class SPARQLValidationException  extends SPARQLException {

    Map<URI, Map<URI, List<URI>>> validationErrors = new HashMap<>();
    
    public void addValidationError(URI invalidObject, URI invalidObjectProperty, URI brokenConstraint) {
        Map<URI, List<URI>> objectErrors = validationErrors.get(invalidObject);
        if (objectErrors == null) {
            objectErrors = new HashMap<>();
            validationErrors.put(invalidObject, objectErrors);
        }
        
        List<URI> propertyBrokenConstraints = objectErrors.get(invalidObjectProperty);
        if (propertyBrokenConstraints == null) {
            propertyBrokenConstraints = new ArrayList<>();
            objectErrors.put(invalidObjectProperty, propertyBrokenConstraints);
        }
        
        propertyBrokenConstraints.add(brokenConstraint);
    }
    
    public Map<URI, Map<URI, List<URI>>> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public String getMessage() {
        StringBuilder msg = new StringBuilder("SHACL Validation error:\n");
        
        validationErrors.forEach((uri, objectErrors) -> {
            msg.append(uri.toString() + "\n");
            objectErrors.forEach((property, brokenConstraints) -> {
                msg.append("  " + property.toString() +"\n");
                brokenConstraints.forEach((brokenConstraint) -> {
                    msg.append("    " + brokenConstraint.toString() +"\n");
                });
            });
        });
        
        return msg.toString();
    }
    
    
    
    
    
}
