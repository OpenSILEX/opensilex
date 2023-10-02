package org.opensilex.migration;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.provenance.dal.ActivityModel;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.api.ORCIDClient;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

public class ObjectMigrationFromAccountToPersonTest extends AbstractMongoIntegrationTest {
    private static SPARQLService sparql;
    private static ObjectMigrationFromAccountToPerson migration;
    private static AccountDAO accountDAO;
    private static PersonDAO personDAO;
    private static int userCount;
    @BeforeClass
    public static void prepare() {
        OpenSilex openSilex = getOpensilex();
        sparql = openSilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        migration = new ObjectMigrationFromAccountToPerson();
        migration.setOpensilex(opensilex);
        accountDAO = new AccountDAO(sparql);
        personDAO = new PersonDAO(sparql);
    }

    private static AccountModel createAccount() throws Exception {
        int count = userCount++;

        return accountDAO.create(
                null,
                new InternetAddress("user" + count + "@opensilex.org"),
                false,
                "1234",
                OpenSilex.DEFAULT_LANGUAGE,
                true,
                null
        );
    }

    private static PersonModel createPerson(AccountModel account) throws Exception {
        PersonModel personModel = new PersonModel();
        personModel.setFirstName("user" + userCount + " first name");
        personModel.setLastName("user" + userCount + " last name");
        personModel.setAccount(account);
        return personDAO.create(personModel, new ORCIDClient());
    }

    private static ProjectModel getProjectModel(){
        ProjectModel projectModel = new ProjectModel();

        projectModel.setName("migration test project");
        projectModel.setStartDate(LocalDate.now());
        return projectModel;
    }

    private static ExperimentModel getExperimentModel(){
        ExperimentModel experimentModel = new ExperimentModel();

        experimentModel.setName("migration test experiment");
        experimentModel.setStartDate(LocalDate.now());
        experimentModel.setObjective("testing the migration");
        return experimentModel;
    }

    @Test
    public void project_hasScientificContact() throws Exception {
        AccountModel account = createAccount();
        PersonModel person = createPerson(account);
        ProjectDAO projectDAO = new ProjectDAO(sparql);
        ProjectModel project = projectDAO.create(getProjectModel());

        createOldPredicateFromSubjectToAccount(project.getUri(), Oeso.hasScientificContact.asNode(), account);

        migration.execute();

        PersonModel scientificContact = projectDAO.get(project.getUri(), account).getScientificContacts().get(0);
        assertEquals(person, scientificContact);
    }

    @Test
    public void project_hasAdministrativeContact() throws Exception {
        AccountModel account = createAccount();
        PersonModel person = createPerson(account);
        ProjectDAO projectDAO = new ProjectDAO(sparql);
        ProjectModel project = projectDAO.create(getProjectModel());

        createOldPredicateFromSubjectToAccount(project.getUri(), Oeso.hasAdministrativeContact.asNode(), account);

        migration.execute();

        PersonModel administrativeContact = projectDAO.get(project.getUri(), account).getAdministrativeContacts().get(0);
        assertEquals(person, administrativeContact);
    }

    @Test
    public void project_hasCoordinator() throws Exception {
        AccountModel account = createAccount();
        PersonModel person = createPerson(account);
        ProjectDAO projectDAO = new ProjectDAO(sparql);
        ProjectModel project = projectDAO.create(getProjectModel());

        createOldPredicateFromSubjectToAccount(project.getUri(), Oeso.hasCoordinator.asNode(), account);

        migration.execute();

        PersonModel coordinator = projectDAO.get(project.getUri(), account).getCoordinators().get(0);
        assertEquals(person, coordinator);
    }

    @Test
    public void experiment_hasScientificSupervisor() throws Exception {
        AccountModel account = createAccount();
        PersonModel person = createPerson(account);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, getMongoDBService());
        ExperimentModel experiment = experimentDAO.create(getExperimentModel());

        createOldPredicateFromSubjectToAccount(experiment.getUri(), Oeso.hasScientificSupervisor.asNode(), account);

        migration.execute();

        PersonModel scientificSupervisor = experimentDAO.get(experiment.getUri(), account).getScientificSupervisors().get(0);
        assertEquals(person, scientificSupervisor);
    }

    @Test
    public void provenance_operator() throws Exception{
        AccountModel account1 = createAccount();
        AccountModel account2 = createAccount();

        Set<URI> personsURIs = new HashSet<>();
        personsURIs.add( SPARQLDeserializers.formatURI(createPerson(account1).getUri()) );
        personsURIs.add( SPARQLDeserializers.formatURI(createPerson(account2).getUri()) );

        ProvenanceModel provenanceBeforeMigration = createProvenanceWithOperatorsAreUsers(account1, account2);

        migration.execute();

        ProvenanceDAO provenanceDAO = new ProvenanceDAO(getMongoDBService(), getSparqlService());
        ProvenanceModel provenanceAfterMigration = provenanceDAO.get(provenanceBeforeMigration.getUri());

        Set<URI> operatorsURIs = new HashSet<>();
        provenanceAfterMigration.getAgents().forEach( agentModel -> operatorsURIs.add(SPARQLDeserializers.formatURI( agentModel.getUri())) );

        assertEquals(personsURIs, operatorsURIs);
    }

    private ProvenanceModel createProvenanceWithOperatorsAreUsers(AccountModel operator1, AccountModel operator2) throws Exception {
        ProvenanceModel provenanceModel = new ProvenanceModel();

        provenanceModel.setName("test");
        provenanceModel.setDescription("test !");

        ActivityModel activityModel = new ActivityModel();
        activityModel.setRdfType(URI.create(Oeso.ImageAnalysis.toString()));
        List<ActivityModel> activities = new ArrayList<>();
        activities.add(activityModel);
        provenanceModel.setActivity(activities);

        URI operatorType = new URI( Oeso.Operator.getURI() );
        List<AgentModel> agents = new ArrayList<>();
        AgentModel agent1 = new AgentModel();
        agent1.setRdfType(operatorType);
        agent1.setUri(operator1.getUri());
        agents.add(agent1);
        AgentModel agent2 = new AgentModel();
        agent2.setRdfType(operatorType);
        agent2.setUri(operator2.getUri());
        agents.add(agent2);
        provenanceModel.setAgents(agents);

        getMongoDBService().getDatabase().getCollection(ProvenanceDAO.PROVENANCE_COLLECTION_NAME).createIndex(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
        getMongoDBService().create(provenanceModel, ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, ProvenanceDAO.PROVENANCE_PREFIX);

        return provenanceModel;
    }

    /**
     * create the triplet beetween a ressource and an account, as it was before the new data model
     */
    private static void createOldPredicateFromSubjectToAccount(URI ressource, Node predicate, AccountModel account) throws SPARQLException {
        UpdateBuilder query = new UpdateBuilder().addInsert(
                SPARQLDeserializers.nodeURI( ressource ),
                predicate,
                SPARQLDeserializers.nodeURI( account.getUri()) );

        sparql.executeUpdateQuery(query);
    }
}