//**********************************************************************************************
//                                       DataSourceDAOPhisBrapi.java 
//
// Author(s): Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: Datasource for Phis database, create a pool of connexion for this database
//***********************************************************************************************
package opensilex.service.dao.datasource;

import java.sql.Connection;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import static phis2ws.service.PropertiesFileManager.getSQLPoolDataSourceProperties;

/**
 * Source de données qui gère un ensemble de connexion pour la base de données
 * relationnelle Postgresql Phis pattern SingletonHolder
 *
 * @date 05/2016
 * @author Arnaud Charleroy
 */
public abstract class PostgreSQLDataSource extends AbstractPostgreSQLDataSource {

    private PostgreSQLDataSource() {
        setPropertyFileName("phis_sql_config");
        // récupération des propriétés
        final PoolProperties p = getSQLPoolDataSourceProperties(propertyFileName);

        try {
            this.setPoolProperties(p);   // S'l n'y a aucune connexion le web service propage une exception INTERNAL_SERVER_ERROR
        } catch (Exception e) {
            LOGGER.error("Can not access to Phis Database.", e);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Can not access to Phis Database : " + e.getMessage()).build());
        }
    }

    /**
     * ThreadSafe
     */
    private static class DataSourceDAOPhisBrapiHolder {

        final private static PostgreSQLDataSource instance = new PostgreSQLDataSource() {
        };
    }

    /**
     * Récupère une et unique instance du pool de connexion
     *
     * @return DataSourceDAOPhisBrapi
     */
    public static PostgreSQLDataSource getInstance() {
        return DataSourceDAOPhisBrapiHolder.instance;
    }

    /**
     * Récupère une connexion du pool de connexion
     *
     * @return Connection
     */
    public static Connection getInstanceConnection() {
        try {
            return getInstance().getConnection();
        } catch (Exception e) {
            LOGGER.error("Can not access to Phis Database.", e);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Can not access to Phis Database : " + e.getMessage())
                    .build());
        }

    }
}
