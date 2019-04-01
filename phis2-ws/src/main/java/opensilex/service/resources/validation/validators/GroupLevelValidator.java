//******************************************************************************
//                                       GroupLevelValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21, Jun 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.configuration.GroupLevels;
import opensilex.service.resources.validation.interfaces.GroupLevel;

/**
 * Validator used to validate group levels.
 * {@code null} elements are considered valid.
 * @see GroupLevel
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class GroupLevelValidator implements ConstraintValidator<GroupLevel, String> {
    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(GroupLevel constraintAnnotation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateGroupLevel(value);
    }
    
    /**
     * Check if the given group level is one of the existings levels 
     * from GroupLevels (guest or owner)
     * @param groupLevel
     * @return true if the group exist
     *         false if it does not exist
     */
    public boolean validateGroupLevel(String groupLevel) {
         return groupLevel.equals(GroupLevels.GUEST.toString())
                 || groupLevel.equals(GroupLevels.OWNER.toString());
    }
}
