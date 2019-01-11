//**********************************************************************************************
//                                       TokenResourceService.java 
//
// Author(s): Samuel Cherimont, Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2015
// Creation date: november 2015
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  March, 2018
// Subject: Represents the token data service
//***********************************************************************************************
package phis2ws.service.resources.brapi;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.authentication.Session;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.authentication.TokenManager;
import phis2ws.service.authentication.TokenResponseStructure;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.resources.dto.LogoutDTO;
import phis2ws.service.resources.dto.TokenDTO;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseUnique;
import phis2ws.service.view.model.phis.Call;

/**
 * REST Web Service
 *
 * TokenResourceService - Classe correspondant au chemin brapi/v1/token ou token
 * du Web Service
 *
 * @version 1.0
 *
 * @author Samuël Chérimont
 * @date 26/11/2015
 * @update 03/08/2016 Ajout du JWT 03/2018 Update for jwt
 * @update [Alice Boizet] 27 July, 2018 : override callInfo() to add token call description in the brapi calls service
 * @see https://jwt.io/introduction/
 * @see
 * http://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-rsa-signature
 */
//@Path("{brapi/v1/token|token}")
//@RolesAllowed("ADMIN")
//@PermitAll
@Api(value = "/brapi/v1/token")
@Path("brapi/v1/token")
public class TokenResourceService implements BrapiCall{
    
    final static Logger LOGGER = LoggerFactory.getLogger(TokenResourceService.class);
    static final Map<String, String> ISSUERS_PUBLICKEY;
    static final List<String> GRANTTYPE_AUTHORIZED = Collections.unmodifiableList(Arrays.asList("jwt", "password"));

    static {
        Map<String, String> temporaryMap = new HashMap<>();
        // can put multiple issuers of jwt
        temporaryMap.put("GnpIS", "gnpisPublicKeyFileName");
        temporaryMap.put("Phis", "phisPublicKeyFileName");
        ISSUERS_PUBLICKEY = Collections.unmodifiableMap(temporaryMap);
    }
    //SILEX:conception
    // To keep jwt claimset information during the loggin
    private JWTClaimsSet jwtClaimsSet = null;
    

    /**Overriding BrapiCall method
     * @date 27 July, 2018 
     * @return Token call information
     */
    @Override
    public ArrayList<Call> callInfo(){
        ArrayList<Call> calls = new ArrayList();
        ArrayList<String> calldatatypes = new ArrayList<>();        
        calldatatypes.add("json");        
        ArrayList<String> callMethods = new ArrayList<>();  
        callMethods.add("POST");
        callMethods.add("DELETE");        
        ArrayList<String> callVersions = new ArrayList<>();  
        callVersions.add("1.2");    
        Call tokencall = new Call("token",calldatatypes,callMethods,callVersions);
        calls.add(tokencall);
        return calls;
    }
    
    //\SILEX:conception

    /**
     * getToken() - Méthode appelé par la requête HTTP GET suivi de l'URI de la
     * classe TokenResourceService Crée un identifiant de session avec
     * createId(), ou le récupère si il existe déja après avoir vérifié la
     * validité des identifiants grâce à la méthode checkAuthentification
     *
     * exemple GET brapi/v1/jsonToken?username=true&password=true
     *
     * @param jsonToken body json
     * @param ui
     * @return un objet Response contenant le résultat en JSON ou une erreur de
     * type BAD_REQUEST
     *
     * @see
     * createId(),checkConnectionIds(),TokenManager.searchSession(),Sessionb
     * @date 26/11/2015
     * @note La méthode GET doit être remplacée par POST pour des raisons de
     * sécurité
     * @update AC 05/2016 Ajout TokenDAO et modification des fichiers de
     * propriétés
     * @update AC 07/2016 Ajout JWT et modification des fichiers de propriétés +
     * ajout DAO
     * @update AC 08/2016 Change BRAPI API class example Token
     * @update AC 03/2018 Cleaning usage of jwt
     * JWT EXAMPLE 
     * HEADER { "typ": "JWT", "alg": "RS256" } 
     * PAYLOAD { "iss": "Phis", "sub": "arnaud.charleroy@supagro.inra.fr",
     * "iat": 1520261602, "exp": 1583420358 } 
     * PUBLIC KEY
     * eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJQaGlzIiwic3ViIjoibW9yZ2Fu
     * ZS52aWRhbEBzdXBhZ3JvLmlucmEuZnIiLCJpYXQiOjE1MjAyNjE5NTgsImV4cCI6MTU4MzQyMD
     * M1OH0.gbN40oHVLjowFiYFDnKw_vUQGFZBaRXmIRykBzuqavfYZnWHeFOTNd8J5k20dzuRrTCrLa
     * dMVsDk1fw-E6DbA19IMQlGRnqMPjjsK9r36W42WyQ70GxloKMNFqFWBFfhs2CV3ND_UrSquXmDRwp
     * wAtz4wXVna0isAuuSyqaub41AjkomG6XhpDxtJUXYESHFWvRurmu06OzILSMVNWUDJRPelzBeVSx
     * iNGOrp56lGqBK2g4CUImVZsTaucSvWjSsu74E_HtOCA-UB0scaEwStrqsW_iwRJxyFTLWBxc6cXg1
     * 6034hCqP5f_d9u4OmvhpKc_9CLb8oQVbmuflv52iOQ
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Login",
            notes = "Returns an access token when the user is known and it issuer too",
            response = TokenResponseStructure.class)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Access token created by user")
        ,
        @ApiResponse(code = 400, message = "Bad informations send by user")
        ,
        @ApiResponse(code = 200, message = "Access token already exist and send again to user")})
    public Response getToken(@ApiParam(value = "JSON object needed to login") @Valid TokenDTO jsonToken, @Context UriInfo ui) {
        ArrayList<Status> statusList = new ArrayList<>();
//        Verification of grant type
        String grantType = jsonToken.getGrant_type();
        if (grantType == null || grantType.isEmpty() || !GRANTTYPE_AUTHORIZED.contains(grantType)) {
            statusList.add(new Status("Wrong grant type", StatusCodeMsg.ERR, "Authorized grant type : " + GRANTTYPE_AUTHORIZED.toString()));
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST(statusList)).build();
        }
        // Initialize variables
        Boolean isJWT = false;
        String username = jsonToken.getUsername();
        String password = null;

        boolean validJWTToken = false;
        // cas JWT
        if (grantType.equals("jwt") && jsonToken.getClient_id() != null) {
            isJWT = true;
            validJWTToken = validJWTToken(jsonToken, statusList);
        } else if (password == null) {
            // cas unsername /password
            password = jsonToken.getPassword();
        }
        if ((password != null && username != null) || (isJWT && validJWTToken && username != null)) {
            // Is user authorized ?
            User user = new User(username, password);
            try {
                //SILEX:info
                // if we have a jwt the password is not verified because it means that the
                // user is already logged
                // we trust the client
                //\SILEX:info
                if (isJWT && validJWTToken) {
                    user = checkAuthentification(user, false);
                } else {
                    user = checkAuthentification(user, true);
                }

                // No user found
                if (user == null) {
                    statusList.add(new Status("User/password doesn't exist", StatusCodeMsg.ERR, null));
                } else {
                    // User found, create a session
                    // token exist ?
                    String userSessionId = TokenManager.Instance().searchSession(username);
                    Response.Status reponseStatus = Response.Status.OK;
                    String expires_in = null;
                    if (userSessionId == null) {
                        //create a session and add information to this one 
                        userSessionId = this.createId(username);
                        Session newSession = new Session(userSessionId, username, user);
                        newSession.setJwtClaimsSet(this.jwtClaimsSet);
                        TokenManager.Instance().createToken(newSession);
                        reponseStatus = Response.Status.CREATED;
                    } else {
                        // retreive existing session
                        Session session = TokenManager.Instance().getSession(userSessionId);
                        DateTime sessionStartDateTime = Dates.convertStringToDateTime(session.getDateStart(), DateFormats.YMDHMS_FORMAT);
                        if (sessionStartDateTime != null) {
                            Seconds secondsBetween = Seconds.secondsBetween(sessionStartDateTime, new DateTime());
                            int expiration = Integer.valueOf(PropertiesFileManager.getConfigFileProperty("service", "sessionTime")) - secondsBetween.getSeconds();
                            //SILEX:info
                            //sometimes token expiration time become negative and crash the webapp
                            //this code force regeneration of a new token in this case
                            if (expiration <= 0) {
                                TokenManager.Instance().removeSession(userSessionId);
                                TokenManager.Instance().createToken(session);
                                sessionStartDateTime = Dates.convertStringToDateTime(session.getDateStart(), DateFormats.YMDHMS_FORMAT);
                                secondsBetween = Seconds.secondsBetween(sessionStartDateTime, new DateTime());
                                expiration = Integer.valueOf(PropertiesFileManager.getConfigFileProperty("service", "sessionTime")) - secondsBetween.getSeconds();
                            }
                            //\SILEX:info
                            expires_in = Integer.toString(expiration);
                        }
                    }
                    // return result
                    TokenResponseStructure res = new TokenResponseStructure(userSessionId, user.getFirstName() + " " + user.getFamilyName(), expires_in);
                    final URI uri = new URI(ui.getPath());
                    return Response.status(reponseStatus).location(uri).entity(res).build();
                }

            } catch (NoSuchAlgorithmException | SQLException | URISyntaxException ex) {
                LOGGER.error(ex.getMessage(), ex);
                statusList.add(new Status("SQL " + StatusCodeMsg.ERR, StatusCodeMsg.ERR, ex.getMessage()));
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseFormPOST(statusList)).build();
            }
        } else {
            // if an error has occurred
            if (!isJWT && password == null) {
                statusList.add(new Status("Empty password", StatusCodeMsg.ERR, null));
            }
            if (!isJWT && username == null) {
                statusList.add(new Status("Empty username", StatusCodeMsg.ERR, null));
            }
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST(statusList)).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST(statusList)).build();
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Log out",
            notes = "Disconnect a logged user",
            response = ResponseUnique.class)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Access token created by user")
        ,
        @ApiResponse(code = 400, message = "Bad informations send by user")
        ,
        @ApiResponse(code = 200, message = "Access token already exist and send again to user")})
    public Response logOut(@ApiParam(value = "JSON object needed to login" ,required = true) @Valid @Required LogoutDTO logout, @Context UriInfo ui) {
        ArrayList<Status> statusList = new ArrayList<>();
        if (logout == null) {
            statusList.add(new Status("Empty json", StatusCodeMsg.ERR, null));
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST(statusList)).build();
        }
        String sessionId = logout.access_token;
        if (sessionId == null) {
            statusList.add(new Status("Empty access_token", StatusCodeMsg.ERR, null));
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST(statusList)).build();
        }
        Session session = TokenManager.Instance().getSession(sessionId);
        if (session == null) {
            statusList.add(new Status("No session linked to this access_token", StatusCodeMsg.ERR, null));
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST(statusList)).build();
        }

        TokenManager.Instance().removeSession(sessionId);
        statusList.clear();
        statusList.add(new Status("User has been logged out successfully", null));
        return Response.status(Response.Status.CREATED).entity(new ResponseFormPOST(statusList)).build();
    }

    /**
     * createId() - Méthode privée appelée dans getToken() Crée un chaine de
     * caractère encodée et qui sera utilisé comme nouvel identifiant de session
     *
     * @param username Nom de l'utilisateur, utilisé pour l'encodage
     * @return un objet String correspondant à l'identifiant
     *
     * @throws NoSuchAlgorithmException Dans le cas ou l'algorithme d'encodage
     * n'existe pas ou plus
     * @throws SQLException Dans le cas d'un probleme dans la colonne session de
     * la BDD
     *
     * @see getConnection()
     * @date 26/11/2015
     *
     * @update AT 11/02/2016 fonction cré et renvoi un ID crypté (pas besoin de
     * vérifier)
     */
    private String createId(String username) throws NoSuchAlgorithmException, SQLException {
//        SqlRequest sqlr = new SqlRequest();
//        boolean brk = false;
        String id = null;
//        while (brk == false) {
        id = new String(Hex.encodeHex((MessageDigest.getInstance("MD5").digest((username + new Date().toString()).getBytes()))));
//            ResultSet res = sqlr.getSQLRequest("SELECT * FROM session WHERE userSessionId = '" + userSessionId + "'", "phis");
//            if (!res.first()) {
//                brk = true;
//            }
//        }
        return id;
    }

    /**
     *
     * @param user user instance to check
     * @param verifPassword if we need to verify the password
     * @return User|null an instance of user or null
     */
    private User checkAuthentification(User user, boolean verifPassword) {
        UserDaoPhisBrapi uspb = new UserDaoPhisBrapi();
        // choose the database with payload information
        if (this.jwtClaimsSet != null) {
            uspb.setDataSourceFromJwtClaimsSet(this.jwtClaimsSet);
        }

        final String password = user.getPassword();

        try {
            if (uspb.existInDB(user)) {
                // Fixes #12 issue fill user name instead of just 
                // fill password user.setPassword(uspb.getPasswordFromDb(user.getEmail()));
                uspb.find(user);
//              
            } else {
                user = null;
            }
            if (verifPassword && user != null) {
                if (password.equals(user.getPassword())) {
                    uspb.admin = uspb.isAdmin(user);
                } else {
                    user = null;
                }
            }
            return user;
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        return null;
    }

    /**
     * Verify and validate JWT
     *
     * @param jsonToken json body
     * @param statusList return status list
     * @return boolean valid JWT or invalid
     */
    private boolean validJWTToken(TokenDTO jsonToken, ArrayList<Status> statusList) {
        boolean validJWTToken = false;
        String clientId = jsonToken.getClient_id();
        String username = jsonToken.getUsername();
        SignedJWT signedJWT = null;

        try {
            signedJWT = SignedJWT.parse(clientId);
            JWTClaimsSet jwtClaimsSetParsed = signedJWT.getJWTClaimsSet();
            if (ISSUERS_PUBLICKEY.containsKey(jwtClaimsSetParsed.getIssuer())) {

                String FilePropertyName = PropertiesFileManager.getConfigFileProperty("service", ISSUERS_PUBLICKEY.get(jwtClaimsSetParsed.getIssuer()));
                // verify the public key provenance  
                RSAPublicKey publicKey = PropertiesFileManager.parseBinaryPublicKey(FilePropertyName);

                JWSVerifier verifier = new RSASSAVerifier(publicKey);
                // verify the payload
                boolean validPublicKey = signedJWT.verify(verifier);
                boolean strangeJWT = new Date().after(jwtClaimsSetParsed.getIssueTime());
                boolean expireJWT = new Date().before(jwtClaimsSetParsed.getExpirationTime());
                boolean subjectMatch = jwtClaimsSetParsed.getSubject().equals(username);

                if (!subjectMatch) {
                    statusList.add(new Status("conflict with JWT name and username", StatusCodeMsg.ERR, null));
                }
                if (!strangeJWT) {
                    statusList.add(new Status("invalid JWT issued date", StatusCodeMsg.ERR, null));
                }
                if (!validPublicKey) {
                    statusList.add(new Status("invalid JWT signature", StatusCodeMsg.ERR, null));
                }
                if (!expireJWT) {
                    statusList.add(new Status("JWT expired", StatusCodeMsg.ERR, null));
                }
                if (expireJWT && validPublicKey && strangeJWT && subjectMatch) {
                    validJWTToken = true;
                }
                if (subjectMatch && strangeJWT && validPublicKey && expireJWT) {
                    //SILEX:info
                    //We check all important claims in the payload
                    //\SILEX:info
                    this.jwtClaimsSet = jwtClaimsSetParsed;
                }
            } else {
                statusList.add(new Status("Bad Issuer", StatusCodeMsg.ERR, null));
            }

        } catch (ParseException | JOSEException ex) {
            // verify JWT format header.payload.signature
            LOGGER.error(ex.getMessage(), ex);
            statusList.add(new Status("JWT Error", StatusCodeMsg.ERR, ex.getMessage()));
        }
        return validJWTToken;
    }   
}
