//******************************************************************************
//                         ProvenanceDateValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.resource.dto.ProvenanceDTO;
import opensilex.service.resource.validation.interfaces.ProvenanceDate;

/**
 * Provenance date validator. 
 * The date must not be null if the provenance URI is not set.
 * {@code null} elements are considered valid.
 * @update [Morgane Vidal] 10 Oct. 2018: remove the date validation (the @Date 
 * must already be used on the date field). 
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
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
