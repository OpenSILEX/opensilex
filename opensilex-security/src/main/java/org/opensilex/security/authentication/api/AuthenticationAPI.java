//******************************************************************************
//                          AuthenticationAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.opensilex.OpenSilex;
import org.opensilex.security.SecurityConfig;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.dal.AuthenticationDAO;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.security.user.dal.UserDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.rest.validation.Required;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Path("/security")
public class AuthenticationAPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationAPI.class);

    @CurrentUser
    UserModel currentUser;

    /**
     * Inject Authentication service
     */
    @Inject
    private AuthenticationService authentication;

    /**
     * Inject SPARQL service
     */
    @Inject
    private SPARQLService sparql;

    @Inject
    private SecurityModule securityModule;

    @Inject
    private OpenSilex openSilex;

    /**
     * Authenticate a user with it's identifier (email or URI) and password returning a JWT token
     *
     * @see org.opensilex.security.user.dal.UserDAO
     * @param authenticationDTO suer identifier and password message
     * @return user token
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @POST
    @Path("authenticate")
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
        UserDAO userDAO = new UserDAO(sparql);

        // Get user by email or by uri
        UserModel user;
        try {
            InternetAddress email = new InternetAddress(authenticationDTO.getIdentifier());
            user = userDAO.getByEmail(email);
        } catch (AddressException ex2) {
            try {
                URI uri = new URI(authenticationDTO.getIdentifier());
                user = userDAO.get(uri);
            } catch (URISyntaxException ex1) {
                throw new Exception("Submitted user identifier is neither a valid email or URI");
            }
        }

        // Authenticate found user with provided password
        if (user != null && authentication.authenticate(user, authenticationDTO.getPassword(), userDAO.getAccessList(user.getUri()))) {
            // Return user token
            return new SingleObjectResponse<TokenGetDTO>(new TokenGetDTO(user.getToken())).getResponse();
        } else {
            // Otherwise return a 403 - FORBIDDEN error response
            return new ErrorResponse(Status.FORBIDDEN, "Invalid credentials", "User does not exists or password is invalid").getResponse();
        }
    }

    /**
     * Renew a user token if the provided one is still valid extending it's validity
     *
     * @see org.opensilex.security.user.dal.UserDAO
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
     * Logout current user
     *
     * @see org.opensilex.security.user.dal.UserDAO
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
        @ApiResponse(code = 200, message = "User sucessfully authenticated", response = TokenGetDTO.class),
        @ApiResponse(code = 403, message = "Invalid credentials (Bad token provided)", response = ErrorDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticateOpenID(
            @ApiParam("Authorization code") @QueryParam("code") @Required String code
    ) throws Exception {
        // Create user DAO
        UserDAO userDAO = new UserDAO(sparql);

        // Authenticate user with provided openID token
        UserModel user = authentication.authenticateWithOpenID(code, securityModule.getConfig(SecurityConfig.class).openID());

        user = userDAO.getByEmailOrCreate(user, openSilex.getDefaultLanguage());

        // Authenticate found user with provided password
        if (user != null && authentication.authenticate(user, userDAO.getAccessList(user.getUri()))) {
            // Return user token
            return new SingleObjectResponse<TokenGetDTO>(new TokenGetDTO(user.getToken())).getResponse();
        } else {
            // Otherwise return a 403 - FORBIDDEN error response
            return new ErrorResponse(Status.FORBIDDEN, "Invalid credentials", "User does not exists or password is invalid").getResponse();
        }
    }

}
