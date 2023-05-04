package org.opensilex.security.account.api;

import io.swagger.annotations.*;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.*;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
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
        if (Objects.nonNull(accountDTO.getHolderOfTheAccountURI())) {
            checkHolderExistAndHasNoAccountYet(personDAO, accountDTO.getHolderOfTheAccountURI());
            holderOfTheAccount = personDAO.get(accountDTO.getHolderOfTheAccountURI());

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
     * @param personDAO DAO that allow operations on persons
     * @throws NotFoundURIException with personalised message if the holder doesn't exsists in database
     * @throws ConflictException with personalised message if the holder already has an account
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
    public static void checkBeforeCreatingAccount(AccountWithoutHolderDTO accountDTO, AccountDAO accountDAO, AccountModel currentUser, SPARQLService sparql) throws Exception {
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
}
