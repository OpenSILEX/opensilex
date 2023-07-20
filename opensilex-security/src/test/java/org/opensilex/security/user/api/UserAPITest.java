package org.opensilex.security.user.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
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
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class UserAPITest extends AbstractSecurityIntegrationTest {

    /*
     * Uncomment this method to enable test debug logging
     */
//    protected boolean isDebug() {
//        return true;
//    }
    public static String path = "security/users";
    public static String createPath = path ;
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
    public void testUpdate_accountHasNoHolderAndWillNotHave() throws Exception {
        UserGetDTO createdUser = getUser1CreationDTO();
        // create the user
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), createdUser);
        URI userURI = extractUriFromResponse(postResult);
        URI holderOfTheAccountUri = new AccountDAO(getSparqlService()).get(userURI).getLinkedPerson().getUri();

        // update the xp
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUri(userURI);
        dto.setEmail("a@b.com");
        dto.setFirstName("a");
        dto.setLastName("b");
        dto.setLinkedPerson(holderOfTheAccountUri);
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

        AccountDAO accountDAO = new AccountDAO(getSparqlService());
        AccountModel accountModel = accountDAO.get(dto.getUri());
        UserGetDTO dtoFromDatabase = UserGetDTO.fromModel(accountModel);
        compareUsersDTO(dto, dtoFromDatabase);

        //check that email of the person is not updated nor deleted by the User Update
        PersonModel personModel = accountModel.getLinkedPerson();
        assertEquals(createdUser.getEmail(), personModel.getEmail().toString());
    }

    @Test
    public void updateAndSetANewPerson() throws Exception {
        PersonDTO newHolder = new PersonDTO();
        newHolder.setFirstName("bib");
        newHolder.setLastName("elot");
        Response personCreated = getJsonPostResponseAsAdmin(target(PersonAPITest.createPath), newHolder);
        assertEquals(Response.Status.CREATED.getStatusCode(), personCreated.getStatus());
        URI newHolderURI = extractUriFromResponse(personCreated);

        UserGetDTO createdUser = getUser1CreationDTO();
        Response userCreated = getJsonPostResponseAsAdmin(target(createPath), createdUser);
        assertEquals(Response.Status.CREATED.getStatusCode(), userCreated.getStatus());
        URI userURI = extractUriFromResponse(userCreated);


        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setUri(userURI);
        updateDTO.setLinkedPerson(newHolderURI);
        updateDTO.setEmail(createdUser.getEmail());
        updateDTO.setAdmin(createdUser.isAdmin());
        updateDTO.setLanguage(createdUser.getLanguage());

        final Response putResult = getJsonPutResponse(target(updatePath), updateDTO);
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        AccountModel updatedAccount = new AccountDAO(getSparqlService()).get(userURI);
        assertEquals(URIDeserializer.formatURI(newHolderURI), URIDeserializer.formatURI( updatedAccount.getLinkedPerson().getUri() ));
    }

//    @Test
//    public void testDelete() throws Exception {
//        // create object and check if URI exists
//        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getUser1CreationDTO());
//        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
//        String uri = extractUriFromResponse(postResponse).toString();
//
//        // delete object and if URI no longer exists
//        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
//        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());
//
//        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), uri);
//        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
//    }

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
