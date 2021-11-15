//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityCreationDTO;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityAPI;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityDetailsDTO;
import org.opensilex.core.variable.dal.InterestEntityModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author Hamza IKIOU
 */
public class InterestEntityApiTest extends AbstractSecurityIntegrationTest {
    
    public static final String path = InterestEntityAPI.PATH;

    public static final String getByUriPath = path + "/{uri}";
    public static final String createPath = path;
    public static final String updatePath = path;
    public static final String deletePath = path + "/{uri}";
    
    public static InterestEntityCreationDTO getCreationDto() {
        InterestEntityCreationDTO dto = new InterestEntityCreationDTO();
        dto.setName("Plot");
        dto.setDescription("Portion of land with the same cultivation.");
        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath, getByUriPath, deletePath, getCreationDto());
    }
    
    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        InterestEntityCreationDTO dtoWithNoName = new InterestEntityCreationDTO();
        dtoWithNoName.setDescription("only a comment, not a name");

        final Response postResult = getJsonPostResponse(target(createPath), dtoWithNoName);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }
   
    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.EntityOfInterest + "/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }
    
    @Test
    public void testUpdate() throws Exception {

        InterestEntityCreationDTO dto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), dto);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setName("new alias");
        dto.setDescription("new comment");

        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new eoi and compare to the expected eoi
        final Response getResult = getJsonGetByUriResponse(target(getByUriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<InterestEntityDetailsDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<InterestEntityDetailsDTO>>(){});
        InterestEntityDetailsDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getName(), dtoFromApi.getName());
        assertEquals(dto.getDescription(), dtoFromApi.getDescription());
    }
    
    @Test
    public void testGetByUri() throws Exception {

        // Try to insert an eoi, to fetch it and to get fields
        InterestEntityCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<InterestEntityDetailsDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<InterestEntityDetailsDTO>>(){});
        InterestEntityDetailsDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);
        assertEquals(creationDTO.getName(), dtoFromDb.getName());
        assertEquals(creationDTO.getDescription(), dtoFromDb.getDescription());
        
    }
   
    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(InterestEntityModel.class);
    }
}
