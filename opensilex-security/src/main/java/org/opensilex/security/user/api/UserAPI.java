//******************************************************************************
//                          UserAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.user.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static org.apache.jena.vocabulary.RDF.uri;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.security.user.dal.UserDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.group.dal.GroupDAO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.NamedResourcePaginatedListResponse;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

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
@Api(SecurityModule.REST_SECURITY_API_ID)
@Path("/security/users")
@ApiCredentialGroup(
        groupId = UserAPI.CREDENTIAL_GROUP_USER_ID,
        groupLabelKey = UserAPI.CREDENTIAL_GROUP_USER_LABEL_KEY
)
public class UserAPI {

    public static final String CREDENTIAL_GROUP_USER_ID = "Users";
    public static final String CREDENTIAL_GROUP_USER_LABEL_KEY = "credential-groups.users";

    public static final String CREDENTIAL_USER_MODIFICATION_ID = "user-modification";
    public static final String CREDENTIAL_USER_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_USER_DELETE_ID = "user-delete";
    public static final String CREDENTIAL_USER_DELETE_LABEL_KEY = "credential.default.delete";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    /**
     * Inject Authentication service
     */
    @Inject
    private AuthenticationService authentication;

    /**
     * Create a user and return it's URI
     *
     * @see org.opensilex.security.user.dal.UserDAO
     * @param userDTO user model to create
     * @return User URI
     * @throws Exception If creation failed
     */
    @POST
    @ApiOperation("Add an user")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_USER_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_USER_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
        @ApiResponse(code = 201, message = "A user is created"),
        @ApiResponse(code = 403, message = "This current user can't create other users with given parameters"),
        @ApiResponse(code = 409, message = "The user already exists (duplicate email)")
    })
    public Response createUser(
            @ApiParam("User description") @Valid UserCreationDTO userDTO
    ) throws Exception {
        // Check if user is admin to create a new admin user
        if (userDTO.isAdmin() && (currentUser == null || !currentUser.isAdmin())) {
            throw new ForbiddenException("You must be an admin to create other admin users");
        }

        // Create user DAO
        UserDAO userDAO = new UserDAO(sparql);

        // check if user URI already exists
        if (sparql.uriExists(UserModel.class, userDTO.getUri())) {
            // Return error response 409 - CONFLICT if user URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "User already exists",
                    "Duplicated URI: " + userDTO.getUri()
            ).getResponse();
        }

        // check if user email already exists
        InternetAddress userEmail = new InternetAddress(userDTO.getEmail());
        if (userDAO.userEmailexists(userEmail)) {
            // Return error response 409 - CONFLICT if user already exists
            return new ErrorResponse(
                    Status.CONFLICT,
                    "User already exists",
                    "Duplicated email: " + userEmail.toString()
            ).getResponse();
        }

        // create new user
        UserModel user = userDAO.create(
                userDTO.getUri(),
                userEmail,
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.isAdmin(),
                authentication.getPasswordHash(userDTO.getPassword()),
                userDTO.getLanguage()
        );
        // return user URI
        return new ObjectUriResponse(Response.Status.CREATED, user.getUri()).getResponse();
    }

    /**
     * Return a user by URI
     *
     * @see org.opensilex.security.user.dal.UserDAO
     * @param uri URI of the user
     * @return Corresponding user
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get an user")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User retrieved", response = UserGetDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "User not found", response = ErrorDTO.class)
    })
    public Response getUser(
            @ApiParam(value = "User URI", example = "dev-users:Admin_OpenSilex", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        // Get user from DAO by URI
        UserDAO dao = new UserDAO(sparql);
        UserModel model = dao.get(uri);

        // Check if user is found
        if (model != null) {
            // Return user converted in UserGetDTO
            return new SingleObjectResponse<>(
                    UserGetDTO.fromModel(model)
            ).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "User not found",
                    "Unknown user URI: " + uri.toString()
            ).getResponse();
        }
    }

    /**
     * *
     * Return a list of users corresponding to the given URIs
     *
     * @param uris list of users uri
     * @return Corresponding list of users
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("by_uris")
    @ApiOperation("Get users by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return users", response = UserGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Users not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getUsersByURI(
            @ApiParam(value = "Users URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        // Get user list from DAO by URIs
        UserDAO dao = new UserDAO(sparql);
        List<UserModel> models = dao.getList(uris);

        // Check if users are found
        if (!models.isEmpty()) {
            // Return user list converted in UserGetDTO
            List<UserGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                resultDTOList.add(UserGetDTO.fromModel(result));
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Users not found",
                    "Unknown user URIs"
            ).getResponse();
        }
    }

    /**
     * Search users
     *
     * @see org.opensilex.security.user.dal.UserDAO
     * @param pattern Regex pattern for filtering list by names or email
     * @param orderByList List of fields to sort as an array of fieldName=asc|desc
     * @param page Page number
     * @param pageSize Page size
     * @return filtered, ordered and paginated list
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @ApiOperation("Search users")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return users", response = UserGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchUsers(
            @ApiParam(value = "Regex pattern for filtering list by name or email", example = ".*") @DefaultValue(".*") @QueryParam("name") String pattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        // Search users with User DAO
        UserDAO dao = new UserDAO(sparql);
        ListWithPagination<UserModel> resultList = dao.search(
                pattern,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<UserGetDTO> resultDTOList = resultList.convert(
                UserGetDTO.class,
                UserGetDTO::fromModel
        );

        // Return paginated list of user DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @PUT
    @ApiOperation("Update an user")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_USER_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_USER_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User updated", response = String.class),
        @ApiResponse(code = 400, message = "Invalid parameters")
    })
    public Response updateUser(
            @ApiParam("User description") @Valid UserUpdateDTO dto
    ) throws Exception {
        UserDAO dao = new UserDAO(sparql);

        // Get user model from DTO uri
        UserModel model = dao.get(dto.getUri());

        if (model != null) {
            // If model exists, update it
            UserModel user = dao.update(
                    dto.getUri(),
                    new InternetAddress(dto.getEmail()),
                    dto.getFirstName(),
                    dto.getLastName(),
                    dto.isAdmin(),
                    authentication.getPasswordHash(dto.getPassword()),
                    dto.getLanguage()
            );

            return new ObjectUriResponse(Response.Status.OK, user.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "User not found",
                    "Unknown user URI: " + uri
            ).getResponse();
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an user")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_USER_DELETE_ID,
            credentialLabelKey = CREDENTIAL_USER_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(
            @ApiParam(value = "User URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        UserDAO dao = new UserDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}/groups")
    @ApiOperation("Get groups of an user")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return user's  groups", response = NamedResourceDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response getUserGroups(
            @ApiParam(value = "User URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri) throws Exception {
        // Search users with User DAO
        GroupDAO dao = new GroupDAO(sparql);
        List<GroupModel> resultList = dao.getUserGroups(uri);

        // Return paginated list of user DTO
        return new NamedResourcePaginatedListResponse<GroupModel>(resultList).getResponse();
    }

}
