package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.organisation.api.facility.FacilityGetDTO;
import org.opensilex.core.organisation.api.facility.FacilityUpdateDTO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;

public class FacilityApiTest extends AbstractMongoIntegrationTest {

    protected final static String PATH = "/core/facilities";
    protected final static String URI_PATH = PATH + "/{uri}";
    protected final static String URIS_PATH = PATH + "/by_uris";
    protected final static String SEARCH_PATH = PATH;
    protected final static String CREATE_PATH = PATH;
    protected final static String UPDATE_PATH = PATH;
    protected final static String DELETE_PATH = PATH + "/{uri}";

    protected OrganizationModel infra;

    // TypeReference used to parse Response into a List of FacilityGetDTO
    protected static final TypeReference<PaginatedListResponse<FacilityGetDTO>> listTypeReference = new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {};

    @Before
    public void createInfrastructure() throws Exception {
        infra = new OrganizationModel();
        infra.setUri(new URI("test:infra"));
        infra.setName("infra");

        getSparqlService().create(infra);
    }

    public FacilityUpdateDTO getCreationDTO(int count) throws URISyntaxException {

        FacilityUpdateDTO facility = new FacilityUpdateDTO();
        facility.setName("facility"+count);
        facility.setUri(new URI("test:facility"+count));
        List<URI> infraUris = new ArrayList<>();
        infraUris.add(infra.getUri());
        facility.setOrganizations(infraUris);
        return facility;
    }

    @Test
    public void testSearchByName() throws Exception {

        // insert the both facility
        FacilityUpdateDTO facility1 = getCreationDTO(1);
        FacilityUpdateDTO facility2 = getCreationDTO(2);

        Response creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), facility1);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());
        creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), facility2);
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

        Response creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), facility1);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());
        creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), facility2);
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

        results = results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(1,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility1.getUri())));

        // test search with pattern which match only one facility
        searchParams = new HashMap<>();
        searchParams.put(URIS_PARAM_NAME, Collections.singletonList(facility2.getUri()));

        results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(1,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),facility2.getUri())));
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(FacilityModel.class);
    }

}
