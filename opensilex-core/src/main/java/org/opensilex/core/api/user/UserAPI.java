/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.api.user;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.opensilex.server.response.ObjectCreationResponse;
import org.opensilex.server.response.SingleValueResponse;
import org.opensilex.server.rest.RestApplicationAPI;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.server.security.AuthenticationService;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.core.dal.user.UserDAO;
import org.opensilex.server.security.ApiProtected;
import org.opensilex.server.security.user.User;

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

//        if (!userDAO.userEmailexists(userEmail)) {
            User user = userDAO.create(
                    userEmail,
                    userDTO.getFirstName(),
                    userDTO.getLastName(),
                    userDTO.getPassword()
            );

            return new ObjectCreationResponse(user.getUri()).getResponse();
//        } else {
//            return new ErrorResponse(
//                    Status.CONFLICT,
//                    "User already exists",
//                    "Duplicated email: " + userEmail.toString()
//            ).getResponse();
//        }
    }

    @POST
    @Path("authenticate")
    @ApiOperation("Authenticate a user and return an access token")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User sucessfully authenticated"),
        @ApiResponse(code = 403, message = "User already exists (duplicate email)")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(
            @ApiParam("User authentication informations") UserAuthenticationDTO authenticationDTO
    ) throws Exception {

        UserDAO userDAO = new UserDAO(sparql, authentication);

        User user;
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
            return new SingleValueResponse(authentication.generateToken(user.getUri())).getResponse();
        } else {
            return new ErrorResponse("Invalid credentials", "User does not exists or password is invalid").getResponse();
        }
    }
}
