package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.organisation.api.site.SiteGetDTO;
import org.opensilex.core.organisation.api.site.SiteUpdateDTO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Brice MAUSSANG
 */
public class SiteAPITest extends AbstractMongoIntegrationTest {

    protected final static String PATH = "/core/sites";
    protected final static String URI_PATH = PATH + "/{uri}";
    protected final static String URIS_PATH = PATH + "/by_uris";
    protected final static String SEARCH_PATH = PATH;
    protected final static String CREATE_PATH = PATH;
    protected final static String UPDATE_PATH = PATH;
    protected final static String DELETE_PATH = PATH + "/{uri}";

    private final static String URIS_PARAM_NAME = "uris";

    protected OrganizationModel infra;

    protected static final TypeReference<PaginatedListResponse<SiteGetDTO>> listTypeReference = new TypeReference<PaginatedListResponse<SiteGetDTO>>() {};

    @Before
    public void createInfrastructure() throws Exception {
        infra = new OrganizationModel();
        infra.setUri(new URI("test:infra"));
        infra.setName("infra");

        getSparqlService().create(infra);
    }

    public SiteUpdateDTO getCreationDTO(int count) throws URISyntaxException {
        SiteUpdateDTO site = new SiteUpdateDTO();
        site.setName("site"+count);
        site.setUri(new URI("test:site"+count));
        List<URI> infraUris = new ArrayList<>();
        infraUris.add(infra.getUri());
        site.setOrganizations(infraUris);
        return site;
    }


    @Test
    public void testSearchByName() throws Exception {

        // insert both sites
        SiteUpdateDTO site1 = getCreationDTO(1);
        SiteUpdateDTO site2 = getCreationDTO(2);

        Response creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), site1);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());
        creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), site2);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());

        // test search with pattern which match both sites
        Map<String,Object> searchParams = new HashMap<>();
        searchParams.put("pattern","site");

        List<SiteGetDTO> results = getSearchResultsAsAdmin(SEARCH_PATH,searchParams,listTypeReference);
        assertEquals(2,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site1.getUri())));
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site2.getUri())));

        // test search with pattern which match only one site
        searchParams = new HashMap<>();
        searchParams.put("pattern","1");

        results = getSearchResultsAsAdmin(SEARCH_PATH,null,null,searchParams,listTypeReference);
        assertEquals(1,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site1.getUri())));
        Assert.assertFalse(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site2.getUri())));

        // test search with pattern which no site
        searchParams = new HashMap<>();
        searchParams.put("pattern","non-matching pattern");

        results = getSearchResultsAsAdmin(SEARCH_PATH,null,null,searchParams,listTypeReference);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void testSearchByUris() throws Exception {

        // insert both sites
        SiteUpdateDTO site1 = getCreationDTO(1);
        SiteUpdateDTO site2 = getCreationDTO(2);

        Response creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), site1);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());
        creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), site2);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());


        // test search with pattern which match the both facility
        Map<String,Object> searchParams = new HashMap<>();
        searchParams.put(URIS_PARAM_NAME, Arrays.asList(site1.getUri(),site2.getUri()));

        List<SiteGetDTO> results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(2,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site1.getUri())));
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site2.getUri())));

        // test search with pattern which match only one facility
        searchParams = new HashMap<>();
        searchParams.put(URIS_PARAM_NAME, Collections.singletonList(site1.getUri()));

        results = results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(1,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site1.getUri())));

        // test search with pattern which match only one facility
        searchParams = new HashMap<>();
        searchParams.put(URIS_PARAM_NAME, Collections.singletonList(site2.getUri()));

        results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(1,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site2.getUri())));
    }

    @Test
    public void testCreateSite() throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(CREATE_PATH), getCreationDTO(1));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(URI_PATH), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(SiteModel.class);
    }

}