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
import java.util.List;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.service.Service;
import org.opensilex.server.user.dal.UserModel;

/**
 *
 * @author Vincent Migot
 */
public class AuthenticationService implements Service {

    public final static String DEFAULT_AUTHENTICATION_SERVICE = "authentication";
        
    private static final int PASSWORD_HASH_COMPLEXITY = 12;
    
    private static final long TOKEN_VALIDITY_DURATION = 1;
    private static final TemporalUnit TOKEN_VALIDITY_DURATION_UNIT = ChronoUnit.HOURS;
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

    public String generateToken(UserModel user) {
        return generateToken(user, null);
    }

    public String generateToken(UserModel user, List<String> accessList) {
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
        
        return tokenBuilder.sign(algoRSA);
    }

    public URI decodeTokenUserURI(String tokenValue) throws JWTVerificationException, URISyntaxException {
        JWTVerifier verifier = JWT.require(algoRSA)
                .withIssuer(TOKEN_ISSUER)
                .build();

        DecodedJWT jwt = verifier.verify(tokenValue);

        return new URI(jwt.getSubject());
    }

    public static long getExpiresInSec() {
        return TOKEN_VALIDITY_DURATION * TOKEN_VALIDITY_DURATION_UNIT.getDuration().getSeconds();
    }
    
    public UserModel getCurrentUser(SecurityContext securityContext) {
        return (UserModel) securityContext.getUserPrincipal();
    }

}
