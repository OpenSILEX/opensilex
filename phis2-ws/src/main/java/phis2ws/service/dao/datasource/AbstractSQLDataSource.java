//**********************************************************************************************
//                                       AbstractSQLDataSource.java 
//
// Author(s): Arnaud CHARLEROY
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: Abstract class for SQL datasources
//***********************************************************************************************
package phis2ws.service.dao.datasource;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Représente une source de données avec les paramètres communs à toutes les
 * sources de données SQL
 *
 * @author Arnaud CHARLEROY
 */
public abstract class AbstractSQLDataSource extends DataSource {

    // Récupération des logs
    final static Logger logger = LoggerFactory.getLogger(AbstractSQLDataSource.class);
    // Fichier de configuration
    protected static String propertyFileName;

    public static String getPropertyFileName() {
        return propertyFileName;
    }

    public static void setPropertyFileName(String propertyFileName) {
        AbstractSQLDataSource.propertyFileName = propertyFileName;
    }

}
