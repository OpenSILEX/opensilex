//******************************************************************************
//                          UnitApiTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.unit.UnitCreationDTO;
import org.opensilex.core.variable.api.unit.UnitGetDTO;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Renaud COLIN
 */
public class UnitApiTest extends AbstractSecurityIntegrationTest {

    public String path = "/core/variable/unit";

    public String getByUriPath = path + "/get/{uri}";
    public String searchPath = path + "/search";
    public String createPath = path + "/create";
    public String updatePath = path + "/update";
    public String deletePath = path + "/delete/{uri}";
    
    
    private UnitCreationDTO getCreationDto() {
        UnitCreationDTO dto = new UnitCreationDTO();
        dto.setLabel("minute");
        dto.setComment("I really need to comment it ?");
        dto.setSymbol("m");
        dto.setAlternativeSymbol("mn");
        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath,getByUriPath, deletePath, getCreationDto());
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        UnitCreationDTO dtoWithNoName = new UnitCreationDTO();
        dtoWithNoName.setComment("only a comment, not a name");

        final Response postResult = getJsonPostResponse(target(createPath),dtoWithNoName);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.Unit+"/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        UnitCreationDTO dto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), dto);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setLabel("new alias");
        dto.setComment("new comment");

        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponse(target(getByUriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<UnitCreationDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<UnitCreationDTO>>() {
        });
        UnitCreationDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getLabel(), dtoFromApi.getLabel());
        assertEquals(dto.getComment(), dtoFromApi.getComment());
    }

    @Test
    public void testGetByUri() throws Exception {

        // Try to insert an Entity, to fetch it and to get fields
        UnitCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<UnitGetDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<UnitGetDTO>>() {
        });
        UnitGetDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);
        assertEquals(creationDTO.getLabel(),dtoFromDb.getLabel());
        assertEquals(creationDTO.getComment(),dtoFromDb.getComment());
        assertEquals(creationDTO.getSymbol(),dtoFromDb.getSymbol());
        assertEquals(creationDTO.getAlternativeSymbol(),dtoFromDb.getAlternativeSymbol());

        assertEquals(Oeso.Unit.getURI(),dtoFromDb.getType().toString());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(UnitModel.class);
    }

}
