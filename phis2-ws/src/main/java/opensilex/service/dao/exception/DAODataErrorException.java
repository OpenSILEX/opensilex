//******************************************************************************
//                           DAODataErrorException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * Exception thrown by a DAO when detecting a data error.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public abstract class DAODataErrorException extends DAOException {
    public static String GENERIC_MESSAGE = "A DAO issue occured";
    
    public DAODataErrorException() {
        super(GENERIC_MESSAGE);
    }
    
    public DAODataErrorException(Throwable throwableCause) {
        super(GENERIC_MESSAGE, throwableCause);
    }
    
    public DAODataErrorException(String errorMessage) {
        super(errorMessage);
    }
    
    public DAODataErrorException(String message, Throwable throwableCause) {
        super(message, throwableCause);
    }
}
