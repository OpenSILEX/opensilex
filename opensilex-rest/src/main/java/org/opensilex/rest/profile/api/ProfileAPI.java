//******************************************************************************
//                          UserAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.profile.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.profile.dal.ProfileDAO;
import org.opensilex.rest.profile.dal.ProfileModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.rest.user.dal.UserModel;

/**
 * <pre>
 * User API for OpenSilex which provides:
 *
 * - create: Create a user
 * - get: Get a user by URI
 * - search: Search a filtered, ordered and paginated list of users
 * - update: Update a user with optionnaly a new password
 * - delete: Delete a user
 * </pre>
 *
 * @author Vincent Migot
 */
@Api("Profile")
@Path("/profile")
public class ProfileAPI {

    /**
     * Inject SPARQL service
     */
    @Inject
    private SPARQLService sparql;

    /**
     * Create a user and return it's URI
     *
     * @see org.opensilex.rest.user.dal.UserDAO
     * @param userDTO user model to create
     * @param securityContext injected security context to get current user
     * @return User URI
     * @throws Exception
     */
    @POST
    @Path("create")
    @ApiOperation("Create a profile and return it's URI")
    @ApiResponses({
        @ApiResponse(code = 201, message = "User sucessfully created"),
        @ApiResponse(code = 403, message = "Current user can't create other users with given parameters"),
        @ApiResponse(code = 409, message = "User already exists (duplicate email)")
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfile(
            @ApiParam("User creation informations") @Valid ProfileCreationDTO profileDTO,
            @Context SecurityContext securityContext
    ) throws Exception {
        // Create profile DAO
        ProfileDAO profileDAO = new ProfileDAO(sparql);

        // check if user email already exists
        ProfileModel profile = profileDAO.profileNameExists(profileDTO.getName());
        if (profile == null) {
            // create new user
            profile = profileDAO.create(
                    profileDTO.getUri(),
                    profileDTO.getName(),
                    profileDTO.getCredentials()
            );
            // return user URI
            return new ObjectUriResponse(Response.Status.CREATED, profile.getUri()).getResponse();
        } else {
            // Return error response 409 - CONFLICT if user already exists
            return new ErrorResponse(
                    Status.CONFLICT,
                    "User already exists",
                    "Duplicated name: " + profileDTO.getName()
            ).getResponse();
        }
    }

}
