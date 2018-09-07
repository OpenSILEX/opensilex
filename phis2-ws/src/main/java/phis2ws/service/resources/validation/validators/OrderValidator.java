//******************************************************************************
//                                       OrderValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21, Jun 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import phis2ws.service.configuration.Orders;
import phis2ws.service.resources.validation.interfaces.Order;

/**
 * Validator used to validate sort parameters.
 * {@code null} elements are considered valid.
 * @see Order
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class OrderValidator implements ConstraintValidator<Order, String> {
    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(Order constraintAnnotation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateOrder(value);
    }
    
    /**
     * Check if the sort parameter is one of these provide by
     * Orders (asc or desc)
     * @param order
     * @return true if the sort parameter exist
     *         false if it does not exist
     */
    public boolean validateOrder(String order) {
         return order.equals(Orders.DESC.toString())
                 || order.equals(Orders.ASC.toString());
    }
}
