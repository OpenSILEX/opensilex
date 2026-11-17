//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.steps;

import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The factors an experiment already has, and the levels of each: what the files' factors are
 * compared with. An experiment can have several factors, so factors are told apart by name.
 *
 * @author Arnaud Charleroy
 */
public final class ExperimentFactors {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentFactors.class);

    /**
     * What an experiment that cannot be read, or is not known, has.
     */
    public static final ExperimentFactors NONE = new ExperimentFactors(Map.of());

    private final Map<String, Set<String>> levelsByFactor;

    /**
     * @param levelsByFactor the level names of each factor name, lower case
     */
    public ExperimentFactors(Map<String, Set<String>> levelsByFactor) {
        this.levelsByFactor = levelsByFactor;
    }

    /**
     * Reads the factors of an experiment from the instance. A failure to read is not an answer: it
     * is logged, and the experiment is taken to have none, which at worst offers a step again.
     */
    public static ExperimentFactors read(SPARQLService sparql, URI experiment, String lang) {
        try {
            Map<String, Set<String>> levels = new LinkedHashMap<>();
            for (FactorModel factor : new FactorDAO(sparql).getByExperiment(experiment, lang)) {
                Set<String> names = levels.computeIfAbsent(key(factor.getName()), name -> new HashSet<>());
                if (factor.getFactorLevels() != null) {
                    for (FactorLevelModel level : factor.getFactorLevels()) {
                        names.add(key(level.getName()));
                    }
                }
            }
            return new ExperimentFactors(levels);
        } catch (Exception e) {
            LOGGER.warn("Could not read the factors of experiment {}", experiment, e);
            return NONE;
        }
    }

    public boolean hasFactor(String factor) {
        return factor != null && levelsByFactor.containsKey(key(factor));
    }

    /**
     * @param factor the factor the level belongs to, or {@code null} when the file does not say,
     *               in which case a level of that name in any factor counts
     */
    public boolean hasLevel(String factor, String level) {
        if (factor != null) {
            return levelsByFactor.getOrDefault(key(factor), Set.of()).contains(key(level));
        }
        return levelsByFactor.values().stream().anyMatch(levels -> levels.contains(key(level)));
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
