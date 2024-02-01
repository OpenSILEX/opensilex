package org.opensilex.security.group.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class GroupAPITest extends AbstractSecurityIntegrationTest {

    public String path = "/security/groups";
    public ServiceDescription create = new ServiceDescription(
            GroupAPI.class.getMethod("createGroup", GroupCreationDTO.class),
            path
    );
    public ServiceDescription update = new ServiceDescription(
            GroupAPI.class.getMethod("updateGroup", GroupUpdateDTO.class),
            path
    );
    public ServiceDescription get = new ServiceDescription(
            GroupAPI.class.getMethod("getGroup", URI.class),
            path + "/{uri}"
    );
    public ServiceDescription delete = new ServiceDescription(
            GroupAPI.class.getMethod("deleteGroup", URI.class),
            path + "/{uri}"
    );
    public ServiceDescription search = new ServiceDescription(
            GroupAPI.class.getMethod("searchGroups", String.class, List.class, int.class, int.class),
            path
    );
    public String searchPath = path;

    private final static String USER1_URI = "http://example.org/users/user1";
    private final static String USER2_URI = "http://example.org/users/user2";
    private final static String PROFILE1_URI = "http://example.org/profiles/profile1";
    private final static String PROFILE2_URI = "http://example.org/profiles/profile2";

    public GroupAPITest() throws NoSuchMethodException {
    }

    protected void createTestEnv() throws Exception {
        SPARQLService sparql = this.getSparqlService();
        sparql.getDefaultGraph(GroupModel.class);
        AuthenticationService authentication = this.getAuthenticationService();

        AccountDAO accountDao = new AccountDAO(sparql);
        accountDao.create(new URI(USER1_URI), new InternetAddress("user1@opensilex.org"), true, authentication.getPasswordHash("azerty"), OpenSilex.DEFAULT_LANGUAGE, null, null);
        accountDao.create(new URI(USER2_URI), new InternetAddress("user2@opensilex.org"), false, authentication.getPasswordHash("azerty"), OpenSilex.DEFAULT_LANGUAGE, null, null);

        ProfileDAO profileDao = new ProfileDAO(sparql);
        profileDao.create(new URI(PROFILE1_URI), "profile1", new ArrayList<>(), new URI(USER1_URI));
        profileDao.create(new URI(PROFILE2_URI), "profile2", new ArrayList<>(), new URI(USER2_URI));
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
    public void testGroupBasicCRUDAsAdmin() throws Exception {
        createTestEnv();

        Map<String, Object> attributesMap = convertToNestedMap(getGroupGetDTO());
        attributesMap.values().removeAll(Collections.singleton(null));
        testBasicCRUDAsAdmin(
                create, get, update, delete,
                getGroupCreationDTO(), getGroupUpdateDTO(),
                attributesMap,
                new TypeReference<SingleObjectResponse<GroupGetDTO>>() {
                }
        );
    }

    private static GroupUpdateDTO getGroupUpdateDTO() throws URISyntaxException {
        GroupUpdateDTO dto = new GroupUpdateDTO();
        dto.setName("new group name");
        dto.setDescription("New description");

        List<GroupUserProfileDTO> userProfiles = new ArrayList<>();
        GroupUserProfileModificationDTO userProfile = new GroupUserProfileModificationDTO();

        userProfile.setUserURI(new URI(USER1_URI));
        userProfile.setProfileURI(new URI(PROFILE2_URI));
        userProfiles.add(userProfile);

        dto.setUserProfiles(userProfiles);
        return dto;
    }

    private static GroupGetDTO getGroupGetDTO() throws URISyntaxException {
        GroupGetDTO dto = new GroupGetDTO();
        dto.setName("new group name");
        dto.setDescription("New description");

        List<GroupUserProfileDTO> userProfiles = new ArrayList<>();
        GroupUserProfileModificationDTO userProfile = new GroupUserProfileModificationDTO();

        userProfile.setUserURI(new URI(USER1_URI));
        userProfile.setProfileURI(new URI(PROFILE2_URI));
        userProfiles.add(userProfile);

        dto.setUserProfiles(userProfiles);
        return dto;
    }

    /*@Test
    public void testSearch() throws Exception {
        createTestEnv();

        new UserCallBuilder(create).setBody(getGroupCreationDTO()).buildAdmin().executeCallAndReturnURI();

        GroupCreationDTO dto = getCreationDTO();

        new UserCallBuilder(create).setBody(dto).buildAdmin().executeCallAndReturnURI();

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
    }*/

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
