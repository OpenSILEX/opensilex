//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.GraphImportModule;
import org.opensilex.graphimport.graph.GuardedInstanceIndex;
import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.ClassCatalogReader;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Factors and their levels are offered as classes, and found by name only among those of the chosen
 * experiment: two experiments may each have an "Irrigation" factor with a "Dry" level.
 *
 * @author Arnaud Charleroy
 */
public class FactorScopeTest extends AbstractMongoIntegrationTest {

    private static final URI FACTOR = URI.create(Oeso.Factor.getURI());
    private static final URI FACTOR_LEVEL = URI.create(Oeso.FactorLevel.getURI());
    private static int accounts;

    private final List<InstanceScope> scopes = new GraphImportModule().getInstanceScopes();
    private AccountModel user;
    private ExperimentModel first;
    private ExperimentModel second;
    private FactorModel firstFactor;
    private FactorModel secondFactor;

    @Before
    public void createData() throws Exception {
        accounts++;
        user = new AccountDAO(getSparqlService())
                .create(null, new InternetAddress("gi-factor" + accounts + "@test.test"), false, "pwd", "en");
        first = experiment("Essai facteurs A " + accounts);
        second = experiment("Essai facteurs B " + accounts);
        firstFactor = factor(first);
        secondFactor = factor(second);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class, FactorModel.class, FactorLevelModel.class);
    }

    @Test
    public void factorsAndLevelsAreClassesASheetMayHold() throws Exception {
        ClassCatalog catalog = new ClassCatalogReader(getSparqlService())
                .read(scopes.stream().map(InstanceScope::rootClass).toList());

        assertTrue(catalog.contains(FACTOR));
        assertTrue(catalog.contains(FACTOR_LEVEL));
    }

    @Test
    public void aFactorIsFoundOnlyInTheChosenExperiment() throws Exception {
        Map<String, Set<URI>> found = index(first).lookUp(FACTOR, List.of("Irrigation"));

        assertEquals(Set.of(Uris.expand(firstFactor.getUri())), found.get("irrigation"));
    }

    @Test
    public void aLevelIsFoundOnlyAmongTheFactorsOfTheChosenExperiment() throws Exception {
        Map<String, Set<URI>> found = index(second).lookUp(FACTOR_LEVEL, List.of("Sec"));

        assertEquals(Set.of(Uris.expand(secondFactor.getFactorLevels().get(0).getUri())), found.get("sec"));
    }

    @Test
    public void withoutAnExperimentNoFactorIsLookedUp() throws Exception {
        assertTrue(index(null).lookUp(FACTOR, List.of("Irrigation")).isEmpty());
        assertTrue(index(null).lookUp(FACTOR_LEVEL, List.of("Sec")).isEmpty());
    }

    private InstanceIndex index(ExperimentModel experiment) throws Exception {
        URI uri = experiment == null ? null : experiment.getUri();
        return new GuardedInstanceIndex(LookupContext.of(getSparqlService(), getMongoDBService(), getFs(), user, uri),
                scopes);
    }

    private ExperimentModel experiment(String name) throws Exception {
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName(name);
        experiment.setObjective("Objective of " + name);
        experiment.setStartDate(LocalDate.parse("2026-04-01"));
        experiment.setIsPublic(true);
        getSparqlService().create(ExperimentModel.class, List.of(experiment));
        return experiment;
    }

    /** A factor with one level, written the way the platform's factor API writes it. */
    private FactorModel factor(ExperimentModel experiment) throws Exception {
        FactorModel factor = new FactorModel();
        factor.setName("Irrigation");
        FactorLevelModel level = new FactorLevelModel();
        level.setName("Sec");
        level.setFactor(factor);
        factor.setFactorLevels(List.of(level));
        factor.setExperiment(experiment);
        factor.setAssociatedExperiments(List.of(experiment));
        new FactorDAO(getSparqlService()).create(factor);
        return factor;
    }
}
