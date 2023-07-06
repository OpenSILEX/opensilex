package org.opensilex.dataverse;

import com.researchspace.dataverse.entities.facade.DatasetAuthor;
import com.researchspace.dataverse.entities.facade.DatasetContact;
import com.researchspace.dataverse.entities.facade.DatasetFacade;
import org.junit.Test;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.dataverse.api.DataverseAPIPostDatasetDTO;
import org.opensilex.dataverse.api.DataverseClient;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.rest.validation.model.OpenSilexLocale;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DataverseClientTest {

    @Test
    public void testCreateAFacade() throws ParseException, URISyntaxException, AddressException, MalformedURLException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date date = format.parse("2020-02-20");
        ExperimentModel experimentModel = new ExperimentModel();
        experimentModel.setDescription("Tests_dataset_description");
        experimentModel.setUri(new URI("test:test_uri"));
        experimentModel.setObjective("Tests_dataset_objective");
        PersonModel currentPerson = new PersonModel();
        currentPerson.setFirstName("Tests_first_name");
        currentPerson.setLastName("Tests_last_name");
        AccountModel currentAccount = new AccountModel();
        currentAccount.setHolderOfTheAccount(currentPerson);
        List<PersonModel> datasetAuthors = Arrays.asList(
                new PersonModel(),
                new PersonModel()
        );
        for (PersonModel person: datasetAuthors) {
            person.setFirstName("Tests_first_name");
            person.setLastName("Tests_last_name");
            person.setEmail(new InternetAddress("Tests_email@opensilex.org"));
        }
        List<PersonModel> datasetContacts = Arrays.asList(
                new PersonModel(),
                new PersonModel()
        );
        for (PersonModel person: datasetContacts) {
            person.setFirstName("Tests_first_name");
            person.setLastName("Tests_last_name");
            person.setEmail(new InternetAddress("Tests_email@opensilex.org"));
        }
        String datasetTitle = "Tests_dataset_title";
        DataverseAPIPostDatasetDTO datasetDTO = new DataverseAPIPostDatasetDTO(
                experimentModel,
                currentAccount,
                datasetTitle,
                datasetAuthors,
                datasetContacts,
                new OpenSilexLocale("en"),
                new OpenSilexLocale("en"),
                date
        );
        DataverseClient dataverseClient = new DataverseClient(
                new URL("http://tests_fake_url.opensilex.org"),
                "opensilex-tests",
                "fake_test_key"
        );
        DatasetFacade facade = dataverseClient.createAFacade(datasetDTO);
        assertEquals("Facade tiltle must be the same", facade.getTitle(), datasetTitle);
        assertEquals("Facade production date must be the same", facade.getProductionDate(), date);
        List<DatasetContact> facadeContacts = facade.getContacts();
        for (int i=0; i<facadeContacts.size(); i++) {
            PersonModel actualContact = datasetContacts.get(i);
            assertEquals("Facade contacts names must be the same", facadeContacts.get(i).getDatasetContactName(), actualContact.getFirstName() + " " + actualContact.getLastName().toUpperCase());
            assertEquals("Facade contacts emails must be the same", facadeContacts.get(i).getDatasetContactEmail(), actualContact.getEmail().toString());
        }
        List<DatasetAuthor> facadeAuthors = facade.getAuthors();
        for (int i=0; i<facadeAuthors.size(); i++) {
            PersonModel actualAuthor = datasetAuthors.get(i);
            assertEquals("Facade contacts names must be the same", facadeAuthors.get(i).getAuthorName(), actualAuthor.getFirstName() + " " + actualAuthor.getLastName().toUpperCase());
        }
        assertEquals("Facade description must be the same", facade.getDescriptions().get(0).getDescription(), experimentModel.getDescription());
        assertEquals("Facade subject must be the same", facade.getSubject(), "Agricultural Sciences");
        assertEquals("Facade languages must be the same", facade.getLanguages().get(0), "English");
        assertEquals("Facade depositor must be the same", facade.getDepositor(), datasetDTO.getCurrentAccount().getHolderOfTheAccount().getFirstName() + " " + datasetDTO.getCurrentAccount().getHolderOfTheAccount().getLastName().toUpperCase());
    }
}
