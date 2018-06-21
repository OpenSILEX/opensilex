//******************************************************************************
//                                       ProvenanceDateValidator.java
//
// Author(s): Arnaud Charleroy<arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 21 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  21 juin 2018
// Subject:
//******************************************************************************
package phis2ws.service.resources.dto.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;
import phis2ws.service.resources.dto.ProvenanceDTO;

/**
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class ProvenanceDateValidator implements ConstraintValidator<ProvenanceDateCheck, ProvenanceDTO> {

    @Override
    public void initialize(ProvenanceDateCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(ProvenanceDTO provenance, ConstraintValidatorContext context) {
        if (provenance == null) {
            return true;
        }
        // if uri is valid and date not return false
        return !(provenance.getUri() != null && provenance.getCreationDate() == null);
    }

}
