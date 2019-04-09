//******************************************************************************
//                           TokenResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2015
// Creation date: 26 November 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.brapi;

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
import opensilex.service.PropertiesFileManager;
import opensilex.service.authentication.Session;
import opensilex.service.dao.UserDAO;
import opensilex.service.authentication.TokenManager;
import opensilex.service.authentication.TokenResponseStructure;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.User;
import opensilex.service.resource.dto.LogoutDTO;
import opensilex.service.resource.dto.TokenDTO;
import opensilex.service.utils.date.Dates;
import opensilex.service.configuration.DateFormats;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.view.brapi.form.ResponseUnique;
import opensilex.service.model.Call;

/**
 * Token resource service.
 * @update [Arnaud Charleroy] 3 Aug. 2016: Add JWT
 * @update [Alice Boizet] 27 July 2018: override callInfo() to add token call 
 * description in the Brapi calls service
 * @see https://jwt.io/introduction/
 * @see http://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-rsa-signature
 * @author Samuël Chérimont
 */
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
    

    /**
     * Overriding BrapiCall method
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
     * POST identifiers to get a session id.
     * @param jsonToken body JSON
     * @param ui
     * @return the POST result.
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
        
        // Check grant type
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
        
        // JWT case
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
                    user = checkAuthentication(user, false);
                } else {
                    user = checkAuthentication(user, true);
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
                            //this code forces regeneration of a new token in this case
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
            // if an error occurred
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

    /**
     * Token DELETE service.
     * @param logout
     * @param ui
     * @return the DELETE result
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Log out",
            notes = "Disconnect a logged user",
            response = ResponseUnique.class)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Access token created by user"),
        @ApiResponse(code = 400, message = "Bad informations send by user"),
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
     * Create a session id.
     * @param username
     * @return the session id
     * @throws NoSuchAlgorithmException
     * @throws SQLException
     */
    private String createId(String username) throws NoSuchAlgorithmException, SQLException {
        return new String(Hex.encodeHex((MessageDigest.getInstance("MD5").digest((username + new Date().toString()).getBytes()))));
    }

    /**
     * Checks authentication.
     * @param user user instance to check
     * @param verifPassword if we need to verify the password
     * @return User|null an instance of user or null
     */
    private User checkAuthentication(User user, boolean verifPassword) {
        UserDAO uspb = new UserDAO();
        // choose the database with payload information
        if (this.jwtClaimsSet != null) {
            uspb.setDataSourceFromJwtClaimsSet(this.jwtClaimsSet);
        }

        final String password = user.getPassword();

        try {
            if (uspb.existInDB(user)) {
                uspb.find(user);
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
     * Verifies and validates JWT.
     * @param jsonToken
     * @param statusList return status list
     * @return boolean valid JWT or invalid
     */
    private boolean validJWTToken(TokenDTO jsonToken, ArrayList<Status> statusList) {
        boolean validJWTToken = false;
        String clientId = jsonToken.getClient_id();
        String username = jsonToken.getUsername();
        SignedJWT signedJWT;

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
