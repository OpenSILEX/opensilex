package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.QualityModel;
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

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Renaud COLIN
 */
public class VariableApiTest extends AbstractSecurityIntegrationTest {

    public String path = "/core/variable";

    public String getByUriPath = path + "/get/{uri}";
    public String createPath = path + "/create";
    public String updatePath = path + "/update";
    public String deletePath = path + "/delete/{uri}";

    private VariableCreationDTO getCreationDto() throws Exception {

        SPARQLService service = getSparqlService();
        
        EntityModel entity = new EntityModel();
        entity.setName("Artemisia absinthium");
        entity.setComment("A plant which was used in the past for building methanol");

        service.create(entity);

        QualityModel quality = new QualityModel();
        quality.setName("size");
        quality.setComment("The size of an object");
        service.create(quality);

        MethodModel method = new MethodModel();
        method.setName("SVM");
        method.setComment("A machine learning based method");
        service.create(method);

        UnitModel unit = new UnitModel();
        unit.setName("minute");
        unit.setComment("I really need to comment it ?");
        unit.setSymbol("m");
        unit.setAlternativeSymbol("mn");
        service.create(unit);

        VariableCreationDTO variableDto = new VariableCreationDTO();
        variableDto.setName(entity.getName()+quality.getName());
        variableDto.setLongName(variableDto.getName()+method.getName()+unit.getName());
        variableDto.setComment("A comment about a variable");

        variableDto.setEntity(entity.getUri());
        variableDto.setQuality(quality.getUri());
        variableDto.setMethod(method.getUri());
        variableDto.setUnit(unit.getUri());

        variableDto.setSynonym("a synonym");
        variableDto.setTraitUri(new URI("http://purl.obolibrary.org/obo/TO_0002644"));
        variableDto.setTraitName("dry matter digestibility");
        variableDto.setTimeInterval("minutes");

        return variableDto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath,getByUriPath, deletePath, getCreationDto());
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        VariableCreationDTO dto = new VariableCreationDTO();
        dto.setName("name");
        dto.setComment("only a comment, not a name");

        Response postResult = getJsonPostResponse(target(createPath),dto);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        dto = getCreationDto();
//        dto.setMethod(null);
        dto.setUnit(null);
//
//        postResult = getJsonPostResponse(target(createPath),dto);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());

        dto = getCreationDto();
        dto.setEntity(null);
        dto.setQuality(null);

        postResult = getJsonPostResponse(target(createPath),dto);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.Variable+"/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        VariableCreationDTO dto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), dto);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setName("new alias");
        dto.setComment("new comment");
        dto.setTraitUri(new URI("http://purl.obolibrary.org/obo/TO_0002644_new"));

        // create a new entity to associate with the variable
        EntityModel entity = new EntityModel();
        entity.setName("Artemisia absinthium");
        entity.setComment("A plant which was used in the past for building methanol");
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
        assertEquals(dto.getComment(), dtoFromApi.getComment());
        assertEquals(dto.getEntity(), dtoFromApi.getEntity().getUri());
        assertEquals(dto.getTraitUri(), dtoFromApi.getTraitUri());

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
        SingleObjectResponse<VariableDetailsDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<VariableDetailsDTO>>() {
        });
        VariableDetailsDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);

        assertEquals(creationDTO.getName(),dtoFromDb.getName());
        assertEquals(creationDTO.getLongName(),dtoFromDb.getLongName());
        assertEquals(creationDTO.getComment(),dtoFromDb.getComment());
        assertEquals(creationDTO.getTraitUri(),dtoFromDb.getTraitUri());
        assertEquals(creationDTO.getTraitName(),dtoFromDb.getTraitName());

        assertEquals(creationDTO.getEntity(),dtoFromDb.getEntity().getUri());
        assertEquals(creationDTO.getQuality(),dtoFromDb.getQuality().getUri());
        assertEquals(creationDTO.getMethod(),dtoFromDb.getMethod().getUri());
        assertEquals(creationDTO.getUnit(),dtoFromDb.getUnit().getUri());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(VariableModel.class);
    }

}
