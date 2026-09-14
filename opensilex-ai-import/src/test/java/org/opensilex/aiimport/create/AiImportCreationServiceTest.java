//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import org.opensilex.core.data.api.DataCreationDTO;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks what each creation asks for before it is attempted.
 * <p>
 * Requirements are computed from the session alone, so the databases are not needed here and are
 * passed as null on purpose: a test that needed a running instance to answer "which fields are
 * required" would be testing the wrong thing.
 *
 * @author Arnaud Charleroy
 */
public class AiImportCreationServiceTest {

    private static final URI ACCOUNT = URI.create("http://opensilex.test/id/account/alice");
    private static final URI EXPERIMENT = URI.create("http://opensilex.test/id/experiment/cepinnov");
    private static final URI FACILITY = URI.create("http://opensilex.test/id/facility/melgueil");
    private static final URI SHARED_INSTANCE = URI.create("http://phenome.inrae.fr/rest");

    private static WorkbookStructure workbook;
    private static ExtractedImportPlan plan;
    private static List<DataPoint> dataPoints;

    private final AiImportCreationService service =
            new AiImportCreationService(null, null, null, null);

    @BeforeClass
    public static void readTheSampleFile() throws Exception {
        VitisExplorerProfile profile = new VitisExplorerProfile();
        workbook = WorkbookFixture.vitis();
        plan = profile.extract(workbook);
        dataPoints = profile.extractDataPoints(workbook);
    }

    //#region project

    @Test
    public void aProjectNeedsANameAndAStartDate() {
        CreationRequirements requirements =
                service.requirementsFor(CreationTarget.PROJECT, sessionWithNothingFound());

        assertTrue(requirements.isAvailable());
        assertTrue(isRequired(requirements, "name"));
        assertTrue(isRequired(requirements, "start_date"));
        assertFalse(isRequired(requirements, "shortname"));
        assertFalse(isRequired(requirements, "objective"));
    }

    @Test
    public void theProjectDatesAreSuggestedFromTheObservations() {
        CreationRequirements requirements =
                service.requirementsFor(CreationTarget.PROJECT, sessionWithNothingFound());

        RequiredField startDate = field(requirements, "start_date");
        assertEquals("2020-04-06", startDate.getSuggestedValue());
        assertEquals("AiImport.proposal.from.earliestObservation", startDate.getSuggestedFrom());

        assertEquals("2020-09-28", field(requirements, "end_date").getSuggestedValue());
    }

    //#endregion

    //#region experiment

    @Test
    public void anExperimentAlsoNeedsAnObjective() {
        CreationRequirements requirements =
                service.requirementsFor(CreationTarget.EXPERIMENT, sessionWithNothingFound());

        assertTrue(requirements.isAvailable());
        assertTrue(isRequired(requirements, "name"));
        assertTrue(isRequired(requirements, "start_date"));
        assertTrue("the experiment model demands one and no data entry file states it",
                isRequired(requirements, "objective"));
    }

    @Test
    public void theExperimentNameIsSuggestedFromTheTrial() {
        CreationRequirements requirements =
                service.requirementsFor(CreationTarget.EXPERIMENT, sessionWithNothingFound());

        RequiredField name = field(requirements, "name");
        assertEquals("CEPInnov_Champagne", name.getSuggestedValue());
        assertEquals("AiImport.proposal.from.trialColumn", name.getSuggestedFrom());
    }

    //#endregion

    //#region data

    @Test
    public void dataCannotBeInsertedWhileTheExperimentIsMissing() {
        CreationRequirements requirements =
                service.requirementsFor(CreationTarget.DATA, sessionWithNothingFound());

        assertFalse(requirements.isAvailable());
        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.experimentMissing"));
        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.variablesMissing"));
    }

    @Test
    public void dataCannotBeInsertedWhileVariablesAreOnlyOnASharedResourceInstance() {
        AiImportSession session = sessionWithEverythingFound();
        session.getReport().getVariables().get(0)
                .setStatus(ResolutionStatus.FOUND_IN_SHARED_RESOURCE);

        CreationRequirements requirements = service.requirementsFor(CreationTarget.DATA, session);

        assertFalse("a variable available elsewhere is not a variable in this instance",
                requirements.isAvailable());
        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.variablesMissing"));
    }

    @Test
    public void dataCannotBeInsertedWhileAVariableIsAmbiguous() {
        AiImportSession session = sessionWithEverythingFound();
        session.getReport().getVariables().get(0).setStatus(ResolutionStatus.AMBIGUOUS);

        CreationRequirements requirements = service.requirementsFor(CreationTarget.DATA, session);

        assertFalse(requirements.isAvailable());
        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.variablesAmbiguous"));
    }

    @Test
    public void dataCannotBeInsertedWhilePlotsAreMissing() {
        AiImportSession session = sessionWithEverythingFound();
        session.getReport().getScientificObjects().get(0).setStatus(ResolutionStatus.MISSING);

        CreationRequirements requirements = service.requirementsFor(CreationTarget.DATA, session);

        assertFalse(requirements.isAvailable());
        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.objectsMissing"));
    }

    @Test
    public void dataCanBeInsertedOnceEverythingResolves() {
        CreationRequirements requirements =
                service.requirementsFor(CreationTarget.DATA, sessionWithEverythingFound());

        assertTrue(requirements.getBlockers().toString(), requirements.isAvailable());
        assertTrue(isRequired(requirements, "experiment"));
        assertTrue(isRequired(requirements, "provenance_name"));
        assertEquals(EXPERIMENT.toString(), field(requirements, "experiment").getSuggestedValue());
        assertEquals("SaisieVitisExplorer20", field(requirements, "provenance_name").getSuggestedValue());
    }

    @Test
    public void aFileWithNoReadableObservationBlocksTheInsertion() {
        AiImportSession session = sessionWithEverythingFound();
        session.setDataPoints(List.of());

        CreationRequirements requirements = service.requirementsFor(CreationTarget.DATA, session);

        assertFalse(requirements.isAvailable());
        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.noDataPoints"));
    }

    /**
     * The STAR profile has to recompose plot identifiers before any observation can be attached.
     * That recomposition is a decision about the user's data, so the insertion asks them to accept
     * it — as a checkbox, not as a blocker: a blocker would take the decision away and simply
     * refuse, never asking.
     */
    @Test
    public void aRecomposedIdentifierHasToBeConfirmedBeforeInserting() {
        AiImportSession session = sessionWithEverythingFound();
        // A plan of its own: the shared one is read by every other test in this class, and a note
        // left on it would follow them.
        session.setPlan(new ExtractedImportPlan()
                .setProfileId(StarProfile.ID)
                .note(StarProfile.NOTE_RECONCILIATION,
                        "The plot sheet writes A1 where the data sheets write 1A."));

        CreationRequirements requirements = service.requirementsFor(CreationTarget.DATA, session);

        assertTrue("the confirmation must not make the insertion unavailable",
                requirements.isAvailable());
        RequiredField confirmation = field(requirements, "reconciliation_confirmed");
        assertTrue(confirmation.isRequired());
        assertEquals(RequiredField.KIND_BOOLEAN, confirmation.getKind());
        assertTrue("the user has to be shown what they are confirming",
                confirmation.getSuggestedValue().contains("A1"));

        // An unticked box submits "false", which is not a decision taken.
        assertFalse(confirmation.isSatisfiedBy(""));
        assertFalse(confirmation.isSatisfiedBy("false"));
        assertTrue(confirmation.isSatisfiedBy("true"));
    }

    /**
     * A file whose identifiers agree must not be asked to confirm anything.
     */
    @Test
    public void aFileThatNeededNoRecompositionIsNotAskedToConfirmOne() {
        CreationRequirements requirements =
                service.requirementsFor(CreationTarget.DATA, sessionWithEverythingFound());

        assertTrue(requirements.getFields().stream()
                .noneMatch(f -> "reconciliation_confirmed".equals(f.getName())));
    }

    //#endregion

    //#region importing variables

    /**
     * A variable that exists on a shared resource instance is the cheapest path to a resolved
     * variable: importing brings its entity, characteristic, method and unit with it, where
     * creating them here would add four permanent entries to this instance's referential.
     */
    @Test
    public void aVariableFoundOnASharedInstanceCanBeImported() {
        AiImportSession session = sessionWithEverythingFound();
        ResolvedItem item = session.getReport().getVariables().get(0);
        item.setStatus(ResolutionStatus.FOUND_IN_SHARED_RESOURCE)
                .setMatches(List.of(new ResourceReference(
                        URI.create("http://phenome.inrae.fr/id/variable/plant_height"), "Plant height")
                        .setSharedResourceInstance(SHARED_INSTANCE.toString())));

        CreationRequirements requirements = service.requirementsFor(CreationTarget.VARIABLE, session);

        assertTrue(requirements.getBlockers().toString(), requirements.isAvailable());
        assertTrue(field(requirements, "variable_summary").getSuggestedValue()
                .contains(SHARED_INSTANCE.toString()));
    }

    /**
     * Nothing to import is a blocker, not an empty form: the assistant has to say so and move on
     * to creating the components instead.
     */
    @Test
    public void nothingToImportBlocksTheVariableTarget() {
        CreationRequirements requirements =
                service.requirementsFor(CreationTarget.VARIABLE, sessionWithEverythingFound());

        assertFalse(requirements.isAvailable());
        assertTrue(requirements.getBlockers()
                .contains("AiImport.proposal.block.noImportableVariable"));
    }

    /**
     * Importing part of the file's variables leaves the rest to create, and saying so up front is
     * what stops the user thinking the job is done.
     */
    @Test
    public void variablesThatExistNowhereAreFlaggedAsStillToCreate() {
        AiImportSession session = sessionWithEverythingFound();
        List<ResolvedItem> variables = session.getReport().getVariables();
        variables.get(0).setStatus(ResolutionStatus.FOUND_IN_SHARED_RESOURCE)
                .setMatches(List.of(new ResourceReference(
                        URI.create("http://phenome.inrae.fr/id/variable/plant_height"), "Plant height")
                        .setSharedResourceInstance(SHARED_INSTANCE.toString())));
        variables.get(1).setStatus(ResolutionStatus.MISSING).setMatches(List.of());

        CreationRequirements requirements = service.requirementsFor(CreationTarget.VARIABLE, session);

        assertTrue(requirements.isAvailable());
        assertTrue(requirements.getWarnings()
                .contains("AiImport.proposal.warn.variablesStillMissing"));
    }

    /**
     * The import needs the core module to know where the declared instances are; without it the
     * service must say so rather than fail somewhere deeper.
     */
    @Test
    public void importingWithoutTheCoreModuleSaysWhatIsMissing() {
        AiImportSession session = sessionWithEverythingFound();

        try {
            service.importVariables(session, Map.of());
            fail("the import cannot work without the core module");
        } catch (Exception e) {
            assertTrue(String.valueOf(e.getMessage()), e instanceof IllegalStateException);
            assertTrue(e.getMessage().contains("core module"));
        }
    }

    //#endregion

    //#region insertion, all or nothing

    /**
     * The central guarantee of the insertion: one row that cannot be placed stops the whole thing.
     * <p>
     * The DAOs are null, so any attempt to write would throw rather than return a refusal. A test
     * that passes here has proved both halves at once: the refusal happens, and it happens before
     * anything is written.
     */
    @Test
    public void oneUnresolvedTargetRefusesTheWholeInsertion() throws Exception {
        AiImportSession session = sessionWithEverythingFound();
        ResolvedItem plot = session.getReport().getScientificObjects().get(0);
        plot.setStatus(ResolutionStatus.MISSING).setMatches(List.of());

        DataInsertionResult result = service.insertData(session, insertionValues());

        assertTrue("the insertion should have been refused", result.isRefused());
        assertEquals(0, result.getInsertedCount());
        assertTrue(result.getUnresolvedCount() > 0);

        UnresolvedRow row = result.getUnresolved().get(0);
        assertEquals("AiImport.proposal.unresolved.object", row.getReasonKey());
        assertEquals(plot.getSourceValue(), row.getValue());
        assertNotNull("the refusal has to name the sheet", row.getSheet());
        assertTrue("the refusal has to name the row", row.getRowNumber() > 0);
    }

    /**
     * A missing variable refuses too, and says so as a variable rather than as an object: the two
     * are fixed in different places.
     */
    @Test
    public void anUnresolvedVariableRefusesTheWholeInsertion() throws Exception {
        AiImportSession session = sessionWithEverythingFound();
        session.getReport().getVariables()
                .forEach(item -> item.setStatus(ResolutionStatus.MISSING).setMatches(List.of()));

        DataInsertionResult result = service.insertData(session, insertionValues());

        assertTrue(result.isRefused());
        assertEquals("AiImport.proposal.unresolved.variable",
                result.getUnresolved().get(0).getReasonKey());
    }

    /**
     * Weather is measured at the field, not on a micro plot, so a data sheet can point at a
     * facility. Resolving those against the scientific objects would have silently dropped them.
     */
    @Test
    public void aFacilityTargetResolvesAgainstTheFacilities() throws Exception {
        AiImportSession session = sessionWithEverythingFound();
        session.setDataPoints(List.of(new DataPoint()
                .setSheet("data_meteo")
                .setRowNumber(4)
                .setObjectName("Melgueil")
                .setTargetKind(DataPoint.TargetKind.FACILITY)
                .setDate(LocalDate.of(2020, 5, 12))
                .setVariableKey(firstVariableKey(session))
                .setRawValue("18.4")));
        session.getReport().getFacilities().add(new ResolvedItem("Melgueil")
                .setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(FACILITY, "Melgueil"))));

        List<DataCreationDTO> drafts = new ArrayList<>();
        List<UnresolvedRow> unresolved = service.resolveRows(session, drafts);

        assertTrue("a resolved facility must not be reported as unresolved: " + unresolved,
                unresolved.isEmpty());
        assertEquals(1, drafts.size());
        assertEquals(FACILITY, drafts.get(0).getTarget());
    }

    @Test
    public void anUnknownFacilityRefusesAsAFacility() throws Exception {
        AiImportSession session = sessionWithEverythingFound();
        session.setDataPoints(List.of(new DataPoint()
                .setSheet("data_meteo")
                .setRowNumber(4)
                .setObjectName("Melgueil")
                .setTargetKind(DataPoint.TargetKind.FACILITY)
                .setDate(LocalDate.of(2020, 5, 12))
                .setVariableKey(firstVariableKey(session))
                .setRawValue("18.4")));

        DataInsertionResult result = service.insertData(session, insertionValues());

        assertTrue(result.isRefused());
        UnresolvedRow row = result.getUnresolved().get(0);
        assertEquals("AiImport.proposal.unresolved.facility", row.getReasonKey());
        assertEquals("data_meteo", row.getSheet());
        assertEquals(4, row.getRowNumber());
    }

    /**
     * The refusal shows a sample rather than thousands of lines, but says how many there are.
     */
    @Test
    public void theRefusalSamplesTheRowsAndCountsThemAll() throws Exception {
        AiImportSession session = sessionWithEverythingFound();
        session.getReport().getScientificObjects()
                .forEach(item -> item.setStatus(ResolutionStatus.MISSING).setMatches(List.of()));

        DataInsertionResult result = service.insertData(session, insertionValues());

        assertTrue(result.getUnresolvedCount() > result.getUnresolved().size());
        assertEquals(session.getDataPoints().size(), result.getUnresolvedCount());
    }

    /**
     * A data sheet whose observations sit on a facility must be blocked while that facility is
     * unknown, so that the refusal arrives before the user clicks rather than after.
     */
    @Test
    public void aMissingFacilityBlocksTheInsertionUpFront() {
        AiImportSession session = sessionWithEverythingFound();
        session.setDataPoints(List.of(new DataPoint()
                .setSheet("data_meteo")
                .setRowNumber(4)
                .setObjectName("Melgueil")
                .setTargetKind(DataPoint.TargetKind.FACILITY)
                .setDate(LocalDate.of(2020, 5, 12))
                .setVariableKey(firstVariableKey(session))
                .setRawValue("18.4")));
        session.getReport().getFacilities()
                .add(new ResolvedItem("Melgueil").setStatus(ResolutionStatus.MISSING));

        CreationRequirements requirements = service.requirementsFor(CreationTarget.DATA, session);

        assertFalse(requirements.isAvailable());
        assertTrue(requirements.getBlockers()
                .contains("AiImport.proposal.block.facilitiesMissing"));
    }

    /**
     * The same missing facility must not block a file that never measures anything at one.
     */
    @Test
    public void aMissingFacilityDoesNotBlockAFileThatIgnoresFacilities() {
        AiImportSession session = sessionWithEverythingFound();
        session.getReport().getFacilities()
                .add(new ResolvedItem("Melgueil").setStatus(ResolutionStatus.MISSING));

        CreationRequirements requirements = service.requirementsFor(CreationTarget.DATA, session);

        assertTrue(requirements.getBlockers().toString(), requirements.isAvailable());
    }

    //#endregion

    //#region session fixtures

    private AiImportSession sessionWithNothingFound() {
        AiImportSession session = new AiImportSession("test", ACCOUNT);
        session.setFileName(WorkbookFixture.VITIS_FILE_NAME)
                .setWorkbook(workbook)
                .setPlan(plan)
                .setDataPoints(dataPoints);

        ResolutionReport report = new ResolutionReport();
        plan.getExperimentNames().forEach(name -> report.getExperiments()
                .add(new ResolvedItem(name).setStatus(ResolutionStatus.MISSING)));
        for (VariableCandidate candidate : plan.getVariables()) {
            report.getVariables().add(new ResolvedItem(candidate.getColumnKey())
                    .setStatus(ResolutionStatus.MISSING));
        }
        session.setReport(report);
        return session;
    }

    private AiImportSession sessionWithEverythingFound() {
        AiImportSession session = sessionWithNothingFound();
        ResolutionReport report = new ResolutionReport();

        report.getExperiments().add(new ResolvedItem("CEPInnov_Champagne")
                .setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(EXPERIMENT, "CEPInnov_Champagne"))));

        for (VariableCandidate candidate : plan.getVariables()) {
            report.getVariables().add(new ResolvedItem(candidate.getColumnKey())
                    .setStatus(ResolutionStatus.FOUND)
                    .setMatches(List.of(new ResourceReference(
                            URI.create("http://opensilex.test/id/variable/"
                                    + candidate.getColumnKey().toLowerCase()),
                            candidate.getColumnKey()))));
        }
        for (String plot : plan.getScientificObjectNames()) {
            report.getScientificObjects().add(new ResolvedItem(plot)
                    .setStatus(ResolutionStatus.FOUND)
                    .setMatches(List.of(new ResourceReference(
                            URI.create("http://opensilex.test/id/object/plot" + plot), plot))));
        }
        session.setReport(report);
        return session;
    }

    //#endregion

    //#region helpers

    private Map<String, String> insertionValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("experiment", EXPERIMENT.toString());
        values.put("provenance_name", "test");
        return values;
    }

    private String firstVariableKey(AiImportSession session) {
        return session.getReport().getVariables().get(0).getSourceValue();
    }


    private RequiredField field(CreationRequirements requirements, String name) {
        RequiredField found = requirements.getFields().stream()
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .orElse(null);
        assertNotNull("no field named " + name, found);
        return found;
    }

    private boolean isRequired(CreationRequirements requirements, String name) {
        return field(requirements, name).isRequired();
    }

    //#endregion
}
