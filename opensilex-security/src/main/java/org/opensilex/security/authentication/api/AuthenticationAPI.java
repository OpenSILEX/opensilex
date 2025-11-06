//******************************************************************************
//                          AuthenticationAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication.api;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.security.SecurityConfig;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.dal.AuthenticationDAO;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.email.EmailService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.ServerModule;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import static org.opensilex.security.SecurityModule.DEFAULT_SUPER_ADMIN_EMAIL;

/**
 * <pre>
 * Security API for OpenSilex which provides:
 *
 * - authenticate: authenticate user with identifier/password
 * - renewToken: Renew a user token
 * - logout: Logout a user even if it's token is still valid
 * - getCredentialMap: Return list of all existing credentials in the application
 * </pre>
 *
 * @author Vincent Migot
 */
@Api(SecurityModule.REST_AUTHENTICATION_API_ID)
@Path(AuthenticationAPI.PATH)
public class AuthenticationAPI {

    public static final String PATH = "/security";
    public static final String AUTHENTICATE_PATH = "authenticate";

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationAPI.class);

    @CurrentUser
    AccountModel currentUser;

    /**
     * Inject Authentication service
     */
    @Inject
    private AuthenticationService authentication;

    
    
    /**
     * Inject Email service
     */
    @Inject
    private EmailService emailService;

    /**
     * Inject SPARQL service
     */
    @Inject
    private SPARQLService sparql;

    @Inject
    private SecurityModule securityModule;

    @Inject
    private OpenSilex openSilex;

    
    
    private final static String EMAIL_USERNAME_KEY = "username";
    private final static String EMAIL_TEMPLATE_REDIRECTURL_KEY = "redirectUrl";
    private final static String EMAIL_RESET_PASSWORD_APP_PATH = "reset-password"; 
    
    /**
     * Authenticate a user with it's identifier (email or URI) and password returning a JWT token
     *
     * @see AccountDAO
     * @param authenticationDTO suer identifier and password message
     * @return user token
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @POST
    @Path(AuthenticationAPI.AUTHENTICATE_PATH)
    @ApiOperation("Authenticate a user and return an access token")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User sucessfully authenticated", response = TokenGetDTO.class),
        @ApiResponse(code = 403, message = "Invalid credentials (user does not exists or invalid password)", response = ErrorDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(
            @ApiParam("User authentication informations") @Valid AuthenticationDTO authenticationDTO
    ) throws Exception {
        // Create user DAO
        AccountDAO accountDAO = new AccountDAO(sparql);

        // Get user by email or by uri
        AccountModel user;
        try {
            InternetAddress email = new InternetAddress(authenticationDTO.getIdentifier());
            user = accountDAO.getByEmail(email);
        } catch (AddressException ex2) {
            try {
                URI uri = new URI(authenticationDTO.getIdentifier());
                user = accountDAO.get(uri);
            } catch (URISyntaxException ex1) {
                throw new Exception("Submitted user identifier is neither a valid email or URI");
            }
        }

        // Authenticate found user with provided password
        if (user != null && authentication.authenticate(user, authenticationDTO.getPassword(), accountDAO.getCredentialList(user.getUri()))) {
            // Return user token
            return new SingleObjectResponse<TokenGetDTO>(new TokenGetDTO(user.getToken())).getResponse();
        } else {
            // Otherwise return a 403 - FORBIDDEN error response
            return new ErrorResponse(Status.FORBIDDEN, "Invalid credentials", "User does not exists, is disabled or password is invalid").getResponse();
        }
    }

    /**
     * Renew a user token if the provided one is still valid extending it's validity
     *
     * @see AccountDAO
     * @param userToken actual valid token for user
     * @return Renewed JWT token
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @PUT
    @Path("renew-token")
    @ApiOperation("Send back a new token if the provided one is still valid")
    @ApiProtected
    @ApiResponses({
        @ApiResponse(code = 200, message = "Token sucessfully renewed", response = TokenGetDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renewToken(
            @ApiParam(hidden = true) @HeaderParam(ApiProtected.HEADER_NAME) String userToken
    ) throws Exception {
        authentication.renewToken(currentUser);
        return new SingleObjectResponse<TokenGetDTO>(new TokenGetDTO(currentUser.getToken())).getResponse();
    }
    
    /**
     * Send an email to renew password
     *
     * @param identifier
     * @see AccountDAO
     * @return Renewed JWT token
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @POST
    @Path("forgot-password")
    @ApiOperation("Send an e-mail confirmation")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Email successfully sent"),
        @ApiResponse(code = 400, message = "Email not send")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response forgotPassword(
            @ApiParam(value = "User e-mail or uri", required = true) @QueryParam("identifier")  @NotNull String identifier
    ) throws Exception {
        if(!emailService.isEnable()){
            return new ErrorResponse(Status.SERVICE_UNAVAILABLE, "Functionality not available", "Email service must be started").getResponse();
        }
        
        // Create user DAO
        AccountDAO accountDAO = new AccountDAO(sparql);

        // Get user by email or by uri
        AccountModel user;
        try {
            InternetAddress userEmail = new InternetAddress(identifier);
            user = accountDAO.getByEmail(userEmail);
        } catch (AddressException ex2) {
            try {
                URI uri = new URI(identifier);
                user = accountDAO.get(uri);
            } catch (URISyntaxException ex1) {
                throw new IllegalArgumentException("Submitted user identifier is neither a valid email or URI");
            }
        }

        // Authenticate found user with provided e-mail
        if (user != null) {
            if(user.getEmail().equals(new InternetAddress(DEFAULT_SUPER_ADMIN_EMAIL))){
                // Otherwise return a 403 - FORBIDDEN error response
                return new ErrorResponse(Status.FORBIDDEN, "Invalid identifier", "Admin password can't be reset by this service").getResponse();
            }else{
               
                // Return user forgot token
                URI userForgottenToken = authentication.addForgotPasswordId(user.getUri());
                sendForgotPasswordRedirectEmail(user,userForgottenToken);
                return new ObjectUriResponse(Response.Status.OK, userForgottenToken).getResponse(); 
            }
        } else {
            // Otherwise return a 403 - FORBIDDEN error response
            return new ErrorResponse(Status.FORBIDDEN, "Invalid credentials", "User does not exists or password is invalid").getResponse();
        }
    }
    /**
     * 
     * @param user user model
     * @param userForgottenToken renew token uuid
     * @throws OpenSilexModuleNotFoundException
     * @throws UnsupportedEncodingException
     * @throws AddressException
     * @throws IOException 
     */
    private void sendForgotPasswordRedirectEmail(AccountModel user, URI userForgottenToken) throws OpenSilexModuleNotFoundException, IOException, AddressException {
        Map<String,Object> infos = new HashMap<>();
        List<InternetAddress> receivers = new LinkedList<>();
        receivers.add(new InternetAddress(user.getEmail().getAddress()));

        // get address
        String username = null;
        if ( Objects.nonNull(user.getLinkedPerson())) {
            username = StringUtils.capitalize(user.getLinkedPerson().getFirstName()) + " " + StringUtils.capitalize(user.getLinkedPerson().getLastName());
        }
        infos.put(EMAIL_USERNAME_KEY, username); 
        // get getForgotPasswordRedirectUrl address
        String redirectUrl = getForgotPasswordRedirectUrl(userForgottenToken); 
        infos.put(EMAIL_TEMPLATE_REDIRECTURL_KEY, redirectUrl); 
        emailService.sendAnEmail(receivers, "[OpenSILEX] Reset your password", "forgot-password.mustache",infos,true);
    }
    
    private String getForgotPasswordRedirectUrl(URI userForgottenToken) throws OpenSilexModuleNotFoundException {
        // get address
        String redirectUrl = emailService.getOpenSilex().getModuleByClass(ServerModule.class).getBaseURL();
        
        redirectUrl = Paths.get(redirectUrl , "app" , EMAIL_RESET_PASSWORD_APP_PATH , URLEncoder.encode(userForgottenToken.toString(), StandardCharsets.UTF_8)).toString();
        return redirectUrl;
    }
    
     /**
     * Renew a user password
     *
     * @param renewToken renew token linked to user
     * @param checkOnly only check if renew token uri exist
     * @param password new password to update
     * @see AccountDAO
     * @return User uri
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @PUT
    @Path("renew-password")
    @ApiOperation("Update user password")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Password sucessfully renewed", response = TokenGetDTO.class),
        @ApiResponse(code = 400, message = "Invalid token", response = ErrorResponse.class),
        @ApiResponse(code = 400, message = "Invalid password", response = ErrorResponse.class),
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renewPassword( 
            @ApiParam(value = "User renew token", required = true) @QueryParam("renew_token") @NotNull @ValidURI URI renewToken,
            @ApiParam(value = "Check only renew token", example = "false") @DefaultValue("false") @QueryParam("check_only") Boolean checkOnly,
            @ApiParam(value = "User password") @QueryParam("password") String password
    ) throws Exception {
        if(!emailService.isEnable()){
            return new ErrorResponse(Status.SERVICE_UNAVAILABLE, "Functionnality not available", "Email service must be started").getResponse();
        }
        URI userUri =  authentication.getForgottenPasswordUserURIFromRenewToken(renewToken);
        if(checkOnly){
            if(userUri == null){
                return new ErrorResponse(Status.BAD_REQUEST, "Invalid token", "Token has expired").getResponse();
            }else{
                return new ObjectUriResponse(Status.OK, userUri).getResponse();
            }
        }
       if(StringUtils.isEmpty(password)){
            return new ErrorResponse(Status.BAD_REQUEST, "Invalid password", "Password must not be empty").getResponse();
        }
        if(userUri == null){
            return new ErrorResponse(Status.BAD_REQUEST, "Invalid token", "Token has expired").getResponse();
        }
        AccountDAO accountDAO = new AccountDAO(sparql);
        AccountModel user = accountDAO.get(userUri);
        if(user == null){
            return new ErrorResponse(Status.FORBIDDEN, "Invalid token", "User does not exists or forgotten password token is invalid").getResponse();
        }else{
            accountDAO.update(
                    user.getUri(),
                    user.getEmail(),
                    user.isAdmin(),
                    authentication.getPasswordHash(password),
                    user.getLanguage(),
                    user.getIsEnabled(),
                    null,
                    user.getFavorites()
            );
            authentication.removeForgottenPasswordUserFromRenewToken(renewToken);
        }
        return new ObjectUriResponse(Status.OK, userUri).getResponse();
    }

    /**
     * Logout current user
     *
     * @see AccountDAO
     * @return Empty ok response
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @DELETE
    @Path("logout")
    @ApiOperation("Logout by discarding a user token")
    @ApiProtected
    @ApiResponses({
        @ApiResponse(code = 200, message = "User sucessfully logout")})
    public Response logout() throws Exception {
        authentication.logout(currentUser);
        return Response.ok().build();
    }

    /**
     * <pre>
     * Return map of existing application credential indexed by @Api and credential id
     * Label for each credential is based on @ApiOperation message
     *
     * Produced JSON example:
     * {
     *      "Security": {
     *          "/security/logout|POST": "Logout by discarding a user token",
     *          "/security/renew-token|GET": "Send back a new token if the provided one is still valid"
     *      },
     *      "User": {
     *          "/user/create|POST": "Create a user and return it's URI",
     *          "/user/search|GET": "Search users",
     *          "/user/{uri}|GET": "Get a user"
     *      },
     *      ...
     * }
     * </pre>
     *
     * @return Map of existing application credential.
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("credentials")
    @ApiOperation(value = "Get list of existing credentials indexed by Swagger @API concepts in the application")
    @ApiResponses({
        @ApiResponse(code = 200, message = "List of existing credentials by group in the application", response = CredentialsGroupDTO.class, responseContainer = "List")
    })
    public Response getCredentialsGroups() throws Exception {
        if (credentialsGroupList == null) {
            credentialsGroupList = new ArrayList<>();
            AuthenticationDAO securityDAO = new AuthenticationDAO(sparql);
            Map<String, String> groupLabels = securityDAO.getCredentialsGroupLabels();
            securityDAO.getCredentialsGroups().forEach((String groupId, Map<String, String> credentialMap) -> {
                CredentialsGroupDTO credentialsGroup = new CredentialsGroupDTO();
                credentialsGroup.setGroupId(groupId);
                List<CredentialDTO> credentials = new ArrayList<>(credentialMap.size());
                credentialMap.forEach((id, label) -> {
                    CredentialDTO credential = new CredentialDTO();
                    credential.setId(id);
                    credential.setLabel(label);
                    credentials.add(credential);
                });
                credentials.sort(Comparator.comparing(CredentialDTO::getLabel).reversed());
                credentialsGroup.setGroupKeyLabel(groupLabels.get(groupId));
                credentialsGroup.setCredentials(credentials);
                credentialsGroupList.add(credentialsGroup);
            });
        }

        return new PaginatedListResponse<CredentialsGroupDTO>(credentialsGroupList).getResponse();
    }

    private static List<CredentialsGroupDTO> credentialsGroupList;

    @GET
    @Path("openid")
    @ApiOperation("Authenticate a user and return an access token")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User successfully authenticated", response = TokenGetDTO.class),
        @ApiResponse(code = 403, message = "Invalid credentials (Bad token provided)", response = ErrorDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticateOpenID(
            @ApiParam("Authorization code") @QueryParam("code") @Required String code
    ) throws Exception {
        // Create user DAO
        AccountDAO accountDAO = new AccountDAO(sparql);

        // Authenticate user with provided openID token
        AccountModel user = authentication.authenticateWithOpenID(code, securityModule.getConfig(SecurityConfig.class).openID());

        user = accountDAO.getByEmailOrCreate(user, openSilex.getDefaultLanguage());

        // Authenticate found user with provided password
        if (user != null && authentication.authenticate(user, accountDAO.getCredentialList(user.getUri()))) {
            // Return user token
            return new SingleObjectResponse<TokenGetDTO>(new TokenGetDTO(user.getToken())).getResponse();
        } else {
            // Otherwise return a 403 - FORBIDDEN error response
            return new ErrorResponse(Status.FORBIDDEN, "Invalid credentials", "User does not exists or password is invalid").getResponse();
        }
    }

    @GET
    @Path("saml")
    @ApiOperation("Authenticate a user and return an access token from SAML response")
    @ApiResponses(value = {
            @ApiResponse(code = 302, message = "User successfully authenticated"),
            @ApiResponse(code = 403, message = "Invalid SAML authentication")
    })
    @Produces(MediaType.TEXT_PLAIN)
    public Response authenticateSAML(@Context HttpServletRequest request) throws Exception {
        AccountDAO accountDAO = new AccountDAO(sparql);

        AccountModel user = authentication.authenticateWithSAML(request);

        user = accountDAO.getByEmailOrCreate(user, openSilex.getDefaultLanguage());

        if (authentication.authenticate(user, accountDAO.getCredentialList(user.getUri()))) {
            String token = user.getToken();
            URI samlLandingPageUri = authentication.buildSAMLLandingPageURI(token);
            return Response
                    .status(Status.FOUND)
                    .header("Location", samlLandingPageUri.toString())
                    .build();
        }

        return new ErrorResponse(Status.FORBIDDEN, "Invalid SAML authentication", "Could not authenticate user with SAML")
                .getResponse();
    }
}
