//******************************************************************************
//                                DAOException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * DAO exception.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class DAOException extends Exception {
    public static String GENERIC_MESSAGE = "A DAO issue occured";
    
    public DAOException() {
        super(GENERIC_MESSAGE);
    }
    
    public DAOException(Throwable throwableCause) {
        super(GENERIC_MESSAGE, throwableCause);
    }
    
    public DAOException(String errorMessage) {
        super(errorMessage);
    }
    
    public DAOException(String message, Throwable throwableCause) {
        super(message, throwableCause);
    }
}
