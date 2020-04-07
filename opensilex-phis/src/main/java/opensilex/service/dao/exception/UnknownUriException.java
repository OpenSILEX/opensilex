//******************************************************************************
//                            UnknownUriException.java
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
public class UnknownUriException extends SemanticInconsistencyException {
    
    public final static String GENERIC_MESSAGE = "Unknown URI";
    public final static String DETAILS = "The URI %s of %s is unknown";
    
    public UnknownUriException() {
        super(DETAILS);
    }
    
    public UnknownUriException(Throwable throwableCause) {
        super(DETAILS, throwableCause);
    }
    
    public UnknownUriException(String uri, String objectWhoseUriIsUnknown) {
        super(String.format(DETAILS, uri, objectWhoseUriIsUnknown));
    }
    
    public UnknownUriException(String uri, String uriObject, Throwable throwableCause) {
        super(String.format(DETAILS, uri, uriObject), throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
