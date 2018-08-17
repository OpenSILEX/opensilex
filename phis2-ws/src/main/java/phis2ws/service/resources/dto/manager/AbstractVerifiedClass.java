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
import java.util.Set;
import java.util.Map;
import javax.validation.ValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

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
//    @Override
//    public Map<String, Object> isOk() {
//        // Map contains result of format and is empty check
//        Map<String, Object> verificationResultMap = new HashMap<>();
//        // boolean
//        Boolean validationBool = Boolean.FALSE;
//        // retreive constraints
//        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//        Validator validator = factory.getValidator();
//        Set<ConstraintViolation<AbstractVerifiedClass>> constraintViolations = validator.validate(this);
//        // iterate on constraints  
//        if (constraintViolations.size() > 0) {
//            for (ConstraintViolation<AbstractVerifiedClass> contraintes : constraintViolations) {
//                verificationResultMap.put(contraintes.getPropertyPath().toString(), contraintes.getMessage());
//            }
//        } else {
//            validationBool = Boolean.TRUE;
//        }
//        verificationResultMap.put(STATE, validationBool);
//        return verificationResultMap;
//    }
    
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
