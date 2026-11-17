//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.apache.jena.vocabulary.RDFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.graphimport.GraphImportModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Names looked up as a user who is not an administrator: only what the platform lets that user see
 * is ever found, and the answer is the same whether a hidden instance exists or not.
 *
 * @author Arnaud Charleroy
 */
public class GuardedInstanceIndexTest extends AbstractMongoIntegrationTest {

    private static final URI EXPERIMENT = URI.create(Oeso.Experiment.getURI());
    private static final URI SCIENTIFIC_OBJECT = URI.create(Oeso.ScientificObject.getURI());
    private static int accounts;

    private final List<InstanceScope> scopes = new GraphImportModule().getInstanceScopes();
    private AccountModel admin;
    private AccountModel user;
    private URI openExperiment;
    private URI secretExperiment;

    @Before
    public void createData() throws Exception {
        AccountDAO accountDAO = new AccountDAO(getSparqlService());
        accounts++;
        admin = accountDAO.create(null, new InternetAddress("gi-admin" + accounts + "@test.test"), true, "pwd", "en");
        user = accountDAO.create(null, new InternetAddress("gi-user" + accounts + "@test.test"), false, "pwd", "en");

        openExperiment = experiment("Essai ouvert", true);
        secretExperiment = experiment("Essai secret", false);

        ScientificObjectModel plot = new ScientificObjectModel();
        plot.setName("Placette 1");
        plot.setType(SCIENTIFIC_OBJECT);
        getSparqlService().create(SPARQLDeserializers.nodeURI(openExperiment), plot);
    }

    @After
    public void dropExperimentGraph() throws Exception {
        getSparqlService().clearGraph(openExperiment);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class, ScientificObjectModel.class);
    }

    @Test
    public void aUserFindsOnlyTheExperimentsThePlatformShowsThem() throws Exception {
        List<String> names = List.of("Essai ouvert", "essai SECRET");

        assertEquals(Set.of("essai ouvert"), index(user, null).lookUp(EXPERIMENT, names).keySet());
        assertEquals(Set.of("essai ouvert", "essai secret"), index(admin, null).lookUp(EXPERIMENT, names).keySet());
    }

    @Test
    public void aHiddenInstanceChangesNothingForTheUser() throws Exception {
        Map<String, Set<URI>> withHidden = index(user, null).lookUp(EXPERIMENT, List.of("Essai ouvert", "Essai secret"));
        Map<String, Set<URI>> withoutHidden = index(user, null).lookUp(EXPERIMENT, List.of("Essai ouvert", "Essai absent"));

        assertEquals(withoutHidden, withHidden);
    }

    @Test
    public void scientificObjectsAreFoundOnlyInTheChosenExperiment() throws Exception {
        assertEquals(Set.of("placette 1"),
                index(user, openExperiment).lookUp(SCIENTIFIC_OBJECT, List.of("Placette 1")).keySet());
        assertTrue(index(user, null).lookUp(SCIENTIFIC_OBJECT, List.of("Placette 1")).isEmpty());
    }

    @Test
    public void closeNamesAreOnlyThoseTheUserMaySee() throws Exception {
        List<String> typos = List.of("Essai ouvret", "Essai secrte");

        Map<String, List<Suggestion>> forUser = index(user, null).near(EXPERIMENT, typos);
        Map<String, List<Suggestion>> forAdmin = index(admin, null).near(EXPERIMENT, typos);

        assertEquals(Set.of("essai ouvret"), forUser.keySet());
        assertEquals("Essai ouvert", forUser.get("essai ouvret").get(0).name());
        assertEquals(openExperiment, forUser.get("essai ouvret").get(0).uri());
        assertEquals(Set.of("essai ouvret", "essai secrte"), forAdmin.keySet());
    }

    @Test
    public void closeScientificObjectsAreSuggestedOnlyInTheChosenExperiment() throws Exception {
        assertEquals(Set.of("placete 1"), index(user, openExperiment).near(SCIENTIFIC_OBJECT, List.of("Placete 1")).keySet());
        assertTrue(index(user, null).near(SCIENTIFIC_OBJECT, List.of("Placete 1")).isEmpty());
        assertTrue("another number is another object",
                index(user, openExperiment).near(SCIENTIFIC_OBJECT, List.of("Placette 2")).isEmpty());
    }

    @Test
    public void aUriExistsForTheUserOnlyWhenTheUserMaySeeIt() throws Exception {
        List<URI> uris = List.of(openExperiment, secretExperiment, URI.create("http://test.opensilex.org/id/absent"));

        Map<URI, Set<URI>> forUser = index(user, null).existing(uris);
        Map<URI, Set<URI>> forAdmin = index(admin, null).existing(uris);

        assertEquals(Set.of(openExperiment), forUser.keySet());
        assertTrue(forUser.get(openExperiment).contains(EXPERIMENT));
        assertEquals(Set.of(openExperiment, secretExperiment), forAdmin.keySet());
    }

    @Test
    public void valuesAreReadOnlyForInstancesFoundVisible() throws Exception {
        InstanceIndex index = index(user, null);
        List<URI> label = List.of(URI.create(RDFS.label.getURI()));

        assertTrue("nothing was found yet", index.storedValues(List.of(openExperiment), label).isEmpty());
        index.existing(List.of(openExperiment, secretExperiment));
        Map<URI, Map<URI, List<StoredValue>>> values = index.storedValues(List.of(openExperiment, secretExperiment),
                label);

        assertEquals(Set.of(openExperiment), values.keySet());
        assertEquals("Essai ouvert", values.get(openExperiment).get(label.get(0)).get(0).lexical());
    }

    @Test
    public void theValuesOfAScientificObjectAreReadInItsExperiment() throws Exception {
        InstanceIndex index = index(user, openExperiment);
        URI plot = index.lookUp(SCIENTIFIC_OBJECT, List.of("Placette 1")).get("placette 1").iterator().next();
        URI label = URI.create(RDFS.label.getURI());

        Map<URI, Map<URI, List<StoredValue>>> values = index.storedValues(List.of(plot), List.of(label));

        assertEquals("Placette 1", values.get(plot).get(label).get(0).lexical());
    }

    @Test(expected = ForbiddenURIAccessException.class)
    public void aUserCannotLookUpInAnExperimentTheyCannotRead() throws Exception {
        LookupContext.of(getSparqlService(), getMongoDBService(), getFs(), user, secretExperiment);
    }

    @Test
    public void anInstanceOfAnotherClassIsNotFound() throws Exception {
        assertTrue(index(admin, null).lookUp(URI.create(Oeso.Germplasm.getURI()), List.of("Essai ouvert")).isEmpty());
    }

    @Test
    public void manyNamesAndQuotesAreSafe() throws Exception {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < GuardedInstanceIndex.NAMES_PER_QUERY + 50; i++) {
            names.add("absent " + i);
        }
        names.add("\" ) } DROP ALL ; #");
        names.add("Essai ouvert");

        assertEquals(Set.of("essai ouvert"), index(user, null).lookUp(EXPERIMENT, names).keySet());
    }

    private InstanceIndex index(AccountModel account, URI experiment) throws Exception {
        return new GuardedInstanceIndex(
                LookupContext.of(getSparqlService(), getMongoDBService(), getFs(), account, experiment), scopes);
    }

    private URI experiment(String name, boolean open) throws Exception {
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName(name);
        experiment.setObjective("Objective of " + name);
        experiment.setStartDate(LocalDate.parse("2026-04-01"));
        experiment.setIsPublic(open);
        getSparqlService().create(ExperimentModel.class, List.of(experiment));
        return experiment.getUri();
    }
}
