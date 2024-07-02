package org.opensilex.core;


import org.junit.Test;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ActivityModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.util.List;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

}