//******************************************************************************
//                         DAOPersistenceException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 15 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * A persistence exception occured during a DAO operation.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class DAOPersistenceException extends DAOException {
    public static String GENERIC_MESSAGE = "An error occured during an operation in the data persistence system.";
    
    public DAOPersistenceException() {
        super(GENERIC_MESSAGE);
    }
    
    public DAOPersistenceException(Throwable throwableCause) {
        super(GENERIC_MESSAGE, throwableCause);
    }
    
    public DAOPersistenceException(String message) {
        super(message);
    }
    
    public DAOPersistenceException(String message, Throwable throwableCause) {
        super(message, throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
