//******************************************************************************
//                          ExperimentAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.project.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Julien BONNEFONT
 */
public class ProjectAPITest extends AbstractSecurityIntegrationTest {

    protected static String path = "/core/projects";

    protected String uriPath = path + "/{uri}";
    protected String searchPath = path;
    public static final ServiceDescription create;

    static {
        try {
            create = new ServiceDescription(
                    ProjectAPI.class.getMethod("createProject", ProjectCreationDTO.class),
                    path
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected String updatePath = path;
    protected String deletePath = path + "/{uri}";

    protected ProjectCreationDTO getCreationDTO() {

        ProjectCreationDTO pjctDto = new ProjectCreationDTO();
        pjctDto.setName("prjjj");

        LocalDate currentDate = LocalDate.now();
        pjctDto.setStartDate(currentDate.minusDays(3));
        pjctDto.setEndDate(currentDate.plusDays(3));
        return pjctDto;
    }

    @Test
    public void testCreate() throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), createdUri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreateAll() throws Exception {

        List<ProjectCreationDTO> creationDTOS = Arrays.asList(getCreationDTO(), getCreationDTO());

        for (ProjectCreationDTO creationDTO : creationDTOS) {
            final Response postResult = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), creationDTO);
            assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

            URI uri = extractUriFromResponse(postResult);
            final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
            assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        }

    }

    @Test
    public void testUpdate() throws Exception {

        // create the pj
        ProjectCreationDTO pjctDto = getCreationDTO();
        final Response postResult = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), pjctDto);

        // update the pj
        pjctDto.setUri(extractUriFromResponse(postResult));
        pjctDto.setName("new");
        pjctDto.setEndDate(LocalDate.now());

        final Response updateResult = getJsonPutResponse(target(updatePath), pjctDto);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new pj and compare to the expected pj
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), pjctDto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ProjectCreationDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ProjectCreationDTO>>() {
        });
        ProjectCreationDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(pjctDto.getShortname(), dtoFromApi.getShortname());
        assertEquals(pjctDto.getEndDate(), dtoFromApi.getEndDate());
    }

    @Test
    public void testGetByUri() throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), getCreationDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ProjectCreationDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ProjectCreationDTO>>() {
        });
        ProjectCreationDTO pjGetDto = getResponse.getResult();
        assertNotNull(pjGetDto);
    }

    @Test
    public void testSearch() throws Exception {

        ProjectCreationDTO creationDTO = getCreationDTO();
        final Response postResult = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("startDate", creationDTO.getStartDate());
                put("name", creationDTO.getName());
                put("uri", uri);
            }
        };

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 50, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<ProjectCreationDTO> xpListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ProjectCreationDTO>>() {
        });
        List<ProjectCreationDTO> xps = xpListResponse.getResult();

        assertFalse(xps.isEmpty());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(ProjectModel.class);
    }
}
