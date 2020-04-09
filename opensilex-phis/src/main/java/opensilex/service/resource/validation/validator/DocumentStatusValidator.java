//******************************************************************************
//                           DocumentStatusValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.resource.validation.interfaces.DocumentStatus;

/**
 * Validator used to validate document's status.
 * {@code null} elements are considered valid.
 * @see DocumentStatus
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DocumentStatusValidator implements ConstraintValidator<DocumentStatus, String> {
    
    @Override
    public void initialize(DocumentStatus constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateDocumentStatus(value);
    }
    
    /**
     * Checks if the given document status is one of the existing document's status 
     * from DocumentStatus (linked or unlinked).
     * @param documentStatus
     * @return true if the document status exists
     *         false if it does not exist
     */
    public boolean validateDocumentStatus(String documentStatus) {
         return documentStatus.equals(opensilex.service.configuration.DocumentStatus.LINKED.toString())
                 || documentStatus.equals(opensilex.service.configuration.DocumentStatus.UNLINKED.toString());
    }
}
