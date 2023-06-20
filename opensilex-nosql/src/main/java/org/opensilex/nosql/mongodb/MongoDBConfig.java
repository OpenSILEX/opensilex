//******************************************************************************
//                           MongoDBConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opensilex.config.ConfigDescription;
import org.opensilex.nosql.mongodb.auth.MongoAuthenticationService;
import org.opensilex.service.ServiceConfig;

/**
 * MongoDB configuration interface
 *
 * @author Vincent Migot
 */
public interface MongoDBConfig extends ServiceConfig {

    String DEFAULT_CONFIG_PATH = "big-data.mongodb.config";

    @ConfigDescription(
            value = "MongoDB main host",
            defaultString = "localhost"
    )
    String host();

    @ConfigDescription(
            value = "MongoDB main host port",
            defaultInt = 27017
    )
    int port();

    @ConfigDescription(
            value = "MongoDB database",
            defaultString = "opensilex"
    )
    String database();

    @ConfigDescription(
            value = "Mongo authentication method"
    )
    MongoAuthenticationService authentication();

    @ConfigDescription(
            value = "MongoDB other connection options"
    )
    Map<String, String> options();

    @ConfigDescription(
            value = "timezone",
            defaultString = "UTC"
    )
    String timezone();

    /**
     * @see com.mongodb.connection.SocketSettings#getConnectTimeout(TimeUnit)
     */
    @ConfigDescription(
            value = "Max socket connect timeout",
            defaultInt = 10000
    )
    int connectTimeoutMs();

    /**
     * @see com.mongodb.connection.ClusterSettings#getServerSelectionTimeout(TimeUnit)
     */
    @ConfigDescription(
            value = "Max MongoDB server selection timeout",
            defaultInt = 10000
    )
    int serverSelectionTimeout();

    /**
     * @see com.mongodb.connection.SocketSettings#getReadTimeout(TimeUnit)
     */
    @ConfigDescription(
            value = "Max socket read timeout",
            defaultInt = 30000
    )
    int readTimeoutMs();

}
