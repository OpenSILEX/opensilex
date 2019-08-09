/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.opensilex.service.Service;

/**
 *
 * @author vincent
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

    public String generateToken(URI userURI) {
        Date issuedDate = new Date();
        Date expirationDate = Date.from(issuedDate.toInstant().plus(TOKEN_VALIDITY_DURATION, TOKEN_VALIDITY_DURATION_UNIT));
        JWTCreator.Builder tokenBuilder = JWT.create()
                .withIssuer(TOKEN_ISSUER)
                .withSubject(userURI.toString())
                .withIssuedAt(issuedDate)
                .withExpiresAt(expirationDate);

        return tokenBuilder.sign(algoRSA);
    }

    public URI decodeTokenUserURI(String tokenValue) throws JWTVerificationException, URISyntaxException {
        JWTVerifier verifier = JWT.require(algoRSA)
                .withIssuer(TOKEN_ISSUER)
                .build();

        DecodedJWT jwt = verifier.verify(tokenValue);

        return new URI(jwt.getSubject());
    }

}
