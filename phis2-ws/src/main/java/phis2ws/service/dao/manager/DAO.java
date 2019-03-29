//******************************************************************************
//                                 DAO.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.manager;

import java.util.List;

/**
 * DAO mother class
 * @author Arnaud Charleroy
 * @param <T>
 */
public abstract class DAO<T> {

    /**
     * Creates in the storage the list of objects given
     * @param objects
     * @return the given list with the generated IDs 
     * @throws java.lang.Exception
     */
    public abstract List<T> create(List<T> objects) throws Exception;

    /**
     * Deletes in the storage the list of objects given
     * @param objects
     * @throws java.lang.Exception
     */
    public abstract void delete(List<T> objects) throws Exception;

    /**
     * Updates in the storage the list of objects given
     * @param objects
     * @return the given list with the data updated
     * @throws java.lang.Exception
     */
    public abstract List<T> update(List<T> objects) throws Exception;

    /**
     * Finds in the storage the object given
     * @param object
     * @return the object found
     * @throws java.lang.Exception
     */
    public abstract T find(T object) throws Exception;

    /**
     * Finds in the storage the objects with the ID given
     * @param id
     * @return the object found
     * @throws java.lang.Exception
     */
    public abstract T findById(String id) throws Exception;
}
