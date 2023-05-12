//******************************************************************************
//                          InfrastructureAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;

import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.ResourceDagDTO;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.*;

import static junit.framework.TestCase.*;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class OrganizationAPITest extends AbstractMongoIntegrationTest {

    protected String path = "/core/organisations";

    protected String uriPath = path + "/{uri}";
    protected String searchPath = path ;
    protected String createPath = path ;
    protected String updatePath = path;
    protected String deletePath = path + "/{uri}";

    private static int infraCount = 0;

    protected OrganizationCreationDTO getCreationDTO(URI parent) {
        infraCount++;
        OrganizationCreationDTO dto = new OrganizationCreationDTO();
        dto.setName("Infra " + infraCount);
        if (parent != null) {
            List<URI> parents = new ArrayList<>();
            parents.add(parent);
            dto.setParents(parents);
        }
        return dto;
    }

    protected OrganizationUpdateDTO getUpdateDTO(URI organizationUri, List<URI> updatedParents, String newName) {
        OrganizationUpdateDTO dto = new OrganizationUpdateDTO();
        dto.setUri(organizationUri);
        if (updatedParents != null) {
            dto.setParents(updatedParents);
        } else {
            dto.setParents(Collections.emptyList());
        }
        if (StringUtils.isNotEmpty(newName)) {
            dto.setName(newName);
        }
        return dto;
    }

    protected OrganizationUpdateDTO getUpdateDTO(URI organizationURI, List<URI> updatedParents) {
        return getUpdateDTO(organizationURI, updatedParents, null);
    }

    @Test
    public void testCreate() throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    /**
     * Updating an organization to change its parent
     *
     */
    @Test
    public void testUpdateParents() throws Exception {
        Response creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root1 = extractUriFromResponse(creationResponse);

        creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root2 = extractUriFromResponse(creationResponse);

        Response updateResponse = getJsonPutResponse(target(updatePath), getUpdateDTO(root1, new ArrayList<URI>() {{ add(root2); }}));

        assertEquals(Status.OK.getStatusCode(), updateResponse.getStatus());
    }

    /**
     * Updating an organization to create a cycle should fail
     *
     */
    @Test
    public void testUpdateParentsCycle() throws Exception {
        Response creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root1 = extractUriFromResponse(creationResponse);

        creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(root1));
        URI root2 = extractUriFromResponse(creationResponse);

        Response updateResponse = getJsonPutResponse(target(updatePath), getUpdateDTO(root1, new ArrayList<URI>() {{ add(root2); }}));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    }

    /**
     * Updating an organization to have itself as a parent should fail
     *
     */
    @Test
    public void testUpdateParentsSelf() throws Exception {
        Response creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root1 = extractUriFromResponse(creationResponse);

        Response updateResponse = getJsonPutResponse(target(updatePath), getUpdateDTO(root1, new ArrayList<URI>() {{ add(root1); }}));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void testSearch() throws Exception {
        Response creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root1 = extractUriFromResponse(creationResponse);

        creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root2 = extractUriFromResponse(creationResponse);

        creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(root1));
        URI root1Child1 = extractUriFromResponse(creationResponse);

        final Response getResult = appendAdminToken(target(searchPath)).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);

        PaginatedListResponse<ResourceDagDTO<OrganizationModel>> response = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {
        });

        List<ResourceDagDTO<OrganizationModel>> list = response.getResult();
        assertFalse(list.isEmpty());
        Optional<ResourceDagDTO<OrganizationModel>> searchedRoot1 = list.stream().filter(orgDto -> SPARQLDeserializers.compareURIs(orgDto.getUri(), root1)).findFirst();
        Optional<ResourceDagDTO<OrganizationModel>> searchedRoot2 = list.stream().filter(orgDto -> SPARQLDeserializers.compareURIs(orgDto.getUri(), root2)).findFirst();
        assertTrue(searchedRoot1.isPresent());
        assertTrue(searchedRoot2.isPresent());

        List<URI> root1Children = searchedRoot1.get().getChildren();
        assertEquals(1, root1Children.size());
        assertEquals(0, searchedRoot2.get().getChildren().size());
        assertTrue(SPARQLDeserializers.compareURIs(root1Child1, root1Children.get(0)));
    }

    /**
     * Checks that the cache is correctly updated after a creation
     */
    @Test
    public void testSearchAfterCreate() throws Exception {
        Response creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root1 = extractUriFromResponse(creationResponse);

        Response getResult = getJsonGetResponseAsAdmin(target(searchPath));
        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<ResourceDagDTO<OrganizationModel>> response = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {
        });

        assertEquals(1, response.getResult().size());

        creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root2 = extractUriFromResponse(creationResponse);


        getResult = getJsonGetResponseAsAdmin(target(searchPath));
        node = getResult.readEntity(JsonNode.class);
        response = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {
        });

        assertEquals(2, response.getResult().size());
    }

    /**
     * Checks that the cache is correctly updated after an update
     */
    @Test
    public void testSearchAfterUpdate() throws Exception {
        Response creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root1 = extractUriFromResponse(creationResponse);

        Response getResult = getJsonGetResponseAsAdmin(target(searchPath));
        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<ResourceDagDTO<OrganizationModel>> response = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {
        });

        assertEquals(1, response.getResult().size());

        Response updateResponse = getJsonPutResponse(target(updatePath), getUpdateDTO(root1, null, "newName"));
        URI root2 = extractUriFromResponse(updateResponse);


        getResult = getJsonGetResponseAsAdmin(target(searchPath));
        node = getResult.readEntity(JsonNode.class);
        response = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {
        });

        assertEquals(1, response.getResult().size());
        assertEquals("newName", response.getResult().get(0).getName());
    }

    /**
     * Checks that the cache is correctly updated after a delete
     */
    @Test
    public void testSearchAfterDelete() throws Exception {
        Response creationResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(null));
        URI root1 = extractUriFromResponse(creationResponse);

        Response getResult = getJsonGetResponseAsAdmin(target(searchPath));
        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<ResourceDagDTO<OrganizationModel>> response = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {
        });

        assertEquals(1, response.getResult().size());

        Response deleteResponse = getDeleteByUriResponse(target(deletePath), root1.toString());

        getResult = getJsonGetResponseAsAdmin(target(searchPath));
        node = getResult.readEntity(JsonNode.class);
        response = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {
        });

        assertEquals(0, response.getResult().size());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(OrganizationModel.class);
    }
}
