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
     * multipleErrorObject are map by their index to easily access to the errors.
     */
    private final Map<Integer, T> errors;
    /**
     * A map to keep track of the index of each object (models), allowing to easily retrieve the index of a model to add it an error.
     */
    private final Map<Object, Integer> indexMap = new HashMap<>();
    /**
     * A constructor to create a new MultipleErrorObject.
     */
    private final Supplier<T> multipleErrorObjectConstructor;

    /**
     * @param title of the error that will be raised if there is an error in the list.
     * @param modelsToMapInWrightOrder the list of models to map in the right order,
     *this is used to ensure that the errors are added to the wright model, using their index in the list.
     * @param multipleErrorObjectConstructor a supplier to create a new instance of the MultipleErrorObject. If you don't use personalized implementation, simply use MultipleErrorObject::new.
     */
    public MultipleErrorObjectList(String title, List<Object> modelsToMapInWrightOrder,Supplier<T> multipleErrorObjectConstructor) {
        this.errors = new HashMap<>();
        this.title = title;
        this.multipleErrorObjectConstructor = multipleErrorObjectConstructor;
        for (int i = 0; i < modelsToMapInWrightOrder.size(); i++) {
            Object model = modelsToMapInWrightOrder.get(i);
            indexMap.put(model, i);
        }
    }

    public MultipleErrorListDTO toDTO() {
        List<MultipleErrorDTO> dtos = errors.values().stream()

                .map(MultipleErrorObject::toDTO)
                .toList();
        return new MultipleErrorListDTO(title, dtos);
    }

    /**
     * Add an error to the model, using the model as a key to retrieve the index of the model in the list passed in constructor.
     * @param model the model to which the error is related, it should be one of the models passed in the constructor.
     * @param error the error to add to the model, it should be a string describing the error.
     */
    public void addError(Object model, String error) {
        Integer key = indexMap.get(model);

        var errorList = errors.computeIfAbsent(key, k -> {
            T errorObject = multipleErrorObjectConstructor.get();
            errorObject.setIndex(k);
            return errorObject;
        });

        errorList.addError(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Map<Integer, T> getErrors() {
        return errors;
    }
}
