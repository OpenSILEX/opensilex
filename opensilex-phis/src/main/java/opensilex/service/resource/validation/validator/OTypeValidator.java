//******************************************************************************
//                            OTypeValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.resource.validation.interfaces.OType;

/**
 * Validator used to validate "o" type.
 * {@code null} elements are considered valid.
 * @see OType
 * @see https://www.w3.org/wiki/JSON_Triple_Sets
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class OTypeValidator implements ConstraintValidator<OType, String> {

    @Override
    public void initialize(OType constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateOType(value);
    }
    
    /**
     * Checks if the given "o" type is one of the existing "o" type from OType 
     * (literal or URI).
     * @param oType
     * @return true if the "o" type exist
     *         false if it does not exist
     */
    public boolean validateOType(String oType) {
         return oType.equals(opensilex.service.configuration.OType.LITERAL.toString())
                 || oType.equals(opensilex.service.configuration.OType.URI.toString());
    }
}
