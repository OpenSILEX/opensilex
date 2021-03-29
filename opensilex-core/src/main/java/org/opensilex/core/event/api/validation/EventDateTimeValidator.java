package org.opensilex.core.event.api.validation;

import org.opensilex.core.event.api.EventCreationDTO;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EventDateTimeValidator implements ConstraintValidator<EventDateTimeConstraint, EventCreationDTO> {

    @Override
    public boolean isValid(EventCreationDTO eventCreationDTO, ConstraintValidatorContext constraintValidatorContext) {

        boolean isValid = true;
        String message = "";

        if(eventCreationDTO.getIsInstant() == null){
            isValid = false;
            message = "is_instant must be not null";
        }
        else if (eventCreationDTO.getStart() == null && eventCreationDTO.getEnd() == null) {
            isValid = false;
            message = "Start or end must be not null";
        }
        else if (eventCreationDTO.getIsInstant() && eventCreationDTO.getEnd() == null) {
            isValid = false;
            message = "End must be not null if is_instant is true";
        }

        if(!isValid){
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        return isValid;
    }

}
