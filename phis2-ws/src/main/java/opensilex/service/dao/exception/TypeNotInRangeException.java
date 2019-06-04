//******************************************************************************
//                       TypeNotInRangeException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * Exception to throw when an object type is not in the range of a predicate.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class TypeNotInRangeException extends SemanticInconsistencyException {
    public final static String GENERIC_MESSAGE = 
            "The object type is not in the range of the predicate";
    public final static String DETAILS = 
            "The type %s of the object %s is not in the range of the predicate %s";
    
    public TypeNotInRangeException() {
        super(GENERIC_MESSAGE);
    }
    
    public TypeNotInRangeException(Throwable throwableCause) {
        super(GENERIC_MESSAGE, throwableCause);
    }
    
    public TypeNotInRangeException(String message) {
        super(message);
    }
    
    public TypeNotInRangeException(String message, Throwable throwableCause) {
        super(message, throwableCause);
    }
    
    public TypeNotInRangeException(String objectUriOrValue, String objectType, String predicate) {
        super(String.format(DETAILS, objectType, objectUriOrValue, predicate));
    }
    
    public TypeNotInRangeException(String objectUriOrValue, String objectType, String predicate, Throwable throwableCause) {
        super(String.format(DETAILS, objectType, objectUriOrValue, predicate), throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
