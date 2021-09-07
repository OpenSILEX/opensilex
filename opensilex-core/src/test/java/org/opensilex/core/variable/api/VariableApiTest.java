package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import static junit.framework.TestCase.assertTrue;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 * @author Renaud COLIN
 */
public class VariableApiTest extends AbstractMongoIntegrationTest {

    public String path = VariableAPI.PATH;

    public String getByUriPath = path + "/{uri}";
    public String createPath = path ;
    public String updatePath = path ;
    public String deletePath = path + "/{uri}";

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

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(VariableModel.class);
    }

}
