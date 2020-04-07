//******************************************************************************
//                          GrantTypeValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.configuration.GrantTypes;
import opensilex.service.resource.validation.interfaces.GrantType;

/**
 * Validator used to validate grant types.
 * {@code null} elements are considered valid.
 * @see GrantType
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class GrantTypeValidator implements ConstraintValidator<GrantType, String> {
   
    @Override
    public void initialize(GrantType constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateGrantType(value);
    }
    
    /**
     * Checks if the given grant type level is one of the existing grant types 
     * from GrantTypes (JWT or password).
     * @param grantType
     * @return true if the grant type exist
     *         false if it does not exist
     */
    public boolean validateGrantType(String grantType) {
         return grantType.equals(GrantTypes.JWT.toString())
                 || grantType.equals(GrantTypes.PASSWORD.toString());
    }
}
