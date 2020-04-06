//******************************************************************************
//                           NotAnAdminException.java
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
public class NotAnAdminException extends ResourceAccessDeniedException {
    public static String GENERIC_MESSAGE = "The user has to be admin to access this resource";
    
    public NotAnAdminException() {
        super(GENERIC_MESSAGE);
    }
    
    public NotAnAdminException(Throwable throwableCause) {
        super(GENERIC_MESSAGE, throwableCause);
    }
    
    public NotAnAdminException(String message) {
        super(message);
    }
    
    public NotAnAdminException(String message, Throwable throwableCause) {
        super(message, throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
