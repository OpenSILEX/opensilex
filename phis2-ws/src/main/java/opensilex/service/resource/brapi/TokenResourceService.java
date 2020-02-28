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
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.internet.InternetAddress;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.PropertiesFileManager;
import opensilex.service.authentication.TokenResponseStructure;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.LogoutDTO;
import opensilex.service.resource.dto.TokenDTO;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.view.brapi.form.ResponseUnique;
import opensilex.service.model.Call;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.sparql.service.SPARQLService;

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
@Singleton
public class TokenResourceService implements BrapiCall{
    
    final static Logger LOGGER = LoggerFactory.getLogger(TokenResourceService.class);
    static final Map<String, String> ISSUERS_PUBLICKEY;
    static final List<String> GRANTTYPE_AUTHORIZED = Collections.unmodifiableList(Arrays.asList("jwt", "password"));

    @Inject
    private SPARQLService sparql;

    @Inject
    private AuthenticationService authentication;
    
    static {
        Map<String, String> temporaryMap = new HashMap<>();
        // can put multiple issuers of jwt
        temporaryMap.put("GnpIS", "gnpisPublicKeyFileName");
        temporaryMap.put("Phis", "phisPublicKeyFileName");
        ISSUERS_PUBLICKEY = Collections.unmodifiableMap(temporaryMap);
    }  

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
        @ApiResponse(code = 201, message = "Access token created by user"),
        @ApiResponse(code = 400, message = "Bad informations send by user"),
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
            // UserModel user = new UserModel(username, password);
            //SILEX:info
            // if we have a jwt the password is not verified because it means that the
            // user is already logged
            // we trust the client
            //\SILEX:info
            org.opensilex.rest.user.dal.UserDAO userDAO = new org.opensilex.rest.user.dal.UserDAO(sparql);
            org.opensilex.rest.user.dal.UserModel user;
            try {
                user = userDAO.getByEmail(new InternetAddress(username));

                if (!isJWT) {
                    if (!authentication.authenticate(user, password, userDAO.getAccessList(user.getUri()))) {
                        user = null;
                    }
                }
            } catch (Exception ex) {
                LOGGER.warn("Exception while authenticating user: " + username, ex);
                user = null;
            }

            // No user found
            if (user == null) {
                statusList.add(new Status("User/password doesn't exist", StatusCodeMsg.ERR, null));
            } else {
                try {
                    authentication.authenticate(user, userDAO.getAccessList(user.getUri()));
                    Response.Status reponseStatus = Response.Status.OK;
                    String expires_in = "" + authentication.getExpiresInSec();
                    TokenResponseStructure res = new TokenResponseStructure(user.getToken(), user.getFirstName() + " " + user.getLastName(), expires_in);
                    final URI uri = new URI(ui.getPath());
                    return Response.status(reponseStatus).location(uri).entity(res).build();
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    statusList.add(new Status("Error while authenticating user " + StatusCodeMsg.ERR, StatusCodeMsg.ERR, ex.getMessage()));
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseFormPOST(statusList)).build();
                }
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
    public Response logOut(@ApiParam(value = "JSON object needed to login") @Valid LogoutDTO logout, @Context UriInfo ui) {
        ArrayList<Status> statusList = new ArrayList<>();
        statusList.add(new Status("User has been logged out successfully", null));
        return Response.status(Response.Status.CREATED).entity(new ResponseFormPOST(statusList)).build();
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
