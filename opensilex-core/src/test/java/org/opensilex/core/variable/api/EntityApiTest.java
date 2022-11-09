//******************************************************************************
//                          EntityAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.entity.EntityAPI;
import org.opensilex.core.variable.api.entity.EntityCreationDTO;
import org.opensilex.core.variable.api.entity.EntityDetailsDTO;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import static junit.framework.TestCase.assertTrue;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Renaud COLIN
 */
public class EntityApiTest extends AbstractSecurityIntegrationTest {

    public static final String path = EntityAPI.PATH;

    public static final String getByUriPath = path + "/{uri}";
    public static final String createPath = path;
    public static final String updatePath = path;
    public static final String deletePath = path + "/{uri}";
    //public static final String searchEntitiesOfInterestPath = path + "/"+EntityAPI.SEARCH_ENTITIES_OF_INTEREST_PATH;


    public static EntityCreationDTO getCreationDto() {
        EntityCreationDTO dto = new EntityCreationDTO();
        dto.setName("Artemisia absinthium");
        dto.setDescription("A plant which was used in the past for building methanol");
        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath,getByUriPath, deletePath, getCreationDto());
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        EntityCreationDTO dtoWithNoName = new EntityCreationDTO();
        dtoWithNoName.setDescription("only a comment, not a name");

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),dtoWithNoName);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), Oeso.Entity+"/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        EntityCreationDTO dto = getCreationDto();
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), dto);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setName("new alias");
        dto.setDescription("new comment");

        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EntityDetailsDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<EntityDetailsDTO>>() {
        });
        EntityDetailsDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getName(), dtoFromApi.getName());
        assertEquals(dto.getDescription(), dtoFromApi.getDescription());
    }

    @Test
    public void testGetByUri() throws Exception {

        // Try to insert an Entity, to fetch it and to get fields
        EntityCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<EntityDetailsDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<EntityDetailsDTO>>() {
        });
        EntityDetailsDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);
        assertEquals(creationDTO.getName(),dtoFromDb.getName());
        assertEquals(creationDTO.getDescription(),dtoFromDb.getDescription());
        
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(EntityModel.class);
    }

}
