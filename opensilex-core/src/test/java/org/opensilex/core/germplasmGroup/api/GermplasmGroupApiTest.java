package org.opensilex.core.germplasmGroup.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.germplasm.api.GermplasmCreationDTO;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupDAO;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.ResourceDagDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class GermplasmGroupApiTest extends AbstractMongoIntegrationTest {

    public static final String path = "/core/germplasm_group";

    public static final String uriPath = path + "/{uri}";
    public static final String searchPath = path + "/search";
    public static final String createPath = path;
    public static final String updatePath = path;
    public static final String deletePath = path + "/{uri}";

    public static GermplasmDAO germplasmDAO;
    public static GermplasmGroupDAO germplasmGroupDAO;
    public static GermplasmModel testSpecies1CreatedModel;

    public static GermplasmModel testSpecies2CreatedModel;

    public static GermplasmGroupModel preCreatedGroup;
    public static GermplasmCreationDTO getCreationSpeciesDTO(String suffix) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testSpecies" + suffix);
        germplasmDTO.setRdfType(new URI(Oeso.Species.toString()));
        return germplasmDTO;
    }
    public static GermplasmGroupCreationDTO getCreationGroupDTO(String suffix) throws URISyntaxException {
        GermplasmGroupCreationDTO newGermsGroup = new GermplasmGroupCreationDTO();
        newGermsGroup.setName("testGermsEmpty"+suffix);
        newGermsGroup.setDescription("some random description");
        return newGermsGroup;
    }

    @Before
    public void beforeStuff() throws Exception {
        //2 Species used to create and update groups
        SPARQLService sparqlService = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        MongoDBService mongoDBService = getOpensilex().getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        germplasmDAO = new GermplasmDAO(sparqlService, mongoDBService);
        GermplasmCreationDTO testSpecies1Dto = getCreationSpeciesDTO("1");
        GermplasmCreationDTO testSpecies2Dto = getCreationSpeciesDTO("2");
        GermplasmModel testSpecies1model = testSpecies1Dto.newModel();
        testSpecies1CreatedModel = germplasmDAO.create(testSpecies1model);
        GermplasmModel testSpecies2model = testSpecies2Dto.newModel();
        testSpecies2CreatedModel = germplasmDAO.create(testSpecies2model);

        //a pre created group to test stuff on
        GermplasmGroupCreationDTO preCreatedGroupDTO = getCreationGroupDTO("pre");
        List<URI> germplasmUris = new ArrayList<>();
        germplasmUris.add(testSpecies1CreatedModel.getUri());
        germplasmUris.add(testSpecies2CreatedModel.getUri());
        preCreatedGroupDTO.setGermplasmList(germplasmUris);
    germplasmGroupDAO = new GermplasmGroupDAO(sparqlService);
        preCreatedGroup = germplasmGroupDAO.create(preCreatedGroupDTO.newModel());
    }

    /**
     * Tests :
     * - Create an empty group, test that it was indeed created and that it is empty
     *
     */
    @Test
    public void testCreateEmptyGermplasmGroup() throws Exception {
        GermplasmGroupCreationDTO newGermsGroup = getCreationGroupDTO("empty");
        // create group
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), newGermsGroup);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResultSpecies = getJsonGetByUriResponseAsAdmin(target(uriPath), createdUri.toString());

        JsonNode node = getResultSpecies.readEntity(JsonNode.class);
        SingleObjectResponse<GermplasmGroupGetDTO> response = mapper.convertValue(node, new TypeReference<SingleObjectResponse<GermplasmGroupGetDTO>>() {
        });
        assertEquals(0, response.getResult().getGermplasmCount());
    }

    /**
     * Tests :
     * - Create a group, test that it was indeed created and that it has the correct amount of germplasms (2)
     *
     */
    @Test
    public void testCreateGermplasmGroup() throws Exception {
        GermplasmGroupCreationDTO newGermsGroup = getCreationGroupDTO("createTest");
        List<URI> germplasmUris = new ArrayList<>();
        germplasmUris.add(testSpecies1CreatedModel.getUri());
        germplasmUris.add(testSpecies2CreatedModel.getUri());
        newGermsGroup.setGermplasmList(germplasmUris);
        // create group
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), newGermsGroup);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResultSpecies = getJsonGetByUriResponseAsAdmin(target(uriPath), createdUri.toString());

        JsonNode node = getResultSpecies.readEntity(JsonNode.class);
        SingleObjectResponse<GermplasmGroupGetDTO> response = mapper.convertValue(node, new TypeReference<SingleObjectResponse<GermplasmGroupGetDTO>>() {
        });
        assertEquals(2, response.getResult().getGermplasmCount());

        assertEquals(Response.Status.OK.getStatusCode(), getResultSpecies.getStatus());
    }

    @Test
    public void validatePreCreatedGroup(){
        assertEquals(2, preCreatedGroup.getGermplasmList().size());
    }

    /**
     * Tests :
     * - Search all, check that the response status is OK
     *
     */
    @Test
    public void testSearchAllGermplasmGroups() throws Exception {

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, new HashMap<String, Object>());
        //Post response as the get function is an @POST
        //final Response getResult = appendAdminToken(searchTarget).get();
        final Response getResult = getJsonPostResponseAsAdmin(target(searchPath), null);
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

    }

    /**
     * Tests for getByUri:
     * - Returned group has correct uri
     * - Returned group has correct amount of germplasms
     * - Returned group has a name and a description
     * - Response OK
     *
     */
    @Test
    public void testGetByUriGermplasmGroup() throws Exception {
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), preCreatedGroup.getUri().toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<GermplasmGroupGetDTO> response = mapper.convertValue(node, new TypeReference<SingleObjectResponse<GermplasmGroupGetDTO>>() {
        });
        assertEquals(preCreatedGroup.getUri().toString(), response.getResult().getUri().toString());
        assertEquals(response.getResult().getGermplasmCount(), preCreatedGroup.getGermplasmList().size());
        assertTrue(response.getResult().name != null && response.getResult().getDescription()!=null);
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }


    /**
     * Tests for Update group, after update we run a get:
     * - Returned group has correct uri
     * - The new name is present
     * - Description has not changed
     * - Response OK
     *
     */
    @Test
    public void testUpdateGermplasmGroup() throws Exception {
        GermplasmGroupUpdateDTO updateDto = new GermplasmGroupUpdateDTO();
        updateDto.setName("new alias");
        updateDto.setUri(preCreatedGroup.getUri());
        updateDto.setGermplasmList(preCreatedGroup.getGermplasmList().stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList()));
        updateDto.setDescription(preCreatedGroup.getDescription());

        //check that you can update species
        final Response updateSpecies = getJsonPutResponse(target(updatePath), updateDto);

        //Perform a get to test modified fields
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), preCreatedGroup.getUri().toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<GermplasmGroupGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<GermplasmGroupGetDTO>>() {
        });
        assertEquals(preCreatedGroup.getUri().toString(), getResponse.getResult().getUri().toString());
        assertEquals("new alias", getResponse.getResult().getName());
        assertEquals(preCreatedGroup.getDescription(), getResponse.getResult().getDescription());

        assertEquals(Response.Status.OK.getStatusCode(), updateSpecies.getStatus());
    }

    @Test
    public void testDeleteGermplasmGroup() throws Exception {
        String preCreatedUri = preCreatedGroup.getUri().toString();
        // delete the species that can be deleted
        Response delResult = getDeleteByUriResponse(target(deletePath), preCreatedUri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        // check if URI no longer exists
        Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), preCreatedUri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return new ArrayList<Class<? extends SPARQLResourceModel>>() {{
            add(GermplasmGroupModel.class);
            add(GermplasmModel.class);
        }};
    }
}