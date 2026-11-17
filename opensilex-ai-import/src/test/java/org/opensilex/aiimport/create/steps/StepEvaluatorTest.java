//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.steps;

import org.junit.Test;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.UserDefinedProfile;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.service.AiImportSession;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.workbook;

/**
 * The order an import is declared in: an experiment, its factors, their levels, then the objects and
 * the data — each step waiting for the ones it depends on.
 *
 * @author Arnaud Charleroy
 */
public class StepEvaluatorTest {

    private static final ExperimentFactors NOTHING = ExperimentFactors.NONE;

    private static ResolvedItem item(String name, ResolutionStatus status) {
        return new ResolvedItem(name).setStatus(status);
    }

    /**
     * A conversation about a file with a sheet of levels of two factors, {@code Traitement} (T1, T2)
     * and {@code Bloc} (B1).
     */
    @SuppressWarnings("unchecked")
    private static AiImportSession session(ResolutionReport report) {
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setProfileId(UserDefinedProfile.ID);
        session.setWorkbook(workbook(sheet("modalites", List.of("facteur", "modalite"),
                row("Traitement", "T1"), row("Traitement", "T2"), row("Bloc", "B1"))));
        session.getDescription().sheet("modalites").setKind(SheetKind.FACTORS)
                .getRoles().putAll(Map.of("facteur", DescribedRole.FACTOR, "modalite", DescribedRole.FACTOR_LEVEL));
        session.setReport(report);
        return session;
    }

    private static ResolutionReport report(ResolutionStatus experiment) {
        ResolutionReport report = new ResolutionReport();
        if (experiment != null) {
            report.getExperiments().add(item("Essai", experiment));
        }
        return report;
    }

    private static ExperimentFactors existing(Map<String, Set<String>> levels) {
        return new ExperimentFactors(levels);
    }

    private static StepState state(List<StepState> states, ImportStep step) {
        return states.stream().filter(candidate -> candidate.step() == step).findFirst().orElseThrow();
    }

    private static List<StepState> evaluate(AiImportSession session, ExperimentFactors existing) {
        return new StepEvaluator(session, existing).evaluate();
    }

    @Test
    public void everyStepIsReportedInTheOrderTheyAreDeclaredIn() {
        List<StepState> states = evaluate(session(report(null)), NOTHING);

        assertEquals(List.of(ImportStep.values()),
                states.stream().map(StepState::step).collect(Collectors.toList()));
    }

    @Test
    public void everyStepComesAfterTheStepsItDependsOn() {
        for (ImportStep step : ImportStep.values()) {
            for (ImportStep dependency : step.dependsOn()) {
                assertTrue(step + " after " + dependency, dependency.ordinal() < step.ordinal());
            }
        }
    }

    @Test
    public void aFileThatNamesNothingHasNothingToDeclare() {
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setReport(new ResolutionReport());

        evaluate(session, NOTHING).forEach(state -> assertEquals(state.step().toString(), StepStatus.NOT_APPLICABLE,
                state.status()));
    }

    @Test
    public void withoutAnExperimentTheFactorsAndTheirLevelsWait() {
        List<StepState> states = evaluate(session(report(ResolutionStatus.MISSING)), NOTHING);

        assertEquals(StepStatus.POSSIBLE, state(states, ImportStep.EXPERIMENT).status());
        StepState factors = state(states, ImportStep.FACTORS);
        assertEquals(StepStatus.BLOCKED, factors.status());
        assertEquals(List.of(ImportStep.EXPERIMENT), factors.blockedBy());
        assertEquals(List.of(ImportStep.FACTORS), state(states, ImportStep.FACTOR_LEVELS).blockedBy());
    }

    @Test
    public void anExperimentIsNeededEvenWhenNoFileNamesIt() {
        List<StepState> states = evaluate(session(report(null)), NOTHING);

        assertEquals(StepStatus.POSSIBLE, state(states, ImportStep.EXPERIMENT).status());
        assertEquals(StepStatus.BLOCKED, state(states, ImportStep.FACTORS).status());
    }

    @Test
    public void onceTheExperimentExistsItsFactorsCanBeDeclared() {
        List<StepState> states = evaluate(session(report(ResolutionStatus.FOUND)), NOTHING);

        assertEquals(StepStatus.DONE, state(states, ImportStep.EXPERIMENT).status());
        StepState factors = state(states, ImportStep.FACTORS);
        assertEquals(StepStatus.POSSIBLE, factors.status());
        assertEquals("an experiment can have several factors", 2, factors.total());
        assertEquals(StepStatus.BLOCKED, state(states, ImportStep.FACTOR_LEVELS).status());
    }

    @Test
    public void aFactorThatExistsIsDoneWhateverItsMissingLevels() {
        List<StepState> states = evaluate(session(report(ResolutionStatus.FOUND)),
                existing(Map.of("traitement", Set.of("t1"))));

        StepState factors = state(states, ImportStep.FACTORS);
        assertEquals("Traitement exists, Bloc does not", 1, factors.done());
        assertEquals(2, factors.total());
        assertEquals(StepStatus.POSSIBLE, factors.status());
    }

    @Test
    public void theLevelsOfAFactorAreCountedPerFactor() {
        List<StepState> states = evaluate(session(report(ResolutionStatus.FOUND)),
                existing(Map.of("traitement", Set.of("t1"), "bloc", Set.of("t2"))));

        StepState levels = state(states, ImportStep.FACTOR_LEVELS);
        assertEquals("only T1 of Traitement exists: T2 is a level of another factor", 1, levels.done());
        assertEquals(3, levels.total());
        assertEquals(StepStatus.DONE, state(states, ImportStep.FACTORS).status());
        assertEquals(StepStatus.POSSIBLE, levels.status());
    }

    @Test
    public void whenEverythingExistsEveryStepIsDone() {
        List<StepState> states = evaluate(session(report(ResolutionStatus.FOUND)),
                existing(Map.of("traitement", Set.of("t1", "t2"), "bloc", Set.of("b1"))));

        assertEquals(StepStatus.DONE, state(states, ImportStep.FACTORS).status());
        assertEquals(StepStatus.DONE, state(states, ImportStep.FACTOR_LEVELS).status());
    }

    /**
     * A STAR workbook has one factor and does not name it: it is the user who does, so until then
     * the factor is still to be declared.
     */
    @Test
    public void aFactorTheFileDoesNotNameCountsAsMissing() throws Exception {
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setProfileId(StarProfile.ID).setWorkbook(WorkbookFixture.starExample());
        session.setReport(report(ResolutionStatus.FOUND));

        StepState factors = state(evaluate(session, existing(Map.of("traitement", Set.of("t1")))),
                ImportStep.FACTORS);

        assertEquals(0, factors.done());
        assertEquals(1, factors.total());
        assertEquals(StepStatus.POSSIBLE, factors.status());
    }

    @Test
    public void objectsWaitForTheLevelsTheirColumnsName() {
        ResolutionReport report = report(ResolutionStatus.FOUND);
        report.getScientificObjects().add(item("P01", ResolutionStatus.MISSING));
        AiImportSession session = session(report);

        StepState objects = state(evaluate(session, NOTHING), ImportStep.OBJECTS);

        assertEquals(StepStatus.BLOCKED, objects.status());
        assertEquals(List.of(ImportStep.FACTOR_LEVELS), objects.blockedBy());
    }

    @Test
    public void objectsAreNotBlockedByLevelsTheFileDoesNotName() {
        ResolutionReport report = report(ResolutionStatus.FOUND);
        report.getScientificObjects().add(item("P01", ResolutionStatus.MISSING));
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setReport(report);

        StepState objects = state(evaluate(session, NOTHING), ImportStep.OBJECTS);

        assertEquals(StepStatus.POSSIBLE, objects.status());
    }

    @Test
    public void objectsNeedAnExperimentToo() {
        ResolutionReport report = report(ResolutionStatus.MISSING);
        report.getScientificObjects().add(item("P01", ResolutionStatus.NOT_CHECKED));
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setReport(report);

        StepState objects = state(evaluate(session, NOTHING), ImportStep.OBJECTS);

        assertEquals(StepStatus.BLOCKED, objects.status());
        assertEquals(List.of(ImportStep.EXPERIMENT), objects.blockedBy());
    }

    @Test
    public void dataWaitForTheExperimentTheVariablesAndTheObjects() {
        ResolutionReport report = report(ResolutionStatus.MISSING);
        report.getVariables().add(item("hauteur", ResolutionStatus.MISSING));
        report.getScientificObjects().add(item("P01", ResolutionStatus.MISSING));
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setReport(report);
        session.setDataPoints(List.of(new DataPoint().setObjectName("P01")));

        StepState data = state(evaluate(session, NOTHING), ImportStep.DATA);

        assertEquals(StepStatus.BLOCKED, data.status());
        assertTrue(data.blockedBy().containsAll(
                List.of(ImportStep.EXPERIMENT, ImportStep.VARIABLES, ImportStep.OBJECTS)));
    }

    @Test
    public void whatTheFileDoesNotNameDoesNotHoldBackTheData() {
        ResolutionReport report = report(ResolutionStatus.FOUND);
        report.getVariables().add(item("hauteur", ResolutionStatus.FOUND));
        report.getFacilities().add(item("meteo", ResolutionStatus.FOUND));
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setReport(report);
        session.setDataPoints(List.of(new DataPoint().setObjectName("meteo")
                .setTargetKind(DataPoint.TargetKind.FACILITY)));

        StepState data = state(evaluate(session, NOTHING), ImportStep.DATA);

        assertEquals("no object is named, so none is waited for", StepStatus.POSSIBLE, data.status());
    }

    @Test
    public void dataAreDoneOnceTheDraftThatWroteThemIsApplied() {
        ResolutionReport report = report(ResolutionStatus.FOUND);
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setReport(report);
        session.setDataPoints(List.of(new DataPoint().setObjectName("P01")));
        assertEquals(StepStatus.POSSIBLE, state(evaluate(session, NOTHING), ImportStep.DATA).status());

        session.restoreProposal(new CreationProposal("p", CreationTarget.DATA)
                .setStatus(CreationProposal.Status.APPLIED));

        assertEquals(StepStatus.DONE, state(evaluate(session, NOTHING), ImportStep.DATA).status());
    }

    @Test
    public void eventsWaitForTheExperimentAndTheObjectsAndAreDoneOnceWritten() {
        ResolutionReport report = report(ResolutionStatus.MISSING);
        report.getScientificObjects().add(item("P01", ResolutionStatus.MISSING));
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setReport(report);
        session.setEvents(List.of(new EventCandidate().setDate(java.time.LocalDate.of(2026, 5, 12))));

        StepState events = state(evaluate(session, NOTHING), ImportStep.EVENTS);

        assertEquals(StepStatus.BLOCKED, events.status());
        assertTrue(events.blockedBy().containsAll(List.of(ImportStep.EXPERIMENT, ImportStep.OBJECTS)));

        ResolutionReport found = report(ResolutionStatus.FOUND);
        found.getScientificObjects().add(item("P01", ResolutionStatus.FOUND));
        session.setReport(found);
        assertEquals(StepStatus.POSSIBLE, state(evaluate(session, NOTHING), ImportStep.EVENTS).status());

        session.restoreProposal(new CreationProposal("e", CreationTarget.EVENT)
                .setStatus(CreationProposal.Status.APPLIED));
        assertEquals(StepStatus.DONE, state(evaluate(session, NOTHING), ImportStep.EVENTS).status());
    }

    @Test
    public void aConversationNotYetAnalysedHasNothingToReport() {
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));

        evaluate(session, NOTHING).forEach(state -> assertEquals(StepStatus.NOT_APPLICABLE, state.status()));
    }

    @Test
    public void eachStepNamesTheCreationThatDeclaresIt() {
        assertEquals(CreationTarget.FACTORS, ImportStep.FACTOR_LEVELS.target().orElseThrow());
        assertEquals(ImportStep.FACTORS, ImportStep.of(CreationTarget.FACTORS).orElseThrow());
        assertTrue(ImportStep.FACILITIES.target().isEmpty());
        assertTrue("only the creations that wait are refused early", ImportStep.FACTORS.blocksCreation()
                && ImportStep.OBJECTS.blocksCreation() && !ImportStep.DATA.blocksCreation());
    }
}
