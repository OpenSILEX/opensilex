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

    @ConfigDescription(
            value = "MongoDB main host",
            defaultString = "localhost"
    )
    public String host();

    @ConfigDescription(
            value = "MongoDB main host port",
            defaultInt = 8668
    )
    public int port();

    @ConfigDescription(
            value = "MongoDB database",
            defaultString = "opensilex"
    )
    public String database();

    @ConfigDescription(
            value = "MongoDB username"
    )
    public String username();

    @ConfigDescription(
            value = "MongoDB password"
    )
    public String password();

    @ConfigDescription(
            value = "MongoDB authentication database"
    )
    public String authDB();

    @ConfigDescription(
            value = "MongoDB other connection options"
    )
    public Map<String, String> options();
}
