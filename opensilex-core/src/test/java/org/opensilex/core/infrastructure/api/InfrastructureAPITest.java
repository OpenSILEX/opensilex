//******************************************************************************
//                          ExperimentAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.infrastructure.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.opensilex.integration.test.AbstractIntegrationTest;
import java.util.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import org.junit.Test;
import org.opensilex.rest.sparql.dto.ResourceTreeDTO;
import org.opensilex.rest.sparql.response.ResourceTreeResponse;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class InfrastructureAPITest extends AbstractIntegrationTest {

    protected String path = "/core/infrastructure";

    protected String uriPath = path + "/get/{uri}";
    protected String searchPath = path + "/search";
    protected String createPath = path + "/create";
    protected String updatePath = path + "/update";
    protected String deletePath = path + "/delete/{uri}";

    private static int infraCount = 0;

    protected InfrastructureCreationDTO getCreationDTO(URI parent) {
        infraCount++;
        InfrastructureCreationDTO dto = new InfrastructureCreationDTO();
        dto.setName("Infra " + infraCount);
        dto.setParent(parent);
        return dto;
    }

    @Test
    public void testCreate() throws Exception {

        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO(null));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponse(target(uriPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearch() throws Exception {
        Response creationResponse = getJsonPostResponse(target(createPath), getCreationDTO(null));
        URI root1 = extractUriFromResponse(creationResponse);

        creationResponse = getJsonPostResponse(target(createPath), getCreationDTO(null));
        URI root2 = extractUriFromResponse(creationResponse);

        creationResponse = getJsonPostResponse(target(createPath), getCreationDTO(root1));
        URI root1Child1 = extractUriFromResponse(creationResponse);

        final Response getResult = appendToken(target(searchPath)).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);

        ObjectMapper mapper = new ObjectMapper();
        ResourceTreeResponse response = mapper.convertValue(node, new TypeReference<ResourceTreeResponse>() {
        });

        List<ResourceTreeDTO> list = response.getResult();
        assertFalse(list.isEmpty());
        Optional<ResourceTreeDTO> searchedRoot1 = list.stream().filter(treeDTO -> treeDTO.getUri().equals(root1)).findFirst();
        Optional<ResourceTreeDTO> searchedRoot2 = list.stream().filter(treeDTO -> treeDTO.getUri().equals(root2)).findFirst();
        assertTrue(searchedRoot1.isPresent());
        assertTrue(searchedRoot2.isPresent());

        List<ResourceTreeDTO> root1Children = searchedRoot1.get().getChildren();
        assertEquals(1, root1Children.size());
        assertEquals(0, searchedRoot2.get().getChildren().size());
        assertEquals(root1Child1, root1Children.get(0).getUri());
    }

    @Override
    protected List<String> getGraphsToCleanNames() {
        return Collections.singletonList("infrastructures");
    }
}
