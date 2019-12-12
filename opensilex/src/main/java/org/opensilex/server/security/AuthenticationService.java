//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
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
import javax.ws.rs.core.SecurityContext;
import org.opensilex.OpenSilex;
import org.opensilex.service.Service;
import org.opensilex.server.user.dal.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Vincent Migot
 */
public class AuthenticationService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    public static final String DEFAULT_AUTHENTICATION_SERVICE = "authentication";

    private static final int PASSWORD_HASH_COMPLEXITY = 12;

    private static final long TOKEN_VALIDITY_DURATION = 30;
    private static final TemporalUnit TOKEN_VALIDITY_DURATION_UNIT = ChronoUnit.MINUTES;
//    private static final long TOKEN_VALIDITY_DURATION = 10;
//    private static final TemporalUnit TOKEN_VALIDITY_DURATION_UNIT = ChronoUnit.SECONDS;

    private static final String TOKEN_ISSUER = "opensilex";

    // JWT claim key definitions: https://www.iana.org/assignments/jwt/jwt.xhtml
    private static final String CLAIM_FIRST_NAME = "given_name";
    private static final String CLAIM_LAST_NAME = "family_name";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_FULL_NAME = "name";
    private static final String CLAIM_IS_ADMIN = "is_admin";
    private static final String CLAIM_ACCESS_LIST = "access_list";

    private Map<URI, UserModel> userRegistry = new HashMap<>();
    private Map<URI, Thread> schedulerRegistry = new HashMap<>();

    private final Algorithm algoRSA;

    public AuthenticationService() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
        algoRSA = Algorithm.RSA512(publicKey, privateKey);
    }

    public String getPasswordHash(String password) {
        return BCrypt.withDefaults().hashToString(PASSWORD_HASH_COMPLEXITY, password.toCharArray());
    }

    public boolean checkPassword(String password, String passwordHash) {
        return BCrypt.verifyer().verify(password.getBytes(), passwordHash.getBytes()).verified;
    }

    public void generateToken(UserModel user, List<String> accessList) {
        Date issuedDate = new Date();
        Date expirationDate = Date.from(issuedDate.toInstant().plus(TOKEN_VALIDITY_DURATION, TOKEN_VALIDITY_DURATION_UNIT));
        JWTCreator.Builder tokenBuilder = JWT.create()
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

        if (accessList != null) {
            tokenBuilder.withArrayClaim(CLAIM_ACCESS_LIST, accessList.toArray(new String[accessList.size()]));
        }

        OpenSilex.getInstance().getModules().forEach(module -> {
            module.addLoginClaims(user, tokenBuilder);
        });

        user.setToken(tokenBuilder.sign(algoRSA));
    }

    public void renewToken(UserModel user) {
        if (user.getToken() == null) {

        }
        JWTVerifier verifier = JWT.require(algoRSA)
                .withIssuer(TOKEN_ISSUER)
                .build();

        DecodedJWT jwt = verifier.verify(user.getToken());

        Date issuedDate = new Date();
        Date expirationDate = Date.from(issuedDate.toInstant().plus(TOKEN_VALIDITY_DURATION, TOKEN_VALIDITY_DURATION_UNIT));
        JWTCreator.Builder tokenBuilder = JWT.create()
                .withIssuer(TOKEN_ISSUER)
                .withSubject(jwt.getSubject())
                .withIssuedAt(issuedDate)
                .withExpiresAt(expirationDate);

        jwt.getClaims().forEach((key, claim) -> {
            try {
                String[] claimArray = claim.asArray(String.class);
                if (claimArray == null) {
                    String claimValue = claim.asString();
                    tokenBuilder.withClaim(key, claimValue);
                } else {
                    tokenBuilder.withArrayClaim(key, claimArray);
                }
            } catch (JWTDecodeException ex) {
                String claimValue = claim.asString();
                tokenBuilder.withClaim(key, claimValue);
            }
        });

        user.setToken(tokenBuilder.sign(algoRSA));
    }

    public URI decodeTokenUserURI(String tokenValue) throws JWTVerificationException, URISyntaxException {
        JWTVerifier verifier = JWT.require(algoRSA)
                .withIssuer(TOKEN_ISSUER)
                .build();

        DecodedJWT jwt = verifier.verify(tokenValue);

        return new URI(jwt.getSubject());
    }

    public String[] decodeTokenAccessList(String tokenValue) {
        JWTVerifier verifier = JWT.require(algoRSA)
                .withIssuer(TOKEN_ISSUER)
                .build();

        DecodedJWT jwt = verifier.verify(tokenValue);

        return jwt.getClaim(CLAIM_ACCESS_LIST).asArray(String.class);
    }

    public long getExpiresInSec() {
        return TOKEN_VALIDITY_DURATION * TOKEN_VALIDITY_DURATION_UNIT.getDuration().getSeconds();
    }

    public UserModel getCurrentUser(SecurityContext securityContext) {
        return (UserModel) securityContext.getUserPrincipal();
    }

    public long getExpireInMs() {
        return getExpiresInSec() * 1000;
    }

    public boolean hasUser(UserModel user) {
        return hasUserURI(user.getUri());
    }

    public void addUser(UserModel user, long expireMs) {
        URI userURI = user.getUri();

        if (hasUserURI(userURI)) {
            removeUserByURI(userURI);
        }

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(expireMs);
                LOGGER.debug("User connection timeout: " + userURI);
                removeUser(user);
            } catch (InterruptedException e) {
                LOGGER.debug("Revoke user: " + userURI + " - " + e.getMessage());
            }
        });

        t.start();

        userRegistry.put(userURI, user);
        schedulerRegistry.put(userURI, t);

        LOGGER.debug("Register user: " + userURI);
    }

    public UserModel removeUser(UserModel user) {
        return removeUserByURI(user.getUri());
    }

    public UserModel removeUserByURI(URI userURI) {
        if (hasUserURI(userURI)) {
            LOGGER.debug("Unregister user: " + userURI);
            schedulerRegistry.get(userURI).interrupt();
            schedulerRegistry.remove(userURI);
            return userRegistry.remove(userURI);
        }

        return null;
    }

    public boolean hasUserURI(URI userURI) {
        return userRegistry.containsKey(userURI);
    }

    public UserModel getUserByUri(URI userURI) {
        return userRegistry.get(userURI);
    }
}
