/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest.validation;

import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 *
 * @author charlero
 */
class ValidTranslationMapValidator  implements ConstraintValidator<ValidTranslationMap, Map<String,String>> {
    
    private String langVal;

    @Override
    public void initialize(ValidTranslationMap validTranslationMap) {
       langVal = validTranslationMap.languageValue();
    }
    /**
     * Check is map is made of translationMap.
     *
     * @param translationMap list of values linked to lang
     * @param context constraint context to store errors
     * @return true if "en" language is available
     */
    @Override
    public boolean isValid(Map<String,String> translationMap, ConstraintValidatorContext context) {
        if (translationMap == null) {
            return false;
        }

        if(translationMap.get(langVal) == null){
            return false;
        } 
        return true;
    }
}
