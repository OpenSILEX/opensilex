//******************************************************************************
//                          InfrastructureAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import org.junit.Test;
import org.opensilex.core.organisation.dal.InfrastructureModel;

import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.response.ResourceTreeResponse;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class InfrastructureAPITest extends AbstractSecurityIntegrationTest {

    protected String path = "/core/organisations";

    protected String uriPath = path + "/{uri}";
    protected String searchPath = path ;
    protected String createPath = path ;
    protected String updatePath = path;
    protected String deletePath = path + "/{uri}";

    private static int infraCount = 0;

    protected InfrastructureCreationDTO getCreationDTO(URI parent) {
        infraCount++;
        InfrastructureCreationDTO dto = new InfrastructureCreationDTO();
        dto.setName("Infra " + infraCount);
        dto.setParent(parent);
        return dto;
    }

//    @Test
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

        ResourceTreeResponse response = mapper.convertValue(node, new TypeReference<ResourceTreeResponse>() {
        });

        List<ResourceTreeDTO> list = response.getResult();
        assertFalse(list.isEmpty());
        Optional<ResourceTreeDTO> searchedRoot1 = list.stream().filter(treeDTO -> SPARQLDeserializers.compareURIs(treeDTO.getUri(), root1)).findFirst();
        Optional<ResourceTreeDTO> searchedRoot2 = list.stream().filter(treeDTO -> SPARQLDeserializers.compareURIs(treeDTO.getUri(), root2)).findFirst();
        assertTrue(searchedRoot1.isPresent());
        assertTrue(searchedRoot2.isPresent());

        List<ResourceTreeDTO> root1Children = searchedRoot1.get().getChildren();
        assertEquals(1, root1Children.size());
        assertEquals(0, searchedRoot2.get().getChildren().size());
        assertTrue(SPARQLDeserializers.compareURIs(root1Child1, root1Children.get(0).getUri()));
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(InfrastructureModel.class);
    }
}
