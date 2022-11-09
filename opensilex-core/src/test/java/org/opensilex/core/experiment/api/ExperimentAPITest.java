//******************************************************************************
//                          ExperimentAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.api.facility.FacilityGetDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.ObjectUriResponse;
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
import static org.junit.Assert.*;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class ExperimentAPITest extends AbstractSecurityIntegrationTest {

    protected static String path = "/core/experiments";

    public static String uriPath = path + "/{uri}";
    public static String searchPath = path ;
    public static String createPath = path;
    public static String updatePath = path ;
    public static String deletePath = path + "/{uri}";
    public static String getAvailableFacilitiesPath = uriPath + "/available_facilities";

    public static ExperimentCreationDTO getCreationDTO() {

        ExperimentCreationDTO xpDto = new ExperimentCreationDTO();
        xpDto.setName("xp");

        LocalDate currentDate = LocalDate.now();
        xpDto.setStartDate(currentDate.minusDays(3));
        xpDto.setEndDate(currentDate.plusDays(3));
        xpDto.setObjective("Objective");
        return xpDto;
    }

    private FacilityModel createFacility(URI uri) throws Exception {
        FacilityModel facilityModel = new FacilityModel();
        facilityModel.setUri(uri);
        getSparqlService().create(facilityModel);
        return facilityModel;
    }

    private OrganizationModel createOrganization(URI organizationUri, URI facilityUri) throws Exception {
        OrganizationModel organizationModel = new OrganizationModel();
        organizationModel.setUri(organizationUri);
        if (Objects.nonNull(facilityUri)) {
            organizationModel.setFacilities(new ArrayList<FacilityModel>() {{
                add(createFacility(facilityUri));
            }});
        }
        getSparqlService().create(organizationModel);
        return organizationModel;
    }

    private void deleteFacility(URI uri) throws Exception {
        getSparqlService().delete(FacilityModel.class, uri);
    }

    private void deleteOrganization(URI uri) throws Exception {
        getSparqlService().delete(OrganizationModel.class, uri);
    }

    public static ExperimentCreationDTO getCreationDTOWithOrganization(URI organizationURI) {

        ExperimentCreationDTO xpDto = new ExperimentCreationDTO();
        xpDto.setName("xpWithOrganization");

        LocalDate currentDate = LocalDate.now();
        xpDto.setStartDate(currentDate.minusDays(3));
        xpDto.setEndDate(currentDate.plusDays(3));
        xpDto.setObjective("Objective");

        xpDto.setInfrastructures(new ArrayList<URI>() {{
            add(organizationURI);
        }});

        return xpDto;
    }

    @Test
    public void testCreate() throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), createdUri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreateAll() throws Exception {

        List<ExperimentCreationDTO> creationDTOS = Arrays.asList(getCreationDTO(), getCreationDTO());

        for (ExperimentCreationDTO creationDTO : creationDTOS) {
            final Response postResult = getJsonPostResponseAsAdmin(target(createPath), creationDTO);
            assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

            URI uri = extractUriFromResponse(postResult);
            final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
            assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        }

    }

    @Test
    public void testUpdate() throws Exception {

        // create the xp
        ExperimentCreationDTO xpDto = getCreationDTO();
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), xpDto);

        // update the xp
        xpDto.setUri(extractUriFromResponse(postResult));
        xpDto.setName("new name");
        xpDto.setEndDate(LocalDate.now());

        final Response updateResult = getJsonPutResponse(target(updatePath), xpDto);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), xpDto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {
        });
        ExperimentGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(xpDto.getName(), dtoFromApi.getName());
        assertEquals(xpDto.getEndDate(), dtoFromApi.getEndDate());
    }

    @Test
    public void testDelete() throws Exception {

        // create object and check if URI exists
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO());
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and check if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri);
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {
        });
        ExperimentGetDTO xpGetDto = getResponse.getResult();
        assertNotNull(xpGetDto);
    }

    @Test
    public void testGetByUriFail() throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO());
        JsonNode node = postResult.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        String uri = postResponse.getResult();

        // call the service with a non existing pseudo random URI
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri + "7FG4FG89FG4GH4GH57");
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    

    @Test
    public void testSearch() throws Exception {

        ExperimentCreationDTO creationDTO = getCreationDTO();
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("start_date", creationDTO.getStartDate());
                put("name", creationDTO.getName());
                put("uri", uri);
            }
        };

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 50, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<ExperimentGetDTO> xpListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ExperimentGetDTO>>() {
        });
        List<ExperimentGetDTO> xps = xpListResponse.getResult();

        assertFalse(xps.isEmpty());
    }

    /**
     * Tests that `getAllFacilities` is correctly restricted when the experiment is associated with an organization
     *
     * @throws Exception
     */
    @Test
    public void testGetAvailableFacilitiesWithOrganization() throws Exception {
        URI facilityWithoutOrganizationUri = new URI("test:facilityWithoutOrganization");
        URI facilityWithOrganizationUri = new URI("test:facilityWithOrganization");
        URI organizationUri = new URI("test:organization");

        createFacility(facilityWithoutOrganizationUri);
        createOrganization(organizationUri, facilityWithOrganizationUri);

        ExperimentCreationDTO creationDTO = getCreationDTOWithOrganization(organizationUri);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetResponseAsAdmin(target(getAvailableFacilitiesPath).resolveTemplate("uri", uri));
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<FacilityGetDTO> facilitiesListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {
        });
        List<FacilityGetDTO> facilitiesList = facilitiesListResponse.getResult();

        assertEquals(1, facilitiesList.size());
        assertEquals(facilityWithOrganizationUri, facilitiesList.get(0).getUri());

        deleteFacility(facilityWithOrganizationUri);
        deleteFacility(facilityWithoutOrganizationUri);
        deleteOrganization(organizationUri);
    }

    /**
     * Tests that `getAllFacilities` returns all facilities when the experiment is not associated with an organization
     *
     * @throws Exception
     */
    @Test
    public void testGetAvailableFacilitiesWithoutOrganization() throws Exception {
        URI facilityWithoutOrganizationUri = new URI("test:facilityWithoutOrganization");
        URI facilityWithOrganizationUri = new URI("test:facilityWithOrganization");
        URI organizationUri = new URI("test:organization");

        createFacility(facilityWithoutOrganizationUri);
        createOrganization(organizationUri, facilityWithOrganizationUri);

        ExperimentCreationDTO creationDTO = getCreationDTO();
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetResponseAsAdmin(target(getAvailableFacilitiesPath).resolveTemplate("uri", uri));
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<FacilityGetDTO> facilitiesListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {
        });
        List<FacilityGetDTO> facilitiesList = facilitiesListResponse.getResult();

        assertEquals(2, facilitiesList.size());
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityWithOrganizationUri)));
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityWithoutOrganizationUri)));

        deleteFacility(facilityWithOrganizationUri);
        deleteFacility(facilityWithoutOrganizationUri);
        deleteOrganization(organizationUri);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(ExperimentModel.class);
    }
}
