//******************************************************************************
//                       PhisPostgreSQLConfig.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package opensilex.service;

import java.util.Objects;

/**
 * Phis PostGreSQL configuration
 */
public interface PhisPostgreSQLConfig {
    String host();
    String port();
    String database();
    String driver();
    String username();
    String password();
    boolean jmxEnabled();
    boolean testWhileIdle();
    boolean testOnBorrow();
    String validationQuery();
    boolean testOnReturn();
    long validationInterval();
    int timeBetweenEvictionRunsMillis();
    int maxActive();
    int minIdle();
    int maxIdle();
    int initialSize();
    int maxWait();
    int removeAbandonedTimeout();
    long maxAge();
    boolean logAbandoned();
    boolean removeAbandoned();
    String jdbcInterceptors();
}
