//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.user.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.rest.RestApplicationAPI;
import org.opensilex.server.security.ApiProtected;
import org.opensilex.server.security.AuthenticationService;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.server.user.dal.UserDAO;
import org.opensilex.server.user.dal.UserModel;

@Api("Users")
@Path("/user")
public class UserAPI implements RestApplicationAPI {

    @Inject
    private SPARQLService sparql;

    @Inject
    private AuthenticationService authentication;

    @POST
    @Path("create")
    @ApiOperation("Create a user and return it's URI")
    @ApiResponses({
        @ApiResponse(code = 201, message = "User sucessfully created"),
        @ApiResponse(code = 403, message = "Current user can't create other users with given parameters"),
        @ApiResponse(code = 409, message = "User already exists (duplicate email)")
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(
            @ApiParam("User creation informations") UserCreationDTO userDTO,
            @Context SecurityContext securityContext
    ) throws Exception {

        UserModel currentUser = authentication.getCurrentUser(securityContext);
        if (userDTO.isAdmin() && (currentUser == null || !currentUser.isAdmin())) {
            throw new ForbiddenException("You must be an admin to create other admin users");
        }

        UserDAO userDAO = new UserDAO(sparql, authentication);

        InternetAddress userEmail = new InternetAddress(userDTO.getEmail());

        if (!userDAO.userEmailexists(userEmail)) {
            UserModel user = userDAO.create(
                    userEmail,
                    userDTO.getFirstName(),
                    userDTO.getLastName(),
                    userDTO.isAdmin(),
                    userDTO.getPassword()
            );

            return new ObjectUriResponse(Response.Status.CREATED, user.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Status.CONFLICT,
                    "User already exists",
                    "Duplicated email: " + userEmail.toString()
            ).getResponse();
        }
    }

}
