package org.opensilex.security.account.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Objects;

import static junit.framework.TestCase.*;

public class AccountAPITest extends AbstractSecurityIntegrationTest {

    public final static String path = "security/accounts";
    public final static String createPath = path ;
    private final static String updatePath = path;
    public static String getPath = path + "/{uri}";

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

    private AccountModel createAccount1WithPerson() throws Exception {
        URI URIPersonToLinkWith = createPerson();

        AccountCreationDTO accountToCreate = getAccount1CreationDTO();
        accountToCreate.setLinkedPerson(URIPersonToLinkWith);
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), accountToCreate);
        assertEquals("crerate account failed", Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        URI createdUri = extractUriFromResponse(postResponse);
        return accountDAO.get(createdUri);
    }

    private AccountModel createAccount1WithoutPerson() throws Exception {
        AccountCreationDTO accountToCreate = getAccount1CreationDTO();
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), accountToCreate);

        URI createdUri = extractUriFromResponse(postResponse);
        return accountDAO.get(createdUri);
    }

    private URI createPerson() throws Exception {
        PersonDTO personToLinkWith = getDefaultPersonDTO();
        Response postResponse = getJsonPostResponseAsAdmin(target(PersonAPITest.createPath), personToLinkWith);
        assertEquals("create person failed", Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        return extractUriFromResponse(postResponse);
    }

    private AccountUpdateDTO getDTOFromModel(AccountModel accountModel){
        AccountUpdateDTO account = new AccountUpdateDTO();
        account.setUri(accountModel.getUri());
        account.setPassword(accountModel.getPasswordHash());
        account.setAdmin(accountModel.isAdmin());
        account.setEnable(accountModel.getIsEnabled());
        account.setFavorites(accountModel.getFavorites());
        account.setLanguage(accountModel.getLanguage());

        if ( Objects.nonNull(accountModel.getLinkedPerson()) ) {
            account.setLinkedPerson(accountModel.getLinkedPerson().getUri());
        }
        if ( Objects.nonNull(accountModel.getEmail()) ) {
            account.setEmail(accountModel.getEmail().toString());
        }

        return account;
    }

    @Before
    public void deleteBothdefaultAccounts() throws Exception {
        AccountModel accountModel = accountDAO.getByEmail(new InternetAddress(getAccount1CreationDTO().getEmail()));
        if (Objects.nonNull(accountModel)) {
            accountDAO.delete(accountModel.getUri(), getOpensilex());
        }

        accountModel = accountDAO.getByEmail(new InternetAddress(getAccount2CreationDTO().getEmail()));
        if (Objects.nonNull(accountModel)) {
            accountDAO.delete(accountModel.getUri(), getOpensilex());
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
        URI URIPersonToLinkWith = createPerson();

        //create the user
        AccountCreationDTO accountToCreate = getAccount1CreationDTO();
        accountToCreate.setLinkedPerson(URIPersonToLinkWith);
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), accountToCreate);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        //check that account (user URI) and person are linked
        URI createdUri = extractUriFromResponse(postResponse);
        AccountModel createdAccount = accountDAO.get(createdUri);
        PersonModel linkedperson = new PersonDAO(getSparqlService()).get(URIPersonToLinkWith);
        assertEquals(createdAccount.getLinkedPerson().getUri(), URIDeserializer.formatURI((linkedperson.getUri())));
    }

    @Test
    public void testCreateWithAnExistingPerson_personDoesntExist() throws Exception {
        AccountCreationDTO accountToCreate = getAccount1CreationDTO();
        accountToCreate.setLinkedPerson(new URI("http://inventedURI"));

        //check that API return the right code
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), accountToCreate);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());

        //check that the user (account) was not insert in the dataBase
        assertFalse(accountDAO.accountEmailExists(new InternetAddress(accountToCreate.getEmail())));
    }

    @Test
    public void testCreateWithAnExistingPerson_personAlreadyHasAnAccount() throws Exception {
        URI URIPersonToLinkWith = createPerson();

        //create an account with this person
        AccountCreationDTO accountToCreate = getAccount1CreationDTO();
        accountToCreate.setLinkedPerson(URIPersonToLinkWith);
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), accountToCreate);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        //try to create an other account with this same person
        AccountCreationDTO accountThatWillNotBeCreated = getAccount2CreationDTO();
        accountThatWillNotBeCreated.setLinkedPerson(URIPersonToLinkWith);
        postResponse = getJsonPostResponseAsAdmin(target(createPath), accountThatWillNotBeCreated);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), postResponse.getStatus());
    }

    @Test
    public void generalUpdate() throws Exception {
        AccountUpdateDTO accountUpdateDTO = getDTOFromModel(createAccount1WithPerson());

        accountUpdateDTO.setLanguage("fr");
        accountUpdateDTO.setAdmin(false);
        accountUpdateDTO.setPassword("new_pwd");
        accountUpdateDTO.setEnable(false);
        accountUpdateDTO.setEmail("new@email.com");

        Response response = getJsonPutResponse(target(updatePath), accountUpdateDTO);
        assertEquals("update account failed", Response.Status.OK.getStatusCode(), response.getStatus());

        AccountModel accountUpdated = accountDAO.get(accountUpdateDTO.getUri());
        compareDTOAndModelWithPerson(accountUpdateDTO, accountUpdated);
    }

    @Test
    public void updateWithoutUri() throws Exception {
        AccountUpdateDTO accountUpdateDTO = getDTOFromModel(createAccount1WithoutPerson());
        accountUpdateDTO.setUri(null);
        Response response = getJsonPutResponse(target(updatePath), accountUpdateDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateWithUnexistenttUri() throws Exception {
        AccountUpdateDTO accountUpdateDTO = getDTOFromModel(createAccount1WithoutPerson());
        accountUpdateDTO.setUri( new URI("http://unexisting/uri") );
        Response response = getJsonPutResponse(target(updatePath), accountUpdateDTO);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateWithUriAlreadyExists() throws Exception {
        AccountModel firstCreatedAccount = createAccount1WithPerson();

        AccountCreationDTO accountCreationDTO = getAccount2CreationDTO();
        accountCreationDTO.setUri(firstCreatedAccount.getUri());
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), accountCreationDTO);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void updateAddPerson() throws Exception {
        AccountUpdateDTO accountUpdateDTO = getDTOFromModel(createAccount1WithoutPerson());

        accountUpdateDTO.setLinkedPerson( createPerson() );
        Response response = getJsonPutResponse(target(updatePath), accountUpdateDTO);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        AccountModel updatedAccount = accountDAO.get( accountUpdateDTO.getUri() );
        compareDTOAndModelWithPerson(accountUpdateDTO, updatedAccount);
    }

    @Test
    public void updateAddPersonButPersonDoesntExist() throws Exception {
        AccountUpdateDTO accountUpdateDTO = getDTOFromModel(createAccount1WithoutPerson());

        accountUpdateDTO.setLinkedPerson( new URI("http://this/uri/does/not/exists") );
        Response response = getJsonPutResponse(target(updatePath), accountUpdateDTO);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateAddPersonButPersonIsAlreadyLinkedToAnAccount() throws Exception {
        AccountModel accountWithPerson = createAccount1WithPerson();

        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getAccount2CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
        URI accountUri = extractUriFromResponse(postResult);

        AccountUpdateDTO accountUpdateDTO = new AccountUpdateDTO();
        accountUpdateDTO.setUri(accountUri);
        accountUpdateDTO.setLinkedPerson( accountWithPerson.getLinkedPerson().getUri());

        Response putResult = getJsonPostResponseAsAdmin(target(updatePath), accountUpdateDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), putResult.getStatus());

        assertNull("this account should not have person", accountDAO.get(accountUri).getLinkedPerson());
    }

    @Test
    public void updateChangePersonIsNotAllowed() throws Exception {
        AccountModel accountWitPerson = createAccount1WithPerson();

        PersonDTO newPerson = new PersonDTO();
        newPerson.setFirstName("new person");
        newPerson.setLastName("new person");
        Response postResponse = getJsonPostResponseAsAdmin(target(PersonAPITest.createPath), newPerson);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        URI NewPersonURI = extractUriFromResponse(postResponse);

        AccountUpdateDTO accountUpdateDTO = getDTOFromModel(accountWitPerson);
        accountUpdateDTO.setLinkedPerson(NewPersonURI);

        Response response = getJsonPutResponse(target(updatePath), accountUpdateDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        URI uriHolderBeforeUpdate = accountWitPerson.getLinkedPerson().getUri();
        URI uriHolderAfterUpdate = accountDAO.get(accountUpdateDTO.getUri()).getLinkedPerson().getUri();
        assertTrue("linked person should not be updated", SPARQLDeserializers.compareURIs(uriHolderBeforeUpdate, uriHolderAfterUpdate) );
    }

    @Test
    public void getAccount() throws Exception {
        AccountModel accountWitPerson = createAccount1WithPerson();

        Response getResponse = getJsonGetByUriResponseAsAdmin(target(getPath), accountWitPerson.getUri().toString());
        assertEquals("get call didn't work", Response.Status.OK.getStatusCode(), getResponse.getStatus());

        AccountDTO accountDTO = extractDTOFromGetResponse(getResponse);
        compareDTOAndModelWithPerson(accountDTO, accountWitPerson);
    }

    @Test
    public void getAccountUriDoesntExists() throws Exception {
        Response getResponse = getJsonGetByUriResponseAsAdmin(target(getPath), "http://unexistent/uri");
        assertEquals("get call didn't work", Response.Status.NOT_FOUND.getStatusCode(), getResponse.getStatus());
    }

    @Test
    public void getAccountUriIsNotFromAnAccount() throws Exception {
        URI randomPersonUri = createPerson();
        Response getResponse = getJsonGetByUriResponseAsAdmin(target(getPath), randomPersonUri.toString() );
        assertEquals("get call didn't work", Response.Status.NOT_FOUND.getStatusCode(), getResponse.getStatus());
    }

    private AccountDTO extractDTOFromGetResponse(Response getResponse){
        JsonNode node = getResponse.readEntity(JsonNode.class);
        return extractDTOFromJsonNode(node);
    }

    private AccountDTO extractDTOFromJsonNode(JsonNode node){
        SingleObjectResponse<AccountGetDTO> getObjectResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<AccountGetDTO>>() {
        });
        return getObjectResponse.getResult();
    }

    private void compareDTOAndModelWithPerson(AccountDTO accountDTO, AccountModel accountModel){
        assertTrue ( SPARQLDeserializers.compareURIs( accountDTO.linkedPerson, accountModel.getLinkedPerson().getUri() ) );
        compareDTOAndModelWithoutPerson(accountDTO, accountModel);
    }

    private void compareDTOAndModelWithoutPerson(AccountDTO accountDTO, AccountModel accountModel) {
        assertTrue ( "uri are different", SPARQLDeserializers.compareURIs( accountDTO.uri, accountModel.getUri() ) );
        assertEquals("email are different", accountDTO.email, accountModel.getEmail().toString());
        assertEquals("admin bool are different", accountDTO.admin, accountModel.isAdmin().booleanValue());
        assertEquals("language are different", accountDTO.language, accountModel.getLanguage());
        assertEquals("enable bool are different", accountDTO.enable, accountModel.getIsEnabled());
    }
}