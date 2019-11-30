//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.user.api;

import org.opensilex.core.user.dal.UserDAO;
import io.swagger.annotations.*;
import java.net.*;
import javax.inject.*;
import javax.mail.internet.*;
import javax.validation.*;
import javax.ws.rs.Path;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.*;
import org.opensilex.server.security.*;
import org.opensilex.server.security.model.*;
import org.opensilex.sparql.*;

@Api("Users")
@Path("/core/user")
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
        @ApiResponse(code = 409, message = "User already exists (duplicate email)")
    })
//    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(
            @ApiParam("User creation informations") UserCreationDTO userDTO
    ) throws Exception {

        UserDAO userDAO = new UserDAO(sparql, authentication);

        InternetAddress userEmail = new InternetAddress(userDTO.getEmail());

        if (!userDAO.userEmailexists(userEmail)) {
            UserModel user = userDAO.create(
                    userEmail,
                    userDTO.getFirstName(),
                    userDTO.getLastName(),
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
