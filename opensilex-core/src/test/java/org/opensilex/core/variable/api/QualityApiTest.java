//******************************************************************************
//                          QualityApiTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.quality.QualityCreationDTO;
import org.opensilex.core.variable.api.quality.QualityGetDTO;
import org.opensilex.core.variable.dal.QualityModel;
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
public class QualityApiTest extends AbstractSecurityIntegrationTest {

    public String path = "/core/variable/quality";

    public String getByUriPath = path + "/get/{uri}";
    public String searchPath = path + "/search";
    public String createPath = path + "/create";
    public String updatePath = path + "/update";
    public String deletePath = path + "/delete/{uri}";


    private QualityCreationDTO getCreationDto() {
        QualityCreationDTO dto = new QualityCreationDTO();
        dto.setLabel("size");
        dto.setComment("The size of an object");
        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath,getByUriPath, deletePath, getCreationDto());
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        QualityCreationDTO dtoWithNoName = new QualityCreationDTO();
        dtoWithNoName.setComment("only a comment, not a name");

        final Response postResult = getJsonPostResponse(target(createPath),dtoWithNoName);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.Quality+"/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        QualityCreationDTO dto = getCreationDto();
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
        SingleObjectResponse<QualityGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<QualityGetDTO>>() {
        });
        QualityGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getLabel(), dtoFromApi.getLabel());
        assertEquals(dto.getComment(), dtoFromApi.getComment());
    }

    @Test
    public void testGetByUri() throws Exception {

        // Try to insert an Entity, to fetch it and to get fields
        QualityCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<QualityGetDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<QualityGetDTO>>() {
        });
        QualityGetDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);
        assertEquals(creationDTO.getLabel(),dtoFromDb.getLabel());
        assertEquals(creationDTO.getComment(),dtoFromDb.getComment());
        assertEquals(Oeso.Quality.getURI(),dtoFromDb.getType().toString());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(QualityModel.class);
    }

}
