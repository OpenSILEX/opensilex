package org.opensilex.security.account.api;

import io.swagger.annotations.*;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.*;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.group.dal.GroupDAO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.user.api.FavoriteCreationDTO;
import org.opensilex.security.user.api.FavoriteGetDTO;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Api(SecurityModule.REST_SECURITY_API_ID)
@Path("/security/accounts")
@ApiCredentialGroup(
        groupId = AccountAPI.CREDENTIAL_GROUP_ACCOUNT_ID,
        groupLabelKey = AccountAPI.CREDENTIAL_GROUP_ACCOUNT_LABEL_KEY
)
public class AccountAPI {
    public static final String CREDENTIAL_GROUP_ACCOUNT_ID = "Accounts";
    public static final String CREDENTIAL_GROUP_ACCOUNT_LABEL_KEY = "credential-groups.accounts";

    public static final String CREDENTIAL_ACCOUNT_MODIFICATION_ID = "account-modification";
    public static final String CREDENTIAL_ACCOUNT_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_ACCOUNT_DELETE_ID = "account-modification";
    public static final String CREDENTIAL_ACCOUNT_DELETE_LABEL_KEY = "credential.default.modification";


    @CurrentUser
    AccountModel currentUser;
    @Inject
    private SPARQLService sparql;
    @Inject
    private AuthenticationService authentication;

    @POST
    @ApiOperation("Add an account")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ACCOUNT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ACCOUNT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
            @ApiResponse(code = 201, message = "account created"),
            @ApiResponse(code = 403, message = "you don't have permission to create an account"),
            @ApiResponse(code = 409, message = "The account already exists (duplicate email)")
    })
    public Response createAccount(

            @ApiParam("Account description") @Valid AccountCreationDTO accountDTO
    ) throws Exception {

        AccountDAO accountDAO = new AccountDAO(sparql);

        checkBeforeCreatingAccount(accountDTO, accountDAO, currentUser, sparql);

        PersonDAO personDAO = new PersonDAO(sparql);
        PersonModel holderOfTheAccount = null;
        if (Objects.nonNull(accountDTO.getLinkedPerson())) {
            checkHolderExistAndHasNoAccountYet(personDAO, accountDTO.getLinkedPerson());
            holderOfTheAccount = personDAO.get(accountDTO.getLinkedPerson());

        }

        sparql.startTransaction();
        try {
            AccountModel account = accountDAO.create(
                    accountDTO.getUri(),
                    new InternetAddress(accountDTO.getEmail()),
                    accountDTO.isAdmin(),
                    authentication.getPasswordHash(accountDTO.getPassword()),
                    accountDTO.getLanguage(),
                    accountDTO.isEnable(),
                    holderOfTheAccount
            );

            sparql.commitTransaction();
            return new ObjectUriResponse(Response.Status.CREATED, account.getUri()).getResponse();
        } catch (Exception e) {
            sparql.rollbackTransaction();
            throw e;
        }
    }

    /**
     * @param holderOfTheAccountURI is the uri of the person we want to check for
     * @param personDAO             DAO that allow operations on persons
     * @throws NotFoundURIException with personalised message if the holder doesn't exsists in database
     * @throws ConflictException    with personalised message if the holder already has an account
     */
    public static void checkHolderExistAndHasNoAccountYet(PersonDAO personDAO, URI holderOfTheAccountURI) throws Exception {
        PersonModel holderOfTheAccount = personDAO.get(holderOfTheAccountURI);

        if (Objects.isNull(holderOfTheAccount)) {
            throw new NotFoundURIException("Person does not exist", holderOfTheAccountURI);
        }

        if (Objects.nonNull(holderOfTheAccount.getAccount())) {
            throw new ConflictException("Person " + holderOfTheAccountURI + " is already linked to an existing account");
        }
    }

    /**
     * @param accountDTO : account information to check before creating
     * @param accountDAO : an instance of AccountDao used to perform verifications
     * @throws ConflictException with personalised message if another account already has this uri or this email
     */
    public static void checkBeforeCreatingAccount(AccountDTO accountDTO, AccountDAO accountDAO, AccountModel currentUser, SPARQLService sparql) throws Exception {
        if (accountDTO.isAdmin() && (currentUser == null || !currentUser.isAdmin())) {
            throw new ForbiddenException("You must be an admin to create other admin accounts");
        }

        if (sparql.uriExists(AccountModel.class, accountDTO.getUri())) {
            throw new ConflictException("Account URI already exists: " + accountDTO.getUri());
        }

        InternetAddress email = new InternetAddress(accountDTO.getEmail());
        if (accountDAO.accountEmailExists(email)) {
            throw new ConflictException("Email already exists: " + email);
        }
    }

    /**
     * Update account data. linked person is not updatable, it only can be added
     *
     * @param accountDTO new information for updating the account
     * @return a message to know if updating worked
     * @throws Exception if update fail
     * @see AccountDAO
     */
    @PUT
    @ApiOperation("Update an account")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ACCOUNT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_ACCOUNT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Account updated", response = String.class),
            @ApiResponse(code = 400, message = "Invalid parameters, remind that changing the linked person is forbidden"),
            @ApiResponse(code = 404, message = "Account not found")
    })
    public Response updateAccount(
            @ApiParam("Account description") @Valid AccountUpdateDTO accountDTO
    ) throws Exception {
        AccountDAO accountDAO = new AccountDAO(sparql);

        AccountModel accountModel = accountDAO.get(accountDTO.getUri());

        if (Objects.isNull(accountModel)) {
            throw new NotFoundURIException("Unknown person URI: ", accountDTO.getUri());
        }


        boolean tryToChangeHolderOfTheAccount = Objects.nonNull(accountDTO.getLinkedPerson())
                && Objects.nonNull(accountModel.getLinkedPerson())
                && !SPARQLDeserializers.compareURIs(accountDTO.getLinkedPerson(), accountModel.getLinkedPerson().getUri().toString());
        if (tryToChangeHolderOfTheAccount) {
            throw new BadRequestException("you can't change the person linked to an account, you only can add one if there is none");
        }

        boolean addHolderOfTheAccount = Objects.nonNull(accountDTO.getLinkedPerson()) && Objects.isNull(accountModel.getLinkedPerson());
        PersonModel linkedPerson = accountModel.getLinkedPerson();
        if (addHolderOfTheAccount) {
            PersonDAO personDAO = new PersonDAO(sparql);
            checkHolderExistAndHasNoAccountYet(personDAO, accountDTO.getLinkedPerson());
            linkedPerson = personDAO.get(accountDTO.getLinkedPerson());
        }

        InternetAddress mail = Objects.nonNull(accountDTO.getEmail()) ? new InternetAddress(accountDTO.getEmail()) : null;

        accountDAO.update(
                accountDTO.getUri(),
                mail,
                accountDTO.isAdmin(),
                authentication.getPasswordHash(accountDTO.getPassword()),
                accountDTO.getLanguage(),
                accountDTO.isEnable(),
                linkedPerson,
                accountDTO.getFavorites()
        );

        return new ObjectUriResponse(Response.Status.OK, accountModel.getUri()).getResponse();

    }

    /**
     * Search accounts
     *
     * @param pattern     Regex pattern for filtering list by names or email
     * @param orderByList List of fields to sort as an array of fieldName=asc|desc
     * @param page        Page number
     * @param pageSize    Page size
     * @return filtered, ordered and paginated list
     * @see AccountDAO
     */
    @GET
    @ApiOperation("Search accounts")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return accounts", response = AccountGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchAccounts(
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

        ListWithPagination<AccountGetDTO> resultDTOList = resultList.convert(
                AccountGetDTO.class,
                AccountGetDTO::fromModel
        );

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Return an account by URI
     *
     * @param uri URI of the account
     * @return Corresponding account
     * @see AccountDAO
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get an account")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Account retrieved", response = AccountGetDTO.class),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Account not found", response = ErrorDTO.class)
    })
    public Response getAccount(
            @ApiParam(value = "Account URI", example = "http://opensilex.dev/users#jean.michel.inrae", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        AccountDAO dao = new AccountDAO(sparql);
        AccountModel model = dao.get(uri);

        if (Objects.isNull(model)) {
            throw new NotFoundURIException("Unknown account URI: ", uri);
        }

        return new SingleObjectResponse<>(AccountGetDTO.fromModel(model)).getResponse();
    }

    /**
     * *
     * Return a list of accounts corresponding to the given URIs
     *
     * @param uris list of accounts uri
     * @return Corresponding list of accounts
     */
    @GET
    @Path("by_uris")
    @ApiOperation("Get accounts by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return accounts", response = AccountGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "accounts not found (if any provided URIs is not found)", response = ErrorDTO.class)
    })
    public Response getAccountsByURI(
            @ApiParam(value = "Accounts URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {

        AccountDAO dao = new AccountDAO(sparql);
        List<AccountModel> models = dao.getList(uris);

        List<AccountGetDTO> resultDTOList = new ArrayList<>(models.size());
        models.forEach(result -> resultDTOList.add(AccountGetDTO.fromModel(result)));

        return new PaginatedListResponse<>(resultDTOList).getResponse();
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
                currentUser.getIsEnabled(),
                currentUser.getLinkedPerson(),
                favoriteList
        );

        return new ObjectUriResponse(Response.Status.OK, user.getUri()).getResponse();
    }

    @DELETE
    @Path("favorites/{uriFavorite}")
    @ApiOperation("Delete a favorite")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ACCOUNT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_ACCOUNT_DELETE_LABEL_KEY
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
                currentUser.getIsEnabled(),
                currentUser.getLinkedPerson(),
                favoriteList
        );

        return new ObjectUriResponse(Response.Status.OK, user.getUri()).getResponse();
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
}
