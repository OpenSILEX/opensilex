//**********************************************************************************************
//                                       DAOFactory.java 
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, morgane.vidal@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  MArch, 2017
// Subject: Class which permit to list availableDAO
//***********************************************************************************************
package phis2ws.service.dao.manager;

public class DAOFactory {
    protected DAOFactory() {
    }
    /**
     * Existing Dao for MongoDB
     * @return MongoDBDAOFactory
     */
//    public static MongoDBDAOFactory getMongoDBDAOFactory() {
//        return new MongoDBDAOFactory();
//    }
    /**
     * Existing Dao for SQL Database
     * @return SQLDAOFactory
     */
//    public static SQLDAOFactory getSQLDAOFactory() {
//        return new SQLDAOFactory();
//    }
    /**
     * Existing Dao for Sesame
     * @return SESAMEDAOFactory
     */
    public static SESAMEDAOFactory getSESAMEDAOFactory() {
        return new SESAMEDAOFactory();
    }
}
