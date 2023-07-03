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
import org.opensilex.utils.security.OpenSslCipherConfig;

/**
 * @see <a href="https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/auth/#std-label-scram-sha-256-auth-mechanism">Mongo SCRAM-SHA256</a>
 */
public interface EncryptedCredentialsFileMongoConfig extends CredentialsFileMongoConfig, OpenSslCipherConfig {

    @ConfigDescription(
            value = "Path to your credentials properties file",
            defaultString = "/etc/opensilex/mongo_credentials.yaml"
    )
    String credentialConfigPath();

    @ConfigDescription(
            value = "Path to your credentials properties file",
            defaultString = "/etc/opensilex/mongo_credentials.yaml"
    )
    String credentialEncryptionKeyPath();

    @ConfigDescription(
            value = "OpenSSL decryption configuration"
    )
    OpenSslCipherConfig cipherConfig();
}
