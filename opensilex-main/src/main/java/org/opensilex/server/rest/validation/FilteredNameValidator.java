//******************************************************************************
//                              FilteredNameValidator.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inrae.fr , anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

import java.util.ArrayList;
import java.util.Arrays;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Class used by FilteredName annotation to validate that a name contains bad URL characters for URI generation. {@code null} elements are considered valid.
 * RFC 1738 specification:
 * Thus, only alphanumerics, the special characters "$-_.+!*'(),", and reserved characters used for their reserved purposes may be used unencoded within a URL.
 * @see org.opensilex.server.rest.validation.URL
 * @author Arnaud Charleroy
 */
public class FilteredNameValidator implements ConstraintValidator<FilteredName, String> {
    
    static ArrayList<String> charsToTest = new ArrayList(Arrays.asList(new String[]{
            "-", 
            "+",
            "=",
            "<",
            ">",
            "=",
            "?",
            "/",
            "*",
            "&"
        }));
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        for(String charToTest : charsToTest){
            if(value.contains(charToTest)){
                return false;
            }
        }

        return true;
    } 
}
