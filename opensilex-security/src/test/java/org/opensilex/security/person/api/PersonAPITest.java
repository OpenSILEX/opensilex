package org.opensilex.security.person.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.user.api.UserAPITest;
import org.opensilex.security.user.api.UserCreationDTO;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PersonAPITest extends AbstractSecurityIntegrationTest {

    protected static String path = "security/persons";
    public static String createPath = path;
    protected static String updatePath = path;
    protected static String getPath = path + "/{uri}";
    public static String deletePath = path + "/{uri}";
    protected String searchPath = path;

    public static PersonDTO getDefaultDTO() throws URISyntaxException {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setUri(new URI("http://opensilex.dev/id/user/default.default"));
        personDTO.setFirstName("Default");
        personDTO.setLastName("DEFAULT");
        personDTO.setEmail("default@inrae.fr");
        personDTO.setAffiliation("MISTEA");
        personDTO.setPhoneNumber("+33--142-75-90-00");

        return personDTO;
    }

    protected PersonDTO get2ndDefaultDTO() throws URISyntaxException {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setUri(new URI("http://opensilex.dev/id/user/default2.default2"));
        personDTO.setFirstName("Default2");
        personDTO.setLastName("DEFAULT2");
        personDTO.setEmail("default2@inrae.fr");

        return personDTO;
    }

    @Test
    public void create() throws Exception {
        //check the response
        Response result = getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatus());
        URI createdUri = extractUriFromResponse(result);

        //check the database
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

    }

    @Test
    public void createWithoutPredefinedURI() throws Exception {
        PersonDTO withoutURI = new PersonDTO();
        withoutURI.setFirstName("Default");
        withoutURI.setLastName("DEFAULT");

        Response result = getJsonPostResponseAsAdmin(target(createPath), withoutURI);
        assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatus());

    }

    @Test
    public void createWithExistingURIReturnConflict() throws Exception {
        //create a Person
        getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());
        //try to create the same Person again
        Response result = getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());
        assertEquals(Response.Status.CONFLICT.getStatusCode(), result.getStatus());
    }

    @Test
    public void createWithOrcidBadSyntax()throws Exception {
        URI orcid = new URI("http://orcid.org");

        PersonDTO personWithOrcid = getDefaultDTO();
        personWithOrcid.setOrcid(orcid);

        Response result = getJsonPostResponseAsAdmin(target(createPath), personWithOrcid);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatus());
    }

    @Test
    public void createNeedsAtLeastFirstAndLastName() throws Exception {
        PersonDTO withoutLast = new PersonDTO();
        withoutLast.setUri(new URI("http://opensilex.dev/id/user/default.default"));
        withoutLast.setFirstName("Default");

        PersonDTO withoutFirst = new PersonDTO();
        withoutFirst.setUri(new URI("http://opensilex.dev/id/user/autre.autre"));
        withoutFirst.setLastName("DEFAULT");

        Response result = getJsonPostResponseAsAdmin(target(createPath), withoutLast);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatus());

        result = getJsonPostResponseAsAdmin(target(createPath), withoutFirst);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatus());
    }

    @Test
    public void getPerson() throws Exception {
        PersonDTO person1 = getDefaultDTO();
        PersonDTO person2 = get2ndDefaultDTO();
        //creating persons
        getJsonPostResponseAsAdmin(target(createPath), person1);
        getJsonPostResponseAsAdmin(target(createPath), person2);

        //call to get service and extract the result
        //extract first result
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), person1.getUri().toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<PersonDTO> getObjectResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PersonDTO>>() {
        });
        PersonDTO resultPerson1 = getObjectResponse.getResult();

        //extract second result
        getResult = getJsonGetByUriResponseAsAdmin(target(getPath), person2.getUri().toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
        node = getResult.readEntity(JsonNode.class);
        getObjectResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PersonDTO>>() {
        });
        PersonDTO resultPerson2 = getObjectResponse.getResult();

        //compare dtos to ensure we've got the good person
        comparePersonDTO(person1, resultPerson1);
        comparePersonDTO(person2, resultPerson2);
    }

    @Test
    public void getPersonReturnNotFoundForUniexsistingURI() throws Exception {
        //creating persons
        getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());
        getJsonPostResponseAsAdmin(target(createPath), get2ndDefaultDTO());

        URI unexistingURI = new URI("http://opensilex.dev/id/user/unexistingUSER");

        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), unexistingURI.toString());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void search() throws Exception {
        Response result = getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatus());

        Response result2 = getJsonPostResponseAsAdmin(target(createPath), get2ndDefaultDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), result2.getStatus());

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("name", ".*");
            }
        };

        WebTarget target = appendSearchParams(target(searchPath), 0, 50, params);

        Response searchResponse = getJsonGetResponseAsAdmin(target);
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        //compare URIs
        JsonNode node = searchResponse.readEntity(JsonNode.class);
        PaginatedListResponse<PersonDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<PersonDTO>>() {
        });
        List<PersonDTO> searchResult = listResponse.getResult();
        List<PersonDTO> excpectedResult = new ArrayList<>();
        excpectedResult.add(getDefaultDTO());
        excpectedResult.add(get2ndDefaultDTO());

        assertEquals(excpectedResult.size(), searchResult.size());
        assertTrue(searchResult.containsAll(excpectedResult));
    }

    @Test
    public void searchPersonsWithoutAccount() throws Exception {
        UserCreationDTO userThatWillNotBeInSearchResponse = new UserCreationDTO();
        userThatWillNotBeInSearchResponse.setFirstName("bli");
        userThatWillNotBeInSearchResponse.setLastName("blo");
        userThatWillNotBeInSearchResponse.setPassword("pass");
        userThatWillNotBeInSearchResponse.setEmail("e@mail.valid");
        userThatWillNotBeInSearchResponse.setLanguage("en");
        userThatWillNotBeInSearchResponse.setAdmin(true);
        Response userResponse = getJsonPostResponseAsAdmin(target(UserAPITest.createPath), userThatWillNotBeInSearchResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), userResponse.getStatus());

        Response result = getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatus());

        Response result2 = getJsonPostResponseAsAdmin(target(createPath), get2ndDefaultDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), result2.getStatus());

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("name", ".*");
                put("only_without_account", "true");
            }
        };

        WebTarget target = appendSearchParams(target(searchPath), 0, 50, params);

        Response searchResponse = getJsonGetResponseAsAdmin(target);
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        //compare URIs
        JsonNode node = searchResponse.readEntity(JsonNode.class);
        PaginatedListResponse<PersonDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<PersonDTO>>() {
        });
        List<PersonDTO> searchResult = listResponse.getResult();
        List<PersonDTO> excpectedResult = new ArrayList<>();
        excpectedResult.add(getDefaultDTO());
        excpectedResult.add(get2ndDefaultDTO());

        //the search result should contain the two default persons but not the person created like a user
        assertEquals(excpectedResult.size(), searchResult.size());
        assertTrue(searchResult.containsAll(excpectedResult));
    }

    @Test
    public void update() throws Exception {
        // create the person
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());

        //update the person
        URI uriCreated = extractUriFromResponse(postResult);
        PersonDTO dtoUpdate = new PersonDTO();
        dtoUpdate.setUri(uriCreated);
        dtoUpdate.setFirstName("other");
        dtoUpdate.setLastName("notSame");
        dtoUpdate.setEmail("another@person.com");

        Response putResult = getJsonPutResponse(target(updatePath), dtoUpdate);
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        //extract the dto after data was updated
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), dtoUpdate.getUri().toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<PersonDTO> getObjectResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<PersonDTO>>() {
        });
        PersonDTO resultDto = getObjectResponse.getResult();

        //compare dtos to ensure update worked
        comparePersonDTO(dtoUpdate, resultDto);
    }

    @Test
    public void updateDontChangeDatabaseIfURIDoesntExist() throws Exception {
        //trying update someone who doesn't exist in the dataBase
        PersonDTO dtoUpdate = getDefaultDTO();

        Response putResult = getJsonPutResponse(target(updatePath), dtoUpdate);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), putResult.getStatus());

        //verify that update doesn't create the Person
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), dtoUpdate.getUri().toString());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

//    @Test
//    public void delete() throws Exception {
//        //creating persons
//        getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());
//        getJsonPostResponseAsAdmin(target(createPath), get2ndDefaultDTO());
//
//        URI uriToDelete = getDefaultDTO().getUri();
//
//        Response deleteResult = getDeleteByUriResponse(target(deletePath), uriToDelete.toString());
//        assertEquals(Response.Status.OK.getStatusCode(), deleteResult.getStatus());
//
//        //check the database to ensure that deleted person doesn't exist anymore
//        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), uriToDelete.toString());
//        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
//    }
//
//    @Test
//    public void deletePersonLinkedToAnAccount() throws Exception {
//        SPARQLService sparql = getSparqlService();
//
//        UserCreationDTO user = new UserCreationDTO();
//        user.setUri(new URI("http://user/test/delete"));
//        user.setEmail("mail@test.test");
//        user.setPassword("password");
//        user.setFirstName("prenom");
//        user.setLastName("nom");
//        user.setLanguage("fr");
//
//        //user creation
//        Response createdResult = getJsonPostResponseAsAdmin(target(UserAPITest.createPath), user);
//        assertEquals(Response.Status.CREATED.getStatusCode(), createdResult.getStatus());
//
//        //deleting the person linked to the account
//        PersonModel personModel = new PersonDAO(sparql).getPersonFromAccount(user.getUri());
//        Response deleteResponse = getDeleteByUriResponse(target(deletePath), personModel.getUri().toString());
//        assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());
//
//        //checking the person doesn't exist
//        Response getResponse = getJsonGetByUriResponseAsAdmin(target(getPath), personModel.getUri().toString());
//        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResponse.getStatus());
//
//        //checking the account still exist
//        getResponse = getJsonGetByUriResponseAsAdmin(target(UserAPITest.getPath), user.getUri().toString());
//        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
//
//        //checking that the personUri is not existing anymore. The special problem comes from the persistent triplet {personUri foaf:account accountUri}
//        Node usersGraphNode = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(PersonModel.class));
//        boolean uriSteelExist = sparql.uriExists(usersGraphNode, personModel.getUri());
//        assertFalse(uriSteelExist);
//    }
//
//    @Test
//    public void deleteWithUnexistingURI() throws Exception {
//        //creating persons
//        getJsonPostResponseAsAdmin(target(createPath), getDefaultDTO());
//        getJsonPostResponseAsAdmin(target(createPath), get2ndDefaultDTO());
//
//        URI unexistingURI = new URI("http://opensilex.dev/id/user/unexistingUSER");
//
//        Response deleteResult = getDeleteByUriResponse(target(deletePath), unexistingURI.toString());
//        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), deleteResult.getStatus());
//
//        //check the database to ensure that no one was deleted
//        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), getDefaultDTO().getUri().toString());
//        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
//
//        getResult = getJsonGetByUriResponseAsAdmin(target(getPath), get2ndDefaultDTO().getUri().toString());
//        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
//    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        List<Class<? extends SPARQLResourceModel>> modelList = new ArrayList<>();
        modelList.add(PersonModel.class);
        return modelList;
    }

    @Override
    public void afterEach() throws Exception {
        getOpensilex().getModuleByClass(SecurityModule.class).createDefaultSuperAdmin();
    }

    private void comparePersonDTO(PersonDTO dto1, PersonDTO dto2) {
        assertEquals(dto1.getUri(), dto2.getUri());
        assertEquals(dto1.getFirstName(), dto2.getFirstName());
        assertEquals(dto1.getLastName(), dto2.getLastName());
        assertEquals(dto1.getEmail(), dto2.getEmail());
        assertEquals(dto1.getAffiliation(), dto2.getAffiliation());
        assertEquals(dto1.getPhoneNumber(), dto2.getPhoneNumber());
    }


}
