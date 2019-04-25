//******************************************************************************
//                       TypeNotInDomainException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * Semantic inconsistency exception.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class TypeNotInDomainException extends SemanticInconsistencyException {
    public final static String GENERIC_MESSAGE = 
            "The subject type is not in the domain of the predicate";
    public final static String DETAILS = 
            "The type %s of the subject %s is not in the domain of the predicate %s";
    
    public TypeNotInDomainException() {
        super(GENERIC_MESSAGE);
    }
    
    public TypeNotInDomainException(Throwable throwableCause) {
        super(GENERIC_MESSAGE, throwableCause);
    }
    
    public TypeNotInDomainException(String message) {
        super(message);
    }
    
    public TypeNotInDomainException(String message, Throwable throwableCause) {
        super(message, throwableCause);
    }
    
    public TypeNotInDomainException(String subject, String subjectType, String predicate) {
        super(String.format(DETAILS, subjectType, subject, predicate));
    }
    
    public TypeNotInDomainException(String subject, String subjectType, String predicate, Throwable throwableCause) {
        super(String.format(DETAILS, subjectType, subject, predicate), throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
