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
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.MultipleObjectsResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.RestApplicationAPI;
import org.opensilex.server.security.ApiProtected;
import org.opensilex.server.security.AuthenticationService;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.server.user.dal.UserDAO;
import org.opensilex.server.user.dal.UserModel;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

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
    
    @GET
    @Path("{uri}")
    @ApiOperation("Get a user")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return user", response = UserGetDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters"),
        @ApiResponse(code = 404, message = "User not found")
    })
    public Response get(
            @ApiParam(value = "User URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        UserDAO dao = new UserDAO(sparql, authentication);
        UserModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    UserGetDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "User not found",
                    "Unknown user URI: " + uri.toString()
            ).getResponse();
        }
    }

    @GET
    @Path("search")
    @ApiOperation("Search users")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
        @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return user list", response = UserGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters")
    })
    public Response search(
            @ApiParam(value = "Regex pattern for filtering list by names or email") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {
        UserDAO dao = new UserDAO(sparql, authentication);
        
        ListWithPagination<UserModel> resultList = dao.search(
                pattern,
                orderByList, 
                page, 
                pageSize
        );
        
        ListWithPagination<UserGetDTO> resultDTOList = resultList.convert(
                UserGetDTO.class,
                UserGetDTO::fromModel
        );
        return new MultipleObjectsResponse<>(resultDTOList).getResponse();
    }

}
