//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.drafts;

import org.opensilex.aiimport.profile.FactorLevelCandidate;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sets the factors and levels the files name beside those an experiment has, so that each factor
 * is declared the way it needs to be: created when the experiment has none of that name, completed
 * when it lacks levels, left alone when nothing is missing.
 * <p>
 * Nothing is written. The plan is what the platform's factor form is opened with.
 *
 * @author Arnaud Charleroy
 */
public final class FactorPlanner {

    private final SPARQLService sparql;
    private final String lang;

    public FactorPlanner(SPARQLService sparql, String lang) {
        this.sparql = sparql;
        this.lang = lang;
    }

    /**
     * @param named      the levels the files name, with the factor each belongs to
     * @param experiment the experiment the factors are declared in
     * @return a plan per factor, in the order the files name them; a factor the files do not name
     * comes last
     */
    public List<FactorPlan> plan(List<FactorLevelCandidate> named, URI experiment) throws Exception {
        Map<String, FactorModel> existing = new LinkedHashMap<>();
        for (FactorModel factor : new FactorDAO(sparql).getByExperiment(experiment, lang)) {
            existing.put(key(factor.getName()), factor);
        }

        Map<String, List<FactorLevelCandidate>> byFactor = new LinkedHashMap<>();
        List<FactorLevelCandidate> unnamed = new ArrayList<>();
        for (FactorLevelCandidate level : named) {
            if (level.factor() == null) {
                unnamed.add(level);
            } else {
                byFactor.computeIfAbsent(level.factor(), name -> new ArrayList<>()).add(level);
            }
        }

        List<FactorPlan> plans = new ArrayList<>();
        byFactor.forEach((name, levels) -> plans.add(planOf(name, levels, existing.get(key(name)))));
        if (!unnamed.isEmpty()) {
            plans.add(planOf(null, unnamed, null));
        }
        return plans;
    }

    private static FactorPlan planOf(String name, List<FactorLevelCandidate> wanted, FactorModel factor) {
        List<FactorPlan.Level> levels = new ArrayList<>();
        Set<String> present = new HashSet<>();
        if (factor != null && factor.getFactorLevels() != null) {
            for (FactorLevelModel level : factor.getFactorLevels()) {
                levels.add(new FactorPlan.Level(level.getUri(), level.getName(), level.getDescription(), true));
                present.add(key(level.getName()));
            }
        }
        boolean missing = false;
        for (FactorLevelCandidate candidate : wanted) {
            if (present.add(key(candidate.code()))) {
                levels.add(new FactorPlan.Level(null, candidate.code(), candidate.levelDescription(), false));
                missing = true;
            }
        }
        FactorPlan.Status status = factor == null
                ? FactorPlan.Status.NEW
                : missing ? FactorPlan.Status.INCOMPLETE : FactorPlan.Status.COMPLETE;
        // The instance's own description of a factor it has wins; a new one takes what the file says.
        String described = factor != null && factor.getDescription() != null && !factor.getDescription().isBlank()
                ? factor.getDescription()
                : wanted.stream().map(FactorLevelCandidate::factorDescription).filter(text -> text != null)
                .findFirst().orElse(null);
        return new FactorPlan(name, factor == null ? null : factor.getUri(), described, levels, status);
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
