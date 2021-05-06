//******************************************************************************
//                      AuthenticationService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.impl.PublicClaims;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import java.io.IOException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.SecurityContext;
import net.minidev.json.JSONObject;
import org.opensilex.security.OpenIDConfig;
import org.opensilex.security.SecurityConfig;
import org.opensilex.security.SecurityModule;
import org.opensilex.service.Service;
import org.opensilex.security.user.dal.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensilex.security.extensions.LoginExtension;
import org.opensilex.security.user.dal.UserDAO;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.service.BaseService;

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
 * For existing claim ids, see: https://www.iana.org/assignments/jwt/jwt.xhtml#claims
 * </pre>
 *
 * @author Vincent Migot
 */
public class AuthenticationService extends BaseService implements Service {

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
     * Renew token validity duration unit
     */
    private static final TemporalUnit RENEW_TOKEN_VALIDITY_DURATION_UNIT = ChronoUnit.HOURS;

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
     * Locale claim key
     */
    private static final String CLAIM_LOCALE = "locale";

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
            put(CLAIM_LOCALE, String.class);
            put(CLAIM_IS_ADMIN, Boolean.class);
            put(CLAIM_CREDENTIALS_LIST, List.class);
        }
    };

    public static void registerClaimClass(String claim, Class<?> cls) {
        claimClasses.put(claim, cls);
    }

    /**
     * Map of registred users by URI
     */
    private ConcurrentHashMap<URI, UserModel> userRegistry = new ConcurrentHashMap<>();

    /**
     * Auto-logout thread map by users
     */
    private ConcurrentHashMap<URI, Thread> schedulerRegistry = new ConcurrentHashMap<>();

    
    /**
     * Map of forgot users URI by uuid URI
     */
    private ConcurrentHashMap<URI, URI> forgotPasswordUserRegistry = new ConcurrentHashMap<>();

    /**
     * Forgot password thread map by users
     */
    private ConcurrentHashMap<URI, Thread> schedulerForgotUserPasswordRegistry = new ConcurrentHashMap<>();

    public static final String DEFAULT_SUPER_ADMIN_EMAIL = "admin@opensilex.org";
    
    /**
     * RSA encryption algorithm for JWT token generation
     */
    private final Algorithm algoRSA;

    
    /**
     * reset password uuid validity duration in days
     */
    private static final long RENEW_TOKEN_PASSWORD_VALIDITY_DURATION = 24;
    
    /**
     * Constructor initializing a new RSA key pair for JWT token generation
     *
     * @throws NoSuchAlgorithmException should never happend
     */
    public AuthenticationService() throws NoSuchAlgorithmException {
        super(null);
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
     * @throws Exception in case of token generation failure
     */
    public void generateToken(UserModel user, List<String> credentialsList) throws Exception {
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
                .withClaim(CLAIM_LOCALE, user.getLanguage())
                // custom claim
                .withClaim(CLAIM_IS_ADMIN, user.isAdmin());

        // Add credential list
        if (credentialsList != null) {
            tokenBuilder.withArrayClaim(CLAIM_CREDENTIALS_LIST, credentialsList.toArray(new String[credentialsList.size()]));
        }

        // Allow any module implementing LoginExtension to add custom claims on login
        for (LoginExtension module : getOpenSilex().getModulesImplementingInterface(LoginExtension.class)) {
            module.login(user, tokenBuilder);
        }

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
     * @return true if token has been renewed and false if not (in case user has no previous token)
     * @throws Exception in case of token renew failure
     */
    public boolean renewToken(UserModel user) throws Exception {
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
                } else if (claim.asString() != null) {
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
     * @throws URISyntaxException In case of invalid user URI in token (should never happend)
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

    public String[] decodeStringArrayClaim(String token, String key) {

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
     * Add a user with an authentication delay. Create a thread per user which just sleep until delay is expired.
     *
     * @param user Userto add
     * @param expireMs authentication delay in milliseconds
     * @throws Exception in case previous user connection can't be cleared
     */
    public synchronized void addUser(UserModel user, long expireMs) throws Exception {
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
            } catch (InterruptedException ex) {
                LOGGER.debug("Revoke user: " + userURI + " - " + ex.getMessage());
            } catch (Exception ex) {
                LOGGER.warn("Error while removing user: " + userURI + " - ", ex);
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
     * @throws Exception in case user connection can't be cleared
     */
    public synchronized UserModel removeUser(UserModel user) throws Exception {
        return removeUserByURI(user.getUri());
    }

    /**
     * Help method to remove user by URI
     *
     * @param userURI User URI to remove from registry
     * @return removed user or null if not found
     * @throws Exception in case user connection can't be cleared
     */
    public synchronized UserModel removeUserByURI(URI userURI) throws Exception {
        boolean allowMultiConnection = getOpenSilex().getModuleConfig(SecurityModule.class, SecurityConfig.class).allowMultiConnection();
        if (!allowMultiConnection && hasUserURI(userURI)) {
            LOGGER.debug("Unregister user: " + userURI);
            schedulerRegistry.get(userURI).interrupt();
            schedulerRegistry.remove(userURI);

            UserModel user = userRegistry.remove(userURI);

            // Allow any module implementing LoginExtension to do something on logout
            for (LoginExtension module : getOpenSilex().getModulesImplementingInterface(LoginExtension.class)) {
                module.logout(user);
            }

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

    public boolean logout(UserModel user) throws Exception {
        return (removeUser(user) != null);
    }

    private static OIDCProviderMetadata providerMetadata = null;

    private OIDCProviderMetadata getOpenIDProviderMetadata(OpenIDConfig config) throws Exception {
        if (providerMetadata == null) {
            URI issuerURI = new URI(config.providerURI());
            URL providerConfigurationURL = issuerURI.resolve(".well-known/openid-configuration").toURL();
            InputStream stream = providerConfigurationURL.openStream();

            // Read all data from URL
            String providerInfo = null;
            try (java.util.Scanner s = new java.util.Scanner(stream)) {
                providerInfo = s.useDelimiter("\\A").hasNext() ? s.next() : "";
            }
            providerMetadata = OIDCProviderMetadata.parse(providerInfo);
        }
        
        return providerMetadata;
    }

    public UserModel authenticateWithOpenID(String code, OpenIDConfig config) throws Exception {
        if (!config.enable()) {
            throw new ForbiddenException("OpenID is not enabled on this server");
        }

        OIDCProviderMetadata providerMetadata = getOpenIDProviderMetadata(config);
        Scope scope = new Scope("openid", "email", "profile");
        TokenRequest tokenReq = new TokenRequest(
                providerMetadata.getTokenEndpointURI(),
                new ClientSecretBasic(new ClientID(config.clientID()), new Secret(config.clientSecret())),
                new AuthorizationCodeGrant(new AuthorizationCode(code), new URI(config.redirectURI())),
                scope
        );

        HTTPResponse tokenHTTPResp = null;
        try {
            tokenHTTPResp = tokenReq.toHTTPRequest().send();
        } catch (SerializeException | IOException e) {
            throw new OpenIDException("Unexpected OpenID eror", e);
        }

        TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp);

        if (tokenResponse instanceof TokenErrorResponse) {
            ErrorObject error = ((TokenErrorResponse) tokenResponse).getErrorObject();
            throw new OpenIDException("Error while getting OpenID token: " + error.getDescription());
        }

        AccessTokenResponse accessTokenResponse = (AccessTokenResponse) tokenResponse;
        Tokens tokens = accessTokenResponse.getTokens();

        UserInfoRequest userInfoReq = new UserInfoRequest(
                providerMetadata.getUserInfoEndpointURI(),
                tokens.getBearerAccessToken());

        HTTPResponse userInfoHTTPResp = null;
        try {
            userInfoHTTPResp = userInfoReq.toHTTPRequest().send();
        } catch (SerializeException | IOException e) {
            throw new OpenIDException("Unexpected OpenID eror", e);
        }

        UserInfoResponse userInfoResponse = null;
        userInfoResponse = UserInfoResponse.parse(userInfoHTTPResp);

        if (userInfoResponse instanceof UserInfoErrorResponse) {
            ErrorObject error = ((UserInfoErrorResponse) userInfoResponse).getErrorObject();
            throw new OpenIDException("Error while getting OpenID user info: " + error.getDescription());

        }

        UserInfoSuccessResponse successResponse = (UserInfoSuccessResponse) userInfoResponse;
        JSONObject claims = successResponse.getUserInfo().toJSONObject();

        UserModel user = new UserModel();
        user.setEmail(new InternetAddress(claims.getAsString("email")));
        user.setFirstName(claims.getAsString("given_name"));
        user.setLastName(claims.getAsString("family_name"));

        return user;
    }

    public URI getOpenIDAuthenticationURI() throws Exception {
        OpenIDConfig config = getOpenSilex().getModuleConfig(SecurityModule.class, SecurityConfig.class).openID();

        if (!config.enable()) {
            return null;
        }

        if (config.providerURI() == null || config.providerURI().isEmpty()) {
            return null;
        }

        OIDCProviderMetadata providerMetadata = getOpenIDProviderMetadata(config);

        State state = new State();

        // Generate nonce
        Nonce nonce = new Nonce();

        // Specify scope
        Scope scope = new Scope("openid", "email", "profile");

        URI redirectURI = new URI(config.redirectURI());

        // Compose the request
        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                providerMetadata.getAuthorizationEndpointURI(),
                new ResponseType(ResponseType.Value.CODE),
                scope, new ClientID(config.clientID()), redirectURI, state, nonce);

        URI authReqURI = authenticationRequest.toURI();

        return authReqURI;
    }

     /**
     * Return password token expiration delay in seconds
     *
     * @return renew password token expiration delay in seconds
     */
    private long getRenewTokenExpiresInSec() {
        return RENEW_TOKEN_PASSWORD_VALIDITY_DURATION * RENEW_TOKEN_VALIDITY_DURATION_UNIT.getDuration().getSeconds();
    }

    
     /**
     * Add a user which has forgotten his password 
     * @param userUri user uri
     * @return URI token generated renew token uri
     * @throws Exception in case previous uuid connection can't be cleared
     */
    public synchronized URI addForgotPasswordId(URI userUri) throws Exception {
        UUID uuid = UUID.randomUUID();
        URI uuidURI = new URI("os-reset-pwd:"+ uuid.toString().replaceAll("-", ""));

        // If uuid already registred remove it
        if (schedulerForgotUserPasswordRegistry.containsKey(uuidURI)) {
            schedulerForgotUserPasswordRegistry.get(uuidURI).interrupt();
            schedulerForgotUserPasswordRegistry.remove(uuidURI);
        }
        LOGGER.debug("Renew token expires in :" + getRenewTokenExpiresInSec());
        // Create thread to remove user after expire time
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(getRenewTokenExpiresInSec());
                LOGGER.debug("Renew token UUID expires: " + uuidURI);
                schedulerForgotUserPasswordRegistry.remove(uuidURI);
            } catch (InterruptedException ex) {
                LOGGER.debug("Revoke renew token UUID: " + uuidURI + " - " + ex.getMessage());
            } catch (Exception ex) {
                LOGGER.warn("Error while removing user fogotten password: " + uuidURI + " - ", ex);
            }
        });

        // Start thread
        t.start();

        // Add user and logout thread into concurrent map registry
        forgotPasswordUserRegistry.put(uuidURI, userUri);
        schedulerForgotUserPasswordRegistry.put(uuidURI, t);
        LOGGER.debug("Renew token UUID registered: " + uuidURI + "for user : " + userUri);
        
        return uuidURI;
    }
    
    public URI getForgottenPasswordUserURIFromRenewToken(URI uuidURI) throws Exception {    
        // If uuid already registred remove it
        if (!schedulerForgotUserPasswordRegistry.containsKey(uuidURI)) {
            LOGGER.debug("Unknown Renew token UUID: " + uuidURI);
            return null;
        }       
        
        return forgotPasswordUserRegistry.get(uuidURI);
    }
    
    public boolean removeForgottenPasswordUserFromRenewToken(URI uuidURI) throws Exception {    

        // If uuid already registred remove it
        if (!schedulerForgotUserPasswordRegistry.containsKey(uuidURI)) {
            LOGGER.debug("Unknown Renew token UUID: " + uuidURI);
         }else{
            schedulerForgotUserPasswordRegistry.get(uuidURI).interrupt();
            schedulerForgotUserPasswordRegistry.remove(uuidURI);
            forgotPasswordUserRegistry.remove(uuidURI);
        }
        return true;
    }
}
