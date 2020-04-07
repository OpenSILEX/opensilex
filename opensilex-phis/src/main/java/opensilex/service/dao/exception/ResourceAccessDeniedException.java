//******************************************************************************
//                       ResourceAccessDeniedException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * Resource access denied exception.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class ResourceAccessDeniedException extends DAODataErrorException {
    public static String GENERIC_MESSAGE = "The resource access is denied for this user";
    
    public ResourceAccessDeniedException() {
        super(GENERIC_MESSAGE);
    }
    
    public ResourceAccessDeniedException(Throwable throwableCause) {
        super(GENERIC_MESSAGE, throwableCause);
    }
    
    public ResourceAccessDeniedException(String message) {
        super(message);
    }
    
    public ResourceAccessDeniedException(String message, Throwable throwableCause) {
        super(message, throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
