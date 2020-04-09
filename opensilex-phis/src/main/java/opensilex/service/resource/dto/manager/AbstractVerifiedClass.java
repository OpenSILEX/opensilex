//******************************************************************************
//                            AbstractVerifiedClass.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 Jun 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.manager;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifier of attributes from defined rues.
 * Allows to apply rules defined in DTO objects.
 * @update [Arnaud Charleroy] 18 Jul. 2018: Re-enforce string empty value 
 * verification and check rules on every object of a list
 * @update [Arnaud Charleroy, Morgane Vidal] 30 Aug. 2018: Refactoring rules and
 * modify conception
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public abstract class AbstractVerifiedClass implements VerifiedClassInterface {

    // Represents the state label for the data state
    public final static String STATE = "state";
    // Represents the state label for the data empty
    public final static String EMPTY = "empty";

    /**
     * Allows to transform an object in a HashMap. 
     * Useful for object transformation.
     * @return object in hashmap form
     */
    public Map<String, String> toHashMap() {
        Map<String, String> objectHashMap = new LinkedHashMap<>();
        Field[] attributes = this.getClass().getDeclaredFields();
        try {
            for (Field field : attributes) {
                field.setAccessible(true);
                objectHashMap.put(field.getName(), (String) field.get(this));
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            objectHashMap.put("convertError", ex.getMessage());
        }
        return objectHashMap;
    }
}
