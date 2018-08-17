//******************************************************************************
//                                       DocumentStatusValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import phis2ws.service.resources.dto.validation.interfaces.DocumentStatus;

/**
 * Validator used to validate document's status
 * @see DocumentStatus
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DocumentStatusValidator implements ConstraintValidator<DocumentStatus, String> {
    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(DocumentStatus constraintAnnotation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateDocumentStatus(value);
    }
    
    /**
     * Check if the given document status is one of the existings document's status
     * from DocumentStatus (linked or unlinked)
     * @param documentStatus
     * @return true if the document status exist
     *         false if it does not exist
     */
    public boolean validateDocumentStatus(String documentStatus) {
         return documentStatus.equals(phis2ws.service.configuration.DocumentStatus.LINKED.toString())
                 || documentStatus.equals(phis2ws.service.configuration.DocumentStatus.UNLINKED.toString());
    }
}
