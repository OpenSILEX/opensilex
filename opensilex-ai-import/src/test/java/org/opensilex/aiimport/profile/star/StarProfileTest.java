//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.GenericTabularProfile;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Checked against both revisions of the STAR workbook, because supporting only one of them would
 * mean supporting neither for long.
 *
 * @author Arnaud Charleroy
 */
public class StarProfileTest {

    private static WorkbookStructure standard;
    private static WorkbookStructure example;
    private static ExtractedImportPlan standardPlan;
    private static ExtractedImportPlan examplePlan;

    private final StarProfile profile = new StarProfile();

    @BeforeClass
    public static void readBothRevisions() throws Exception {
        StarProfile profile = new StarProfile();
        standard = WorkbookFixture.starStandard();
        example = WorkbookFixture.starExample();
        standardPlan = profile.extract(standard);
        examplePlan = profile.extract(example);
    }

    //#region recognition

    @Test
    public void bothRevisionsAreRecognised() {
        assertEquals(100, profile.match(standard));
        assertEquals(100, profile.match(example));
    }

    @Test
    public void theRegistryPicksTheRightProfileForEachFile() throws Exception {
        ImportProfileRegistry registry = new ImportProfileRegistry();

        assertEquals(StarProfile.ID, registry.select(standard).getId());
        assertEquals(StarProfile.ID, registry.select(example).getId());
        assertEquals("a grapevine observation file must still get its own profile",
                VitisExplorerProfile.ID, registry.select(WorkbookFixture.vitis()).getId());
    }

    @Test
    public void aFileFromAnotherFamilyIsNotClaimed() throws Exception {
        assertEquals(0, profile.match(WorkbookFixture.vitis()));
        assertTrue(new GenericTabularProfile().match(WorkbookFixture.vitis()) > 0);
    }

    //#endregion

    //#region what the file states

    @Test
    public void theExperimentIsNamedAndItsObjectiveIsStated() {
        assertEquals(List.of("IFV30_teisso_2024"), standardPlan.getExperimentNames());
        assertNotNull("STAR states the objective, which no field observation file does",
                standardPlan.getNotes().get("objective"));
        assertTrue(standardPlan.getNotes().get("objective").contains("biocontrôle"));
    }

    @Test
    public void theExperimentDatesAreRead() {
        assertEquals("2024-01-01", standardPlan.getNotes().get("declared start date"));
        assertEquals("2024-12-31", standardPlan.getNotes().get("declared end date"));
    }

    @Test
    public void theFieldBecomesAFacilityAndItsCultivarAGermplasm() {
        assertEquals(List.of("teissonniere"), standardPlan.getFacilityNames());
        assertEquals(List.of("Grenache blanc"), standardPlan.getGermplasmNames());
        assertEquals("Bellegarde", standardPlan.getNotes().get("commune"));
    }

    @Test
    public void thePlotsBecomeScientificObjects() {
        assertEquals(44, standardPlan.getScientificObjectNames().size());
        assertTrue(standardPlan.getScientificObjectNames().contains("A1"));
        assertEquals("44", standardPlan.getNotes().get("unit plots"));
        // Reading order, not alphabetical: distinctValues keeps the order of first appearance.
        String blocks = standardPlan.getNotes().get("blocks");
        assertNotNull(blocks);
        for (String block : List.of("A", "B", "C", "D")) {
            assertTrue("block " + block + " missing from " + blocks, blocks.contains(block));
        }
    }

    @Test
    public void theTreatmentsAreSurfaced() {
        String treatments = standardPlan.getNotes().get("treatments");
        assertNotNull(treatments);
        assertTrue(treatments.contains("TNT"));
    }

    @Test
    public void theVariablesCarryTheirOntologyUri() {
        VariableCandidate mildew = candidate(standardPlan, "PM_BER_PC");
        assertNotNull(mildew);
        assertEquals("https://cropontology.org/term/CO_356:1000172", mildew.getExternalId());
        assertTrue(mildew.getLabel().contains("mildiou"));

        assertEquals("the reference template declares ten", 10, standardPlan.getVariables().size());
        assertEquals("the earlier revision declares eleven", 11, examplePlan.getVariables().size());
    }

    //#endregion

    //#region column roles

    @Test
    public void theDictionaryDrivesTheRoles() {
        assertEquals(ColumnRole.VARIABLE, profile.roleOf(standard, "data_F1", "PM_LEAF_PC"));
        assertEquals(ColumnRole.OBJECT, profile.roleOf(standard, "ed_placette", "plot_id"));
        assertEquals(ColumnRole.TRIAL, profile.roleOf(standard, "expe", "expe_id"));
        assertEquals(ColumnRole.PROJECT, profile.roleOf(standard, "expe", "proj_id"));
        assertEquals(ColumnRole.GERMPLASM, profile.roleOf(standard, "ed_parcelle", "cultivar_name"));
        assertEquals(ColumnRole.FACTOR_LEVEL, profile.roleOf(standard, "ed_placette", "xp_trt_code"));
        assertEquals(ColumnRole.DATE, profile.roleOf(standard, "data_F1", "observation_date"));
        assertEquals(ColumnRole.LOCATION, profile.roleOf(standard, "ed_parcelle", "field_latitude"));
    }

    @Test
    public void aColumnTheDictionaryIgnoresIsNotCalledAMeasurement() {
        assertEquals(ColumnRole.UNKNOWN, profile.roleOf(standard, "data_F1", "quelque_chose"));
    }

    //#endregion

    //#region what is raised

    @Test
    public void theRainfallColumnLeftOutOfTheVariablesIsRaised() {
        // The reference template files rain_mm under metadata, so an import driven by it would
        // quietly drop rainfall from data_meteo.
        assertTrue(anomaly(standardPlan, "rain_mm").isPresent());
        assertTrue(anomaly(standardPlan, "does not declare").isPresent());

        assertFalse("the earlier revision declares it, so there is nothing to raise there",
                anomaly(examplePlan, "rain_mm").isPresent());
    }

    @Test
    public void theMisspeltTypeIsMentionedOnce() {
        assertTrue(anomaly(standardPlan, "interger").isPresent());
    }

    @Test
    public void thePlotIdentifierMismatchIsRaisedOnBothRevisions() {
        assertTrue(anomaly(standardPlan, "block then treatment").isPresent());
        assertTrue(anomaly(examplePlan, "block then treatment").isPresent());
    }

    //#endregion

    //#region observations

    /**
     * The observations are extracted despite the identifier mismatch, recomposed, because the
     * decision to accept the recomposition belongs to the confirmation before writing — not to the
     * reading. Returning nothing here used to hide the disagreement rather than raise it: the page
     * showed no observation and no reason.
     */
    @Test
    public void theObservationsAreExtractedAndTheMismatchIsStated() {
        assertFalse(profile.extractDataPoints(standard).isEmpty());
        assertFalse(profile.extractDataPoints(example).isEmpty());

        String note = standardPlan.getNotes().get(StarProfile.NOTE_RECONCILIATION);
        assertNotNull("the recomposition has to be stated for the user to confirm it", note);
        assertTrue(note, note.contains("block then treatment"));
    }

    /**
     * Pinned, because every silent loss found so far showed up as a count quietly dropping.
     * <p>
     * The two revisions differ by design: the reference template files {@code rain_mm} under
     * metadata rather than variables — a real defect the profile reports — so its weather sheet
     * yields six columns where the earlier revision yields seven.
     */
    @Test
    public void everyObservationOfBothRevisionsIsRead() {
        assertEquals(5564, profile.extractDataPoints(standard).size());
        assertEquals(5685, profile.extractDataPoints(example).size());

        assertEquals(726, pointsOf(standard, "data_meteo"));
        assertEquals("the earlier revision names the sheet 'meteo', and it must not be skipped",
                847, pointsOf(example, "meteo"));
    }

    private long pointsOf(WorkbookStructure workbook, String sheet) {
        return profile.extractDataPoints(workbook).stream()
                .filter(point -> sheet.equals(point.getSheet()))
                .count();
    }

    @Test
    public void weatherAttachesToTheFieldRatherThanToAPlot() {
        // data_meteo names field_id, not plot_id: it is measured at the field, which is a facility.
        var points = profile.extractDataPoints(standard);

        var weather = points.stream()
                .filter(point -> "data_meteo".equals(point.getSheet()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals(DataPoint.TargetKind.FACILITY, weather.getTargetKind());
        assertEquals("teissonniere", weather.getObjectName());
        assertTrue(standardPlan.getFacilityNames().contains(weather.getObjectName()));
    }

    @Test
    public void theIdentifierIsFoundWhateverItsPosition() {
        // data_F1 puts plot_id third, data_G1 first. The template shows the meaning, not the order.
        var points = profile.extractDataPoints(standard);

        for (String sheet : List.of("data_F1", "data_G1")) {
            assertTrue(sheet + " yielded nothing", points.stream()
                    .anyMatch(point -> sheet.equals(point.getSheet())));
        }
    }

    @Test
    public void thePedagogicalTemplateYieldsNoObservation() {
        assertTrue("data_template holds no rows and is not data",
                profile.extractDataPoints(standard).stream()
                        .noneMatch(point -> "data_template".equals(point.getSheet())));
    }

    @Test
    public void aSheetNamingNoTargetIsRaised() throws Exception {
        // The earlier revision's weather sheet has no identifier column at all; the reference
        // template fixed that by adding field_id.
        ExtractedImportPlan plan = new StarProfile().extract(WorkbookFixture.starExample());

        assertFalse("that sheet is not prefixed data_, so it is not treated as observations",
                plan.getAnomalies().stream().anyMatch(a -> a.contains("data_meteo")));
    }

    @Test
    public void observationsAreReturnedOnceTheReconciliationIsConfirmed() {
        var points = profile.extractDataPoints(standard);
        assertFalse(points.isEmpty());

        var first = points.stream()
                .filter(point -> "data_F1".equals(point.getSheet()))
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertEquals("PM_LEAF_PC", first.getVariableKey());
        assertTrue("the identifier must be the one the plot sheet declares",
                standardPlan.getScientificObjectNames().contains(first.getObjectName()));
        assertNotNull(first.getDate());
    }

    //#endregion

    /**
     * @return a profile that behaves as though the user had accepted the plot identifier
     * reconciliation
     */

    private VariableCandidate candidate(ExtractedImportPlan plan, String columnKey) {
        return plan.getVariables().stream()
                .filter(variable -> variable.getColumnKey().equalsIgnoreCase(columnKey))
                .findFirst()
                .orElse(null);
    }

    private Optional<String> anomaly(ExtractedImportPlan plan, String fragment) {
        return plan.getAnomalies().stream()
                .filter(anomaly -> anomaly.contains(fragment))
                .findFirst();
    }

    //#region events

    /**
     * The trial's own record of what happened is worth as much as its measurements, and it was
     * being dropped: the sheet was read, classified, and then ignored.
     */
    @Test
    public void theEventSheetBecomesEvents() {
        List<EventCandidate> events = profile.extractEvents(example);

        List<EventCandidate> fromSheet = events.stream()
                .filter(event -> StarSheets.EVENT_SHEET.equals(event.getSheet()))
                .collect(Collectors.toList());

        assertEquals(5, fromSheet.size());
        EventCandidate first = fromSheet.get(0);
        assertEquals(LocalDate.of(2024, 6, 5), first.getDate());
        assertEquals("Observation", first.getTypeLabel());
        assertEquals("Observation : observation sur feuilles", first.toEventDescription());
    }

    /**
     * A trial-wide event names no target, and the sheet sits at trial level: it concerns the field.
     */
    @Test
    public void aTrialEventConcernsTheField() {
        EventCandidate event = profile.extractEvents(example).stream()
                .filter(candidate -> StarSheets.EVENT_SHEET.equals(candidate.getSheet()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals(DataPoint.TargetKind.FACILITY, event.getTargetKind());
        assertEquals(1, event.getTargetNames().size());
    }

    /**
     * A spraying is recorded once per treatment but happened on every plot of that treatment, so
     * one row becomes one event concerning them all.
     */
    @Test
    public void aTreatmentApplicationConcernsThePlotsOfItsTreatment() {
        EventCandidate application = profile.extractEvents(example).stream()
                .filter(candidate -> StarSheets.TREATMENT_APPLICATION_SHEET.equals(candidate.getSheet()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals(DataPoint.TargetKind.SCIENTIFIC_OBJECT, application.getTargetKind());
        assertFalse("the plots of the treatment should be named",
                application.getTargetNames().isEmpty());

        // The vocabulary has no class for a spraying, so everything the file said has to survive
        // in the description or it is lost.
        String description = application.toEventDescription();
        assertTrue(description, description.contains("BOUILLIE BORDELAISE RSR DISPERSS"));
        assertTrue(description, description.contains("AMM 9500452"));
        assertTrue(description, description.contains("2 Kg/ha"));
    }

    /**
     * The reference template ships the same example rows as the filled workbook, so both revisions
     * must read them the same way — the point of supporting two revisions at all.
     */
    @Test
    public void bothRevisionsReadTheSameEvents() {
        assertEquals(profile.extractEvents(example).size(), profile.extractEvents(standard).size());
    }

    //#endregion

    //#region what the file cannot settle

    /**
     * Four columns of the plot sheet mean four different things, and three of them were wrong.
     */
    @Test
    public void thePlotColumnsAreToldApart() {
        assertEquals("a coordinate is a dated move, not an attribute",
                ColumnRole.POSITION, profile.roleOf(standard, "ed_placette", "plot_x"));
        assertEquals("a block groups plots; it is not a treatment",
                ColumnRole.PARENT_OBJECT, profile.roleOf(standard, "ed_placette", "block_code"));
        assertEquals("a treatment code names a level of a factor of the experiment",
                ColumnRole.FACTOR_LEVEL, profile.roleOf(standard, "ed_placette", "xp_trt_code"));
        assertEquals("a plant count describes the object",
                ColumnRole.OBJECT_PROPERTY, profile.roleOf(standard, "ed_placette", "plot_n"));
        assertEquals("the field's coordinates locate the facility",
                ColumnRole.LOCATION, profile.roleOf(standard, "ed_parcelle", "field_latitude"));
    }

    /**
     * Two modelling choices the file cannot make, raised rather than defaulted: creating several
     * hundred objects under a type nobody chose is not undone by editing one of them.
     */
    @Test
    public void theTypeOfTheObjectsAndTheShapeOfTheBlocksAreAsked() {
        String anomalies = String.join(" | ", standardPlan.getAnomalies());

        assertTrue(anomalies, anomalies.contains("never says what a plot is"));
        assertTrue(anomalies, anomalies.contains("A block can be a scientific object of its own"));
    }

    //#endregion
}
