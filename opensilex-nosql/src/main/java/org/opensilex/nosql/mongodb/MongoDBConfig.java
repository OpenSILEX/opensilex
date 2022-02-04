//******************************************************************************
//                           MongoDBConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import java.util.Map;

import org.opensilex.config.ConfigDescription;
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
    public String host();

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
            value = "MongoDB username"
    )
    String username();

    @ConfigDescription(
            value = "MongoDB password"
    )
    String password();

    @ConfigDescription(
            value = "MongoDB authentication database"
    )
    String authDB();

    @ConfigDescription(
            value = "MongoDB other connection options"
    )
    Map<String, String> options();

    @ConfigDescription(
            value = "timezone",
            defaultString = "UTC"
    )
    String timezone();
}
