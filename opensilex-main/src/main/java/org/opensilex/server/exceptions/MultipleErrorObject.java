/*
 * *****************************************************************************
 *                         MultipleErrorObject.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 11/03/2025 11:32
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.server.exceptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to manage multiple errors, it aim to be used in multiple error response. The purpose is to list all errors, for each objects(represented by their uri as the key).
 *
 * @see org.opensilex.server.response.MultipleErrorDTO to a better understanding of the structure and the usage.
 */
public class MultipleErrorObject {
    private Map<String, List<String>> errors;

    public MultipleErrorObject() {
        this.errors = new HashMap<>();
    }

    public MultipleErrorObject(Map<String, List<String>> errors) {
        this.errors = errors;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }

    public void addError(String key, String error) {
        errors.computeIfAbsent(key, k -> new ArrayList<>());
        errors.get(key).add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
