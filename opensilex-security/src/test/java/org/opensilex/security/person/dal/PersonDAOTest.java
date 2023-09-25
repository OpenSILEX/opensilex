package org.opensilex.security.person.dal;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.person.api.ORCIDClient;
import org.opensilex.security.person.api.PersonAPITest;
import org.opensilex.security.person.api.PersonDTO;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersonDAOTest extends AbstractSecurityIntegrationTest {

    private static ORCIDClient orcidClientMock;
    private PersonDAO personDAO;

    @BeforeClass
    public static void setup(){
        orcidClientMock = mock(ORCIDClient.class);
        when(orcidClientMock.orcidExists(any())).thenReturn(true);
    }

    @Before
    public void setUpPersonDAO(){
        personDAO = new PersonDAO(getSparqlService());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        List<Class<? extends SPARQLResourceModel>> modelList = new ArrayList<>();
        modelList.add(PersonModel.class);
        return modelList;
    }

    @Test
    public void createWithOrcidChangeTheUri() throws Exception {
        URI orcid = new URI("https://orcid.org/0009-0006-6636-4715");

        PersonModel personWithOrcid = PersonModel.fromDTO( PersonAPITest.getDefaultDTO() , getSparqlService());
        personWithOrcid.setUri(new URI("http://should-be-overwritten"));
        personWithOrcid.setOrcid(orcid);

        URI createdPersonURI = personDAO.create(personWithOrcid, orcidClientMock).getUri();

        assertEquals("URI should have been overwritten by the ORCID of the person", orcid, createdPersonURI);
    }

    @Test
    public void updateWithOrcidAlreadyExistsThrowExcpetion() throws Exception{
        PersonModel modelWithOrcid = PersonModel.fromDTO( PersonAPITest.getDefaultDTO() , getSparqlService());
        modelWithOrcid.setOrcid( new URI("https://orcid.org/0009-0006-6636-4715") );

        modelWithOrcid = personDAO.create(modelWithOrcid, orcidClientMock);
        assertNotNull("personDao.create didn't worked. Test can't be finished", modelWithOrcid);

        PersonModel modelWithoutOrcid = PersonModel.fromDTO( PersonAPITest.getDefaultDTO() , getSparqlService());
        modelWithoutOrcid.setUri( null );

        modelWithoutOrcid = personDAO.create(modelWithoutOrcid, orcidClientMock);
        assertNotNull("personDao.create didn't worked. Test can't be finished", modelWithoutOrcid);


        PersonModel modelWithoutOrcidCopy = modelWithoutOrcid;
        modelWithoutOrcidCopy.setOrcid(modelWithOrcid.getOrcid());
        assertThrows("this case should throws a ConflictException", ConflictException.class, () -> personDAO.update(PersonDTO.fromModel(modelWithoutOrcidCopy), orcidClientMock) );

        PersonModel shouldNotHaveORCID = personDAO.get(modelWithoutOrcid.getUri());
        assertNull("update should not has worked", shouldNotHaveORCID.getOrcid());
    }

    @Test
    public void updateDoNotAllowToChangeOrcid() throws Exception {
        PersonModel modelWithOrcid = PersonModel.fromDTO( PersonAPITest.getDefaultDTO() , getSparqlService());
        modelWithOrcid.setOrcid(new URI("https://orcid.org/0009-0006-6636-4715"));

        modelWithOrcid = personDAO.create(modelWithOrcid, orcidClientMock);
        assertNotNull("personDao.create didn't worked. Test can't be finished", modelWithOrcid);

        PersonModel badRequestPerson = new PersonModel();
        badRequestPerson.setUri(modelWithOrcid.getUri());
        badRequestPerson.setOrcid(new URI("https://orcid.org/0009-0006-6636-4719"));
        assertThrows("this case should throws a BadRequestException", BadRequestException.class,
                () -> personDAO.update(PersonDTO.fromModel(badRequestPerson), orcidClientMock)
        );

        PersonModel orcidShouldNotHasChanged = personDAO.get(modelWithOrcid.getUri());
        assertEquals("update should not has worked", modelWithOrcid.getOrcid(), orcidShouldNotHasChanged.getOrcid());
    }

}