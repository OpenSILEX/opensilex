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
import java.util.stream.Collectors;

/**
 * <pre>
 * Class to manage multiple errors, it aims to be used in multiple error response. The purpose is to list all errors, for each object(represented by their uri as the key).
 *
 * T is the type of the MultipleErrorObject that will be used to store the errors, it should extend {@link MultipleErrorObject}. If you have no personalized implementation, simply use {@link MultipleErrorObject}.
 *
 * M is the type of the model that will be used to map the errors to the right model, it should be the same type as the one used in the list passed in constructor.
 *
 * </pre>
 * @see MultipleErrorListDTO to a better understanding of the structure and the usage.
 */
public class MultipleErrorObjectList<T extends MultipleErrorObject, M> {

    private final String title;

    /**
     * multipleErrorObject are map by their index to easily access to the errors.
     */
    private final Map<Integer, T> errors;
    /**
     * A map to keep track of the index of each object (models), allowing to easily retrieve the index of a model to add it an error.
     */
    private final Map<M, Integer> indexMap = new HashMap<>();
    /**
     * A constructor to create a new MultipleErrorObject.
     */
    private final Supplier<T> multipleErrorObjectConstructor;

    /**
     * @param title of the error that will be raised if there is an error in the list.
     * @param modelsToMapInWrightOrder the list of models to map in the right order,
     * this is used to ensure that the errors are added to the wright model, using their index in the list.
     * @param multipleErrorObjectConstructor a supplier to create a new instance of the MultipleErrorObject. If you don't use personalized implementation, simply use MultipleErrorObject::new.
     */
    public MultipleErrorObjectList(String title, List<M> modelsToMapInWrightOrder,Supplier<T> multipleErrorObjectConstructor) {
        this.errors = new HashMap<>();
        this.title = title;
        this.multipleErrorObjectConstructor = multipleErrorObjectConstructor;
        for (int i = 0; i < modelsToMapInWrightOrder.size(); i++) {
            M model = modelsToMapInWrightOrder.get(i);
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
     * @param model the model to which the error is related, WARNING it should be one of the models passed in the constructor.
     * @param error the error to add to the model, it should be a string describing the error.
     */
    public void addError(M model, String error) {
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

    /**
     * @return the errors as a list of MultipleErrorObject,
     * each MultipleErrorObject has for key the model of type M that caused the errors.
     */
    public Map<M, T> getModelsWithErrorsAsObjects() {
        return indexMap.entrySet().stream()
                .filter(entry -> errors.containsKey(entry.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> errors.get(entry.getValue()
                )));
    }
}
