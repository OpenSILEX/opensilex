//******************************************************************************
//                                       OTypeValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import phis2ws.service.resources.dto.validation.interfaces.OType;

/**
 * Validator used to validate "o" type
 * @see OType
 * @see https://www.w3.org/wiki/JSON_Triple_Sets
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class OTypeValidator implements ConstraintValidator<OType, String> {
    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(OType constraintAnnotation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateOType(value);
    }
    
    /**
     * Check if the given "o" type is one of the existings "o" type 
     * from OType (literal or uri)
     * @param oType
     * @return true if the "o" type exist
     *         false if it does not exist
     */
    public boolean validateOType(String oType) {
         return oType.equals(phis2ws.service.configuration.OType.LITERAL.toString())
                 || oType.equals(phis2ws.service.configuration.OType.URI.toString());
    }
}
