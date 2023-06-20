package org.opensilex.nosql.mongodb.auth;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

/**
 * Interface which define the settings needed by {@link org.opensilex.nosql.mongodb.auth.MongoAuthenticationService}
 * in order to build {@link com.mongodb.MongoCredential}
 *
 * @author rcolin
 */
public interface MongoSecurityConfig extends ServiceConfig {

    /**
     * @return the name of the Mongo authentification database (admin by default)
     * @see <a href="https://www.mongodb.com/docs/manual/core/security-users/#std-label-user-authentication-database">Mongo authentication database</a>
     */
    @ConfigDescription(
            value = "MongoDB authentication database",
            defaultString = "admin"
    )
    String authDB();

    @ConfigDescription(
            value = "MongoDB authentication user"
    )
    String login();

}
