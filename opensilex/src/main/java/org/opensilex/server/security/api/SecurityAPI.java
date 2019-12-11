//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.security.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.RestApplicationAPI;
import org.opensilex.server.security.ApiProtected;
import org.opensilex.server.security.AuthenticationService;
import org.opensilex.server.security.dal.SecurityAccessDAO;
import org.opensilex.server.user.UserRegistryService;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.server.user.dal.UserDAO;
import org.opensilex.server.user.dal.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api("Security")
@Path("/security")
public class SecurityAPI implements RestApplicationAPI {

    /**
     * Logger
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityAPI.class);

    @Inject
    private UserRegistryService userRegistry;

    @Inject
    private SPARQLService sparql;

    @Inject
    private AuthenticationService authentication;

    @POST
    @Path("authenticate")
    @ApiOperation("Authenticate a user and return an access token")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User sucessfully authenticated"),
        @ApiResponse(code = 403, message = "Invalid credentials (user does not exists or invalid password)")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(
            @ApiParam("User authentication informations") @Valid AuthenticationDTO authenticationDTO
    ) throws Exception {

        UserDAO userDAO = new UserDAO(sparql, authentication);

        UserModel user;
        try {
            InternetAddress email = new InternetAddress(authenticationDTO.getIdentifier());
            user = userDAO.getByEmail(email);
        } catch (AddressException ex2) {
            try {
                URI uri = new URI(authenticationDTO.getIdentifier());
                user = userDAO.getByURI(uri);
            } catch (URISyntaxException ex1) {
                throw new Exception("Submitted user identifier is neither a valid email or URI");
            }
        }

        if (userDAO.authenticate(user, authenticationDTO.getPassword())) {
            List<String> accessList = userDAO.getAccessList(user.getUri());
            authentication.generateToken(user, accessList);
            userRegistry.addUser(user, authentication.getExpireInMs());
            return new SingleObjectResponse<String>(user.getToken()).getResponse();
        } else {
            return new ErrorResponse(Status.FORBIDDEN, "Invalid credentials", "User does not exists or password is invalid").getResponse();
        }
    }

    @POST
    @Path("logout")
    @ApiOperation("Logout by discarding a user token")
    @ApiResponses({
        @ApiResponse(code = 200, message = "User sucessfully logout"),})
    @ApiProtected
    public Response logout(
            @ApiParam(hidden = true) @HeaderParam(ApiProtected.HEADER_NAME) String userToken,
            @Context SecurityContext securityContext
    ) {
        userRegistry.removeUser(authentication.getCurrentUser(securityContext));
        return Response.ok().build();
    }

    @GET
    @Path("access-list")
    @ApiOperation("Get list of available access rights")
    @ApiResponses({
        @ApiResponse(code = 200, message = "List of available access rights")
    })
    public Response getAccestList() {
        if (accessMap == null) {
            SecurityAccessDAO securityDAO = new SecurityAccessDAO(sparql);
            accessMap = securityDAO.getSecurityAccessMap();
        }

        return Response.ok().entity(accessMap).build();
    }

    private static Map<String, Map<String, String>> accessMap;
}
