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
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.api.facility.FacilityGetDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
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
public class ExperimentAPITest extends AbstractMongoIntegrationTest {

    protected static String path = "/core/experiments";

    public static String uriPath = path + "/{uri}";
    public static String searchPath = path ;
    public static String createPath = path;
    public static String updatePath = path ;
    public static String deletePath = path + "/{uri}";
    public static String getAvailableFacilitiesPath = uriPath + "/available_facilities";
    private static final TypeReference<PaginatedListResponse<FacilityGetDTO>> GET_AVAILABLE_FACILITIES_RETURN_TYPE =
            new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {};

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
        facilityModel.setName("Facility : " + uri);
        facilityModel.setUri(uri);
        getSparqlService().create(facilityModel);
        return facilityModel;
    }

    private OrganizationModel createOrganizationWithFacilities(URI organizationUri, FacilityModel... facilities) throws Exception {
        OrganizationModel organizationModel = new OrganizationModel();
        organizationModel.setName("Organization : " + organizationUri);
        organizationModel.setUri(organizationUri);
        if (facilities.length > 0) {
            organizationModel.setFacilities(Arrays.asList(facilities));
        }
        getSparqlService().create(organizationModel);
        return organizationModel;
    }

    private ExperimentModel createExperimentSPARQL(URI experimentUri, OrganizationModel organization, FacilityModel... facilities) throws Exception {
        ExperimentModel experimentModel = new ExperimentModel();
        experimentModel.setName("Experiment : " + experimentUri);
        experimentModel.setUri(experimentUri);
        experimentModel.setStartDate(LocalDate.parse("2023-07-07"));
        experimentModel.setObjective("Objective : " + experimentModel);

        if (organization != null) {
            experimentModel.setInfrastructures(Collections.singletonList(organization));
        }

        if (facilities.length > 0) {
            experimentModel.setFacilities(Arrays.asList(facilities));
        }

        getSparqlService().create(experimentModel);

        return experimentModel;
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

    //region GET available_facilities

    /**
     * Organization in experiment
     */
    public static final URI orgInXpUri = URI.create("test:orgInXp");
    /**
     * Organization out of experiment
     */
    public static final URI orgOutOfXpUri = URI.create("test:orgOutOfXp");
    /**
     * Facility in experiment and in organization which is also in experiment
     */
    private static final URI facilityInXpInOrgInXpUri = URI.create("test:facilityInXpInOrgInXp");
    /**
     * Facility out of experiment but in organization which is in experiment
     */
    private static final URI facilityOutOfXpInOrgInXpUri = URI.create("test:facilityOutOfXpInOrgInXp");
    /**
     * Facility in experiment and in organization, but which is not in experiment
     */
    private static final URI facilityInXpInOrgOutOfXpUri = URI.create("test:facilityInXpInOrgOutOfXp");
    /**
     * Facility out of experiment and in organization which is also out of experiment
     */
    private static final URI facilityOutOfXpInOrgOutOfXpUri = URI.create("test:facilityOutOfXpInOrgOutOfXp");
    /**
     * Facility in experiment but out of any organization
     */
    private static final URI facilityInXpOutOfOrgUri = URI.create("test:facilityInXpOutOfOrg");
    /**
     * Facility out of experiment and also out of any organization
     */
    private static final URI facilityOutOfXpOutOfOrgUri = URI.create("test:facilityOutOfXpOutOfOrg");
    private static final URI xpWithoutFacilityWithoutOrgUri = URI.create("test:experimentWithoutFacilityWithoutOrg");
    private static final URI xpWithFacilitiesWithoutOrgUri = URI.create("test:experimentWithFacilitiesWithoutOrg");
    private static final URI xpWithoutFacilityWithOrgUri = URI.create("test:experimentWithoutFacilityWithOrgs");
    private static final URI xpWithFacilitiesWithOrgUri = URI.create("test:experimentWithFacilitiesWithOrgs");

    private void createTestDataForAvailableFacilities() throws Exception {
        FacilityModel facilityInXpInOrgInXp = createFacility(facilityInXpInOrgInXpUri);
        FacilityModel facilityOutOfXpInOrgInXp = createFacility(facilityOutOfXpInOrgInXpUri);
        FacilityModel facilityInXpInOrgOutOfXp = createFacility(facilityInXpInOrgOutOfXpUri);
        FacilityModel facilityOutOfXpInOrgOutOfXp = createFacility(facilityOutOfXpInOrgOutOfXpUri);
        FacilityModel facilityInXpOutOfOrg = createFacility(facilityInXpOutOfOrgUri);
        createFacility(facilityOutOfXpOutOfOrgUri);

        OrganizationModel orgInXp = createOrganizationWithFacilities(orgInXpUri, facilityInXpInOrgInXp, facilityOutOfXpInOrgInXp);
        createOrganizationWithFacilities(orgOutOfXpUri, facilityInXpInOrgOutOfXp, facilityOutOfXpInOrgOutOfXp);

        createExperimentSPARQL(xpWithoutFacilityWithoutOrgUri, null);
        createExperimentSPARQL(xpWithFacilitiesWithoutOrgUri, null, facilityInXpInOrgInXp, facilityInXpInOrgOutOfXp, facilityInXpOutOfOrg);
        createExperimentSPARQL(xpWithoutFacilityWithOrgUri, orgInXp);
        createExperimentSPARQL(xpWithFacilitiesWithOrgUri, orgInXp, facilityInXpInOrgInXp, facilityInXpInOrgOutOfXp, facilityInXpOutOfOrg);
    }

    @Test
    public void testGetAvailableFacilitiesWithoutFacilityWithoutOrg() throws Exception {
        createTestDataForAvailableFacilities();

        final Response getResult = getJsonGetResponseAsAdmin(target(getAvailableFacilitiesPath).resolveTemplate("uri", xpWithoutFacilityWithoutOrgUri));
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        List<FacilityGetDTO> facilitiesList = mapper.convertValue(node, GET_AVAILABLE_FACILITIES_RETURN_TYPE).getResult();

        assertEquals(0, facilitiesList.size());
    }

    @Test
    public void testGetAvailableFacilitiesWithFacilitiesWithoutOrg() throws Exception {
        createTestDataForAvailableFacilities();

        final Response getResult = getJsonGetResponseAsAdmin(target(getAvailableFacilitiesPath).resolveTemplate("uri", xpWithFacilitiesWithoutOrgUri));
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        List<FacilityGetDTO> facilitiesList = mapper.convertValue(node, GET_AVAILABLE_FACILITIES_RETURN_TYPE).getResult();

        assertEquals(3, facilitiesList.size());
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityInXpInOrgInXpUri)));
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityInXpInOrgOutOfXpUri)));
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityInXpOutOfOrgUri)));
    }

    @Test
    public void testGetAvailableFacilitiesWithoutFacilityWithOrg() throws Exception {
        createTestDataForAvailableFacilities();

        final Response getResult = getJsonGetResponseAsAdmin(target(getAvailableFacilitiesPath).resolveTemplate("uri", xpWithoutFacilityWithOrgUri));
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        List<FacilityGetDTO> facilitiesList = mapper.convertValue(node, GET_AVAILABLE_FACILITIES_RETURN_TYPE).getResult();

        assertEquals(2, facilitiesList.size());
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityInXpInOrgInXpUri)));
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityOutOfXpInOrgInXpUri)));
    }

    @Test
    public void testGetAvailableFacilitiesWithFacilitiesWithOrg() throws Exception {
        createTestDataForAvailableFacilities();

        final Response getResult = getJsonGetResponseAsAdmin(target(getAvailableFacilitiesPath).resolveTemplate("uri", xpWithFacilitiesWithOrgUri));
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        List<FacilityGetDTO> facilitiesList = mapper.convertValue(node, GET_AVAILABLE_FACILITIES_RETURN_TYPE).getResult();

        assertEquals(4, facilitiesList.size());
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityInXpInOrgInXpUri)));
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityInXpInOrgOutOfXpUri)));
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityInXpOutOfOrgUri)));
        assertTrue(facilitiesList.stream().anyMatch(facility -> Objects.equals(facility.getUri(), facilityOutOfXpInOrgInXpUri)));
    }

    //endregion

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(ExperimentModel.class, OrganizationModel.class, FacilityModel.class);
    }
}
