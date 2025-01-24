package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.organisation.api.site.*;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Brice MAUSSANG
 */
public class SiteAPITest extends AbstractMongoIntegrationTest {

    protected final static String PATH = "/core/sites";
    protected final static String URI_PATH = PATH + "/{uri}";
    protected final static String URIS_PATH = PATH + "/by_uris";
    protected final static String SEARCH_PATH = PATH;
    protected final static String CREATE_PATH = PATH;

    private final static String URIS_PARAM_NAME = "uris";

    protected OrganizationModel orga;

    protected static final TypeReference<PaginatedListResponse<SiteGetListDTO>> listTypeReference = new TypeReference<PaginatedListResponse<SiteGetListDTO>>() {};

    protected final ServiceDescription create = new ServiceDescription(
            SiteAPI.class.getMethod("createSite", SiteCreationDTO.class),
            PATH
    );

    protected final ServiceDescription getByUri = new ServiceDescription(
            SiteAPI.class.getMethod("getSite", URI.class),
            PATH + "/{uri}"
    );

    protected final ServiceDescription update = new ServiceDescription(
            SiteAPI.class.getMethod("updateSite", SiteUpdateDTO.class),
            PATH
    );

    protected final ServiceDescription delete = new ServiceDescription(
            SiteAPI.class.getMethod("deleteSite", URI.class),
            PATH + "/{uri}"
    );

    protected final ServiceDescription getSites = new ServiceDescription(
            SiteAPI.class.getMethod("getSitesWithLocation"),
            PATH +"/with_location"
    );

    public SiteAPITest() throws NoSuchMethodException {
    }

    @Before
    public void createOrganization() throws Exception {
        orga = new OrganizationModel();
        orga.setUri(new URI("test:orga"));
        orga.setName("orga");

        getSparqlService().create(orga);
    }

    public SiteUpdateDTO getCreationDTO(int count) throws URISyntaxException {
        SiteUpdateDTO site = new SiteUpdateDTO();
        site.setName("site"+count);
        site.setUri(new URI("test:site"+count));
        List<URI> orgaUris = new ArrayList<>();
        orgaUris.add(orga.getUri());
        site.setOrganizations(orgaUris);
        return site;
    }

    public SiteCreationDTO getCreationDTOWithAddress(String name, SiteAddressDTO address) {
        SiteCreationDTO dto = new SiteCreationDTO();
        dto.setName(name);
        List<URI> orgaUris = new ArrayList<>();
        orgaUris.add(orga.getUri());
        dto.setOrganizations(orgaUris);
        dto.setAddress(address);
        return dto;
    }
    public SiteUpdateDTO getUpdateDTOWithAddress(URI uri, SiteAddressDTO address) {
        SiteUpdateDTO dto = new SiteUpdateDTO();
        dto.setUri(uri);
        dto.setAddress(address);
        List<URI> orgaUris = new ArrayList<>();
        orgaUris.add(orga.getUri());
        dto.setOrganizations(orgaUris);
        return dto;
    }

    public SiteAddressDTO getSiteAddressDTO(String countryName, String locality, String postalCode, String region, String streetAddress) {
        SiteAddressDTO dto = new SiteAddressDTO();
        dto.setCountryName(countryName);
        dto.setLocality(locality);
        dto.setPostalCode(postalCode);
        dto.setRegion(region);
        dto.setStreetAddress(streetAddress);
        return dto;
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

        List<SiteGetListDTO> results = getSearchResultsAsAdmin(SEARCH_PATH,searchParams,listTypeReference);
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

        List<SiteGetListDTO> results = getSearchResultsAsAdmin(URIS_PATH,searchParams,listTypeReference);
        assertEquals(2,results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site1.getUri())));
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(),site2.getUri())));

        // test search with pattern which match only one facility
        searchParams = new HashMap<>();
        searchParams.put(URIS_PARAM_NAME, Collections.singletonList(site1.getUri()));

        results = getSearchResultsAsAdmin(URIS_PATH, searchParams, listTypeReference);
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

    @Test
    public void testUpdateWithAddress() throws Exception {
        URI uri = new UserCallBuilder(create).setBody(getCreationDTOWithAddress("test", null)).buildAdmin().executeCallAndReturnURI();

        SiteUpdateDTO updateDto = getUpdateDTOWithAddress(uri, getSiteAddressDTO(
                "France",
                "Montpellier",
                "34000",
                "Occitanie",
                "2 place Pierre Viala"
        ));
        new UserCallBuilder(update).setBody(updateDto).buildAdmin().executeCallAndAssertStatus(Response.Status.OK);

        SingleObjectResponse<SiteGetDTO> singleObjectResponse = new UserCallBuilder(getByUri)
                .setUriInPath(uri)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<SiteGetDTO>>() {
                })
                .getDeserializedResponse();
        assertNotNull(singleObjectResponse.getResult().getAddress());
    }

    @Test
    public void testDelete() throws Exception {
        // create object and check if URI exists
        URI uri = new UserCallBuilder(create).setBody(getCreationDTO(1)).buildAdmin().executeCallAndReturnURI();

        // delete object and check if URI no longer exists
        UserCall deleteCall = new UserCallBuilder(delete).setUriInPath(uri).buildAdmin();
        URI uriDelete = deleteCall.executeCallAndReturnURI();
        assertEquals(uri, uriDelete);

        UserCall getCall = new UserCallBuilder(getByUri).setUriInPath(uri).buildAdmin();
        getCall.executeCallAndAssertStatus(Response.Status.NOT_FOUND);
    }

    @Test
    public void testGetSites() throws Exception {
        //create several sites with address and without
        new UserCallBuilder(create).setBody(getCreationDTO(1)).buildAdmin().executeCallAndAssertStatus(Response.Status.CREATED);
        new UserCallBuilder(create).setBody(getCreationDTO(2)).buildAdmin().executeCallAndAssertStatus(Response.Status.CREATED);
        URI siteWith3 = new UserCallBuilder(create).setBody(getCreationDTOWithAddress("withAddress",getSiteAddressDTO(
                "France",
                "Montpellier",
                "34000",
                null,
                "2 place pierre viala"
        ))).buildAdmin().executeCallAndReturnURI();

        //search sites with spatial coordinates
        PaginatedListResponse<SiteGetWithGeometryDTO> sitelist = new UserCallBuilder(getSites)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<SiteGetWithGeometryDTO>>() {})
                .getDeserializedResponse();

        assertEquals(sitelist.getResult().size(), 1);

        URI resultURI = URI.create(SPARQLDeserializers.getExpandedURI(sitelist.getResult().get(0).getUri()));
        assertEquals(resultURI, siteWith3);
    }

}