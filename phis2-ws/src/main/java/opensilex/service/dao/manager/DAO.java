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

    /**
     * Creates in the storage the list of objects given.
     * @param objects
     * @return the given list with the generated IDs 
     * @throws java.lang.Exception
     */
    public abstract List<T> create(List<T> objects) throws Exception;

    /**
     * Deletes in the storage the list of objects given.
     * @param objects
     * @throws java.lang.Exception
     */
    public abstract void delete(List<T> objects) throws Exception;

    /**
     * Updates in the storage the list of objects given.
     * @param objects
     * @return the given list with the data updated
     * @throws java.lang.Exception
     */
    public abstract List<T> update(List<T> objects) throws Exception;

    /**
     * Finds in the storage the object given.
     * @param object
     * @return the object found
     * @throws java.lang.Exception
     */
    public abstract T find(T object) throws Exception;

    /**
     * Finds in the storage the objects with the ID given.
     * @param id
     * @return the object found
     * @throws java.lang.Exception
     */
    public abstract T findById(String id) throws Exception;
    
    /**
     * Checks the objects can be correctly created.Throws an aggregate exception to handle multiple exceptions.
     * @param objects
     * @throws DAODataErrorAggregateException 
     * @throws opensilex.service.dao.exception.ResourceAccessDeniedException 
     */
    public abstract void checkBeforeCreation(List<T> objects) 
            throws DAODataErrorAggregateException, ResourceAccessDeniedException;
    
    /**
     * Checks and create objects.
     * @param annotations
     * @return the annotations created.
     * @throws opensilex.service.dao.exception.DAODataErrorAggregateException
     * @throws opensilex.service.dao.exception.ResourceAccessDeniedException
     */
    public List<T> checkAndCreate(List<T> annotations) 
            throws DAODataErrorAggregateException, ResourceAccessDeniedException, Exception {
        checkBeforeCreation(annotations);
        return create(annotations);
    }
    
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
