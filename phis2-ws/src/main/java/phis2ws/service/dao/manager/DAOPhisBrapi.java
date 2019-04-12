//******************************************************************************
//                               DAOPhisBrapi.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.manager;

import java.util.List;
import phis2ws.service.dao.datasource.DataSourceDAOPhisBrapi;
import phis2ws.service.utils.POSTResultsReturn;

/**
 * Répresente une définition de la classe DAO permettant de se connecter à la
 * source de données Phis tout en ayant les méthodes déjà définies
 *
 * @author Arnaud Charleroy
 * @date 05/2016
 * @param <T> Classe représentant l'objet
 * @param <D> Classe représentant l'objet à enregistrer en BD
 */
public abstract class DAOPhisBrapi<T, D> extends SQLDAO<T> {

    public DAOPhisBrapi() {
        if (dataSource == null) {
            dataSource = DataSourceDAOPhisBrapi.getInstance();
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
}
