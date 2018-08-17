//******************************************************************************
//                                       GroupLevelValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import phis2ws.service.configuration.GroupLevels;
import phis2ws.service.resources.dto.validation.interfaces.GroupLevel;

/**
 * Validator used to validate group levels
 * @see GroupLevel
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
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
