/*
 *  *************************************************************************************
 *  ScramSha256MongoAuthenticationService.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2023
 * Contact :  renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.nosql.mongodb.auth.password;

import com.mongodb.MongoCredential;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.nosql.mongodb.auth.MongoAuthenticationService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.utils.security.SecretReadUtils;
import org.opensilex.utils.unix.UnixFileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * An implementation of {@link MongoAuthenticationService} which use plain password and ScramSHA256 challenge
 * This method expect password is stored inside specific credentials file.
 *
 * @author rcolin
 * @see <a href="https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/auth/#std-label-scram-sha-256-auth-mechanism">SCRAM-SHA256</a>
 * @see <a href="https://www.mongodb.com/docs/manual/core/security-scram/">Security SCRAM</a>
 */
@ServiceDefaultDefinition(config = CredentialsFileMongoConfig.class)
public class CredentialsFileMongoAuthentication extends AbstractMongoAuthenticationService {

    /**
     * @throws IOException       if some I/O error is encountered during the process of credentials file security check
     * @throws SecurityException if Permissions and ownership of the credentials file are too open/permissive
     * @see UnixFileUtils#checkFilePermissionsAndOwnership(File, String, boolean, boolean)
     */
    public CredentialsFileMongoAuthentication(CredentialsFileMongoConfig config) throws IOException, SecurityException {
        super(config);

        if (StringUtils.isEmpty(config.credentialConfigPath())) {
            throw new IllegalArgumentException("Null or empty credentials config path : credentialConfigPath");
        }

        // Ensure that credentials file is only readable by OpenSILEX
        File credentialsFile = new File(config.credentialConfigPath());
        UnixFileUtils.checkFilePermissionsAndOwnership(credentialsFile, "Mongo credentials file", true, true);
    }

    @Override
    public CredentialsFileMongoConfig getConfig() {
        return (CredentialsFileMongoConfig) super.getConfig();
    }

    /**
     * Check if password is OK, clean it (avoid memory leak of array content) and build {@link MongoCredential}
     *
     * @param password The password to check and to clean
     * @return a {@link MongoCredential} from the input password
     */
    protected MongoCredential buildCredentialsAndCleanPassword(char[] password, Charset encoding) throws IOException, NoSuchAlgorithmException {

        LOGGER.info("[MONGODB_CREDENTIAL_READ] [OK]");

        CredentialsFileMongoConfig config = getConfig();

        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Null or empty password read from credentials file. Empty password character array");
        }

        // If a hash is provided, check if credentials hash is equals
        if (!StringUtils.isEmpty(config.credentialConfigHashFile())) {

            if (config.credentialConfigHashFunction() == null) {
                throw new IllegalArgumentException("No config hash function provided : credentialConfigHashFunction");
            }

            // Get MessageDigest according provided algorithm
            MessageDigest messageDigest = MessageDigest.getInstance(config.credentialConfigHashFunction());

            // compute hash and clean password byte[] copy
            byte[] passwordBytes = SecretReadUtils.getBytes(password, encoding);
            byte[] credentialsHash = messageDigest.digest(passwordBytes);
            SecretReadUtils.wipe(passwordBytes);

            // convert hex-encoded message digest -> string representation of hash
            StringBuilder formattedHashValue = new StringBuilder();
            for (byte b : credentialsHash) {
                formattedHashValue.append(String.format("%02x", b & 0xff));
            }

            // check if hash are equals
            String expectedHash = new String(Files.readAllBytes(Paths.get(config.credentialConfigHashFile())));
            if (!expectedHash.equals(formattedHashValue.toString())) {

                throw new SecurityException("[Error] Invalid credentials file integrity. "
                        + "file: " + config.credentialConfigPath()
                        + " ,algorithm: " + config.credentialConfigHashFunction()
                );
            }
            LOGGER.info("[MONGODB_CREDENTIAL_HASH_VERIFICATION], digest: {} [OK]", messageDigest.getAlgorithm());
        }

        // use SHA256 authentification with login/pwd
        MongoCredential scramSha256Credential = MongoCredential.createScramSha256Credential(
                config.login(),
                config.authDB(),
                password
        );

        // Clear password with empty char.
        // Note: the original password is cloned() inside MongoCredential, so the src password array can be cleaned
        SecretReadUtils.wipe(password);

        return scramSha256Credential;
    }


    @Override
    public MongoCredential readCredentials() throws SecurityException, IOException {

        Charset charset = Charset.forName(getConfig().credentialConfigEncoding());
        File credentialsKeyfile = new File(getConfig().credentialConfigPath());

        // Explicitly use new FileInputStream for reading file, this InputStream don't use a channel with a specific buffer
        char[] password = SecretReadUtils.readSecret(() ->  new FileInputStream(credentialsKeyfile), charset);
        try{
            return buildCredentialsAndCleanPassword(password, charset);
        }catch (NoSuchAlgorithmException e){
            throw new SecurityException(e);
        }
    }


}
