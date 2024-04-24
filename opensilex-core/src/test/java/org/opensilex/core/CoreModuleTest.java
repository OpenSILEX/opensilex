package org.opensilex.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Test;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ActivityModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.api.TokenGetDTO;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opensilex.core.CoreModule.*;
import static org.opensilex.sparql.deserializer.SPARQLDeserializers.getShortURI;

public class CoreModuleTest extends AbstractMongoIntegrationTest {

    private final CoreModule coreModule;
    private final AccountDAO accountDAO;
    private final ProvenanceDAO provenanceDAO;
    private int accountCount = 0;

    public CoreModuleTest() throws OpenSilexModuleNotFoundException {
        coreModule = getOpensilex().getModuleByClass(CoreModule.class);
        accountDAO = new AccountDAO(getSparqlService());
        provenanceDAO = new ProvenanceDAO(getMongoDBService(), getSparqlService());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(ExperimentModel.class);
    }

    @Test
    public void accountIsLinkedWithANosqlEntity_return_false_if_user_created_nothing() throws Exception {
        // creating an account and linking it to a provenance
        AccountModel provenancePublisher = createNewAccount();
        createNewProvenance(provenancePublisher.getUri());

        // creating a brand-new account with no link with other resources
        AccountModel accountWithoutLink = createNewAccount();

        //method should return false as "accountWithoutLink" is not link with any other resource
        assertFalse( coreModule.accountIsLinkedWithANosqlEntity(accountWithoutLink.getUri()) );
    }

    @Test
    public void accountIsLinkedWithANosqlEntity_return_true_if_user_created_provenance() throws Exception {
        // creating an account and linking it to a provenance
        AccountModel provenancePublisher = createNewAccount();

        createNewProvenance(provenancePublisher.getUri());

        // method should return true as this account is linked with a provenance (stored in MongoDB )
        assertTrue( coreModule.accountIsLinkedWithANosqlEntity(provenancePublisher.getUri()) );
    }

    @Test
    public void accountIsLinkedWithANosqlEntity_return_false_if_user_create_and_delete_a_provenance() throws Exception {
        // creating an account and linking it to a provenance then delete it
        AccountModel provenancePublisher = createNewAccount();

        ProvenanceModel provenance = createNewProvenance(provenancePublisher.getUri());

        provenanceDAO.delete(provenance.getUri());

        // method should return true as this account is linked with a provenance (stored in MongoDB )
        assertFalse( coreModule.accountIsLinkedWithANosqlEntity(provenancePublisher.getUri()) );
    }

    private AccountModel createNewAccount() throws Exception {
        accountCount ++;
        return accountDAO.create(
                null,
                new InternetAddress("test@test.test"+ accountCount),
                true,
                "pwd",
                getOpensilex().getDefaultLanguage()
        );
    }

    private ProvenanceModel createNewProvenance(URI provenancePublisherUri) throws Exception {
        ActivityModel activityModel = new ActivityModel();
        activityModel.setRdfType(URI.create(Oeso.ImageAnalysis.toString()));

        ProvenanceModel provenanceModel = new ProvenanceModel();
        provenanceModel.setPublisher(provenancePublisherUri);
        provenanceModel.setName("name");
        provenanceModel.setDescription("description");
        provenanceModel.setActivity(List.of(activityModel));
        return provenanceDAO.create(provenanceModel);
    }

    @Test
    public void testAddExperimentToToken() throws Exception {
        // Create an experiment and obtain the token
        final Response postResultExp = getJsonPostResponseAsAdmin(target(ExperimentAPITest.createPath), ExperimentAPITest.getCreationDTO());
        URI createdUri = extractUriFromResponse(postResultExp);

        TokenGetDTO token = queryAdminToken();

        // Decode the JWT token and retrieve the list of experiments
        DecodedJWT decodedToken = JWT.decode(token.getToken());
        List<String> expeClaimValueList = decodedToken.getClaim(EXPERIMENT_LIST_JWT_CLAIM).asList(String.class);

        // Check if the URI of the created experiment is present in the list of experiments in the token
        assertTrue("The created experiment is not present in the list of experiments in the token",
                expeClaimValueList.contains(getShortURI(createdUri)));
    }


    @Test
    public void testDeleteExperimentFromToken() throws Exception {
        // Create an experiment and obtain the token
        final Response postResultExp = getJsonPostResponseAsAdmin(target(ExperimentAPITest.createPath), ExperimentAPITest.getCreationDTO());
        String createdUri = extractUriFromResponse(postResultExp).toString();

        // Get the token before deletion
        TokenGetDTO tokenBeforeDeletion = queryAdminToken();
        DecodedJWT decodedTokenBeforeDeletion = JWT.decode(tokenBeforeDeletion.getToken());
        List<String> expeClaimValueListBeforeDeletion = decodedTokenBeforeDeletion.getClaim(EXPERIMENT_LIST_JWT_CLAIM).asList(String.class);

        // Check that the URI of the created experiment is present in the list of experiments in the token
        assertTrue("The created experiment is not present in the list of experiments in the token",
                expeClaimValueListBeforeDeletion.contains(getShortURI(createdUri)));

        // Delete the experiment
        Response delResult = getDeleteByUriResponse(target(ExperimentAPITest.deletePath), createdUri);

        // Get the token after deleting the experiment
        TokenGetDTO tokenAfterDeletion = queryAdminToken();

        // Check that the URI of the deleted experiment is no longer present in the list of experiments in the token
        DecodedJWT decodedTokenAfterDeletion = JWT.decode(tokenAfterDeletion.getToken());
        List<String> expeClaimValueListAfterDeletion = decodedTokenAfterDeletion.getClaim(EXPERIMENT_LIST_JWT_CLAIM).asList(String.class);
        assertFalse("The deleted experiment is still present in the list of experiments in the token",
                expeClaimValueListAfterDeletion.contains(getShortURI(createdUri)));
    }

    @Test
    public void testExperimentsExceedLimitInToken() throws Exception {
        // Create a list of experiments exceeding the maximum limit
        List<ExperimentCreationDTO> creationDTOs = new ArrayList<>();
        for (int i = 0; i <= MAX_EXPERIMENTS; i++) {
            ExperimentCreationDTO creationDTO = ExperimentAPITest.getCreationDTO();
            creationDTOs.add(creationDTO);
        }
        for (ExperimentCreationDTO creationDTO : creationDTOs) {
            final Response postResult = getJsonPostResponseAsAdmin(target(ExperimentAPITest.createPath), creationDTO);
        }

        TokenGetDTO token = queryAdminToken();
        DecodedJWT decodedToken = JWT.decode(token.getToken());

        // Check if EXPERIMENTS_EXCEED_LIMIT_CLAIM is present in the token
        Boolean experimentsExceedLimitClaim = decodedToken.getClaim(EXPERIMENTS_EXCEED_LIMIT_CLAIM).asBoolean();
        assertTrue("The EXPERIMENTS_EXCEED_LIMIT_CLAIM is not present in the token",
                experimentsExceedLimitClaim != null && experimentsExceedLimitClaim);

        // Check if EXPERIMENT_LIST_JWT_CLAIM is not present in the token
        List<String> experimentListClaim = decodedToken.getClaim(EXPERIMENT_LIST_JWT_CLAIM).asList(String.class);
        assertTrue("The EXPERIMENT_LIST_JWT_CLAIM is present in the token",
                experimentListClaim == null || experimentListClaim.isEmpty());
    }
}