package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.group.GroupAPITest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.security.account.api.AccountAPITest;
import org.opensilex.security.account.api.AccountCreationDTO;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.group.api.GroupCreationDTO;
import org.opensilex.security.group.api.GroupUserProfileDTO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.api.ProfileAPITest;
import org.opensilex.security.profile.api.ProfileCreationDTO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.URIEquator;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class GermplasmAccessApiTest extends AbstractMongoIntegrationTest {
    private record User(String mail, String password) {}
    private static final User USER_1 = new User("user1@example.org", "user1");
    private static final User USER_2 = new User("user2@example.org", "user2");

    private URI user1Uri = null;
    private URI user2Uri = null;
    private URI groupUri = null;
    private final AccountDAO accountDAO = new AccountDAO(getSparqlService());



    private static final String GERMPLASM_ADMIN_PUBLIC_ATTRIBUTE = "Germplasm Admin Public Attribute";
    private URI germplasmAdminPublicUri = null;
    private static final String GERMPLASM_ADMIN_PRIVATE_ATTRIBUTE = "Germplasm Admin Private Attribute";
    private URI germplasmAdminPrivateUri = null;
    private static final String GERMPLASM_ADMIN_PRIVATE_IN_GROUP_ATTRIBUTE = "Germplasm Admin Private In Group Attribute";
    private URI germplasmAdminPrivateInGroupUri = null;
    private static final String GERMPLASM_USER_1_PUBLIC_ATTRIBUTE = "Germplasm User 1 Public Attribute";
    private URI germplasmUser1PublicUri = null;
    private static final String GERMPLASM_USER_1_PRIVATE_ATTRIBUTE = "Germplasm User 1 Private Attribute";
    private URI germplasmUser1PrivateUri = null;
    private static final String GERMPLASM_USER_1_PRIVATE_IN_GROUP_ATTRIBUTE = "Germplasm User 1 Private In Group Attribute";
    private URI germplasmUser1PrivateInGroupUri = null;
    private static final String GERMPLASM_USER_2_PUBLIC_ATTRIBUTE = "Germplasm User 2 Public Attribute";
    private URI germplasmUser2PublicUri = null;
    private static final String GERMPLASM_USER_2_PRIVATE_ATTRIBUTE = "Germplasm User 2 Private Attribute";
    private URI germplasmUser2PrivateUri = null;
    private static final String GERMPLASM_USER_2_PRIVATE_IN_GROUP_ATTRIBUTE = "Germplasm User 2 Private In Group Attribute";
    private URI germplasmUser2PrivateInGroupUri = null;

    //#region Helper function for resource creation
    private AccountCreationDTO makeAccountCreationDTO(User user) {
        var dto = new AccountCreationDTO();
        dto.setPassword(user.password);
        dto.setEmail(user.mail);
        dto.setLanguage(OpenSilex.DEFAULT_LANGUAGE);
        return dto;
    }

    private ProfileCreationDTO makeProfileCreationDTO(List<String> credentials) {
        var dto = new ProfileCreationDTO();
        dto.setName("Germplasm creator");
        dto.setCredentials(credentials);
        return dto;
    }

    private GroupCreationDTO makeGroupCreationDTO(Map<URI, URI> userProfiles) {
        var dto = new GroupCreationDTO();
        dto.setName("Test group");
        dto.setDescription("Test group description");
        dto.setUserProfiles(userProfiles.entrySet().stream().map((userProfile) -> {
            var userProfileDto = new GroupUserProfileDTO();
            userProfileDto.setUserURI(userProfile.getKey());
            userProfileDto.setProfileURI(userProfile.getValue());
            return userProfileDto;
        }).toList());
        return dto;
    }

    private GermplasmCreationDTO makeGermplasmCreationDTO(String name, Map<String, String> attributes, List<URI> groups, boolean isPublic) {
        var dto = new GermplasmCreationDTO();
        dto.setName(name);
        dto.setRdfType(URI.create(Oeso.Species.getURI()));
        dto.setIsPublic(isPublic);
        dto.setGroups(groups);
        dto.setMetadata(attributes);
        return dto;
    }

    /**
     * Calls a POST service to create a resource for test setup. For resource creation testing, please directly create a
     * {@link org.opensilex.integration.test.security.AbstractSecurityIntegrationTest.UserCall}.
     */
    private URI post(ServiceDescription createService, Object body, User user) throws Exception {
        var builder = new UserCallBuilder(createService)
                .setBody(body);

        UserCall call;
        if (user == null) {
            call = builder.buildAdmin();
        } else {
            call = builder.setUserEmail(user.mail)
                    .setUserPassword(user.password)
                    .build();
        }

        return call.executeCallAndReturnURI();
    }
    //#endregion

    //#region Setup
    @Before
    public void beforeTest() throws Exception {
        user1Uri = post(AccountAPITest.create, makeAccountCreationDTO(USER_1), null);
        user2Uri = post(AccountAPITest.create, makeAccountCreationDTO(USER_2), null);
        var profileUri = post(ProfileAPITest.create, makeProfileCreationDTO(List.of(GermplasmAPI.CREDENTIAL_GERMPLASM_MODIFICATION_ID)), null);
        groupUri = post(GroupAPITest.create, makeGroupCreationDTO(Map.of(user1Uri, profileUri, user2Uri, profileUri)), null);

        createGermplasms();
    }

    private void createGermplasms() throws Exception {
        germplasmAdminPublicUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm Admin Public", Map.of(GERMPLASM_ADMIN_PUBLIC_ATTRIBUTE, ""), List.of(), true), null);
        germplasmAdminPrivateUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm Admin Private", Map.of(GERMPLASM_ADMIN_PRIVATE_ATTRIBUTE, ""), List.of(), false), null);
        germplasmAdminPrivateInGroupUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm Admin Private In Group", Map.of(GERMPLASM_ADMIN_PRIVATE_IN_GROUP_ATTRIBUTE, ""), List.of(groupUri), false), null);
        germplasmUser1PublicUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm User Public", Map.of(GERMPLASM_USER_1_PUBLIC_ATTRIBUTE, ""), List.of(), true), USER_1);
        germplasmUser1PrivateUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm User Private", Map.of(GERMPLASM_USER_1_PRIVATE_ATTRIBUTE, ""), List.of(), false), USER_1);
        germplasmUser1PrivateInGroupUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm User Private In Group", Map.of(GERMPLASM_USER_1_PRIVATE_IN_GROUP_ATTRIBUTE, ""), List.of(groupUri), false), USER_1);
        germplasmUser2PublicUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm User Public", Map.of(GERMPLASM_USER_2_PUBLIC_ATTRIBUTE, ""), List.of(), true), USER_2);
        germplasmUser2PrivateUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm User Private", Map.of(GERMPLASM_USER_2_PRIVATE_ATTRIBUTE, ""), List.of(), false), USER_2);
        germplasmUser2PrivateInGroupUri = post(BaseGermplasmAPITest.create, makeGermplasmCreationDTO(
                "Germplasm User Private In Group", Map.of(GERMPLASM_USER_2_PRIVATE_IN_GROUP_ATTRIBUTE, ""), List.of(groupUri), false), USER_2);
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        // Delete all attributes so that we can delete users
        getMongoDBService().getServiceV2().getDatabase().getCollection(GermplasmDAO.ATTRIBUTES_COLLECTION_NAME).deleteMany(new Document());
        // Must manually delete users to keep the admin account between tests
        accountDAO.delete(user1Uri, getOpensilex());
        accountDAO.delete(user2Uri, getOpensilex());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(
                GroupUserProfileModel.class,
                GroupModel.class,
                ProfileModel.class,
                GermplasmModel.class
        );
    }
    //#endregion

    //#region Tests
    @Test
    public void testAdminListAllGermplasms() throws Exception {
        var result = new UserCallBuilder(BaseGermplasmAPITest.search)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {});
        assertEquals(200, result.getResponse().getStatus());

        var response = result.getDeserializedResponse();
        assertEquals(9, response.getMetadata().getPagination().getTotalCount());

        var germplasmUris = response.getResult().stream().map(GermplasmGetAllDTO::getUri).toList();
        assertTrue(CollectionUtils.isEqualCollection(
                List.of(germplasmAdminPublicUri, germplasmAdminPrivateUri, germplasmAdminPrivateInGroupUri,
                        germplasmUser1PublicUri, germplasmUser1PrivateUri, germplasmUser1PrivateInGroupUri,
                        germplasmUser2PublicUri, germplasmUser2PrivateUri, germplasmUser2PrivateInGroupUri),
                germplasmUris,
                new URIEquator()
        ));
    }

    @Test
    public void testAdminListAllGermplasmAttributes() throws Exception {
        var result = new UserCallBuilder(BaseGermplasmAPITest.getAttributes)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<List<String>>>() {});
        assertEquals(200, result.getResponse().getStatus());

        var germplasmAttributes = result.getDeserializedResponse().getResult();
        assertTrue(CollectionUtils.isEqualCollection(
                List.of(GERMPLASM_ADMIN_PUBLIC_ATTRIBUTE, GERMPLASM_ADMIN_PRIVATE_ATTRIBUTE, GERMPLASM_ADMIN_PRIVATE_IN_GROUP_ATTRIBUTE,
                        GERMPLASM_USER_1_PUBLIC_ATTRIBUTE, GERMPLASM_USER_1_PRIVATE_ATTRIBUTE, GERMPLASM_USER_1_PRIVATE_IN_GROUP_ATTRIBUTE,
                        GERMPLASM_USER_2_PUBLIC_ATTRIBUTE, GERMPLASM_USER_2_PRIVATE_ATTRIBUTE, GERMPLASM_USER_2_PRIVATE_IN_GROUP_ATTRIBUTE),
                germplasmAttributes
        ));
    }

    @Test
    public void testUserGetOwnGermplasm() throws Exception {
        var result = new UserCallBuilder(BaseGermplasmAPITest.get)
                .setUriInPath(germplasmUser2PrivateUri)
                .setUserEmail(USER_2.mail)
                .setUserPassword(USER_2.password)
                .build()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<GermplasmGetSingleDTO>>() {});
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void testUserGetPublicGermplasm() throws Exception {
        var result = new UserCallBuilder(BaseGermplasmAPITest.get)
                .setUriInPath(germplasmUser1PublicUri)
                .setUserEmail(USER_2.mail)
                .setUserPassword(USER_2.password)
                .build()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<GermplasmGetSingleDTO>>() {});
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void testUserGetGermplasmInGroup() throws Exception {
        var result = new UserCallBuilder(BaseGermplasmAPITest.get)
                .setUriInPath(germplasmUser1PrivateInGroupUri)
                .setUserEmail(USER_2.mail)
                .setUserPassword(USER_2.password)
                .build()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<GermplasmGetSingleDTO>>() {});
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void testUserGetPrivateGermplasmShouldFail() throws Exception {
        try (var result = new UserCallBuilder(BaseGermplasmAPITest.get)
                .setUriInPath(germplasmUser2PrivateUri)
                .setUserEmail(USER_2.mail)
                .setUserPassword(USER_2.password)
                .build()
                .executeCall()) {
            assertEquals(200, result.getStatus());
        }
    }

    @Test
    public void testUserListAuthorizedGermplasms() throws Exception {
        var result = new UserCallBuilder(BaseGermplasmAPITest.search)
                .setUserEmail(USER_2.mail)
                .setUserPassword(USER_2.password)
                .build()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {});
        assertEquals(200, result.getResponse().getStatus());

        var response = result.getDeserializedResponse();
        assertEquals(7, response.getMetadata().getPagination().getTotalCount());

        var germplasmUris = response.getResult().stream().map(GermplasmGetAllDTO::getUri).toList();
        assertTrue(CollectionUtils.isEqualCollection(
                List.of(germplasmAdminPublicUri, germplasmAdminPrivateInGroupUri,
                        germplasmUser1PublicUri, germplasmUser1PrivateInGroupUri,
                        germplasmUser2PublicUri, germplasmUser2PrivateUri, germplasmUser2PrivateInGroupUri),
                germplasmUris,
                new URIEquator()
        ));
    }

    @Test
    public void testUserListAuthorizedGermplasmAttributes() throws Exception {
        var result = new UserCallBuilder(BaseGermplasmAPITest.getAttributes)
                .setUserEmail(USER_2.mail)
                .setUserPassword(USER_2.password)
                .build()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<List<String>>>() {});
        assertEquals(200, result.getResponse().getStatus());

        var germplasmAttributes = result.getDeserializedResponse().getResult();
        assertTrue(CollectionUtils.isEqualCollection(
                List.of(GERMPLASM_ADMIN_PUBLIC_ATTRIBUTE, GERMPLASM_ADMIN_PRIVATE_IN_GROUP_ATTRIBUTE,
                        GERMPLASM_USER_1_PUBLIC_ATTRIBUTE, GERMPLASM_USER_1_PRIVATE_IN_GROUP_ATTRIBUTE,
                        GERMPLASM_USER_2_PUBLIC_ATTRIBUTE, GERMPLASM_USER_2_PRIVATE_ATTRIBUTE, GERMPLASM_USER_2_PRIVATE_IN_GROUP_ATTRIBUTE),
                germplasmAttributes
        ));
    }
    //#endregion
}
