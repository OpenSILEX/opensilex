package org.opensilex.core.variable.api.validation;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.variable.api.VariableCreationDTO;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class VariableCreateValidator implements ConstraintValidator<VariableCreateConstraint, VariableCreationDTO> {

    @Override
    public boolean isValid(VariableCreationDTO variableCreationDTO, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;
        String message = "";

        if (variableCreationDTO.getIsMultidimensional()) {
            if (variableCreationDTO.getDataType() != null) {
                isValid = false;
                message = "A multidimensional variable can't have a datatype";
            }
            if (!checkValidDimensions(variableCreationDTO)) {
                isValid = false;
                message = "A multidimensional variable must have at least two distinct dimensions";
            }
        } else {
            if (variableCreationDTO.getDataType() == null) {
                isValid = false;
                message = "A monodimensional variable must have a datatype";
            }
            if (variableCreationDTO.getDimensions() != null) {
                isValid = false;
                message = "A monodimensional variable can't have dimensions";
            }
        }

        if(!isValid){
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        return isValid;
    }

    private boolean checkValidDimensions(VariableCreationDTO variableCreationDTO) {
        if (CollectionUtils.isEmpty(variableCreationDTO.getDimensions())) {
            return false;
        }
        Set<URI> dimensionsDistinct = new HashSet<>(variableCreationDTO.getDimensions());
        if (dimensionsDistinct.size() < 2) {
            return false;
        }
        return true;
    }
}