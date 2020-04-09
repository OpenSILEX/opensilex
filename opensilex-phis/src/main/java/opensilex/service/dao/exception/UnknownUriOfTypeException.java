//******************************************************************************
//                          UnknownUriOfTypeException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * Unknown URI exception.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class UnknownUriOfTypeException extends SemanticInconsistencyException {
    
    public final static String GENERIC_MESSAGE = "Unknown URI of type";
    public final static String DETAILS = "There is no URI %s of type %s.";
    
    public UnknownUriOfTypeException() {
        super(DETAILS);
    }
    
    public UnknownUriOfTypeException(Throwable throwableCause) {
        super(DETAILS, throwableCause);
    }
    
    public UnknownUriOfTypeException(String objectUri, String type) {
        super(String.format(DETAILS, objectUri, type));
    }
    
    public UnknownUriOfTypeException(String objectUri, String type, Throwable throwableCause) {
        super(String.format(DETAILS, objectUri, type), throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
