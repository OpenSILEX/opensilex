//******************************************************************************
//                               DAOException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 10 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * Abstract exception thrown during a DAO operation.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public abstract class DAOException extends Exception {
    
    public final static String GENERIC_MESSAGE = "An error occured during a DAO operation.";
    
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

    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
