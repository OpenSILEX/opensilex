/*******************************************************************************
 *                         OrganizationAccessAPITest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 26/10/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.organisation.api.facility.FacilityAPI;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.core.organisation.api.facility.FacilityGetDTO;
import org.opensilex.core.organisation.api.site.SiteCreationDTO;
import org.opensilex.core.organisation.api.site.SiteGetDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.security.group.api.GroupAPITest;
import org.opensilex.security.group.api.GroupCreationDTO;
import org.opensilex.security.group.api.GroupUserProfileDTO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.api.ProfileAPITest;
import org.opensilex.security.profile.api.ProfileCreationDTO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.user.api.UserAPITest;
import org.opensilex.security.user.api.UserCreationDTO;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.ResourceDTO;
import org.opensilex.sparql.response.ResourceDagDTO;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Valentin RIGOLLE
 */
public class OrganizationAccessAPITest extends AbstractMongoIntegrationTest {

    public static final String PATH = "/core/organisations";

    public static final String URI_PATH = PATH + "/{uri}";
    public static final String SEARCH_PATH = PATH;
    public static final String CREATE_PATH = PATH;
    public static final String UPDATE_PATH = PATH;
    public static final String DELETE_PATH = PATH + "/{uri}";

    // Site API
    public static final String SITE_PATH = "/core/sites";
    public static final String SITE_URI_PATH = SITE_PATH + "/{uri}";
    public static final String SITE_SEARCH_PATH = SITE_PATH;
    public static final String SITE_URIS_PATH = SITE_PATH + "/by_uris";
    public static final String SITE_CREATE_PATH = SITE_PATH;

    private final UserAPITest userAPITest = new UserAPITest();
    private final ProfileAPITest profileAPITest = new ProfileAPITest();
    private final GroupAPITest groupAPITest = new GroupAPITest();

    private static final String USER_MAIL = "user@example.org";
    private static final String USER_PASSWORD = "password";
    private URI user;
    private URI groupWithUser;
    private URI groupWithoutUser;

    private URI orgCreatedByUser;
    private URI orgParent;
    private URI orgChild;
    private URI orgPrivate;
    private URI orgPublic;
    
    private URI facOfOrgCreatedByUser;
    private URI facOfOrgParent;
    private URI facOfOrgChild;
    private URI facOfOrgPrivate;
    private URI facOfOrgPublic;
    private URI facPrivate;
    private URI facCreatedByUser;
    private URI facOfSiteOfOrgCreatedByUser;
    private URI facOfSiteOfOrgParent;
    private URI facOfSiteOfOrgChild;
    private URI facOfSiteOfOrgPrivate;
    private URI facOfSiteOfOrgPrivateWithCorrectGroup;
    private URI facOfSiteOfOrgPrivateWithWrongGroup;
    private URI facOfSiteOfOrgPublic;

    private URI siteOfOrgCreatedByUser;
    private URI siteOfOrgParent;
    private URI siteOfOrgChild;
    private URI siteOfOrgPrivate;
    private URI siteOfOrgPrivateWithCorrectGroup;
    private URI siteOfOrgPrivateWithWrongGroup;
    private URI siteOfOrgPublic;

    // Test assertions
    private final Set<URI> accessibleOrganizationURISet = new HashSet<>();
    private final Set<URI> accessibleFacilityURISet = new HashSet<>();
    private final Set<URI> accessibleSiteURISet = new HashSet<>();
    private final Set<URI> forbiddenOrganizationsURISet = new HashSet<>();
    private final Set<URI> forbiddenFacilityURISet = new HashSet<>();
    private final Set<URI> forbiddenSiteURISet = new HashSet<>();

    @Before
    public void beforeTest() throws Exception {
        createAndRegisterUserProfileGroups();
        createOrganizationHierarchy();
        createFacilities();
        createSites();
        initAssertionSets();
    }

    private OrganizationCreationDTO getCreationDTO(String name, URI parentURI, URI groupURI) {
        OrganizationCreationDTO organizationCreationDTO = new OrganizationCreationDTO();
        organizationCreationDTO.setName(name);
        if (Objects.nonNull(parentURI)) {
            organizationCreationDTO.setParents(Collections.singletonList(parentURI));
        }
        if (Objects.nonNull(groupURI)) {
            organizationCreationDTO.setGroups(Collections.singletonList(groupURI));
        }
        return organizationCreationDTO;
    }

    private URI createOrganization(String name, URI parentURI, URI groupURI, boolean createdByUser) throws Exception {
        WebTarget createTarget = target(CREATE_PATH);
        OrganizationCreationDTO creationDTO = getCreationDTO(name, parentURI, groupURI);
        Response postOrganizationResponse;

        if (createdByUser) {
            postOrganizationResponse = getJsonPostResponse(createTarget, creationDTO, USER_MAIL);
        } else {
            postOrganizationResponse = getJsonPostResponseAsAdmin(createTarget, creationDTO);
        }

        return extractUriFromResponse(postOrganizationResponse);
    }

    private FacilityCreationDTO getFacilityCreationDTO(String name, URI orgURI) {
        FacilityCreationDTO facilityCreationDTO = new FacilityCreationDTO();
        facilityCreationDTO.setName(name);
        if (Objects.nonNull(orgURI)) {
            facilityCreationDTO.setOrganizations(Collections.singletonList(orgURI));
        } else {
            facilityCreationDTO.setOrganizations(Collections.emptyList());
        }
        return facilityCreationDTO;
    }

    private URI createFacility(String name, URI orgURI, boolean createdByUser) throws Exception {
        WebTarget createTarget = target(FacilityApiTest.CREATE_PATH);
        FacilityCreationDTO facilityCreationDTO = getFacilityCreationDTO(name, orgURI);
        Response postFacilityResponse;
        
        if (createdByUser) {
            postFacilityResponse = getJsonPostResponse(createTarget, facilityCreationDTO, USER_MAIL);
        } else {
            postFacilityResponse = getJsonPostResponseAsAdmin(createTarget, facilityCreationDTO);
        }
        
        return extractUriFromResponse(postFacilityResponse);
    }

    private SiteCreationDTO getSiteCreationDTO(String name, URI orgURI, URI facilityURI, URI groupURI) {
        SiteCreationDTO siteCreationDTO = new SiteCreationDTO();
        siteCreationDTO.setName(name);
        siteCreationDTO.setOrganizations(Collections.singletonList(orgURI));

        if (Objects.nonNull(facilityURI)) {
            siteCreationDTO.setFacilities(Collections.singletonList(facilityURI));
        }

        if (Objects.nonNull(groupURI)) {
            siteCreationDTO.setGroups(Collections.singletonList(groupURI));
        }

        return siteCreationDTO;
    }

    private URI createSite(String name, URI orgURI, URI facilityURI, URI groupURI) throws Exception {
        WebTarget createTarget = target(SITE_CREATE_PATH);
        SiteCreationDTO siteCreationDTO = getSiteCreationDTO(name, orgURI, facilityURI, groupURI);
        Response postSiteResponse;

        postSiteResponse = getJsonPostResponseAsAdmin(createTarget, siteCreationDTO);

        return extractUriFromResponse(postSiteResponse);
    }

    private void createAndRegisterUserProfileGroups() throws Exception {
        // User creation
        UserCreationDTO userCreationDTO = new UserCreationDTO();
        userCreationDTO.setPassword(USER_PASSWORD);
        userCreationDTO.setEmail(USER_MAIL);
        userCreationDTO.setFirstName("User");
        userCreationDTO.setLastName("Example");
        userCreationDTO.setLanguage(OpenSilex.DEFAULT_LANGUAGE);
        Response postUserResponse = getJsonPostResponseAsAdmin(target(userAPITest.createPath), userCreationDTO);
        user = extractUriFromResponse(postUserResponse);

        // Profile creation
        ProfileCreationDTO profileCreationDTO = new ProfileCreationDTO();
        profileCreationDTO.setName("Access test profile");
        profileCreationDTO.setCredentials(new ArrayList<String>() {{
            add(OrganizationAPI.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID);
            add(FacilityAPI.CREDENTIAL_FACILITY_MODIFICATION_ID);
        }});
        Response postProfileResponse = getJsonPostResponseAsAdmin(target(profileAPITest.createPath), profileCreationDTO);
        URI profile = extractUriFromResponse(postProfileResponse);

        // User profile for the group
        GroupUserProfileDTO groupUserProfileDTO = new GroupUserProfileDTO();
        groupUserProfileDTO.setUserURI(user);
        groupUserProfileDTO.setProfileURI(profile);

        // Group with user creation
        GroupCreationDTO groupWithUserCreationDTO = new GroupCreationDTO();
        groupWithUserCreationDTO.setName("Group with user");
        groupWithUserCreationDTO.setDescription("Group with user");
        groupWithUserCreationDTO.setUserProfiles(Collections.singletonList(groupUserProfileDTO));
        Response postGroupWithUserResponse = getJsonPostResponseAsAdmin(target(groupAPITest.createPath), groupWithUserCreationDTO);
        groupWithUser = extractUriFromResponse(postGroupWithUserResponse);

        // Group without user creation
        GroupCreationDTO groupWithoutUserCreationDTO = new GroupCreationDTO();
        groupWithoutUserCreationDTO.setName("Group without user");
        groupWithoutUserCreationDTO.setDescription("Group without user");
        Response postGroupWithoutUserResponse = getJsonPostResponseAsAdmin(target(groupAPITest.createPath), groupWithoutUserCreationDTO);
        groupWithoutUser = extractUriFromResponse(postGroupWithoutUserResponse);

        // Register the token for login into tests
        // Must be done after the group / profile creation for correct credentials
        registerToken(USER_MAIL, USER_PASSWORD);
    }

    private void createOrganizationHierarchy() throws Exception {
        orgCreatedByUser = createOrganization("Organization created by user", null, null, true);
        orgParent = createOrganization("Parent organization", null, groupWithUser, false);
        orgChild = createOrganization("Child organization", orgParent, null, false);
        orgPrivate = createOrganization("Private organization", null, groupWithoutUser, false);
        orgPublic = createOrganization("Public organization", null, null, false);
    }
    
    private void createFacilities() throws Exception {
        // Facilities hosting one organization
        facOfOrgCreatedByUser = createFacility("Facility, hosting organization created by user", orgCreatedByUser, false);
        facOfOrgParent = createFacility("Facility, hosting parent organization", orgParent, false);
        facOfOrgChild = createFacility("Facility, hosting child organization", orgChild, false);
        facOfOrgPublic = createFacility("Facility, hosting public organization", orgPublic, false);
        facOfOrgPrivate = createFacility("Facility, hosting private organization", orgPrivate, false);
        // Facilities hosting zero organization
        facCreatedByUser = createFacility("Facility created by user", null, true);
        facPrivate = createFacility("Private facility", null, false);
        // Facilities hosting one site
        facOfSiteOfOrgCreatedByUser = createFacility("Facility, hosting site of organization created by user", null, false);
        facOfSiteOfOrgParent = createFacility("Facility, hosting site of parent organization", null, false);
        facOfSiteOfOrgChild = createFacility("Facility, hosting site of child organization", null, false);
        facOfSiteOfOrgPublic = createFacility("Facility, hosting site of public organization", null, false);
        facOfSiteOfOrgPrivate = createFacility("Facility, hosting site of private organization", null, false);
        facOfSiteOfOrgPrivateWithCorrectGroup = createFacility("Facility, hosting site of private organization, but with the correct group", null, false);
        facOfSiteOfOrgPrivateWithWrongGroup = createFacility("Facility, hosting site of private organization, but with the wrong group", null, false);
    }

    private void createSites() throws Exception {
        siteOfOrgCreatedByUser = createSite("Site of organization created by user", orgCreatedByUser, facOfSiteOfOrgCreatedByUser, null);
        siteOfOrgParent = createSite("Site of parent organization", orgParent, facOfSiteOfOrgParent, null);
        siteOfOrgChild = createSite("Site of child organization", orgChild, facOfSiteOfOrgChild, null);
        siteOfOrgPublic = createSite("Site of public organization", orgPublic, facOfSiteOfOrgPublic, null);
        siteOfOrgPrivate = createSite("Site of private organization", orgPrivate, facOfSiteOfOrgPrivate, null);
        siteOfOrgPrivateWithCorrectGroup = createSite("Site of private organization, but with the correct group", orgPrivate, facOfSiteOfOrgPrivateWithCorrectGroup, groupWithUser);
        siteOfOrgPrivateWithWrongGroup = createSite("Site of private organization, but with the wrong group", orgPrivate, facOfSiteOfOrgPrivateWithCorrectGroup, groupWithoutUser);
    }

    private void initAssertionSets() {
        accessibleOrganizationURISet.add(orgCreatedByUser);
        accessibleOrganizationURISet.add(orgParent);
        accessibleOrganizationURISet.add(orgChild);
        accessibleOrganizationURISet.add(orgPublic);
        forbiddenOrganizationsURISet.add(orgPrivate);

        accessibleFacilityURISet.add(facOfOrgCreatedByUser);
        accessibleFacilityURISet.add(facOfOrgParent);
        accessibleFacilityURISet.add(facOfOrgChild);
        accessibleFacilityURISet.add(facOfOrgPublic);
        accessibleFacilityURISet.add(facCreatedByUser);
        accessibleFacilityURISet.add(facOfSiteOfOrgCreatedByUser);
        accessibleFacilityURISet.add(facOfSiteOfOrgParent);
        accessibleFacilityURISet.add(facOfSiteOfOrgChild);
        accessibleFacilityURISet.add(facOfSiteOfOrgPublic);
        accessibleFacilityURISet.add(facOfSiteOfOrgPrivateWithCorrectGroup);
        forbiddenFacilityURISet.add(facOfOrgPrivate);
        forbiddenFacilityURISet.add(facPrivate);
        forbiddenFacilityURISet.add(facOfSiteOfOrgPrivate);
        forbiddenFacilityURISet.add(facOfSiteOfOrgPrivateWithWrongGroup);

        accessibleSiteURISet.add(siteOfOrgCreatedByUser);
        accessibleSiteURISet.add(siteOfOrgParent);
        accessibleSiteURISet.add(siteOfOrgChild);
        accessibleSiteURISet.add(siteOfOrgPublic);
        accessibleSiteURISet.add(siteOfOrgPrivateWithCorrectGroup);
        forbiddenSiteURISet.add(siteOfOrgPrivate);
        forbiddenSiteURISet.add(siteOfOrgPrivateWithWrongGroup);
    }

    @Test
    public void testSearchOrganizationsAsAdmin() throws Exception {
        List<ResourceDagDTO<OrganizationModel>> result = getSearchResultsAsAdmin(SEARCH_PATH, null, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {});
        assertEquals(accessibleOrganizationURISet.size() + forbiddenOrganizationsURISet.size(), result.size());
    }

    @Test
    public void testSearchOrganizations() throws Exception {
        assert !accessibleOrganizationURISet.isEmpty();
        assert !forbiddenOrganizationsURISet.isEmpty();

        List<URI> organizationSearchResult = getSearchResults(SEARCH_PATH, null, new TypeReference<PaginatedListResponse<ResourceDagDTO<OrganizationModel>>>() {}, USER_MAIL)
                .stream().map(ResourceDTO::getUri)
                .collect(Collectors.toList());

        for (URI accessibleOrganizationURI : accessibleOrganizationURISet) {
            assertTrue(accessibleOrganizationURI + " should be accessible", organizationSearchResult.stream().anyMatch(uri ->
                    SPARQLDeserializers.compareURIs(uri, accessibleOrganizationURI)));
        }

        for (URI forbiddenOrganizationURI: forbiddenOrganizationsURISet) {
            assertTrue(forbiddenOrganizationURI + " should NOT be accessible", organizationSearchResult.stream().noneMatch(uri ->
                    SPARQLDeserializers.compareURIs(uri, forbiddenOrganizationURI)));
        }
    }

    @Test
    public void testGetAccessibleOrganizations() throws Exception {
        assert !accessibleOrganizationURISet.isEmpty();

        Response getResponse;
        for (URI accessibleOrganizationURI : accessibleOrganizationURISet) {
            getResponse = getJsonGetByUriResponse(target(URI_PATH), accessibleOrganizationURI.toString(), USER_MAIL);
            assertEquals(accessibleOrganizationURI + " should return OK",
                    Response.Status.OK.getStatusCode(),
                    getResponse.getStatus());
        }
    }

    @Test
    public void testGetForbiddenOrganizationsShouldFail() throws Exception {
        assert !forbiddenOrganizationsURISet.isEmpty();

        Response getResponse;
        for (URI forbiddenOrganizationURI : forbiddenOrganizationsURISet) {
            getResponse = getJsonGetByUriResponse(target(URI_PATH), forbiddenOrganizationURI.toString(), USER_MAIL);
            assertEquals(forbiddenOrganizationURI + " should return FORBIDDEN",
                    Response.Status.FORBIDDEN.getStatusCode(),
                    getResponse.getStatus());
        }
    }

    @Test
    public void testSearchFacilitiesAsAdmin() throws Exception {
        List<FacilityGetDTO> result = getSearchResultsAsAdmin(FacilityApiTest.SEARCH_PATH, null, new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {});
        assertEquals(accessibleFacilityURISet.size() + forbiddenFacilityURISet.size(), result.size());
    }

    @Test
    public void testSearchFacilities() throws Exception {
        assert !accessibleFacilityURISet.isEmpty();
        assert !forbiddenFacilityURISet.isEmpty();

        List<URI> facilitySearchResult = getSearchResults(FacilityApiTest.SEARCH_PATH, null, new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {}, USER_MAIL)
                .stream().map(RDFObjectDTO::getUri)
                .collect(Collectors.toList());

        for (URI accessibleFacilityURI : accessibleFacilityURISet) {
            assertTrue(accessibleFacilityURI + " should be accessible", facilitySearchResult.stream().anyMatch(uri ->
                    SPARQLDeserializers.compareURIs(uri, accessibleFacilityURI)));
        }

        for (URI forbiddenFacilityURI: forbiddenFacilityURISet) {
            assertTrue(forbiddenFacilityURI + " should NOT be accessible", facilitySearchResult.stream().noneMatch(uri ->
                    SPARQLDeserializers.compareURIs(uri, forbiddenFacilityURI)));
        }
    }

    @Test
    public void testGetAccessibleFacilities() throws Exception {
        assert !accessibleFacilityURISet.isEmpty();

        Response getResponse;
        for (URI accessibleFacilityURI : accessibleFacilityURISet) {
            getResponse = getJsonGetByUriResponse(target(FacilityApiTest.URI_PATH), accessibleFacilityURI.toString(), USER_MAIL);
            assertEquals(accessibleFacilityURI + " should return OK",
                    Response.Status.OK.getStatusCode(),
                    getResponse.getStatus());
        }
    }

    @Test
    public void testGetForbiddenFacilitiesShouldFail() throws Exception {
        assert !forbiddenFacilityURISet.isEmpty();

        Response getResponse;
        for (URI forbiddenFacilityURI : forbiddenFacilityURISet) {
            getResponse = getJsonGetByUriResponse(target(FacilityApiTest.URI_PATH), forbiddenFacilityURI.toString(), USER_MAIL);
            assertEquals(forbiddenFacilityURI + " should return FORBIDDEN",
                    Response.Status.FORBIDDEN.getStatusCode(),
                    getResponse.getStatus());
        }
    }

    @Test
    public void testGetMultipleFacilities() throws Exception {
        assert !accessibleFacilityURISet.isEmpty();
        assert !forbiddenFacilityURISet.isEmpty();

        List<URI> facilitiesToGet = new ArrayList<URI>() {{
            add(accessibleFacilityURISet.stream().findFirst().get());
            add(forbiddenFacilityURISet.stream().findFirst().get());
        }};

        Map<String, Object> searchParams = new HashMap<String, Object>() {{
            put("uris", facilitiesToGet);
        }};

        List<URI> result = getSearchResults(FacilityApiTest.URIS_PATH, searchParams, new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {}, USER_MAIL)
                .stream().map(RDFObjectDTO::getUri)
                .collect(Collectors.toList());

        assertEquals(1, result.size());
        assertTrue(facilitiesToGet.get(0) + " should be accessible", SPARQLDeserializers.compareURIs(result.get(0), facilitiesToGet.get(0)));
    }

    @Test
    public void testSearchSitesAsAdmin() throws Exception {
        List<SiteGetDTO> result = getSearchResultsAsAdmin(SITE_SEARCH_PATH, null, new TypeReference<PaginatedListResponse<SiteGetDTO>>() {});
        assertEquals(accessibleSiteURISet.size() + forbiddenSiteURISet.size(), result.size());
    }

    @Test
    public void testSearchSites() throws Exception {
        assert !accessibleSiteURISet.isEmpty();
        assert !forbiddenSiteURISet.isEmpty();

        List<URI> siteSearchResult = getSearchResults(SITE_SEARCH_PATH, null, new TypeReference<PaginatedListResponse<SiteGetDTO>>() {}, USER_MAIL)
                .stream().map(ResourceDTO::getUri)
                .collect(Collectors.toList());

        for (URI accessibleSiteURI : accessibleSiteURISet) {
            assertTrue(accessibleSiteURI + " should be accessible", siteSearchResult.stream().anyMatch(uri ->
                    SPARQLDeserializers.compareURIs(uri, accessibleSiteURI)));
        }

        for (URI forbiddenSiteURI: forbiddenSiteURISet) {
            assertTrue(forbiddenSiteURI + " should NOT be accessible", siteSearchResult.stream().noneMatch(uri ->
                    SPARQLDeserializers.compareURIs(uri, forbiddenSiteURI)));
        }
    }

    @Test
    public void testGetAccessibleSites() throws Exception {
        assert !accessibleSiteURISet.isEmpty();

        Response getResponse;
        for (URI accessibleSiteURI : accessibleSiteURISet) {
            getResponse = getJsonGetByUriResponse(target(SITE_URI_PATH), accessibleSiteURI.toString(), USER_MAIL);
            assertEquals(accessibleSiteURI + " should return OK",
                    Response.Status.OK.getStatusCode(),
                    getResponse.getStatus());
        }
    }

    @Test
    public void testGetForbiddenSitesShouldFail() throws Exception {
        assert !forbiddenSiteURISet.isEmpty();

        Response getResponse;
        for (URI forbiddenSiteURI : forbiddenSiteURISet) {
            getResponse = getJsonGetByUriResponse(target(SITE_URI_PATH), forbiddenSiteURI.toString(), USER_MAIL);
            assertEquals(forbiddenSiteURI + " should return FORBIDDEN",
                    Response.Status.FORBIDDEN.getStatusCode(),
                    getResponse.getStatus());
        }
    }

    @Test
    public void testGetMultipleSites() throws Exception {
        assert !accessibleSiteURISet.isEmpty();
        assert !forbiddenSiteURISet.isEmpty();

        List<URI> sitesToGet = new ArrayList<URI>() {{
            add(accessibleSiteURISet.stream().findFirst().get());
            add(forbiddenSiteURISet.stream().findFirst().get());
        }};

        Map<String, Object> searchParams = new HashMap<String, Object>() {{
            put("uris", sitesToGet);
        }};

        List<URI> result = getSearchResults(SITE_URIS_PATH, searchParams, new TypeReference<PaginatedListResponse<FacilityGetDTO>>() {}, USER_MAIL)
                .stream().map(RDFObjectDTO::getUri)
                .collect(Collectors.toList());

        assertEquals(1, result.size());
        assertTrue(sitesToGet.get(0) + " should be accessible", SPARQLDeserializers.compareURIs(result.get(0), sitesToGet.get(0)));
    }

    @After
    public void afterTests() throws Exception {
        getDeleteByUriResponse(target(userAPITest.deletePath), user.toString());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return new ArrayList<Class<? extends SPARQLResourceModel>>() {{
            add(OrganizationModel.class);
            add(FacilityModel.class);
            add(SiteModel.class);
            add(ProfileModel.class);
            add(GroupUserProfileModel.class);
            add(GroupModel.class);
        }};
    }
}
