package org.opensilex.nosql.mongodb.auth.password;

import org.opensilex.config.ConfigDescription;
import org.opensilex.nosql.mongodb.auth.MongoSecurityConfig;

public interface PasswordMongoSecurityConfig extends MongoSecurityConfig {

    @ConfigDescription(
            value = "MongoDB authentication password"
    )
    String password();

}
