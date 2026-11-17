//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.steps;

import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.profile.FactorLevelCandidate;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.SessionProfile;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out where each step of the import stands, from what the report says of the instance and
 * what the files name. It reads nothing itself: what the experiment already has is given.
 * <p>
 * A step is done when everything the files name for it exists, not applicable when they name
 * nothing, blocked when something is missing and a step it depends on is not done, and possible
 * otherwise. What a step depends on is the table of {@link ImportStep}, nothing here.
 *
 * @author Arnaud Charleroy
 */
public final class StepEvaluator {

    private record Progress(int done, int total) {
        boolean applicable() {
            return total > 0;
        }

        boolean complete() {
            return done >= total;
        }
    }

    private final AiImportSession session;
    private final ExperimentFactors existing;

    /**
     * @param existing the factors the experiment the files belong to already has
     */
    public StepEvaluator(AiImportSession session, ExperimentFactors existing) {
        this.session = session;
        this.existing = existing;
    }

    public List<StepState> evaluate() {
        Map<ImportStep, Progress> progress = new EnumMap<>(ImportStep.class);
        for (ImportStep step : ImportStep.values()) {
            progress.put(step, progressOf(step));
        }

        Map<ImportStep, StepStatus> statuses = new EnumMap<>(ImportStep.class);
        List<StepState> states = new ArrayList<>();
        // The constants are in dependency order, so what a step waits for is always decided first.
        for (ImportStep step : ImportStep.values()) {
            Progress own = progress.get(step);
            List<ImportStep> unmet = new ArrayList<>();
            for (ImportStep dependency : step.dependsOn()) {
                StepStatus status = statuses.get(dependency);
                if (status != StepStatus.DONE && status != StepStatus.NOT_APPLICABLE) {
                    unmet.add(dependency);
                }
            }
            StepStatus status;
            if (!own.applicable()) {
                status = StepStatus.NOT_APPLICABLE;
            } else if (own.complete()) {
                status = StepStatus.DONE;
            } else {
                status = unmet.isEmpty() ? StepStatus.POSSIBLE : StepStatus.BLOCKED;
            }
            statuses.put(step, status);
            states.add(new StepState(step, status, status == StepStatus.BLOCKED ? unmet : List.of(),
                    own.done(), own.total()));
        }
        return states;
    }

    private Progress progressOf(ImportStep step) {
        ResolutionReport report = session.getReport();
        if (report == null) {
            return new Progress(0, 0);
        }
        switch (step) {
            case PROJECTS:
                return found(report.getProjects());
            case VARIABLES:
                return found(report.getVariables());
            case FACILITIES:
                return found(report.getFacilities());
            case EXPERIMENT:
                return experiment(report);
            case FACTORS:
                return factors();
            case FACTOR_LEVELS:
                return levels();
            case OBJECTS:
                return found(report.getScientificObjects());
            case EVENTS:
                return events();
            case DATA:
            default:
                return data();
        }
    }

    private static Progress found(List<ResolvedItem> items) {
        long done = items.stream().filter(item -> item.getStatus() == ResolutionStatus.FOUND).count();
        return new Progress((int) done, items.size());
    }

    /**
     * The experiment is needed as soon as anything after it is declared, even when no file names it:
     * it is then the user who says which one, or creates it.
     */
    private Progress experiment(ResolutionReport report) {
        Progress named = found(report.getExperiments());
        if (named.applicable()) {
            return named;
        }
        boolean needed = hasLevelsOrFactors() || !report.getScientificObjects().isEmpty()
                || !session.getDataPoints().isEmpty() || !session.getEvents().isEmpty();
        return needed ? new Progress(0, 1) : named;
    }

    private boolean hasLevelsOrFactors() {
        return !factorLevels().isEmpty();
    }

    private Progress factors() {
        Set<String> names = new LinkedHashSet<>();
        boolean unnamed = false;
        for (FactorLevelCandidate level : factorLevels()) {
            if (level.factor() == null) {
                unnamed = true;
            } else {
                names.add(level.factor());
            }
        }
        int done = (int) names.stream().filter(existing::hasFactor).count();
        return new Progress(done, names.size() + (unnamed ? 1 : 0));
    }

    private Progress levels() {
        List<FactorLevelCandidate> levels = factorLevels();
        long done = levels.stream().filter(level -> existing.hasLevel(level.factor(), level.code())).count();
        return new Progress((int) done, levels.size());
    }

    /**
     * Whether the observations were written is known from the draft that wrote them: there is no
     * other trace of it on the conversation.
     */
    private Progress data() {
        if (session.getDataPoints().isEmpty()) {
            return new Progress(0, 0);
        }
        boolean written = session.getProposals().stream().anyMatch(proposal ->
                proposal.getTarget() == CreationTarget.DATA && proposal.getStatus() == CreationProposal.Status.APPLIED);
        return new Progress(written ? 1 : 0, 1);
    }

    /**
     * Whether the events were written is known from the draft that wrote them, as for the data.
     */
    private Progress events() {
        if (session.getEvents().isEmpty()) {
            return new Progress(0, 0);
        }
        boolean written = session.getProposals().stream().anyMatch(proposal ->
                proposal.getTarget() == CreationTarget.EVENT && proposal.getStatus() == CreationProposal.Status.APPLIED);
        return new Progress(written ? 1 : 0, 1);
    }

    private List<FactorLevelCandidate> factorLevels() {
        if (session.getWorkbook() == null) {
            return List.of();
        }
        return SessionProfile.of(session).map(profile -> profile.extractFactorLevels(session.getWorkbook()))
                .orElse(List.of());
    }
}
