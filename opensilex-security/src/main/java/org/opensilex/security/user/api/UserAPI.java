//******************************************************************************
//                          UserAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.user.api;

import io.swagger.annotations.*;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.group.dal.GroupDAO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.NamedResourcePaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.apache.jena.vocabulary.RDF.uri;

/**
 * <pre>
 * A user is basically a Person with an account
 * User API for OpenSilex provides:
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
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    /**
     * Inject Authentication service
     */
    @Inject
    private AuthenticationService authentication;

    /**
     * Create an account and return it's URI, create by the same a Person
     *        with the given first/last-name who is the holder of the account
     *
     * @see AccountDAO
     * @see PersonDAO
     * @param userDTO user model to create
     * @return User URI or null if creation of account or person failed
     * @throws Exception If creation failed
     */
    @POST
    @ApiOperation("Add a user")
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

        AccountDAO accountDAO = new AccountDAO(sparql);

        Response creatingOk = checkBeforeCreatingUser(userDTO, accountDAO);
        if (creatingOk.getStatus() != Status.OK.getStatusCode()) {
            return creatingOk;
        }

        URI finalUserURI = null;

        sparql.startTransaction();
        try {
            PersonDAO personDAO = new PersonDAO(sparql);
            PersonModel person = personDAO.create(
                    null,
                    userDTO.getFirstName(),
                    userDTO.getLastName(),
                    userDTO.getEmail()
            );

            AccountModel user = accountDAO.create(
                    userDTO.getUri(),
                    new InternetAddress(userDTO.getEmail()),
                    userDTO.isAdmin(),
                    authentication.getPasswordHash(userDTO.getPassword()),
                    userDTO.getLanguage()
            );
            finalUserURI = user.getUri();

            personDAO.setAccount(person.getUri(), finalUserURI);

            sparql.commitTransaction();
        } catch (Exception e) {
            sparql.rollbackTransaction();
            throw e;
        }

        return new ObjectUriResponse(Response.Status.CREATED, finalUserURI).getResponse();
    }

    /**
     * Create a user and return it's URI, and link it to the Person's URI in parameters
     *
     * @see AccountDAO
     * @param userDTO user model to create
     * @return User URI or null if creation of account or person failed
     * @throws Exception If creation failed
     */
    @POST
    @Path("person_already_exists")
    @ApiOperation("Add a user from an existing person")
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
            @ApiResponse(code = 404, message = "This person doesn't exist"),
            @ApiResponse(code = 409, message = "The user already exists or the given holder already has an account"),
    })
    public Response createUserWithExistentPerson(

            @ApiParam("User description") @Valid UserCreationWithExistantPersonDTO userDTO
    ) throws Exception {

        AccountDAO accountDAO = new AccountDAO(sparql);

        Response creatingOk = checkBeforeCreatingUser(userDTO, accountDAO);
        if (creatingOk.getStatus() != Status.OK.getStatusCode()) {
            return creatingOk;
        }

        if (! sparql.uriExists(PersonModel.class, userDTO.getPersonHolderUri())) {
            return new ErrorResponse(
                    Status.NOT_FOUND,
                    "Person doesn't exists",
                    "Unexistant URI: " + userDTO.getPersonHolderUri()
            ).getResponse();
        }

        PersonDAO personDAO = new PersonDAO(sparql);

        if (personDAO.hasAnAccount(userDTO.getPersonHolderUri())) {
            return new ErrorResponse(
                    Status.CONFLICT,
                    "The given person already has an account",
                    "URI: " + userDTO.getPersonHolderUri() + " is already linked to an existing account"
            ).getResponse();
        }

        URI finalUserURI = null;

        sparql.startTransaction();
        try {

            AccountModel user = accountDAO.create(
                    userDTO.getUri(),
                    new InternetAddress(userDTO.getEmail()),
                    userDTO.isAdmin(),
                    authentication.getPasswordHash(userDTO.getPassword()),
                    userDTO.getLanguage()
            );
            finalUserURI = user.getUri();

            personDAO.setAccount(userDTO.getPersonHolderUri(), finalUserURI);

            sparql.commitTransaction();
        } catch (Exception e) {
            sparql.rollbackTransaction();
        }

        return new ObjectUriResponse(Response.Status.CREATED, finalUserURI).getResponse();
    }

    /**
     * Return a user by URI
     *
     * @see AccountDAO
     * @param uri URI of the user
     * @return Corresponding user
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get a user")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User retrieved", response = UserGetDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "User not found", response = ErrorDTO.class)
    })
    public Response getUser(
            @ApiParam(value = "User URI", example = "http://opensilex.dev/users#jean.michel.inrae", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        AccountDAO dao = new AccountDAO(sparql);
        AccountModel model = dao.get(uri);

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

        AccountDAO dao = new AccountDAO(sparql);
        List<AccountModel> models = dao.getList(uris);

        if (!models.isEmpty()) {
            List<UserGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> resultDTOList.add(UserGetDTO.fromModel(result)));

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
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
     * @see AccountDAO
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
        AccountDAO dao = new AccountDAO(sparql);
        ListWithPagination<AccountModel> resultList = dao.search(
                pattern,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<UserGetDTO> resultDTOList = resultList.convert(
                UserGetDTO.class,
                UserGetDTO::fromModel
        );

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Update a user (the account and first/last name of the person)
     *
     * @see AccountDAO
     * @see PersonDAO
     * @param userDTO : information to update user
     * @throws Exception if update fail
     */
    @PUT
    @ApiOperation("Update a user")
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
            @ApiParam("User description") @Valid UserUpdateDTO userDTO
    ) throws Exception {
        AccountDAO accountDAO = new AccountDAO(sparql);
        PersonDAO personDAO = new PersonDAO(sparql);

        AccountModel model = accountDAO.get(userDTO.getUri());
        PersonModel holderOfTheAccount = personDAO.getPersonFromAccount(userDTO.getUri());

        if (model != null) {

            sparql.startTransaction();
            try {
                AccountModel user = accountDAO.update(
                        userDTO.getUri(),
                        new InternetAddress(userDTO.getEmail()),
                        userDTO.isAdmin(),
                        authentication.getPasswordHash(userDTO.getPassword()),
                        userDTO.getLanguage(),
                        userDTO.getFavorites()
                );

                if (holderOfTheAccount != null) {
                    personDAO.update(
                            holderOfTheAccount.getUri(),
                            userDTO.getFirstName(),
                            userDTO.getLastName(),
                            holderOfTheAccount.getEmail().toString()
                    );
                }

                sparql.commitTransaction();

                return new ObjectUriResponse(Response.Status.OK, user.getUri()).getResponse();
            } catch (Exception e){
                sparql.rollbackTransaction();
                return new ObjectUriResponse(Status.INTERNAL_SERVER_ERROR, userDTO.getUri()).getResponse();
            }
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "User not found",
                    "Unknown user URI: " + uri
            ).getResponse();
        }
    }

    /**
     * Delete a user (the account and the Person)
     *
     * @see AccountDAO
     * @see PersonDAO
     * @param accountURI : URI of the account to delete
     * @throws Exception if update fail
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a user")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_USER_DELETE_ID,
            credentialLabelKey = CREDENTIAL_USER_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(
            @ApiParam(value = "User URI", example = "http://opensilex.dev/users#jean.michel.inrae", required = true) @PathParam("uri") @NotNull @ValidURI URI accountURI
    ) throws Exception {
        AccountDAO accountDAO = new AccountDAO(sparql);
        PersonDAO personDAO = new PersonDAO(sparql);

        PersonModel holderOfTheAccount = personDAO.getPersonFromAccount(accountURI);
        URI personURI = holderOfTheAccount==null ? null : holderOfTheAccount.getUri();

        sparql.startTransaction();
        try {
            if (personURI != null){
                personDAO.delete(personURI);
            }

            accountDAO.delete(accountURI);

            sparql.commitTransaction();
        } catch (Exception e){
            sparql.rollbackTransaction();
            throw e;
        }

        return new ObjectUriResponse(Response.Status.OK, accountURI).getResponse();
    }

    @GET
    @Path("{uri}/groups")
    @ApiOperation("Get groups of a user")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return user's  groups", response = NamedResourceDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response getUserGroups(
            @ApiParam(value = "User URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        GroupDAO dao = new GroupDAO(sparql);
        List<GroupModel> resultList = dao.getUserGroups(uri);

        return new NamedResourcePaginatedListResponse<GroupModel>(resultList).getResponse();
    }

    /**
     * @param userDTO : user information to check before creating
     * @param accountDAO : an instance of AccountDao used to perform verifications
     * @return a response with the Statut Code equivalent to "OK" if the user can be created, an error response otherwise
     */
    private Response checkBeforeCreatingUser(UserDTO userDTO, AccountDAO accountDAO) throws Exception {
        if (userDTO.isAdmin() && (currentUser == null || !currentUser.isAdmin())) {
            throw new ForbiddenException("You must be an admin to create other admin users");
        }

        if (sparql.uriExists(AccountModel.class, userDTO.getUri())) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "User already exists",
                    "Duplicated URI: " + userDTO.getUri()
            ).getResponse();
        }

        InternetAddress userEmail = new InternetAddress(userDTO.getEmail());
        if (accountDAO.accountEmailExists(userEmail)) {
            return new ErrorResponse(
                    Status.CONFLICT,
                    "User already exists",
                    "Duplicated email: " + userEmail
            ).getResponse();
        }

        return Response.status(Status.OK.getStatusCode()).build();
    }

    @GET
    @Path("favorites")
    @ApiOperation("Get list of favorites for a user")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = FavoriteGetDTO.class, responseContainer = "List")
    })
    public Response getFavorites(
            @ApiParam(value = "Types", required = true) @QueryParam("types") @NotNull List<URI> types
    ) throws Exception {
        List<FavoriteGetDTO> resultList = new ArrayList<>(currentUser.getFavorites().size());
        for (URI favoriteUri : currentUser.getFavorites()) {
            FavoriteGetDTO dto = FavoriteGetDTO.fromNamedResourceModelContextMap(
                    favoriteUri,
                    sparql.getNamedResourceModelContextMap(favoriteUri, types)
            );
            if (!dto.getGraphNameDtoList().isEmpty()) {
                try {
                    URI defaultGraphURI = sparql.getDefaultGraphURI(dto.getType());
                    dto.setDefaultLabelFromGraph(defaultGraphURI);
                } catch (org.opensilex.server.exceptions.NotFoundException ignored) {
                }
                resultList.add(dto);
            }
        }
        return new PaginatedListResponse<>(resultList).getResponse();
    }

    @POST
    @Path("favorites")
    @ApiOperation("Add a favorite")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
    })
    public Response addFavorite(
            @ApiParam(value = "Favorite object URI") @NotNull FavoriteCreationDTO favoriteDTO
    ) throws Exception {
        AccountDAO dao = new AccountDAO(sparql);

        List<URI> favoriteList = currentUser.getFavorites();
        favoriteList.add(favoriteDTO.getUri());

        AccountModel user = dao.update(
                currentUser.getUri(),
                currentUser.getEmail(),
                currentUser.isAdmin(),
                currentUser.getPasswordHash(),
                currentUser.getLanguage(),
                favoriteList
        );

        return new ObjectUriResponse(Response.Status.OK, user.getUri()).getResponse();
    }

    @DELETE
    @Path("favorites/{uriFavorite}")
    @ApiOperation("Delete a favorite")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_USER_DELETE_ID,
            credentialLabelKey = CREDENTIAL_USER_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFavorite(
            @ApiParam(value = "Favorite URI", example = "http://example.com/", required = true) @PathParam("uriFavorite") @NotNull @ValidURI URI uriFavorite
    ) throws Exception {
        AccountDAO dao = new AccountDAO(sparql);

        List<URI> favoriteList = currentUser.getFavorites();
        favoriteList.remove(uriFavorite);

        AccountModel user = dao.update(
                currentUser.getUri(),
                currentUser.getEmail(),
                currentUser.isAdmin(),
                currentUser.getPasswordHash(),
                currentUser.getLanguage(),
                favoriteList
        );

        return new ObjectUriResponse(Response.Status.OK, user.getUri()).getResponse();
    }
}
