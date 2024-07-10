package org.opensilex.core.group;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.group.api.*;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

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

        testBasicCRUDAsAdmin(
                create, get, update, delete,
                getGroupCreationDTO(), getGroupUpdateDTO(),
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

    @Test
    public void testSearch() throws Exception {
        createTestEnv();

        new UserCallBuilder(create)
                .setBody(getGroupCreationDTO())
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CREATED);

        GroupCreationDTO dto = getGroupCreationDTO();
        dto.setName("Group 2");
        dto.setDescription("Description 2");

        GroupUserProfileModificationDTO userProfile = new GroupUserProfileModificationDTO();
        userProfile.setUserURI(new URI(USER2_URI));
        userProfile.setProfileURI(new URI(PROFILE1_URI));

        dto.getUserProfiles().add(
            userProfile
        );

        new UserCallBuilder(create)
                .setBody(dto)
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CREATED);

        PaginatedListResponse<GroupDTO> listResponse = new UserCallBuilder(search).addParam("name", "Group.*")
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<GroupDTO>>() {})
                .getDeserializedResponse();
        List<GroupDTO> users = listResponse.getResult();

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());

        PaginatedListResponse<GroupDTO> listResponse2 = new UserCallBuilder(search).addParam("name", "Group 2")
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<GroupDTO>>() {})
                .getDeserializedResponse();
        List<GroupDTO> users2 = listResponse2.getResult();
        assertEquals(1, users2.size());
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
