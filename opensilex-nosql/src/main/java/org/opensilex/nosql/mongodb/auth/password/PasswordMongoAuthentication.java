package org.opensilex.nosql.mongodb.auth.password;

import com.mongodb.MongoCredential;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.nosql.mongodb.auth.MongoAuthenticationService;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 * An implementation of {@link MongoAuthenticationService} which use plain password and ScramSHA256 challenge
 * Here the password is read in plain from your main OpenSILEX config file
 * @author rcolin
 *
 * @see <a href="https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/auth/#std-label-scram-sha-256-auth-mechanism">SCRAM-SHA256</a>
 * @see <a href="https://www.mongodb.com/docs/manual/core/security-scram/">Security SCRAM</a>
 */
@ServiceDefaultDefinition(config = PasswordMongoSecurityConfig.class)
public class PasswordMongoAuthentication extends AbstractMongoAuthenticationService {

    private final PasswordMongoSecurityConfig passwordConfig;

    public PasswordMongoAuthentication(PasswordMongoSecurityConfig config) {
        super(config);
        this.passwordConfig = config;

        if (StringUtils.isEmpty(config.password())) {
            throw new IllegalArgumentException("Null or empty password");
        }
    }

    @Override
    public MongoCredential readCredentials() throws SecurityException {

        // use SHA256 authentification with login/pwd
        return MongoCredential.createScramSha256Credential(
                passwordConfig.login(),
                passwordConfig.authDB(),
                passwordConfig.password().toCharArray()
        );
    }
}
