//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variablesGroup.api.VariablesGroupAPI;
import org.opensilex.core.variablesGroup.api.VariablesGroupCreationDTO;
import org.opensilex.core.variablesGroup.api.VariablesGroupGetDTO;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;

import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

/**
 * @author Hamza IKIOU
 */
public class VariablesGroupApiTest extends AbstractSecurityIntegrationTest {
    
    public String path = VariablesGroupAPI.PATH;
    
    public String getByUriPath = path + "/{uri}";
    public String createPath = path;
    public String updatePath = path;
    public String deletePath = path + "/{uri}";   
    public String createVariablePath = VariableAPI.PATH;
    public String getVariableByUriPath = VariableAPI.PATH + "/{uri}";
    
    private VariableCreationDTO getVariableCreationDto() throws Exception {

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
        
    private VariablesGroupCreationDTO getCreationDto() throws Exception {
        
        VariablesGroupCreationDTO dto = new VariablesGroupCreationDTO();
        dto.setName("Air");
        dto.setDescription("All air related variables");
        
        return dto;
    }
    
    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath, getByUriPath, deletePath, getCreationDto());
    }
    
    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {
        VariablesGroupCreationDTO dtoWithNoName = new VariablesGroupCreationDTO();
        dtoWithNoName.setDescription("only a comment, not a name");

        final Response postResult = getJsonPostResponse(target(createPath), dtoWithNoName);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }
    
    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.VariablesGroup + "/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }
    
    @Test
    public void testUpdate() throws Exception {
        
        // create the vg
        VariablesGroupCreationDTO dto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), dto);             
                
        // update the vg
        dto.setUri(extractUriFromResponse(postResult));
        dto.setName("new alias");
        dto.setDescription("new comment");

        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());
        
        // retrieve the new vg and compare to the expected vg
        final Response getResult = getJsonGetByUriResponse(target(getByUriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<VariablesGroupCreationDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<VariablesGroupCreationDTO>>() {});
        VariablesGroupCreationDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getName(), dtoFromApi.getName());
        assertEquals(dto.getDescription(), dtoFromApi.getDescription());
        assertEquals(dto.getVariablesList(), dtoFromApi.getVariablesList()); 
    }
    
    @Test
    public void testGetByUri() throws Exception {

        // Try to insert a group of variables, to fetch it and to get fields
        VariablesGroupCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<VariablesGroupCreationDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<VariablesGroupCreationDTO>>() {});
        VariablesGroupCreationDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);
        assertEquals(creationDTO.getName(),dtoFromDb.getName());
        assertEquals(creationDTO.getDescription(),dtoFromDb.getDescription());
        assertEquals(creationDTO.getVariablesList(),dtoFromDb.getVariablesList());
    }    
    
    @Test
    public void testCreateWithVariable() throws Exception{
        
        //Create dto for vg and variable
        VariablesGroupCreationDTO dto = getCreationDto();
        VariableCreationDTO variableDto = getVariableCreationDto();
        
        //Call the api to create a variable and get its uri
        Response postResult = getJsonPostResponse(target(createVariablePath), variableDto);
        URI uriVariable = extractUriFromResponse(postResult);
        
        //Create a variablesList with the variable before and add it to the vg
        List<URI> variablesList = new ArrayList<>();
        variablesList.add(uriVariable);
        dto.setVariablesList(variablesList);
        
        //Call the api to create a vg and get its uri
        Response postResult2 = getJsonPostResponse(target(createPath), dto);      
        URI uriGroup = extractUriFromResponse(postResult2);
        
        //Try to deserialize object and check if the fields value are the same
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uriGroup.toString());
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<VariablesGroupGetDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<VariablesGroupGetDTO>>() {});
        VariablesGroupGetDTO dtoFromDb = getResponse.getResult();
        
        assertEquals(dto.getName(), dtoFromDb.getName());
        assertEquals(dto.getDescription(), dtoFromDb.getDescription());
        Assert.assertEquals(1, dto.getVariablesList().size());
        
        //Try to deserialize object and check if the variables uris are the same
        Response getResult2 = getJsonGetByUriResponse(target(getVariableByUriPath), uriVariable.toString());
        JsonNode node2 = getResult2.readEntity(JsonNode.class);
        SingleObjectResponse<VariableGetDTO> getResponse2 =  mapper.convertValue(node2, new TypeReference<SingleObjectResponse<VariableGetDTO>>() {});
        VariableGetDTO dtoFromDb2 = getResponse2.getResult();
        
        assertEquals(dtoFromDb.getVariablesList().get(0).getUri(), dtoFromDb2.getUri());      
        
    }
    
    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(VariablesGroupModel.class);
    }
}
