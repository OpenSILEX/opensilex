//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.security;

import at.favre.lib.crypto.bcrypt.*;
import com.auth0.jwt.*;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.*;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.*;
import java.net.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.temporal.*;
import java.util.*;
import org.opensilex.server.security.model.*;
import org.opensilex.service.*;

/**
 *
 * @author Vincent Migot
 */
public class AuthenticationService implements Service {

    private static final int PASSWORD_HASH_COMPLEXITY = 12;
    private static final long TOKEN_VALIDITY_DURATION = 1;
    private static final TemporalUnit TOKEN_VALIDITY_DURATION_UNIT = ChronoUnit.HOURS;
    private static final String TOKEN_ISSUER = "opensilex";

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

    public String generateToken(User user) {
        Date issuedDate = new Date();
        Date expirationDate = Date.from(issuedDate.toInstant().plus(TOKEN_VALIDITY_DURATION, TOKEN_VALIDITY_DURATION_UNIT));
        JWTCreator.Builder tokenBuilder = JWT.create()
                .withIssuer(TOKEN_ISSUER)
                .withSubject(user.getUri().toString())
                .withIssuedAt(issuedDate)
                .withExpiresAt(expirationDate)
                .withClaim("firstname", user.getFirstName())
                .withClaim("lastname", user.getLastName())
                .withClaim("email", user.getEmail().toString())
                .withClaim("name", user.getName());

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
}
