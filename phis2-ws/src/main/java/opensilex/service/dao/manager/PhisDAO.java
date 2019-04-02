//******************************************************************************
//                               PhisDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.manager;

import java.util.List;
import opensilex.service.datasource.PostgreSQLDataSource;
import opensilex.service.utils.POSTResultsReturn;

/**
 * DAO to connect a PHIS source.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 * @param <T>
 * @param <D> Object handled in the database
 */
public abstract class PhisDAO<T, D> extends PostgreSQLDAO<T> {

    public PhisDAO() {
        if (dataSource == null) {
            dataSource = PostgreSQLDataSource.getInstance();
        }
    }
    
    /**
     * 
     * @param newObject
     * @return 
     */
    public abstract POSTResultsReturn checkAndInsert(D newObject);
    
    /**
     * Vérifie les données et les enregistre en base de données
     * @param newObjects
     * @return 
     */
    public abstract POSTResultsReturn checkAndInsertList(List<D> newObjects);
    
    /**
     * Vérifie les données et fais les modifications en BD
     * @param newObjects
     * @return 
     */
    public abstract POSTResultsReturn checkAndUpdateList(List<D> newObjects);
    
     /**
     * 
     * @return Les logs qui seront utilisés pour la traçabilité
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
