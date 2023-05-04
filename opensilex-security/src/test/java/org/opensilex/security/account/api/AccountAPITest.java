package org.opensilex.security.account.api;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.api.PersonAPITest;
import org.opensilex.security.person.api.PersonDTO;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.user.api.UserAPITest;
import org.opensilex.sparql.deserializer.URIDeserializer;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Objects;

import static junit.framework.TestCase.*;

public class AccountAPITest extends AbstractSecurityIntegrationTest {

    public static String path = "security/accounts";
    public static String createPath = path ;

    private final AccountDAO accountDAO = new AccountDAO(getSparqlService());

    private AccountCreationDTO getAccount1CreationDTO() {
        AccountCreationDTO dto = new AccountCreationDTO();
        dto.setAdmin(true);
        dto.setEmail("account1@opensilex.org");
        dto.setPassword("1234");
        dto.setLanguage(OpenSilex.DEFAULT_LANGUAGE);
        return dto;
    }

    private AccountCreationDTO getAccount2CreationDTO() {
        AccountCreationDTO dto = new AccountCreationDTO();
        dto.setAdmin(false);
        dto.setEmail("account2@opensilex.org");
        dto.setPassword("6789");
        dto.setLanguage(OpenSilex.DEFAULT_LANGUAGE);
        return dto;
    }

    private PersonDTO getDefaultPersonDTO() {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setFirstName("Jean");
        personDTO.setLastName("Eude");
        return personDTO;
    }

    @Before
    public void deleteBothdefaultAccounts() throws Exception {
        AccountModel accountModel = accountDAO.getByEmail(new InternetAddress(getAccount1CreationDTO().getEmail()));
        if (Objects.nonNull(accountModel)) {
            accountDAO.delete(accountModel.getUri());
        }

        accountModel = accountDAO.getByEmail(new InternetAddress(getAccount2CreationDTO().getEmail()));
        if (Objects.nonNull(accountModel)) {
            accountDAO.delete(accountModel.getUri());
        }
    }

    @Test
    public void testCreateWithoutHolder() throws Exception {
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getAccount1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        Response getResult = getJsonGetByUriResponseAsAdmin(target(UserAPITest.getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        postResult = getJsonPostResponseAsAdmin(target(createPath), getAccount2CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        createdUri = extractUriFromResponse(postResult);
        getResult = getJsonGetByUriResponseAsAdmin(target(UserAPITest.getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
        assertNotNull(accountDAO.get(createdUri));
    }

    @Test
    public void testCreateWithAnExistingPerson() throws Exception {
        //create the person
        PersonDTO personToLinkWith = getDefaultPersonDTO();
        Response postResponse = getJsonPostResponseAsAdmin(target(PersonAPITest.createPath), personToLinkWith);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        URI URIPersonToLinkWith = extractUriFromResponse(postResponse);

        //create the user
        AccountCreationDTO accountToCreate = getAccount1CreationDTO();
        accountToCreate.setHolderOfTheAccountURI(URIPersonToLinkWith);
        postResponse = getJsonPostResponseAsAdmin(target(createPath), accountToCreate);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        //check that account (user URI) and person are linked
        URI createdUri = extractUriFromResponse(postResponse);
        AccountModel createdAccount = accountDAO.get(createdUri);
        PersonModel linkedperson = new PersonDAO(getSparqlService()).get(URIPersonToLinkWith);
        assertEquals(createdAccount.getHolderOfTheAccount().getUri(), URIDeserializer.formatURI((linkedperson.getUri())));
    }

    @Test
    public void testCreateWithAnExistingPerson_personDoesntExist() throws Exception {
        AccountCreationDTO accountToCreate = getAccount1CreationDTO();
        accountToCreate.setHolderOfTheAccountURI(new URI("http://inventedURI"));

        //check that API return the right code
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), accountToCreate);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());

        //check that the user (account) was not insert in the dataBase
        assertFalse(accountDAO.accountEmailExists(new InternetAddress(accountToCreate.getEmail())));
    }

    @Test
    public void testCreateWithAnExistingPerson_personAlreadyHasAnAccount() throws Exception {
        //create the person
        PersonDTO personToLinkWith = getDefaultPersonDTO();
        Response personCreatedResponse = getJsonPostResponseAsAdmin(target(PersonAPITest.createPath), personToLinkWith);
        assertEquals(Response.Status.CREATED.getStatusCode(), personCreatedResponse.getStatus());
        URI URIPersonToLinkWith = extractUriFromResponse(personCreatedResponse);

        //create an account with this person
        AccountCreationDTO accountToCreate = getAccount1CreationDTO();
        accountToCreate.setHolderOfTheAccountURI(URIPersonToLinkWith);
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), accountToCreate);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        //try to create an other account with this same person
        AccountCreationDTO accountThatWillNotBeCreated = getAccount2CreationDTO();
        accountThatWillNotBeCreated.setHolderOfTheAccountURI(URIPersonToLinkWith);
        postResponse = getJsonPostResponseAsAdmin(target(createPath), accountThatWillNotBeCreated);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), postResponse.getStatus());
    }
}