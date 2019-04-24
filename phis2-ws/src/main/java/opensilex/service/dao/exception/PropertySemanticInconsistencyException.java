//******************************************************************************
//                     PropertySemanticInconsistencyException.java
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
public class PropertySemanticInconsistencyException extends SemanticInconsistencyException {
    public final static String GENERIC_MESSAGE = 
            "A semantic inconsistency  of a property prevented the operation to continue";
    
    public PropertySemanticInconsistencyException() {
        super(GENERIC_MESSAGE);
    }
    
    public PropertySemanticInconsistencyException(Throwable throwableCause) {
        super(GENERIC_MESSAGE, throwableCause);
    }
    
    public PropertySemanticInconsistencyException(String message) {
        super(message);
    }
    
    public PropertySemanticInconsistencyException(String message, Throwable throwableCause) {
        super(message, throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
