//******************************************************************************
//                        ProvenanceDateValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21, Jun 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.resources.dto.ProvenanceDTO;
import opensilex.service.resources.validation.interfaces.ProvenanceDate;

/**
 * Check the following rule : the provenance date must not be null if the provenance uri is not set.
 * {@code null} elements are considered valid.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Morgane Vidal] 10 Oct, 2018 : remove the date validation (the @Date must already be used on the date field). 
 */
public class ProvenanceDateValidator implements ConstraintValidator<ProvenanceDate, ProvenanceDTO> {

    @Override
    public void initialize(ProvenanceDate constraintAnnotation) {
    }

    @Override
    public boolean isValid(ProvenanceDTO provenance, ConstraintValidatorContext context) {
        if (provenance == null) {
            return true;
        }
        if (provenance.getUri() != null) { //if there is a provenance uri, return true
             return true;
        } else {
            //if the provenance uri is null : 
            // - creationDate null : return false
            // - creationDate not null : return true
            return provenance.getUri() == null && provenance.getCreationDate() != null;
        }
    }
}
