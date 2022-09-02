package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.germplasm.api.GermplasmAPITest;
import org.opensilex.core.germplasm.api.GermplasmCreationDTO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.species.api.SpeciesDTO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.api.entity.EntityCreationDTO;
import org.opensilex.core.variable.dal.*;
import org.opensilex.core.variablesGroup.api.VariablesGroupCreationDTO;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * @author Renaud COLIN
 */
public class VariableApiTest extends AbstractMongoIntegrationTest {

    public String path = VariableAPI.PATH;

    public String getByUriPath = path + "/{uri}";
    public String searchPath = path;
    public String createPath = path ;
    public String updatePath = path ;
    public String deletePath = path + "/{uri}";

    private GermplasmCreationDTO germplasm;
    private EntityCreationDTO entity;

    @Before
    public void beforeTest() throws Exception {

        // create an entity to use as InterestEntity
        entity = EntityApiTest.getCreationDto();
        final Response postEntityResult = getJsonPostResponse(target(EntityApiTest.createPath), entity);
        assertEquals(Response.Status.CREATED.getStatusCode(), postEntityResult.getStatus());
        entity.setUri(extractUriFromResponse(postEntityResult));

        // create a germplasm to use as InterestEntity
        germplasm = GermplasmAPITest.getCreationSpeciesDTO();
        final Response postGermplasmResult = getJsonPostResponse(target(GermplasmAPITest.createPath), germplasm);
        assertEquals(Response.Status.CREATED.getStatusCode(), postGermplasmResult.getStatus());
        germplasm.setUri(extractUriFromResponse(postGermplasmResult));
    }

    public VariableCreationDTO getCreationDto() throws Exception {

        SPARQLService service = getSparqlService();

        EntityModel entity = new EntityModel();
        entity.setName("Artemisia absinthium");
        entity.setDescription("A plant which was used in the past for building methanol");

        service.create(entity);

        CharacteristicModel characteristic = new CharacteristicModel();
        characteristic.setName("size");
        characteristic.setDescription("The size of an object");
        service.create(characteristic);

        MethodModel method = new MethodModel();
        method.setName("SVM");
        method.setDescription("A machine learning based method");
        service.create(method);

        UnitModel unit = new UnitModel();
        unit.setName("minute");
        unit.setDescription("I really need to comment it ?");
        unit.setSymbol("m");
        unit.setAlternativeSymbol("mn");
        service.create(unit);

        VariableCreationDTO variableDto = new VariableCreationDTO();
        variableDto.setName(entity.getName() + characteristic.getName());
        variableDto.setAlternativeName(variableDto.getName() + method.getName() + unit.getName());
        variableDto.setDescription("A comment about a variable");

        variableDto.setEntity(entity.getUri());
        variableDto.setCharacteristic(characteristic.getUri());
        variableDto.setMethod(method.getUri());
        variableDto.setUnit(unit.getUri());

        variableDto.setTrait(new URI("http://purl.obolibrary.org/obo/TO_0002644"));
        variableDto.setTraitName("dry matter digestibility");
        variableDto.setTimeInterval("minutes");
        variableDto.setDataType(new URI("xsd:decimal"));

        return variableDto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath, getByUriPath, deletePath, getCreationDto());
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        VariableCreationDTO dto = new VariableCreationDTO();
        dto.setName("name");
        dto.setDescription("only a comment, not a name");

        Response postResult = getJsonPostResponse(target(createPath), dto);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        dto = getCreationDto();
        dto.setUnit(null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        dto = getCreationDto();
        dto.setEntity(null);
        dto.setCharacteristic(null);

        postResult = getJsonPostResponse(target(createPath), dto);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }


    @Test
    public void testCreateFailWithInvalidObjects() throws Exception {

        VariableCreationDTO dto = getCreationDto();
        VariableCreationDTO badDto = getCreationDto();
        badDto.setEntity(new URI("test:no-entity"));

        Response postResult = getJsonPostResponse(target(createPath), badDto);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());

        // reset with good entity and bad characteristic
        badDto.setEntity(dto.getEntity());
        badDto.setCharacteristic(new URI("test:no-characteristic"));
        postResult = getJsonPostResponse(target(createPath), badDto);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());

        // reset with good characteristic and bad method
        badDto.setCharacteristic(dto.getCharacteristic());
        badDto.setMethod(new URI("test:no-method"));
        postResult = getJsonPostResponse(target(createPath), badDto);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());

        // reset with good method and bad unit
        badDto.setMethod(dto.getMethod());
        badDto.setUnit(new URI("test:no-unit"));
        postResult = getJsonPostResponse(target(createPath), badDto);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.Variable + "/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        VariableCreationDTO dto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), dto);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setName("new alias");
        dto.setDescription("new comment");
        dto.setTrait(new URI("http://purl.obolibrary.org/obo/TO_0002644_new"));

        // create a new entity to associate with the variable
        EntityModel entity = new EntityModel();
        entity.setName("Artemisia absinthium");
        entity.setDescription("A plant which was used in the past for building methanol");
        getSparqlService().create(entity);
        dto.setEntity(entity.getUri());

        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponse(target(getByUriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<VariableDetailsDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<VariableDetailsDTO>>() {
        });
        VariableDetailsDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getName(), dtoFromApi.getName());
        assertEquals(dto.getDescription(), dtoFromApi.getDescription());
        assertTrue(SPARQLDeserializers.compareURIs(dto.getEntity(), dtoFromApi.getEntity().getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(dto.getTrait(), dtoFromApi.getTrait()));
    }

    @Test
    public void testGetByUri() throws Exception {

        // Try to insert an Entity, to fetch it and to get fields
        VariableCreationDTO creationDTO = getCreationDto();

        Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<VariableDetailsDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<VariableDetailsDTO>>() {
        });
        VariableDetailsDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);

        assertEquals(creationDTO.getName(), dtoFromDb.getName());
        assertEquals(creationDTO.getAlternativeName(), dtoFromDb.getAlternativeName());
        assertEquals(creationDTO.getDescription(), dtoFromDb.getDescription());
        assertEquals(creationDTO.getTrait(), dtoFromDb.getTrait());
        assertEquals(creationDTO.getTraitName(), dtoFromDb.getTraitName());

        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getEntity(), dtoFromDb.getEntity().getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getCharacteristic(), dtoFromDb.getCharacteristic().getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getMethod(), dtoFromDb.getMethod().getUri()));
        assertTrue(SPARQLDeserializers.compareURIs(creationDTO.getUnit(), dtoFromDb.getUnit().getUri()));
    }

    private final static URI GERMPLASM_URI_1 = URI.create("test:species_testCreateWithSpeciesOK_1");
    private final static URI GERMPLASM_URI_2 = URI.create("test:species_testCreateWithSpeciesOK_2");

    @Test
    /**
     * Test that the variable creation is OK with existing valid species
     */
    public void testCreateWithSpeciesOK() throws Exception {

        // create species and ensure that creation was OK
        GermplasmCreationDTO species1 = GermplasmAPITest.getCreationSpeciesDTO();
        species1.setName("species_testCreateWithSpeciesOK_1");
        species1.setUri(GERMPLASM_URI_1);
        Response postGermplasmResult = getJsonPostResponse(target(GermplasmAPITest.createPath), species1);
        assertEquals(Response.Status.CREATED.getStatusCode(), postGermplasmResult.getStatus());

        GermplasmCreationDTO species2 = GermplasmAPITest.getCreationSpeciesDTO();
        species2.setName("species_testCreateWithSpeciesOK_2");
        species2.setUri(GERMPLASM_URI_2);
        postGermplasmResult = getJsonPostResponse(target(GermplasmAPITest.createPath), species2);
        assertEquals(Response.Status.CREATED.getStatusCode(), postGermplasmResult.getStatus());

        // create variable with species -> should be CREATED
        VariableCreationDTO dto = getCreationDto();
        dto.setUri(URI.create("test:variable_testCreateWithSpeciesOK"));
        dto.setSpecies(Arrays.asList(species1.getUri(), species2.getUri()));

        Response postResult = getJsonPostResponse(target(createPath), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), dto.getUri().toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<VariableDetailsDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<VariableDetailsDTO>>() {
        });
        VariableDetailsDTO dtoFromDb = getResponse.getResult();

        assertEquals(
                Sets.newHashSet(
                        SPARQLDeserializers.formatURI(species1.getUri()),
                        SPARQLDeserializers.formatURI(species2.getUri())),
                dtoFromDb.getSpecies().stream().map(speciesDTO -> SPARQLDeserializers.formatURI(speciesDTO.getUri())).collect(Collectors.toSet())
        );

    }

    @Test
    /**
     * Test that the variable creation fail if the provided species is unknown
     */
    public void testFailWithUnknownSpecies() throws Exception {
        // create variable with an unknown species
        VariableCreationDTO dto = getCreationDto();
        dto.setSpecies(Arrays.asList(URI.create("test:unknown_species_1")));

        Response postResult = getJsonPostResponse(target(createPath), dto);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());
    }

    @Test
    /**
     * Test that the variable creation fail if a sub-type of Germplasm, different than species, is provided
     */
    public void testFailWithGermplasmWithoutSpeciesType() throws Exception {

        // create variety and associated germplasm
        GermplasmCreationDTO speciesOfVariety = new GermplasmCreationDTO();
        speciesOfVariety.setName("speciesOfVariety");
        speciesOfVariety.setRdfType(URI.create(Oeso.Species.getURI()));
        speciesOfVariety.setUri(URI.create("test:speciesOfVariety"));

        // ensure species was created
        Response postSpeciesResponse = getJsonPostResponse(target(GermplasmAPITest.createPath), speciesOfVariety);
        assertEquals(Response.Status.CREATED.getStatusCode(), postSpeciesResponse.getStatus());

        GermplasmCreationDTO variety = new GermplasmCreationDTO();
        variety.setName("variety");
        variety.setRdfType(URI.create(Oeso.Germplasm.toString()));
        variety.setUri(URI.create("test:test_variety"));
        variety.setSpecies(speciesOfVariety.getUri());

        // ensure variety was created
        Response postVarietyResponse = getJsonPostResponse(target(GermplasmAPITest.createPath), variety);
        assertEquals(Response.Status.CREATED.getStatusCode(), postVarietyResponse.getStatus());

        // create variable with variety -> should fail, since variable expect species, not variety
        VariableCreationDTO dto = getCreationDto();
        dto.setSpecies(Arrays.asList(variety.getUri()));

        Response postResult = getJsonPostResponse(target(createPath), dto);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), postResult.getStatus());
    }

    @Test
    /**
     * Test that the variable search with species is OK.
     */
    public void testSpeciesSearchOK() throws Exception {

        // reuse test -> create variable with two associated species
        testCreateWithSpeciesOK();

        // create variable with no species and ensure creation was OK
        VariableCreationDTO variableWithNoSpecies = getCreationDto();
        variableWithNoSpecies.setUri(URI.create("test:variable_with_no_species"));
        Response postResult = getJsonPostResponse(target(createPath), variableWithNoSpecies);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // run a search query with species filter
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("species", Arrays.asList(GERMPLASM_URI_1));

        // convert returned JSON into dtos
        List<VariableGetDTO> results = getResults(searchPath, searchParams, new TypeReference<PaginatedListResponse<VariableGetDTO>>() {
        });
        assertEquals(1, results.size());

        // ensure that the returned variable is the good
        boolean variableFound = results.stream()
                .anyMatch(variable -> SPARQLDeserializers
                .compareURIs(variable.getUri().toString(),"test:variable_testCreateWithSpeciesOK"));
        assertTrue(variableFound);

        // ensure that all variables are returned when no filter
        List<VariableGetDTO> allVariables = getResults(searchPath, Collections.emptyMap(), new TypeReference<PaginatedListResponse<VariableGetDTO>>() {
        });
        assertEquals(2, allVariables.size());


        // ensure that no variables is returned when using a species not linked to any variable
        searchParams = new HashMap<>();
        searchParams.put("species", Arrays.asList("test:unknown_species_in_search"));
        List<VariableGetDTO> noVariables = getResults(searchPath, searchParams, new TypeReference<PaginatedListResponse<VariableGetDTO>>() {
        });
        assertTrue(noVariables.isEmpty());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(
                EntityModel.class,
                GermplasmModel.class,
                SpeciesModel.class,
                VariableModel.class
        );
    }

}
