/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.exceptions;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author vince
 */
public class SPARQLValidationException extends SPARQLException {

    Map<URI, Map<URI, Map<URI, String>>> validationErrors = new HashMap<>();

    public void addValidationError(URI invalidObject, URI invalidObjectProperty, URI brokenConstraint, String invalidValue) {
        Map<URI, Map<URI, String>> objectErrors = validationErrors.get(invalidObject);
        if (objectErrors == null) {
            objectErrors = new HashMap<>();
            validationErrors.put(invalidObject, objectErrors);
        }

        Map<URI, String> propertyBrokenConstraints = objectErrors.get(invalidObjectProperty);
        if (propertyBrokenConstraints == null) {
            propertyBrokenConstraints = new HashMap<>();
            objectErrors.put(invalidObjectProperty, propertyBrokenConstraints);
        }

        propertyBrokenConstraints.put(brokenConstraint, invalidValue);
    }

    public Map<URI, Map<URI, Map<URI, String>>> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public String getMessage() {
        StringBuilder msg = new StringBuilder("SHACL Validation error:\n");

        validationErrors.forEach((uri, objectErrors) -> {
            msg.append(uri.toString() + "\n");
            objectErrors.forEach((property, brokenConstraints) -> {
                msg.append("  " + property.toString() + "\n");
                brokenConstraints.forEach((brokenConstraint, invalidValue) -> {
                    msg.append("    " + brokenConstraint.toString() + " -> " + invalidValue + "\n");
                });
            });
        });

        return msg.toString();
    }

}
