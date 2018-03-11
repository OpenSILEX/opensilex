//**********************************************************************************************
//                                       DAO.java 
//
// Author(s): Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: A list of reusable functions for all DAO inherit class
//***********************************************************************************************
package phis2ws.service.dao.manager;

/**
 * Représente les fonctions de bases pour tout DAO
 *
 * @author Arnaud Charleroy
 * @param <T>
 */
public abstract class DAO<T> {

    /**
     * Méthode de création
     *
     * @param obj
     * @return boolean
     * @throws java.lang.Exception
     */
    public abstract Boolean create(T obj) throws Exception;

    /**
     * Méthode pour effacer
     *
     * @param obj
     * @return boolean
     * @throws java.lang.Exception
     */
    public abstract boolean delete(T obj) throws Exception;

    /**
     * Méthode de mise à jour
     *
     * @param obj
     * @return boolean
     * @throws java.lang.Exception
     */
    public abstract boolean update(T obj) throws Exception;

    /**
     * Méthode de recherche des informations
     *
     * @param obj
     * @return T
     * @throws java.lang.Exception
     */
    public abstract T find(T obj) throws Exception;

    /**
     * Méthode de test d'existence
     *
     * @param obj
     * @return T
     * @throws java.lang.Exception
     */
    public abstract boolean existInDB(T obj) throws Exception;

}
