//******************************************************************************
//                            PostgreSQLDataSource.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import static opensilex.service.PropertiesFileManager.getSQLPoolDataSourceProperties;

/**
 * PostgreSQL data source.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public abstract class PostgreSQLDataSource extends AbstractPostgreSQLDataSource {

    private PostgreSQLDataSource() {
        setPropertyFileName("phis_sql_config");
        // Get properties
        final PoolProperties p = getSQLPoolDataSourceProperties(propertyFileName);

        try {
            this.setPoolProperties(p);  // INTERNAL_SERVER_ERROR propagation if connection issues
        } catch (Exception e) {
            LOGGER.error("Can not access to Phis Database.", e);
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Can not access to Phis Database : " + e.getMessage())
                    .build());
        }
    }

    /**
     * Safe thread.
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
     * Gets a connection from the connection pool.
     * @return Connection
     */
    public static Connection getInstanceConnection() {
        try {
            return getInstance().getConnection();
        } catch (SQLException e) {
            LOGGER.error("Can not access to Phis Database.", e);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Can not access to Phis Database : " + e.getMessage())
                    .build());
        }
    }
}
