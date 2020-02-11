//******************************************************************************
//                       PhisPostgreSQLConfig.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service;

import java.util.Objects;
import org.opensilex.config.ConfigDescription;

/**
 * Phis PostGreSQL configuration
 */
public interface PhisPostgreSQLConfig {

    @ConfigDescription(
            value = "Legacy host parameter",
            defaultString = "localhost"
    )
    String host();

    @ConfigDescription(
            value = "Legacy port parameter",
            defaultString = "5432"
    )
    String port();

    @ConfigDescription(
            value = "Legacy database parameter"
    )
    String database();

    @ConfigDescription(
            value = "Legacy username parameter"
    )
    String username();

    @ConfigDescription(
            value = "Legacy password parameter"
    )
    String password();

    //--------------------------------------------------------------------------
    // EVERYTHING BELOW THIS LINE IS NEVER CHANGED, LEAVE DEFAULT VALUES
    //--------------------------------------------------------------------------
    @ConfigDescription(
            value = "Legacy driver parameter",
            defaultString = "org.postgresql.Driver"
    )
    String driver();

    @ConfigDescription(
            value = "Legacy testWhileIdle parameter",
            defaultBoolean = true
    )
    boolean testWhileIdle();

    @ConfigDescription(
            value = "Legacy testOnBorrow parameter",
            defaultBoolean = true
    )
    boolean testOnBorrow();

    @ConfigDescription(
            value = "Legacy testOnReturn parameter",
            defaultBoolean = false
    )
    boolean testOnReturn();

    @ConfigDescription(
            value = "Legacy validationQuery parameter",
            defaultString = "SELECT 1"
    )
    String validationQuery();

    @ConfigDescription(
            value = "Legacy validationInterval parameter",
            defaultLong = 30000
    )
    long validationInterval();

    @ConfigDescription(
            value = "Legacy timeBetweenEvictionRunsMillis parameter",
            defaultInt = 10000
    )
    int timeBetweenEvictionRunsMillis();

    @ConfigDescription(
            value = "Legacy maxActive parameter",
            defaultInt = 3
    )
    int maxActive();

    @ConfigDescription(
            value = "Legacy minIdle parameter",
            defaultInt = 1
    )
    int minIdle();

    @ConfigDescription(
            value = "Legacy maxIdle parameter",
            defaultInt = 3
    )
    int maxIdle();

    @ConfigDescription(
            value = "Legacy maxWait parameter",
            defaultInt = 20000
    )
    int maxWait();

    @ConfigDescription(
            value = "Legacy initialSize parameter",
            defaultInt = 1
    )
    int initialSize();

    @ConfigDescription(
            value = "Legacy removeAbandonedTimeout parameter",
            defaultInt = 60
    )
    int removeAbandonedTimeout();

    @ConfigDescription(
            value = "Legacy removeAbandoned parameter",
            defaultBoolean = true
    )
    boolean removeAbandoned();

    @ConfigDescription(
            value = "Legacy logAbandoned parameter",
            defaultBoolean = true
    )
    boolean logAbandoned();

    @ConfigDescription(
            value = "Legacy jmxEnabled parameter",
            defaultBoolean = true
    )
    boolean jmxEnabled();

    @ConfigDescription(
            value = "Legacy maxAge parameter",
            defaultLong = 3000
    )
    long maxAge();

    @ConfigDescription(
            value = "Legacy jdbcInterceptors parameter",
            defaultString = "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer"
    )
    String jdbcInterceptors();

}
