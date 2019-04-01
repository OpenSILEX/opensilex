//******************************************************************************
//                                       GrantTypeValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21, Jun 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.configuration.GrantTypes;
import opensilex.service.resources.validation.interfaces.GrantType;

/**
 * Validator used to validate grant types.
 * {@code null} elements are considered valid.
 * @see GrantType
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class GrantTypeValidator implements ConstraintValidator<GrantType, String> {
    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(GrantType constraintAnnotation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateGrantType(value);
    }
    
    /**
     * Check if the given grant type level is one of the existings grant types 
     * from GrantTypes (jwt or password)
     * @param grantType
     * @return true if the grant type exist
     *         false if it does not exist
     */
    public boolean validateGrantType(String grantType) {
         return grantType.equals(GrantTypes.JWT.toString())
                 || grantType.equals(GrantTypes.PASSWORD.toString());
    }
}
