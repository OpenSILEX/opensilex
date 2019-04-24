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
    
    public final static String GENERIC_MESSAGE = "Unknown URI with type";
    public final static String DETAILS = "The URI %s of %s with type %s is unknown";
    
    public UnknownUriOfTypeException() {
        super(DETAILS);
    }
    
    public UnknownUriOfTypeException(Throwable throwableCause) {
        super(DETAILS, throwableCause);
    }
    
    public UnknownUriOfTypeException(String uri, String objectOfWhichUriIsUnknown, String typeOfObject) {
        super(String.format(DETAILS, uri, objectOfWhichUriIsUnknown, typeOfObject));
    }
    
    public UnknownUriOfTypeException(String uri, String uriObject, Throwable throwableCause) {
        super(String.format(DETAILS, uri, uriObject), throwableCause);
    }
    
    public UnknownUriOfTypeException(String uri, String uriObject, String objectOfWhichUriIsUnknown, Throwable throwableCause) {
        super(String.format(DETAILS, uri, uriObject, objectOfWhichUriIsUnknown), throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
