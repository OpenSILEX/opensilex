package org.opensilex.core.document.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
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
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.URIEquator;

import javax.ws.rs.core.MediaType;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocumentAccessAPITest extends AbstractMongoIntegrationTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

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
        dto.setPassword(DocumentAccessAPITest.USER_1_PASSWORD);
        dto.setEmail(DocumentAccessAPITest.USER_1_MAIL);
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

    private DocumentCreationDTO makeCreationDocumentDTO(String title, List<URI> target) {
        var dto = new DocumentCreationDTO();
        dto.setType(URI.create(Oeso.Document.getURI()));
        dto.setTitle(title);
        dto.setTargets(target);
        return dto;
    }

    private MultiPart makeMultipart(DocumentCreationDTO creationDTO) throws IOException {
        var file = tmpFolder.newFile();
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("test".getBytes());
        }
        var fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);
        try (var multipart = new FormDataMultiPart()) {
            return multipart
                    .field("description", creationDTO, MediaType.APPLICATION_JSON_TYPE)
                    .bodyPart(fileDataBodyPart);
        }
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

    private URI postMultipart(Object body) throws Exception {
        return new UserCallBuilder(DocumentAPITest.create)
                .setMultipartBody(body)
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
                DocumentModel.class
        );
    }
    //#endregion

    //#region Tests
    @Test
    public void testSearchRestrictedDocumentAsUser() throws Exception {
        postMultipart(makeMultipart(makeCreationDocumentDTO("Private document", List.of(privateExperimentUri))));
        var publicDocumentUri = postMultipart(makeMultipart(makeCreationDocumentDTO("Public document", List.of(publicExperimentUri))));

        var result = new UserCallBuilder(DocumentAPITest.search)
                .setUserEmail(USER_1_MAIL)
                .setUserPassword(USER_1_PASSWORD)
                .build()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<DocumentGetDTO>>() {
                });
        assertEquals(200, result.getResponse().getStatus());

        var response = result.getDeserializedResponse();
        assertEquals(1, response.getMetadata().getPagination().getTotalCount());

        var documentUris = response.getResult().stream().map(DocumentDTO::getUri).toList();
        assertTrue(CollectionUtils.isEqualCollection(
                List.of(publicDocumentUri),
                documentUris,
                new URIEquator()
        ));
    }

    @Test
    public void testSearchRestrictedDocumentAsAdmin() throws Exception {
        var privateDocumentUri = postMultipart(makeMultipart(makeCreationDocumentDTO("Private document", List.of(privateExperimentUri))));
        var publicDocumentUri = postMultipart(makeMultipart(makeCreationDocumentDTO("Public document", List.of(publicExperimentUri))));

        var result = new UserCallBuilder(DocumentAPITest.search)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<DocumentGetDTO>>() {
                });
        assertEquals(200, result.getResponse().getStatus());

        var response = result.getDeserializedResponse();
        assertEquals(2, response.getMetadata().getPagination().getTotalCount());

        var documentUris = response.getResult().stream().map(DocumentDTO::getUri).toList();
        assertTrue(CollectionUtils.isEqualCollection(
                List.of(publicDocumentUri, privateDocumentUri),
                documentUris,
                new URIEquator()
        ));
    }
    //#endregion
}
