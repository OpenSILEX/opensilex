//******************************************************************************
//                      AuthenticationService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.authentication;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.impl.PublicClaims;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.OpenSilex;
import org.opensilex.service.Service;
import org.opensilex.rest.user.dal.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensilex.rest.extensions.LoginExtension;

/**
 * <pre>
 * Default Authentication service implementation for OpenSilex
 *
 * Generate JWT token with java-jwt library
 * For details see: https://github.com/auth0/java-jwt
 * JWT claim key definitions: https://www.iana.org/assignments/jwt/jwt.xhtml
 *
 * Generate and validate password hash with BCrypt java library
 * For details see: https://github.com/patrickfav/bcrypt
 *
 * Logged in users are registred in a concurrent map with their token
 * and automatically unregistred after token expiration.
 *
 * </pre>
 *
 * @author Vincent Migot
 */
public class AuthenticationService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    /**
     * Default service name to be accessed by name
     */
    public static final String DEFAULT_AUTHENTICATION_SERVICE = "authentication";

    /**
     * Password hash complexity for Bcryp password generation
     */
    private static final int PASSWORD_HASH_COMPLEXITY = 12;

    /**
     * RSA key size for JWT token gneration
     */
    private final static int RSA_KEY_SIZE = 2048;

    /**
     * JWT token validity duration
     */
    private static final long TOKEN_VALIDITY_DURATION = 30;
//    private static final long TOKEN_VALIDITY_DURATION = 60;

    /**
     * JWT token validity duration unit
     */
    private static final TemporalUnit TOKEN_VALIDITY_DURATION_UNIT = ChronoUnit.MINUTES;
//    private static final TemporalUnit TOKEN_VALIDITY_DURATION_UNIT = ChronoUnit.SECONDS;

    /**
     * JWT token issuer
     */
    private static final String TOKEN_ISSUER = "opensilex";

    /**
     * First name claim key
     */
    private static final String CLAIM_FIRST_NAME = "given_name";

    /**
     * Last name claim key
     */
    private static final String CLAIM_LAST_NAME = "family_name";

    /**
     * Email claim key
     */
    private static final String CLAIM_EMAIL = "email";

    /**
     * Full name claim key
     */
    private static final String CLAIM_FULL_NAME = "name";

    /**
     * Is admin flag claim key
     */
    private static final String CLAIM_IS_ADMIN = "is_admin";

    /**
     * Credential list claim key
     */
    private static final String CLAIM_CREDENTIALS_LIST = "credentials_list";

    /**
     * Claims type definitions
     */
    private static Map<String, Class<?>> claimClasses = new HashMap<String, Class<?>>() {
        {
            put(PublicClaims.ISSUER, String.class);
            put(PublicClaims.SUBJECT, String.class);
            put(PublicClaims.ISSUED_AT, Date.class);
            put(PublicClaims.EXPIRES_AT, Date.class);
            put(CLAIM_FIRST_NAME, String.class);
            put(CLAIM_LAST_NAME, String.class);
            put(CLAIM_EMAIL, String.class);
            put(CLAIM_FULL_NAME, String.class);
            put(CLAIM_IS_ADMIN, Boolean.class);
            put(CLAIM_CREDENTIALS_LIST, List.class);
        }
    };

    /**
     * Map of registred users by URI
     */
    private ConcurrentHashMap<URI, UserModel> userRegistry = new ConcurrentHashMap<>();

    /**
     * Auto-logout thread map by users
     */
    private ConcurrentHashMap<URI, Thread> schedulerRegistry = new ConcurrentHashMap<>();

    /**
     * RSA encryption algorithm for JWT token generation
     */
    private final Algorithm algoRSA;

    /**
     * Constructor initializing a new RSA key pair for JWT token generation
     *
     * @throws NoSuchAlgorithmException should never happend
     */
    public AuthenticationService() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(RSA_KEY_SIZE);
        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
        algoRSA = Algorithm.RSA512(publicKey, privateKey);
    }

    /**
     * Generate password has with bcrypt
     *
     * @param password Password to hash
     * @return Hashed password
     */
    public String getPasswordHash(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        return BCrypt.withDefaults().hashToString(PASSWORD_HASH_COMPLEXITY, password.toCharArray());
    }

    /**
     * Check if a given password correspond to the given hash
     *
     * @param password Password to check
     * @param passwordHash Hash to check password with
     * @return true if password and hash match, false otherwise
     */
    public boolean checkPassword(String password, String passwordHash) {
        return BCrypt.verifyer().verify(password.getBytes(), passwordHash.getBytes()).verified;
    }

    /**
     * Generate a token for given user and credentials
     *
     * @param user User to build token on
     * @param credentialsList Credential list to setup for user in token
     */
    public void generateToken(UserModel user, List<String> credentialsList) {
        // Set issue date a now
        Date issuedDate = new Date();

        // Compute expiration date
        Date expirationDate = Date.from(issuedDate.toInstant().plus(TOKEN_VALIDITY_DURATION, TOKEN_VALIDITY_DURATION_UNIT));

        // Create token
        JWTCreator.Builder tokenBuilder = JWT.create()
                // Standardized claims
                .withIssuer(TOKEN_ISSUER)
                .withSubject(user.getUri().toString())
                .withIssuedAt(issuedDate)
                .withExpiresAt(expirationDate)
                .withClaim(CLAIM_FIRST_NAME, user.getFirstName())
                .withClaim(CLAIM_LAST_NAME, user.getLastName())
                .withClaim(CLAIM_EMAIL, user.getEmail().toString())
                .withClaim(CLAIM_FULL_NAME, user.getName())
                // custom claim
                .withClaim(CLAIM_IS_ADMIN, user.isAdmin());

        // Add credential list
        if (credentialsList != null) {
            tokenBuilder.withArrayClaim(CLAIM_CREDENTIALS_LIST, credentialsList.toArray(new String[credentialsList.size()]));
        }

        // Allow any module implementing LoginExtension to add custom claims on login
        OpenSilex.getInstance().getModulesImplementingInterface(LoginExtension.class).forEach(module -> {
            module.login(user, tokenBuilder);
        });

        // Set signed token to the user
        user.setToken(tokenBuilder.sign(algoRSA));
    }

    /**
     * <pre>
     * Renew JWT token  for a user.
     * Copy all claims defined from the previous token.
     * Reset issue date to now and compute expiration date again
     * </pre>
     *
     * @param user User having token to renew
     */
    public boolean renewToken(UserModel user) {
        if (user.getToken() != null) {

            // Check and decode current user token
            JWTVerifier verifier = JWT.require(algoRSA)
                    .withIssuer(TOKEN_ISSUER)
                    .build();
            DecodedJWT jwt = verifier.verify(user.getToken());

            // Create a new token
            JWTCreator.Builder tokenBuilder = JWT.create();

            // Copy all claims from previous token
            jwt.getClaims().forEach((key, claim) -> {
                if (claimClasses.containsKey(key)) {
                    switch (claimClasses.get(key).getSimpleName()) {
                        case "Boolean":
                            tokenBuilder.withClaim(key, claim.asBoolean());
                            break;
                        case "Integer":
                            tokenBuilder.withClaim(key, claim.asInt());
                            break;
                        case "Date":
                            tokenBuilder.withClaim(key, claim.asDate());
                            break;
                        case "List":
                            tokenBuilder.withArrayClaim(key, claim.asArray(String.class));
                            break;
                        case "String":
                        default:
                            tokenBuilder.withClaim(key, claim.asString());
                            break;
                    }
                } else {
                    LOGGER.warn("Unmanaged token claim, try to read as string: " + key + " -> " + claim.asString());
                    tokenBuilder.withClaim(key, claim.asString());
                }
            });

            // Reset issued date and expiration date
            Date issuedDate = new Date();
            Date expirationDate = Date.from(issuedDate.toInstant().plus(TOKEN_VALIDITY_DURATION, TOKEN_VALIDITY_DURATION_UNIT));
            tokenBuilder.withIssuer(TOKEN_ISSUER)
                    .withIssuedAt(issuedDate)
                    .withExpiresAt(expirationDate);

            // Set new signed token to user
            user.setToken(tokenBuilder.sign(algoRSA));
            addUser(user, getExpireInMs());
            
            return true;
        }
        
        return false;
    }

    /**
     * Get user URI from a token
     *
     * @param tokenValue JWT token string
     * @return User uri
     * @throws JWTVerificationException In case of token validation error
     * @throws URISyntaxException In case of invalid user URI in token (should
     * never happend)
     */
    public URI decodeTokenUserURI(String tokenValue) throws JWTVerificationException, URISyntaxException {
        JWTVerifier verifier = JWT.require(algoRSA)
                .withIssuer(TOKEN_ISSUER)
                .build();

        DecodedJWT jwt = verifier.verify(tokenValue);

        return new URI(jwt.getSubject());
    }

    /**
     * Get credentials list from token
     *
     * @param tokenValue JWT token string
     * @return credentials id list
     */
    public String[] decodeTokenCredentialsList(String tokenValue) {
        JWTVerifier verifier = JWT.require(algoRSA)
                .withIssuer(TOKEN_ISSUER)
                .build();

        DecodedJWT jwt = verifier.verify(tokenValue);

        return jwt.getClaim(CLAIM_CREDENTIALS_LIST).asArray(String.class);
    }

    public String[] decodeStringArrayClaim(String token, String key){

        return JWT.require(algoRSA)
                .withIssuer(TOKEN_ISSUER)
                .build()
                .verify(token)
                .getClaim(key).asArray(String.class);
    }

    /**
     * Return authentication expiration delay in seconds
     *
     * @return expiration delay in seconds
     */
    public long getExpiresInSec() {
        return TOKEN_VALIDITY_DURATION * TOKEN_VALIDITY_DURATION_UNIT.getDuration().getSeconds();
    }

    /**
     * Return authentication expiration delay in milliseconds
     *
     * @return expiration delay in milliseconds
     */
    public long getExpireInMs() {
        return getExpiresInSec() * 1000;
    }

    /**
     * Helper method to extract user with right type from security context.
     *
     * @param securityContext Application security context
     * @return Current user
     */
    public UserModel getCurrentUser(SecurityContext securityContext) {
        return (UserModel) securityContext.getUserPrincipal();
    }

    /**
     * Check if given user is authenticated
     *
     * @param user User to check
     * @return true if user is authenticated, false otherwise
     */
    public synchronized boolean hasUser(UserModel user) {
        return hasUserURI(user.getUri());
    }

    /**
     * Add a user with an authentication delay. Create a thread per user which
     * just sleep until delay is expired.
     *
     * @param user Userto add
     * @param expireMs authentication delay in milliseconds
     */
    public synchronized void addUser(UserModel user, long expireMs) {
        URI userURI = user.getUri();

        // If user already registred remove it
        if (hasUserURI(userURI)) {
            removeUserByURI(userURI);
        }

        // Create thread to remove user after expire time
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(expireMs);
                LOGGER.debug("User connection timeout: " + userURI);
                removeUser(user);
            } catch (InterruptedException e) {
                LOGGER.debug("Revoke user: " + userURI + " - " + e.getMessage());
            }
        });

        // Start thread
        t.start();

        // Add user and logout thread into concurrent map registry
        userRegistry.put(userURI, user);
        schedulerRegistry.put(userURI, t);

        LOGGER.debug("User registered: " + userURI);
    }

    /**
     * Help method to remove user
     *
     * @param user User to remove from registry
     * @return removed user or null
     */
    public synchronized UserModel removeUser(UserModel user) {
        return removeUserByURI(user.getUri());
    }

    /**
     * Help method to remove user by URI
     *
     * @param userURI User URI to remove from registry
     * @return removed user or null if not found
     */
    public synchronized UserModel removeUserByURI(URI userURI) {
        if (hasUserURI(userURI)) {
            LOGGER.debug("Unregister user: " + userURI);
            schedulerRegistry.get(userURI).interrupt();
            schedulerRegistry.remove(userURI);

            UserModel user = userRegistry.remove(userURI);
            
            // Allow any module implementing LoginExtension to do something on logout
            OpenSilex.getInstance().getModulesImplementingInterface(LoginExtension.class).forEach(module -> {
                module.logout(user);
            });

            return user;
        }

        return null;
    }

    /**
     * Check if user URI is registred
     *
     * @param userURI User URI to check
     * @return true if user is registred false otherwise
     */
    public synchronized boolean hasUserURI(URI userURI) {
        return userRegistry.containsKey(userURI);
    }

    /**
     * Return a registred user by URI
     *
     * @param userURI User URI to get
     * @return registred user or null
     */
    public synchronized UserModel getUserByUri(URI userURI) {
        return userRegistry.get(userURI);
    }
    
    public boolean authenticate(UserModel user, String password, List<String> accessList) throws Exception {
        if ((user != null && checkPassword(password, user.getPasswordHash()))) {
            generateToken(user, accessList);
            addUser(user, getExpireInMs());
            return true;
        }
        return false;
    }

    public boolean authenticate(UserModel user, List<String> accessList) throws Exception {
        generateToken(user, accessList);
        addUser(user, getExpireInMs());
        return true;
    }

    public boolean logout(UserModel user) {
        return (removeUser(user) != null);
    }

}
