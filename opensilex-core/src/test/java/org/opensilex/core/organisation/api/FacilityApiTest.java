package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationDAO;
import org.opensilex.core.organisation.api.facility.*;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;

import static junit.framework.TestCase.*;

public class FacilityApiTest extends AbstractMongoIntegrationTest {

    protected final static String PATH = "/core/facilities";
    protected final static String URI_PATH = PATH + "/{uri}";
    protected final static String URIS_PATH = PATH + "/by_uris";
    protected final static String SEARCH_PATH = PATH;
    public static final ServiceDescription create;

    static {
        try {
            create = new ServiceDescription(
                    FacilityAPI.class.getMethod("createFacility", FacilityCreationDTO.class),
                    PATH
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected final ServiceDescription getFacilities = new ServiceDescription(
            FacilityAPI.class.getMethod("getFacilitiesWithGeometry", String.class),
            PATH+"/with_location"
    );

    protected final static String UPDATE_PATH = PATH;
    protected final static String DELETE_PATH = PATH + "/{uri}";

    protected OrganizationModel orga;

    // TypeReference used to parse Response into a List of FacilityGetDTO
    protected static final TypeReference<PaginatedListResponse<FacilityGetDTO>> listTypeReference = new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {};
    protected static final TypeReference<SingleObjectResponse<FacilityGetDTO>> singleObjectResponseTypeReference = new TypeReference<SingleObjectResponse<FacilityGetDTO>>() {};

    public FacilityApiTest() throws NoSuchMethodException {
    }

    @Before
    public void createOrganization() throws Exception {
        orga = new OrganizationModel();
        orga.setUri(new URI("test:orga"));
        orga.setName("orga");

        getSparqlService().create(orga);
    }

    public FacilityUpdateDTO getCreationDTO(int count) throws URISyntaxException {

        FacilityUpdateDTO facility = new FacilityUpdateDTO();
        facility.setName("facility"+count);
        facility.setUri(new URI("test:facility"+count));
        List<URI> orgaUris = new ArrayList<>();
        orgaUris.add(orga.getUri());
        facility.setOrganizations(orgaUris);
        facility.setLocations(new ArrayList<>());
        return facility;
    }

    public FacilityAddressDTO getFacilityAddressDTO(String countryName, String locality, String postalCode, String region, String streetAddress) {
        FacilityAddressDTO dto = new FacilityAddressDTO();
        dto.setCountryName(countryName);
        dto.setLocality(locality);
        dto.setPostalCode(postalCode);
        dto.setRegion(region);
        dto.setStreetAddress(streetAddress);
        return dto;
    }

    public FacilityCreationDTO getCreationDTOWithGeometry(String name, FacilityAddressDTO address, GeoJsonObject geoJson) {
        FacilityCreationDTO dto = new FacilityCreationDTO();
        dto.setName(name);
        dto.setAddress(address);

        List<LocationObservationDTO> locations = new ArrayList<>();

        if(Objects.nonNull(geoJson)) {
            LocationObservationDTO location = new LocationObservationDTO();
            location.setGeojson(geoJson);
            location.setEndDate(Instant.parse("2021-09-08T00:00:00.00Z"));
            locations.add(location);
        }

        dto.setLocations(locations);

        return dto;
    }

    public FacilityUpdateDTO getUpdateDTOWithGeometry(URI uri, FacilityAddressDTO address, GeoJsonObject geoJson) {
        FacilityUpdateDTO dto = new FacilityUpdateDTO();
        dto.setUri(uri);
        dto.setAddress(address);

        List<LocationObservationDTO> locations = new ArrayList<>();

        if(Objects.nonNull(geoJson)) {
            LocationObservationDTO location = new LocationObservationDTO();
            location.setGeojson(geoJson);
            location.setEndDate(Instant.parse("2021-09-08T12:00:00+01:00"));
            locations.add(location);
        }

        dto.setLocations(locations);

        return dto;
    }

    @Test
    public void testSearchByName() throws Exception {

        // insert the both facility
        FacilityUpdateDTO facility1 = getCreationDTO(1);
        FacilityUpdateDTO facility2 = getCreationDTO(2);

        Response creationResponse = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), facility1);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());
        creationResponse = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), facility2);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());

        // test search with pattern which match the both facility
        Map<String,Object> searchParams = new HashMap<>();
        searchParams.put("pattern","facility");

        List<FacilityGetDTO> results = getSearchResultsAsAdmin(SEARCH_PATH,searchParams,listTypeReference);
        assertEquals(2,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility1.getUri())));
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility2.getUri())));

        // test search with pattern which match only one facility
        searchParams = new HashMap<>();
        searchParams.put("pattern","1");

        results = getSearchResultsAsAdmin(SEARCH_PATH,null,null,searchParams,listTypeReference);
        assertEquals(1,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility1.getUri())));
        Assert.assertFalse(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility2.getUri())));

        // test search with pattern which no facility
        searchParams = new HashMap<>();
        searchParams.put("pattern","non-matching pattern");

        results = getSearchResultsAsAdmin(SEARCH_PATH,null,null,searchParams,listTypeReference);
        Assert.assertTrue(results.isEmpty());
    }

    private final static String URIS_PARAM_NAME = "uris";

    @Test
    public void testSearchByUris() throws Exception{

        // insert the both facility
        FacilityUpdateDTO facility1 = getCreationDTO(1);
        FacilityUpdateDTO facility2 = getCreationDTO(2);

        Response creationResponse = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), facility1);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());
        creationResponse = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), facility2);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());


        // test search with pattern which match the both facility
        Map<String,Object> searchParams = new HashMap<>();
        searchParams.put(URIS_PARAM_NAME,Arrays.asList(facility1.getUri(),facility2.getUri()));

        List<FacilityGetDTO> results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(2,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility1.getUri())));
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility2.getUri())));

        // test search with pattern which match only one facility
        searchParams = new HashMap<>();
        searchParams.put(URIS_PARAM_NAME, Collections.singletonList(facility1.getUri()));

        results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(1,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility1.getUri())));

        // test search with pattern which match only one facility
        searchParams = new HashMap<>();
        searchParams.put(URIS_PARAM_NAME, Collections.singletonList(facility2.getUri()));

        results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(1,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility2.getUri())));
    }

    @Test
    public void testCreateWithAddress() throws Exception {
        FacilityCreationDTO dto = getCreationDTOWithGeometry("test", getFacilityAddressDTO(
                "France",
                "Montpellier",
                "34000",
                null,
                "2 place pierre viala"
        ), null);
        Response response = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // Call "getFacilities" because this service retrieve the address spatial coordinates if there is no geometry - the "get" service retrieve only the last geometry
        PaginatedListResponse<FacilityGetWithGeometryDTO> facilityList = new UserCallBuilder(getFacilities)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<FacilityGetWithGeometryDTO>>() {
                })
                .getDeserializedResponse();
        assertNotNull(facilityList.getResult().get(0).getGeometry());
        assertNotNull(facilityList.getResult().get(0).getAddress());
    }

    @Test
    public void testCreateWithGeometry() throws Exception {
        FacilityCreationDTO dto = getCreationDTOWithGeometry("test", null, new Point(49, 3));
        Response response = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        ObjectUriResponse objectUriResponse = mapper.convertValue(response.readEntity(JsonNode.class), objectUriResponseTypeReference);
        URI createdUri = new URI(objectUriResponse.getResult());

        response = getJsonGetByUriResponseAsAdmin(target(URI_PATH), createdUri.toString());
        SingleObjectResponse<FacilityGetDTO> singleObjectResponse = mapper.convertValue(response.readEntity(JsonNode.class), singleObjectResponseTypeReference);
        Feature feature = (Feature) singleObjectResponse.getResult().getLastPosition().getGeojson();
        assertEquals(new Point(49, 3), feature.getGeometry());
        assertNull(singleObjectResponse.getResult().getAddress());
    }

    @Test
    public void testCreateWithAddressAndGeometry() throws Exception {
        FacilityCreationDTO dto = getCreationDTOWithGeometry("test", getFacilityAddressDTO(
                "France",
                "Montpellier",
                "34000",
                null,
                "2 place Pierre Viala"
        ), new Point(49, 3));
        Response response = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        ObjectUriResponse objectUriResponse = mapper.convertValue(response.readEntity(JsonNode.class), objectUriResponseTypeReference);
        URI createdUri = new URI(objectUriResponse.getResult());

        response = getJsonGetByUriResponseAsAdmin(target(URI_PATH), createdUri.toString());
        SingleObjectResponse<FacilityGetDTO> singleObjectResponse = mapper.convertValue(response.readEntity(JsonNode.class), singleObjectResponseTypeReference);
        Feature feature = (Feature) singleObjectResponse.getResult().getLastPosition().getGeojson();
        assertEquals(new Point(49, 3), feature.getGeometry());
        assertNotNull(singleObjectResponse.getResult().getAddress());
    }

    @Test
    public void testCreateWithoutAddressOrGeometry() throws Exception {
        FacilityCreationDTO dto = getCreationDTOWithGeometry("test", null, null);
        Response response = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        ObjectUriResponse objectUriResponse = mapper.convertValue(response.readEntity(JsonNode.class), objectUriResponseTypeReference);
        URI createdUri = new URI(objectUriResponse.getResult());

        response = getJsonGetByUriResponseAsAdmin(target(URI_PATH), createdUri.toString());
        SingleObjectResponse<FacilityGetDTO> singleObjectResponse = mapper.convertValue(response.readEntity(JsonNode.class), singleObjectResponseTypeReference);
        assertNull(singleObjectResponse.getResult().getLastPosition());
        assertNull(singleObjectResponse.getResult().getAddress());
    }

    @Test
    public void testUpdateWithGeometry() throws Exception {
        FacilityCreationDTO dto = getCreationDTOWithGeometry("test", null, null);
        Response response = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        ObjectUriResponse objectUriResponse = mapper.convertValue(response.readEntity(JsonNode.class), objectUriResponseTypeReference);
        URI createdUri = new URI(objectUriResponse.getResult());

        FacilityUpdateDTO updateDto = getUpdateDTOWithGeometry(createdUri, null, new Point(49, 3));
        response = getJsonPutResponse(target(UPDATE_PATH), updateDto);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        response = getJsonGetByUriResponseAsAdmin(target(URI_PATH), createdUri.toString());
        SingleObjectResponse<FacilityGetDTO> singleObjectResponse = mapper.convertValue(response.readEntity(JsonNode.class), singleObjectResponseTypeReference);
        Feature feature = (Feature) singleObjectResponse.getResult().getLastPosition().getGeojson();
        assertEquals(new Point(49, 3), feature.getGeometry());
    }

    @Test
    public void testUpdateWithAddress() throws Exception {
        FacilityCreationDTO dto = getCreationDTOWithGeometry("test", null, null);
        Response response = getJsonPostResponseAsAdmin(target(create.getPathTemplate()), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        ObjectUriResponse objectUriResponse = mapper.convertValue(response.readEntity(JsonNode.class), objectUriResponseTypeReference);
        URI createdUri = new URI(objectUriResponse.getResult());

        FacilityUpdateDTO updateDto = getUpdateDTOWithGeometry(createdUri, getFacilityAddressDTO(
                "France",
                "Montpellier",
                "34000",
                "Occitanie",
                "2 place Pierre Viala"
        ), null);
        response = getJsonPutResponse(target(UPDATE_PATH), updateDto);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Call "getFacilities" because this service retrieve the address spatial coordinates if there is no geometry - the "get" service retrieve only the last geometry
        PaginatedListResponse<FacilityGetWithGeometryDTO> facilityList = new UserCallBuilder(getFacilities)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<FacilityGetWithGeometryDTO>>() {
                })
                .getDeserializedResponse();
        assertNotNull(facilityList.getResult().get(0).getGeometry());
        assertNotNull(facilityList.getResult().get(0).getAddress());
    }

    @Test
    public void testGetFacilities() throws Exception {
        FacilityAddressDTO address = getFacilityAddressDTO(
                "France",
                "Montpellier",
                "34000",
                null,
                "2 place pierre viala"
        );

        //create several facilities with address, positions and without
        new UserCallBuilder(create).setBody(getCreationDTOWithGeometry("testWithout", null, null))
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CREATED);
        new UserCallBuilder(create).setBody(getCreationDTOWithGeometry("testWithPosition", null, new Point(49, 3)))
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CREATED);
        new UserCallBuilder(create).setBody(getCreationDTOWithGeometry("testWithAddress", address, null))
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CREATED);
        new UserCallBuilder(create).setBody(getCreationDTOWithGeometry("testWithBoth", address, new Point(49, 3)))
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CREATED);

        //search sites with spatial coordinates
        PaginatedListResponse<FacilityGetWithGeometryDTO> facilityList = new UserCallBuilder(getFacilities)
                    .buildAdmin()
                    .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<FacilityGetWithGeometryDTO>>() {
                    })
                    .getDeserializedResponse();

        assertEquals(facilityList.getResult().size(), 3);
    }


    @Override
    protected List<String> getCollectionsToClearNames() {
        return Collections.singletonList(LocationObservationDAO.LOCATION_COLLECTION_NAME);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        List<Class<? extends SPARQLResourceModel>> modelList = new ArrayList<>();

        modelList.add(FacilityModel.class);
        modelList.add(LocationObservationCollectionModel.class);
        return modelList;
    }
}
