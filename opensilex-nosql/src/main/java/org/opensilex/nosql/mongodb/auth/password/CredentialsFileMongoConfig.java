/*
 *  *************************************************************************************
 *  MongoDbPasswordConfig.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2023
 * Contact :  renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.nosql.mongodb.auth.password;

import org.opensilex.config.ConfigDescription;
import org.opensilex.nosql.mongodb.auth.MongoSecurityConfig;

/**
 * @see <a href="https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/auth/#std-label-scram-sha-256-auth-mechanism">Mongo SCRAM-SHA256</a>
 */
public interface CredentialsFileMongoConfig extends MongoSecurityConfig {

    @ConfigDescription(
            value = "Path to your credentials properties file",
            defaultString = "/etc/opensilex/mongo_credentials.yaml"
    )
    String credentialConfigPath();

    @ConfigDescription(
            value = "Credentials file encoding format, available format are : [UTF-8, UTF-16, UTF-16BE, UTF-16LE, ISO-8859-1]",
            defaultString = "UTF-8"
    )
    String credentialConfigEncoding();

    @ConfigDescription(
            value = "Credentials file hash, used to verify credentials  integrity",
            defaultString = ""
    )
    String credentialConfigHashFile();

    @ConfigDescription(
            value = "Credentials hash function",
            defaultString = "SHA-512/256"
    )
    String credentialConfigHashFunction();
}
