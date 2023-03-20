package org.opensilex.security.user.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.ws.rs.client.WebTarget;

import org.opensilex.OpenSilex;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.api.PersonAPITest;
import org.opensilex.security.person.api.PersonDTO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

import static junit.framework.TestCase.*;

public class UserAPITest extends AbstractSecurityIntegrationTest {

    /*
     * Uncomment this method to enable test debug logging
     */
//    protected boolean isDebug() {
//        return true;
//    }
    public static String path = "security/users";
    public static String createPath = path ;
    public String createWithExistingPersonPath = path + "/person_already_exists";
    public String updatePath = path ;
    public static String getPath = path + "/{uri}";
    public String deletePath = path + "/{uri}";
    public String searchPath = path ;
    protected String urisListPath = path + "/by_uris";

    private int userCount = 0;

    protected UserCreationDTO getUser1CreationDTO() {
        int count = userCount++;

        UserCreationDTO dto = new UserCreationDTO();
        dto.setAdmin(true);
        dto.setEmail("user" + count + "@opensilex.org");
        dto.setFirstName("user" + count + "");
        dto.setLastName("user" + count + " last name");
        dto.setPassword("1234");
        dto.setLanguage(OpenSilex.DEFAULT_LANGUAGE);
        return dto;
    }

    protected UserCreationDTO getUser2CreationDTO() {
        int count = userCount++;

        UserCreationDTO dto = new UserCreationDTO();
        dto.setAdmin(false);
        dto.setEmail("user" + count + "@opensilex.org");
        dto.setFirstName("user" + count + "");
        dto.setLastName("user" + count + " last name");
        dto.setPassword("6789");
        dto.setLanguage(OpenSilex.DEFAULT_LANGUAGE);
        return dto;
    }

    protected UserCreationWithExistantPersonDTO getUserCreationWithExistantPersonDTO() throws URISyntaxException {
        UserCreationWithExistantPersonDTO userCreation = new UserCreationWithExistantPersonDTO();
        userCreation.setUri(new URI("http://opensilex.dev/id/user/default.account"));
        userCreation.setEmail("test@test.ts");
        userCreation.setPassword("pwd");
        userCreation.setPersonHolderUri(new URI("http://opensilex.dev/id/user/default.person"));
        userCreation.setAdmin(false);
        userCreation.setLanguage("en");

        return userCreation;
    }

    @Test
    public void testCreate() throws Exception {
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getUser1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        postResult = getJsonPostResponseAsAdmin(target(createPath), getUser2CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        createdUri = extractUriFromResponse(postResult);
        getResult = getJsonGetByUriResponseAsAdmin(target(getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreateWithAnExistingPerson() throws Exception {
        //create the person
        PersonDTO personToLinkWith = PersonAPITest.getDefaultDTO();
        Response postResult = getJsonPostResponseAsAdmin(target(PersonAPITest.createPath), personToLinkWith);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        //create the user
        UserCreationWithExistantPersonDTO userCreation = getUserCreationWithExistantPersonDTO();
        userCreation.setPersonHolderUri(personToLinkWith.getUri());
        postResult = getJsonPostResponseAsAdmin(target(createWithExistingPersonPath), userCreation);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        //check that account (user URI) and person are linked
        AccountDAO accountDAO = new AccountDAO(getSparqlService());
        AccountModel createdUser = accountDAO.get(userCreation.getUri());
        assertEquals(createdUser.getHolderOfTheAccount().getUri(), personToLinkWith.getUri());
    }

    @Test
    public void testCreateWithAnExistingPerson_personDoesntExist() throws Exception {
        UserCreationWithExistantPersonDTO userCreation = getUserCreationWithExistantPersonDTO();

        //check that API return the right code
        Response postResult = getJsonPostResponseAsAdmin(target(createWithExistingPersonPath), userCreation);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());

        //check that the user (account) was not insert in the dataBase
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), userCreation.getUri().toString());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreateWithAnExistingPerson_personAlreadyHasAnAccount() throws Exception {
        //creation of a user (account + person)
        UserCreationDTO user = getUser1CreationDTO();
        user.setUri(new URI("http://opensilex.dev/id/user/account.willBeCreated"));
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), user);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());

        //get the person created
        AccountDAO accountDAO = new AccountDAO(getSparqlService());
        PersonModel person = accountDAO.get(user.getUri()).getHolderOfTheAccount();

        //try to create a user with this same person
        UserCreationWithExistantPersonDTO userTest = getUserCreationWithExistantPersonDTO();
        userTest.setPersonHolderUri(person.getUri());
        postResponse = getJsonPostResponseAsAdmin(target(createWithExistingPersonPath), userTest);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), postResponse.getStatus());

        //check that the account(user) was not created
        assertNull(accountDAO.get(userTest.getUri()));
    }

    @Test
    public void testUpdate() throws Exception {
        // create the user
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getUser1CreationDTO());

        // update the xp
        URI uri = extractUriFromResponse(postResult);
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUri(uri);
        dto.setEmail("a@b.com");
        dto.setFirstName("a");
        dto.setLastName("b");
        dto.setAdmin(false);
        dto.setLanguage("fr");

        final Response putResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<UserGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<UserGetDTO>>() {
        });
        UserGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        compareUsersDTO(dto, dtoFromApi);
    }

    @Test
    public void testDelete() throws Exception {
        // create object and check if URI exists
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getUser1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUriFail() throws Exception {
        final Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getUser1CreationDTO());
        String uri = extractUriFromResponse(postResponse).toString();

        // call the service with a non-existing pseudo random URI
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), uri + "7FG4FG89FG4GH4GH57");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearch() throws Exception {
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getUser1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        postResult = getJsonPostResponseAsAdmin(target(createPath), getUser2CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("name", "user.*");
            }
        };

        WebTarget target = appendSearchParams(target(searchPath), 0, 50, params);
        Response getSearchResult = appendAdminToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getSearchResult.getStatus());

        JsonNode node = getSearchResult.readEntity(JsonNode.class);
        PaginatedListResponse<UserGetDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<UserGetDTO>>() {
        });
        List<UserGetDTO> users = listResponse.getResult();

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());

        params.put("name", "user1");
        target = appendSearchParams(target(searchPath), 0, 50, params);
        getSearchResult = appendAdminToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getSearchResult.getStatus());

        node = getSearchResult.readEntity(JsonNode.class);
        listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<UserGetDTO>>() {
        });
        users = listResponse.getResult();
        assertEquals(1, users.size());
    }

    @Test
    public void testGetByURIs() throws Exception {
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getUser1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
        URI user1URI = extractUriFromResponse(postResult);

        postResult = getJsonPostResponseAsAdmin(target(createPath), getUser2CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
        URI user2URI = extractUriFromResponse(postResult);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("uris", Arrays.asList(user1URI, user2URI));
            }
        };

        WebTarget target = appendQueryParams(target(urisListPath), params);
        Response getResult = appendAdminToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<UserGetDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<UserGetDTO>>() {
        });
        List<UserGetDTO> users = listResponse.getResult();

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());
    }

    private void compareUsersDTO(UserGetDTO expectedUserDTO, UserGetDTO actualUserDTO) {
        assertEquals(expectedUserDTO.getUri(), actualUserDTO.getUri());
        assertEquals(expectedUserDTO.getEmail(), actualUserDTO.getEmail());
        assertEquals(expectedUserDTO.getFirstName(), actualUserDTO.getFirstName());
        assertEquals(expectedUserDTO.getLastName(), actualUserDTO.getLastName());
        assertEquals(expectedUserDTO.isAdmin(), actualUserDTO.isAdmin());
        assertEquals(expectedUserDTO.getLanguage(), actualUserDTO.getLanguage());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        List<Class<? extends SPARQLResourceModel>> modelList = new ArrayList<>();
        modelList.add(AccountModel.class);
        return modelList;
    }

    @Override
    public void afterEach() throws Exception {
        getOpensilex().getModuleByClass(SecurityModule.class).createDefaultSuperAdmin();
    }
}
