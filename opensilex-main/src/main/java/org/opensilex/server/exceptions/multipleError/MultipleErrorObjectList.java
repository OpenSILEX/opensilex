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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Class to manage multiple errors, it aims to be used in multiple error response. The purpose is to list all errors, for each object(represented by their uri as the key).
 *
 * @see MultipleErrorListDTO to a better understanding of the structure and the usage.
 */
public class MultipleErrorObjectList<T extends MultipleErrorObject> {

    private final String title;

    /**
     * multipleErrorObject are map by their uri to easily access to the errors.
     */
    private final Map<String, T> errors;

    private final Supplier<T> multipleErrorObjectConstructor;

    public MultipleErrorObjectList(String title, Supplier<T> multipleErrorObjectConstructor) {
        this.errors = new HashMap<>();
        this.title = title;
        this.multipleErrorObjectConstructor = multipleErrorObjectConstructor;
    }

    public MultipleErrorListDTO toDTO() {
        List<MultipleErrorDTO> dtos = errors.values().stream()
                .map(MultipleErrorObject::toDTO)
                .toList();
        return new MultipleErrorListDTO(title, dtos);
    }

    public void addError(String key, String error) {
        var errorList = errors.computeIfAbsent(key, k -> {
            T errorObject = multipleErrorObjectConstructor.get();
            errorObject.setUri(k);
            return errorObject;
        });

        errorList.addError(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
