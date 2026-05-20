package org.opensilex.core.annotation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.vocabulary.OA;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.group.GroupAPITest;
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
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.URIEquator;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnnotationAccessAPITest extends AbstractMongoIntegrationTest {

    private static final String USER_1_MAIL = "user@example.org";
    private static final String USER_1_PASSWORD = "password";

    private URI privateExperimentUri;
    private URI publicExperimentUri;
    private URI user1Uri;

    private final AccountDAO accountDAO = new AccountDAO(getSparqlService());

    //#region Helper functions for resource creation
    private ExperimentCreationDTO makeExperimentCreationDTO(String name, boolean isPublic) {
        var dto = new ExperimentCreationDTO();
        dto.setName(name);
        dto.setStartDate(LocalDate.parse("2026-04-27"));
        dto.setObjective("Test experiment for document access : " + name);
        dto.setIsPublic(isPublic);
        return dto;
    }

    private AccountCreationDTO makeAccountCreationDTO() {
        var dto = new AccountCreationDTO();
        dto.setPassword(USER_1_PASSWORD);
        dto.setEmail(USER_1_MAIL);
        dto.setLanguage(OpenSilex.DEFAULT_LANGUAGE);
        return dto;
    }

    private ProfileCreationDTO makeProfileCreationDTO(List<String> credentials) {
        var dto = new ProfileCreationDTO();
        dto.setName("Test profile");
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

    private AnnotationCreationDTO makeAnnotationCreationDTO(String description, List<URI> targets) {
        var dto = new AnnotationCreationDTO();
        dto.setTargets(targets);
        dto.setMotivation(URI.create(OA.describing.getURI()));
        dto.setDescription(description);
        return dto;
    }

    /**
     * Calls a POST service to create a resource for test setup. For resource creation testing, please directly create a
     * {@link org.opensilex.integration.test.security.AbstractSecurityIntegrationTest.UserCall}.
     */
    private URI post(ServiceDescription createService, Object body) throws Exception {
        return new UserCallBuilder(createService)
                .setBody(body)
                .buildAdmin()
                .executeCallAndReturnURI();
    }
    //#endregion

    //#region Setup and cleanup
    @Before
    public void beforeTest() throws Exception {
        privateExperimentUri = post(ExperimentAPITest.create, makeExperimentCreationDTO("Private experiment", false));
        publicExperimentUri = post(ExperimentAPITest.create, makeExperimentCreationDTO("Public experiment", true));
        user1Uri = post(AccountAPITest.create, makeAccountCreationDTO());
        var profileUri = post(ProfileAPITest.create, makeProfileCreationDTO(List.of()));
        post(GroupAPITest.create, makeGroupCreationDTO(Map.of(user1Uri, profileUri)));
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        accountDAO.delete(user1Uri, getOpensilex());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(
                GroupUserProfileModel.class,
                GroupModel.class,
                ProfileModel.class,
                ExperimentModel.class,
                AnnotationModel.class
        );
    }
    //#endregion

    //#region Tests
    @Test
    public void testSearchRestrictedAnnotationAsUser() throws Exception {
        post(AnnotationAPITest.create, makeAnnotationCreationDTO("Private annotation", List.of(privateExperimentUri)));
        var publicAnnotationUri = post(AnnotationAPITest.create, makeAnnotationCreationDTO("Public annotation", List.of(publicExperimentUri)));

        var result = new UserCallBuilder(AnnotationAPITest.search)
                .setUserEmail(USER_1_MAIL)
                .setUserPassword(USER_1_PASSWORD)
                .build()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<AnnotationGetDTO>>() {
                });
        assertEquals(200, result.getResponse().getStatus());

        var response = result.getDeserializedResponse();
        assertEquals(1, response.getMetadata().getPagination().getTotalCount());

        var annotationUris = response.getResult().stream().map(AnnotationGetDTO::getUri).toList();
        assertTrue(CollectionUtils.isEqualCollection(
                List.of(publicAnnotationUri),
                annotationUris,
                new URIEquator()
        ));
    }

    @Test
    public void testSearchRestrictedAnnotationAsAdmin() throws Exception {
        var privateAnnotationUri = post(AnnotationAPITest.create, makeAnnotationCreationDTO("Private annotation", List.of(privateExperimentUri)));
        var publicAnnotationUri = post(AnnotationAPITest.create, makeAnnotationCreationDTO("Public annotation", List.of(publicExperimentUri)));

        var result = new UserCallBuilder(AnnotationAPITest.search)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<AnnotationGetDTO>>() {
                });
        assertEquals(200, result.getResponse().getStatus());

        var response = result.getDeserializedResponse();
        assertEquals(2, response.getMetadata().getPagination().getTotalCount());

        var annotationUris = response.getResult().stream().map(AnnotationGetDTO::getUri).toList();
        assertTrue(CollectionUtils.isEqualCollection(
                List.of(publicAnnotationUri, privateAnnotationUri),
                annotationUris,
                new URIEquator()
        ));
    }
    //#endregion
}
