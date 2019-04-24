//******************************************************************************
//                       TypeNotInRangeException.java
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
public class TypeNotInRangeException extends SemanticInconsistencyException {
    public final static String GENERIC_MESSAGE = 
            "The object type is not in the range of the property";
    public final static String DETAILS = 
            "The type %s of the value %s is not in the range of the property %s";
    
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
    
    public TypeNotInRangeException(String valueType, String propertyName) {
        super(String.format(DETAILS, valueType, propertyName));
    }
    
    public TypeNotInRangeException(String valueType, String propertyName, Throwable throwableCause) {
        super(String.format(DETAILS, valueType, propertyName), throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
