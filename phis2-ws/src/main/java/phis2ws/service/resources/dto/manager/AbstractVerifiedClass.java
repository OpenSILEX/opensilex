//**********************************************************************************************
//                                       AbstractVerifiedClass.java 
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: may 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date: 20 Juin, 2018
// Subject: A class which contains methods to verify automatically class's attributes from rules defined by user
//***********************************************************************************************
package phis2ws.service.resources.dto.manager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Permits to apply rules defined in DTO objects
 *
 * @author Arnaud Charleroy
 */
public abstract class AbstractVerifiedClass implements VerifiedClassInterface {

    //Represents the state label for the data state
    public final static String STATE = "state";
    //Represents the state label for the data empty
    public final static String EMPTY = "empty";

    /**
     * Return a map with the object status linked with the "state" key. Contains
     * the missing fields in the others key
     *
     * @return object status map
     */
    @Override
    public Map<String, Object> isOk() {
        Map<String, Object> ok = new HashMap<>();
        Map<String, Boolean> rules = this.rules();

        Field[] attributes = this.getClass().getDeclaredFields();

        Boolean validationBool = Boolean.TRUE;

        try {
            for (Field field : attributes) {
                field.setAccessible(true);
                Object fieldObject = field.get(this);
                // If the field value is a List
                if (fieldObject instanceof List) {
                    List list = (List) ((List) fieldObject);
                    if (!list.isEmpty()
                            && Objects.equals(rules.get(field.getName()), Boolean.TRUE)) {
                        if (list.get(0) instanceof AbstractVerifiedClass) {
                            Map<String, Object> verifiedClassInstance = ((AbstractVerifiedClass) list.get(0)).isOk();
                            if (verifiedClassInstance.get(STATE) == Boolean.FALSE) {
                                validationBool = Boolean.FALSE;
                                verifiedClassInstance.remove(STATE);
                                ok.put(field.getName(), verifiedClassInstance);
                            }
                        }
                    } else if (list.isEmpty()
                            && Objects.equals(rules.get(field.getName()), Boolean.TRUE)) {
                        validationBool = Boolean.FALSE;
                        ok.put(field.getName(), EMPTY);
                    }
                    // If the field value is an other AbstractVerifiedClass with rules
                } else if (fieldObject instanceof AbstractVerifiedClass) {
                    Map<String, Object> verifiedClassInstance = ((AbstractVerifiedClass) fieldObject).isOk();
                    if (verifiedClassInstance.get(STATE) == Boolean.FALSE) {
                        validationBool = Boolean.FALSE;
                        verifiedClassInstance.remove(STATE);
                        ok.put(field.getName(), verifiedClassInstance);
                    }
                } else {
                    // Test if the field value need to be checked
                    if (Objects.equals(rules.get(field.getName()), Boolean.TRUE)
                            && // If the field value is null or if the field value is a empty string or a string only whitespace, the field is defined as empty
                            (Objects.equals(fieldObject, null) || (fieldObject instanceof String && (fieldObject == "" || String.valueOf(fieldObject).matches("^\\s*$"))))) {
                        validationBool = Boolean.FALSE;
                        ok.put(field.getName(), EMPTY);
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            ok.replace(STATE, Boolean.FALSE);
        }
        ok.put(STATE, validationBool);
        return ok;
    }

    /**
     * Permits to transfom an object in a HashMap. Useful for object
     * transformation
     *
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
