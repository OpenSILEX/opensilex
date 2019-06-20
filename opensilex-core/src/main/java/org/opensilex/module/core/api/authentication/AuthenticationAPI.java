/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.api.authentication;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.module.core.dal.user.User;
import org.opensilex.module.core.dal.user.UserDAO;
import org.opensilex.server.rest.RestApplicationAPI;
import org.opensilex.module.core.service.sparql.SPARQLService;

/**
 *
 * @author vincent
 */
@Api("/authentication")
@Path("/authentication")
public class AuthenticationAPI implements RestApplicationAPI {
    
    @Inject
    SPARQLService sparql;
    
    @POST
    @ApiOperation(
        value = "Login user",
        notes = "Authenticate a user and return a JWT token containing all user credentials"
    )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User logged in"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 403, message = "Invalid credentials"),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(
        @ApiParam(value = "Login informations") @Valid UserAuthDTO authDTO
    ) {
        
        UserDAO userDAO = new UserDAO(sparql);
        
        User user = userDAO.getByEmail(authDTO.getEmail());

        if (user != null && user.checkPassword(authDTO.getPassword())) {
            return Response.ok(new UserInfosDTO(user)).build();
        } else {
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "Unknown user").build();
        }
    }
}
