//******************************************************************************
//                          UserAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.user.api;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import static org.apache.jena.vocabulary.RDF.uri;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.rest.validation.ValidURI;
import org.opensilex.sparql.utils.OrderBy;
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
@Api(UserAPI.CREDENTIAL_GROUP_USER_ID)
@Path("/user")
public class UserAPI {

    public static final String CREDENTIAL_GROUP_USER_ID = "Users";
    public static final String CREDENTIAL_GROUP_USER_LABEL_KEY = "credential-groups.users";

    public static final String CREDENTIAL_USER_MODIFICATION_ID = "user-modification";
    public static final String CREDENTIAL_USER_MODIFICATION_LABEL_KEY = "credential.user.modification";

    public static final String CREDENTIAL_USER_DELETE_ID = "user-delete";
    public static final String CREDENTIAL_USER_DELETE_LABEL_KEY = "credential.user.delete";

    public static final String CREDENTIAL_USER_READ_ID = "user-read";
    public static final String CREDENTIAL_USER_READ_LABEL_KEY = "credential.user.read";

    private final SPARQLService sparql;

    /**
     * Inject SPARQL service
     */
    @Inject
    public UserAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     * Inject Authentication service
     */
    @Inject
    private AuthenticationService authentication;

    /**
     * Create a user and return it's URI
     *
     * @see org.opensilex.rest.user.dal.UserDAO
     * @param userDTO user model to create
     * @param securityContext injected security context to get current user
     * @return User URI
     * @throws Exception If creation failed
     */
    @POST
    @Path("create")
    @ApiOperation("Create a user and return it's URI")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_USER_ID,
            groupLabelKey = CREDENTIAL_GROUP_USER_LABEL_KEY,
            credentialId = CREDENTIAL_USER_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_USER_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
        @ApiResponse(code = 201, message = "User sucessfully created"),
        @ApiResponse(code = 403, message = "Current user can't create other users with given parameters"),
        @ApiResponse(code = 409, message = "User already exists (duplicate email)")
    })
    public Response createUser(
            @ApiParam("User creation informations") @Valid UserCreationDTO userDTO,
            @Context SecurityContext securityContext
    ) throws Exception {
        // Get current user
        UserModel currentUser = authentication.getCurrentUser(securityContext);

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
                userDTO.getLang()
        );
        // return user URI
        return new ObjectUriResponse(Response.Status.CREATED, user.getUri()).getResponse();
    }

    /**
     * Return a user by URI
     *
     * @see org.opensilex.rest.user.dal.UserDAO
     * @param uri URI of the user
     * @return Corresponding user
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a user by it's URI")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_USER_ID,
            groupLabelKey = CREDENTIAL_GROUP_USER_LABEL_KEY,
            credentialId = CREDENTIAL_USER_READ_ID,
            credentialLabelKey = CREDENTIAL_USER_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return user", response = UserGetDTO.class),
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
    @Path("get-list-by-uris")
    @ApiOperation("Get a list of users by their URIs")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_USER_ID,
            groupLabelKey = CREDENTIAL_GROUP_USER_LABEL_KEY,
            credentialId = CREDENTIAL_USER_READ_ID,
            credentialLabelKey = CREDENTIAL_USER_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return user", response = UserGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "User not found (if any provided URIs is not found", response = ErrorDTO.class)
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
            List<UserGetDTO> resultDTOList = new ArrayList<>();
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
     * @see org.opensilex.rest.user.dal.UserDAO
     * @param pattern Regex pattern for filtering list by names or email
     * @param orderByList List of fields to sort as an array of
     * fieldName=asc|desc
     * @param page Page number
     * @param pageSize Page size
     * @return filtered, ordered and paginated list
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("search")
    @ApiOperation("Search users")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_USER_ID,
            groupLabelKey = CREDENTIAL_GROUP_USER_LABEL_KEY,
            credentialId = CREDENTIAL_USER_READ_ID,
            credentialLabelKey = CREDENTIAL_USER_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return user list", response = UserGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchUsers(
            @ApiParam(value = "Regex pattern for filtering list by names or email", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
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
    @Path("update")
    @ApiOperation("Update a user")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_USER_ID,
            groupLabelKey = CREDENTIAL_GROUP_USER_LABEL_KEY,
            credentialId = CREDENTIAL_USER_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_USER_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return user uri of the updated user", response = String.class),
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
                    dto.getLang()
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
    @Path("delete/{uri}")
    @ApiOperation("Delete a user")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_USER_ID,
            groupLabelKey = CREDENTIAL_GROUP_USER_LABEL_KEY,
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

}
