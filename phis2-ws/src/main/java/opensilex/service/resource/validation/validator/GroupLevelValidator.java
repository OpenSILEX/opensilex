//******************************************************************************
//                          GroupLevelValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.configuration.GroupLevels;
import opensilex.service.resource.validation.interfaces.GroupLevel;

/**
 * Validator used to validate group levels.
 * {@code null} elements are considered valid.
 * @see GroupLevel
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class GroupLevelValidator implements ConstraintValidator<GroupLevel, String> {
  
    @Override
    public void initialize(GroupLevel constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateGroupLevel(value);
    }
    
    /**
     * Checks if the given group level is one of the existing levels from 
     * GroupLevels (guest or owner).
     * @param groupLevel
     * @return true if the group exists
     *         false if it does not exist
     */
    public boolean validateGroupLevel(String groupLevel) {
         return groupLevel.equals(GroupLevels.GUEST.toString())
                 || groupLevel.equals(GroupLevels.OWNER.toString());
    }
}
