//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The writers count on the platform's DAOs writing inside the transaction they open: a failure after a
 * first resource written leaves nothing written.
 *
 * @author Arnaud Charleroy
 */
public class WriterTransactionTest extends AbstractMongoIntegrationTest {

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class, FactorModel.class, FactorLevelModel.class);
    }

    @Test
    public void aFailureAfterAFirstWriteWritesNothing() throws Exception {
        ExperimentDAO experiments = new ExperimentDAO(getSparqlService(), getMongoDBService(), getFs());
        ExperimentModel experiment = experiment("Essai de transaction");
        experiments.create(experiment);

        FactorModel factor = new FactorModel();
        factor.setName("Irrigation annulée");
        FactorLevelModel level = new FactorLevelModel();
        level.setName("Sec");
        level.setFactor(factor);
        factor.setFactorLevels(List.of(level));
        factor.setExperiment(experiment);
        factor.setAssociatedExperiments(List.of(experiment));

        getSparqlService().startTransaction();
        try {
            new FactorDAO(getSparqlService()).create(factor);
            experiments.create(experiment("Essai annulé"));
            throw new IllegalStateException("A later resource is refused");
        } catch (IllegalStateException e) {
            getSparqlService().rollbackTransaction();
        }

        assertFalse(getSparqlService().uriExists(FactorModel.class, factor.getUri()));
        assertTrue(getSparqlService().search(ExperimentModel.class, null).stream()
                .noneMatch(written -> "Essai annulé".equals(written.getName())));
        assertTrue("what was written before stays", getSparqlService().uriExists(ExperimentModel.class,
                experiment.getUri()));
    }

    private static ExperimentModel experiment(String name) {
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName(name);
        experiment.setObjective("Objectif");
        experiment.setStartDate(LocalDate.parse("2026-04-01"));
        return experiment;
    }
}
