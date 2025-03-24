/*
 * *****************************************************************************
 *                         MultipleErrorObject.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 24/03/2025 14:53
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.server.exceptions.multipleError;

import org.opensilex.server.response.multipleError.MultipleErrorDTO;
import org.opensilex.server.response.multipleError.MultipleErrorListDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to manage multiple errors, it aims to be used in multiple error response. The purpose is to list all errors, for one object.
 *
 * @see MultipleErrorListDTO to a better understanding of the structure and the usage.
 */
public class MultipleErrorObject {
    private final String uri;
    private final List<String> errors;

    public MultipleErrorObject(String uri) {
        this.errors = new ArrayList<>();
        this.uri = uri;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public MultipleErrorDTO toDTO() {
        return new MultipleErrorDTO(uri, errors);
    }
}
