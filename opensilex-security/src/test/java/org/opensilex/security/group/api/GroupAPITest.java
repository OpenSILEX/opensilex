package org.opensilex.security.group.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.client.WebTarget;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import org.opensilex.OpenSilex;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

public class GroupAPITest extends AbstractSecurityIntegrationTest {

    public String path = "/security/groups";
    public String createPath = path;
    public String updatePath = path ;
    public String getPath = path + "/{uri}";
    public String deletePath = path + "/{uri}";
    public String searchPath = path;

    private final static String USER1_URI = "http://example.org/users/user1";
    private final static String USER2_URI = "http://example.org/users/user2";
    private final static String PROFILE1_URI = "http://example.org/profiles/profile1";
    private final static String PROFILE2_URI = "http://example.org/profiles/profile2";

    protected void createTestEnv() throws Exception {
        SPARQLService sparql = this.getSparqlService();
        sparql.getDefaultGraph(GroupModel.class);
        AuthenticationService authentication = this.getAuthenticationService();

        AccountDAO accountDao = new AccountDAO(sparql);
        accountDao.create(new URI(USER1_URI), new InternetAddress("user1@opensilex.org"), true, authentication.getPasswordHash("azerty"), OpenSilex.DEFAULT_LANGUAGE);
        accountDao.create(new URI(USER2_URI), new InternetAddress("user2@opensilex.org"), false, authentication.getPasswordHash("azerty"), OpenSilex.DEFAULT_LANGUAGE);

        ProfileDAO profileDao = new ProfileDAO(sparql);
        profileDao.create(new URI(PROFILE1_URI), "profile1", new ArrayList<>());
        profileDao.create(new URI(PROFILE2_URI), "profile2", new ArrayList<>());
    }

    protected GroupCreationDTO getGroupCreationDTO() throws URISyntaxException {

        GroupCreationDTO dto = new GroupCreationDTO();
        dto.setName("Group 1");
        dto.setDescription("Description 1");

        List<GroupUserProfileDTO> userProfiles = new ArrayList<>();
        GroupUserProfileModificationDTO userProfile = new GroupUserProfileModificationDTO();
        userProfile.setUserURI(new URI(USER1_URI));
        userProfile.setProfileURI(new URI(PROFILE1_URI));
        userProfiles.add(userProfile);

        userProfile = new GroupUserProfileModificationDTO();
        userProfile.setUserURI(new URI(USER2_URI));
        userProfile.setProfileURI(new URI(PROFILE2_URI));
        userProfiles.add(userProfile);

        dto.setUserProfiles(userProfiles);

        return dto;
    }

    @Test
    public void testCreate() throws Exception {
        createTestEnv();

        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getGroupCreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {
        createTestEnv();

        // create the user
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getGroupCreationDTO());

        // update the xp
        URI uri = extractUriFromResponse(postResult);
        GroupUpdateDTO dto = new GroupUpdateDTO();
        dto.setUri(uri);
        dto.setName("new group name");
        dto.setDescription("New description");

        List<GroupUserProfileDTO> userProfiles = new ArrayList<>();
        GroupUserProfileModificationDTO userProfile = new GroupUserProfileModificationDTO();

        userProfile = new GroupUserProfileModificationDTO();
        userProfile.setUserURI(new URI(USER1_URI));
        userProfile.setProfileURI(new URI(PROFILE2_URI));
        userProfiles.add(userProfile);

        dto.setUserProfiles(userProfiles);

        final Response putResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<GroupDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<GroupDTO>>() {
        });
        GroupDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        compareGroupsDTO(dto, dtoFromApi);
    }

    @Test
    public void testDelete() throws Exception {
        createTestEnv();
        // create object and check if URI exists
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getGroupCreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponseAsAdmin(target(getPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearch() throws Exception {
        createTestEnv();
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getGroupCreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        GroupCreationDTO dto = new GroupCreationDTO();
        dto.setName("Group 2");
        dto.setDescription("Description 2");

        List<GroupUserProfileDTO> userProfiles = new ArrayList<>();
        GroupUserProfileModificationDTO userProfile = new GroupUserProfileModificationDTO();
        userProfile.setUserURI(new URI(USER1_URI));
        userProfile.setProfileURI(new URI(PROFILE2_URI));
        userProfiles.add(userProfile);

        userProfile = new GroupUserProfileModificationDTO();
        userProfile.setUserURI(new URI(USER2_URI));
        userProfile.setProfileURI(new URI(PROFILE1_URI));
        userProfiles.add(userProfile);

        dto.setUserProfiles(userProfiles);

        postResult = getJsonPostResponseAsAdmin(target(createPath), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("name", "Group.*");
            }
        };

        WebTarget target = appendSearchParams(target(searchPath), 0, 50, params);
        Response getSearchResult = appendAdminToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getSearchResult.getStatus());

        JsonNode node = getSearchResult.readEntity(JsonNode.class);
        PaginatedListResponse<GroupDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GroupDTO>>() {
        });
        List<GroupDTO> users = listResponse.getResult();

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());

        params.put("name", "Group 2");
        target = appendSearchParams(target(searchPath), 0, 50, params);
        getSearchResult = appendAdminToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getSearchResult.getStatus());

        node = getSearchResult.readEntity(JsonNode.class);
        listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GroupDTO>>() {
        });
        users = listResponse.getResult();
        assertEquals(1, users.size());
    }

    private void compareGroupsDTO(GroupUpdateDTO expectedGroupDTO, GroupDTO actualGroupDTO) {
        assertEquals(expectedGroupDTO.getUri(), actualGroupDTO.getUri());
        assertEquals(expectedGroupDTO.getName(), actualGroupDTO.getName());
        assertEquals(expectedGroupDTO.getUserProfiles().size(), actualGroupDTO.getUserProfiles().size());

        Map<String, String> userProfilesExpected = new HashMap<>();
        expectedGroupDTO.getUserProfiles().forEach((userProfile) -> {
            userProfilesExpected.put(
                    userProfile.getUserURI().toString(),
                    userProfile.getProfileURI().toString()
            );
        });

        Map<String, String> userProfilesActual = new HashMap<>();
        expectedGroupDTO.getUserProfiles().forEach((userProfile) -> {
            userProfilesActual.put(
                    userProfile.getUserURI().toString(),
                    userProfile.getProfileURI().toString()
            );
        });

        assertTrue(compareMaps(userProfilesExpected, userProfilesActual));
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        List<Class<? extends SPARQLResourceModel>> modelList = new ArrayList<>();
        modelList.add(GroupModel.class);
        modelList.add(GroupUserProfileModel.class);
        modelList.add(ProfileModel.class);
        modelList.add(AccountModel.class);
        return modelList;
    }

    @Override
    public void afterEach() throws Exception {
        getOpensilex().getModuleByClass(SecurityModule.class).createDefaultSuperAdmin();
    }
}
