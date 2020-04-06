//******************************************************************************
//                                 DAO.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.manager;

import java.util.List;

import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.model.User;

/**
 * DAO mother class.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 * @param <T> the type of object handled
 */
public abstract class DAO<T> {
    
    /**
     * Remote user address for logging
     */
    public String remoteUserAdress;

	public User user;
	
	
    public String getRemoteUserAdress() {
		return remoteUserAdress;
	}

	public void setRemoteUserAdress(String remoteUserAdress) {
		this.remoteUserAdress = remoteUserAdress;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

    /**
     * Creates in the storage the list of objects given.
     * @param objects
     * @return the given list with the generated IDs 
     * @throws opensilex.service.dao.exception.DAOPersistenceException 
     * @throws java.lang.Exception
     */
    public abstract List<T> create(List<T> objects) throws DAOPersistenceException, Exception;

    /**
     * Deletes in the storage the list of objects given.
     * @param objects
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     * @throws java.lang.Exception
     */
    public abstract void delete(List<T> objects) throws DAOPersistenceException, Exception;

    /**
     * Updates in the storage the list of objects given.
     * @param objects
     * @return the given list with the data updated
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     * @throws java.lang.Exception
     */
    public abstract List<T> update(List<T> objects) throws DAOPersistenceException, Exception;

    /**
     * Finds in the storage the object given.
     * @param object
     * @return the object found
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     * @throws java.lang.Exception
     */
    public abstract T find(T object) throws DAOPersistenceException, Exception;

    /**
     * Finds in the storage the objects with the ID given.
     * @param id
     * @return the object found
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     * @throws java.lang.Exception
     */
    public abstract T findById(String id) throws DAOPersistenceException, Exception;
    
    /**
     * Validates the objects given.
     * @param objects
     * @throws DAODataErrorAggregateException to handle multiple data error exceptions.
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     * @throws opensilex.service.dao.exception.ResourceAccessDeniedException 
     */
    public abstract void validate(List<T> objects) 
            throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException;
    
     /**
     * @return the traceability logs
     */
    protected String getTraceabilityLogs() {
        String log = "";
        if (remoteUserAdress != null) {
            log += "IP Address " + remoteUserAdress + " - ";
        }
        if (user != null) {
            log += "User : " + user.getEmail() + " - ";
        }
        
        return log;
    }
}
