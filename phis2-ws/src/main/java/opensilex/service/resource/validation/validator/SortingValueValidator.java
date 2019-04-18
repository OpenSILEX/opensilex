//******************************************************************************
//                            SortingValueValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.configuration.SortingValues;
import opensilex.service.resource.validation.interfaces.SortingValue;

/**
 * Validator used to validate sort parameters.
 * {@code null} elements are considered valid.
 * @see SortingValue
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SortingValueValidator implements ConstraintValidator<SortingValue, String> {

    @Override
    public void initialize(SortingValue constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateOrder(value);
    }
    
    /**
     * Checks if the sort parameter is one of those provide by SortingValues.
     * @example asc or desc
     * @param order
     * @return true if the sort parameter exists
     *         false if it does not exist
     */
    public boolean validateOrder(String order) {
         return order.equals(SortingValues.DESC.toString())
                 || order.equals(SortingValues.ASC.toString());
    }
}
