//******************************************************************************
//                        AbstractPostgreSQLDataSource.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.datasource;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Représente une source de données avec les paramètres communs à toutes les
 * sources de données SQL
 *
 * @author Arnaud Charleroy
 */
public abstract class AbstractPostgreSQLDataSource extends DataSource {

    // Récupération des logs
    final static Logger LOGGER = LoggerFactory.getLogger(AbstractPostgreSQLDataSource.class);
    // Fichier de configuration
    protected static String propertyFileName;

    public static String getPropertyFileName() {
        return propertyFileName;
    }

    public static void setPropertyFileName(String propertyFileName) {
        AbstractPostgreSQLDataSource.propertyFileName = propertyFileName;
    }

}
