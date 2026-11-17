//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.api.FactorAPI;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.preview.NodeComparison;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Creates factors with their levels, the way the platform's factor API does: the experiment read with
 * the user's rights, the levels attached to their factor, the factor to its experiment both ways, the
 * user as publisher, then the factor DAO. A level is created with its factor: adding a level to a factor
 * that exists already is not supported yet.
 *
 * @author Arnaud Charleroy
 */
public class FactorWriter implements ResourceWriter {

    public static final String STEP = "factors";

    static final URI FACTOR = URI.create(Oeso.Factor.getURI());
    static final URI LEVEL = URI.create(Oeso.FactorLevel.getURI());
    static final URI HAS_FACTOR = URI.create(Oeso.hasFactor.getURI());
    static final URI CATEGORY = URI.create(Oeso.hasCategory.getURI());
    static final URI EXPERIMENT = URI.create(Oeso.studiedEffectIn.getURI());

    private static final Set<URI> FACTOR_WRITTEN = Set.of(ExperimentWriter.NAME, ExperimentWriter.DESCRIPTION, CATEGORY,
            EXPERIMENT);
    private static final Set<URI> LEVEL_WRITTEN = Set.of(ExperimentWriter.NAME, ExperimentWriter.DESCRIPTION, HAS_FACTOR);

    @Override
    public String step() {
        return STEP;
    }

    @Override
    public Set<URI> rootClasses() {
        return Set.of(FACTOR, LEVEL);
    }

    @Override
    public String credentialId() {
        return FactorAPI.CREDENTIAL_FACTOR_MODIFICATION_ID;
    }

    @Override
    public List<String> after() {
        return List.of(ExperimentWriter.STEP);
    }

    @Override
    public Optional<ReportMessage> whyNot(NodeComparison node, WriteContext context) {
        NodeValues values = NodeValues.of(node);
        boolean level = isLevel(node, context);
        Set<URI> written = level ? LEVEL_WRITTEN : FACTOR_WRITTEN;
        for (URI property : values.properties()) {
            if (!written.contains(property)) {
                return Optional.of(CreationMessages.unwritable(property));
            }
        }
        if (level) {
            return values.targets(HAS_FACTOR).isEmpty() ? Optional.empty()
                    : Optional.of(CreationMessages.existingFactor());
        }
        boolean hasExperiment = !values.targets(EXPERIMENT).isEmpty() || !values.rows(EXPERIMENT).isEmpty()
                || context.experiment() != null;
        return hasExperiment ? Optional.empty() : Optional.of(CreationMessages.noExperiment());
    }

    @Override
    public List<WriteProblem> check(List<NodeComparison> nodes, WriteContext context) throws Exception {
        List<WriteProblem> problems = new ArrayList<>();
        ExperimentDAO experiments = new ExperimentDAO(context.sparql(), context.nosql(), context.fs());
        for (NodeComparison node : nodes) {
            Optional<ReportMessage> reason = whyNot(node, context);
            if (reason.isPresent()) {
                problems.add(WriteProblem.of(node, reason.get()));
            } else if (!isLevel(node, context)) {
                try {
                    experiments.get(experimentOf(node, context), context.user());
                } catch (Exception e) {
                    problems.add(WriteProblem.of(node, CreationMessages.refused(e.getMessage())));
                }
            }
        }
        return problems;
    }

    @Override
    public List<URI> write(List<NodeComparison> nodes, WriteContext context) throws Exception {
        ExperimentDAO experiments = new ExperimentDAO(context.sparql(), context.nosql(), context.fs());
        FactorDAO factors = new FactorDAO(context.sparql());
        URI publisher = context.user().getUri();
        List<URI> created = new ArrayList<>();
        context.sparql().startTransaction();
        try {
            for (NodeComparison node : nodes) {
                if (isLevel(node, context)) {
                    continue;
                }
                NodeValues values = NodeValues.of(node);
                ExperimentModel experiment = experiments.get(experimentOf(node, context), context.user());
                FactorModel factor = new FactorModel();
                factor.setName(values.literal(ExperimentWriter.NAME).orElse(node.node().getName()));
                values.literal(ExperimentWriter.DESCRIPTION).ifPresent(factor::setDescription);
                values.targets(CATEGORY).stream().findFirst().ifPresent(factor::setCategory);
                factor.setPublisher(publisher);
                List<FactorLevelModel> levels = new ArrayList<>();
                for (NodeComparison candidate : nodes) {
                    if (isLevel(candidate, context)
                            && NodeValues.of(candidate).rows(HAS_FACTOR).contains(node.node().getKey())) {
                        levels.add(levelOf(candidate, factor, publisher));
                    }
                }
                factor.setFactorLevels(levels);
                factor.setExperiment(experiment);
                factor.setAssociatedExperiments(List.of(experiment));
                factors.create(factor);
                created.add(factor.getUri());
                levels.forEach(level -> created.add(level.getUri()));
            }
            context.sparql().commitTransaction();
        } catch (Exception e) {
            context.sparql().rollbackTransaction(e);
            throw e;
        }
        return created;
    }

    private static FactorLevelModel levelOf(NodeComparison node, FactorModel factor, URI publisher) {
        NodeValues values = NodeValues.of(node);
        FactorLevelModel level = new FactorLevelModel();
        level.setName(values.literal(ExperimentWriter.NAME).orElse(node.node().getName()));
        values.literal(ExperimentWriter.DESCRIPTION).ifPresent(level::setDescription);
        level.setFactor(factor);
        level.setPublisher(publisher);
        return level;
    }

    /** The experiment the factor is studied in: the one its values name, else the import's. */
    private static URI experimentOf(NodeComparison node, WriteContext context) {
        return NodeValues.of(node).targets(EXPERIMENT).stream().findFirst().orElse(context.experiment());
    }

    private static boolean isLevel(NodeComparison node, WriteContext context) {
        return LEVEL.equals(node.classUri()) || context.catalog().isA(node.classUri(), LEVEL);
    }
}
