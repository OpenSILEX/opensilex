//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.miappe;

import org.junit.Test;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The same profile against a submission somebody actually filled in.
 * <p>
 * The training spreadsheet ships empty, so a filled one is built here rather than shipped: the
 * point is the reading rules, not the file format, and the format is already covered by the tests
 * that read the real workbook. The rows follow the checklist's own examples, documentation rows
 * included, because those rows are exactly what the reader has to step over.
 *
 * @author Arnaud Charleroy
 */
public class FilledMiappeSubmissionTest {

    private final MiappeProfile profile = new MiappeProfile();
    private final WorkbookStructure workbook = filledSubmission();
    private final ExtractedImportPlan plan = profile.extract(workbook);

    @Test
    public void theInvestigationBecomesAProject() {
        assertEquals(Arrays.asList("Maize adaptation to temperate climates"), plan.getProjectNames());
    }

    @Test
    public void theStudyBecomesAnExperimentAndItsSiteAFacility() {
        assertEquals(Arrays.asList("Flowering time at Mauguio 2002"), plan.getExperimentNames());
        assertEquals(Arrays.asList("INRA Diascope"), plan.getFacilityNames());
        assertEquals("2002-04-04", plan.getNotes().get("study.startDate"));
    }

    @Test
    public void theObservationUnitsBecomeScientificObjects() {
        assertEquals(Arrays.asList("plot:894", "plot:895"), plan.getScientificObjectNames());
        assertEquals("plot", plan.getNotes().get("observationUnit.types"));
    }

    @Test
    public void theBiologicalMaterialBecomesGermplasm() {
        assertEquals(Arrays.asList("INRA:W95115"), plan.getGermplasmNames());
        assertEquals("Zea mays B73", plan.getNotes().get("material.botanicalName"));
    }

    /**
     * The reason MIAPPE matters here: it is the only supported template that says what a variable
     * is made of, so a missing variable can be proposed for creation rather than only reported.
     */
    @Test
    public void aVariableArrivesWithItsComponents() {
        assertEquals(1, plan.getVariables().size());
        VariableCandidate variable = plan.getVariables().get(0);

        assertEquals("Ant_Cmp_Cday", variable.getColumnKey());
        assertEquals("Anthesis computed in growing degree days", variable.getLabel());
        assertEquals("CO_322:0000794", variable.getExternalId());

        assertTrue(variable.hasComponents());
        assertEquals("Anthesis time", variable.getTrait().getName());
        assertEquals("CO_322:0000030", variable.getTrait().getAccession());
        assertEquals("Growing degree days to anthesis", variable.getMethod().getName());
        assertEquals("°C day", variable.getUnit().getName());
        assertEquals("CO_322:0000510", variable.getUnit().getAccession());
    }

    /**
     * The trait is not split into an entity and a characteristic by the reader.
     * <p>
     * "Anthesis time" is a characteristic of something, and which something is a judgement about
     * the user's science. The resolution offers the trait as a characteristic — the closer of the
     * two — and leaves the entity for the user to choose in the form; a wrong entity created here
     * would sit in the instance's referential for good.
     */
    @Test
    public void theTraitIsCarriedWholeRatherThanSplit() {
        VariableCandidate variable = plan.getVariables().get(0);

        // Carried exactly as the file wrote it. The alternative — splitting "Anthesis time" into an
        // entity and a characteristic here — would put a guess about the user's science into the
        // referential, so the split happens in the form where they can see and correct it.
        assertEquals("Anthesis time", variable.getTrait().getName());
        assertEquals("CO_322:0000030", variable.getTrait().getAccession());

        // And the two components the checklist does map one-to-one arrive intact.
        assertEquals("Growing degree days to anthesis", variable.getMethod().getName());
        assertEquals("°C day", variable.getUnit().getName());
    }

    /**
     * A MIAPPE event names the observation unit it concerned, unlike a trial-wide STAR event.
     */
    @Test
    public void anEventConcernsTheObservationUnitItNames() {
        List<EventCandidate> events = profile.extractEvents(workbook);

        assertEquals(1, events.size());
        EventCandidate event = events.get(0);
        assertEquals(LocalDate.of(2002, 4, 4), event.getDate());
        assertEquals(Arrays.asList("plot:894"), event.getTargetNames());
        assertEquals("Planting : Sowing using seed drill", event.toEventDescription());
    }

    /**
     * MIAPPE dates carry an optional time and zone; only the day is kept.
     */
    @Test
    public void anEventDatedToTheSecondIsKeptToTheDay() {
        WorkbookStructure withTime = filledSubmission();
        section(withTime, MiappeSheets.EVENT).getRows().get(4)
                .set(6, "2006-09-27T10:23:21+00:00");

        assertEquals(LocalDate.of(2006, 9, 27), profile.extractEvents(withTime).get(0).getDate());
    }

    /**
     * A submission that fills only part of the checklist must be told which mandatory fields it
     * still owes, field by field rather than as a count.
     */
    @Test
    public void theMissingMandatoryFieldsAreNamed() {
        String anomalies = String.join(" | ", plan.getAnomalies());

        assertTrue(anomalies, anomalies.contains("Contact institution"));
        assertFalse("a filled field must not be reported missing",
                anomalies.contains("Study title"));
    }

    //#region a submission with values under the documentation

    private WorkbookStructure filledSubmission() {
        List<SheetStructure> sheets = new ArrayList<>();

        sheets.add(transposed(MiappeSheets.INVESTIGATION, Arrays.asList(
                row("Investigation unique ID", "EBI:12345678"),
                row("Investigation title*", "Maize adaptation to temperate climates"),
                row("MIAPPE version*", "1.1"))));

        sheets.add(tabular(MiappeSheets.STUDY,
                headers("Study unique ID*", "Study title*", "Study description",
                        "Start date of study*", "End date of study", "Contact institution*",
                        "Experimental site name*"),
                Arrays.asList(
                        documentation("Definition"),
                        documentation("Example"),
                        documentation("Format"),
                        documentation("Values (add rows if necessary)"),
                        row("", "study_1", "Flowering time at Mauguio 2002",
                                "Male and female flowering time", "2002-04-04", "2002-09-27",
                                "", "INRA Diascope"))));

        sheets.add(tabular(MiappeSheets.OBSERVATION_UNIT,
                headers("Study unique ID*", "Biological Material ID*", "Observation unit ID*",
                        "Observation unit type*"),
                Arrays.asList(
                        documentation("Definition"),
                        documentation("Example"),
                        documentation("Format"),
                        row("", "study_1", "INRA:W95115", "plot:894", "plot"),
                        row("", "study_1", "INRA:W95115", "plot:895", "plot"))));

        sheets.add(tabular(MiappeSheets.BIOLOGICAL_MATERIAL,
                headers("Study unique ID*", "Biological material ID*", "Organism*", "Genus",
                        "Species", "Infraspecific name"),
                Arrays.asList(
                        documentation("Definition"),
                        documentation("Example"),
                        documentation("Format"),
                        row("", "study_1", "INRA:W95115", "NCBITAXON:4577", "Zea", "mays", "B73"))));

        sheets.add(tabular(MiappeSheets.OBSERVED_VARIABLE,
                headers("Study unique ID*", "Variable ID*", "Variable name",
                        "Variable accession number", "Trait*", "Trait accession number",
                        "Method*", "Method accession number", "Scale*", "Scale accession number"),
                Arrays.asList(
                        documentation("Definition"),
                        documentation("Example"),
                        documentation("Format"),
                        row("", "study_1", "Ant_Cmp_Cday",
                                "Anthesis computed in growing degree days", "CO_322:0000794",
                                "Anthesis time", "CO_322:0000030",
                                "Growing degree days to anthesis", "CO_322:0000189",
                                "°C day", "CO_322:0000510"))));

        sheets.add(tabular(MiappeSheets.EVENT,
                headers("Study unique ID*", "Observation unit ID", "Event type*",
                        "Event accession number", "Event description", "Event date*"),
                Arrays.asList(
                        documentation("Definition"),
                        documentation("Example"),
                        documentation("Format"),
                        documentation("Values (add rows if necessary)"),
                        row("", "study_1", "plot:894", "Planting", "CO_715:0000007",
                                "Sowing using seed drill", "2002-04-04"))));

        return new WorkbookStructure()
                .setFileName("filled_miappe.xlsx")
                .setSheets(sheets);
    }

    private SheetStructure section(WorkbookStructure workbook, String name) {
        return workbook.getSheet(name).orElseThrow(AssertionError::new);
    }

    private SheetStructure transposed(String name, List<List<String>> rows) {
        return tabular(name, headers("Value", "Definition", "Example", "Format"), rows);
    }

    private SheetStructure tabular(String name, List<String> headers, List<List<String>> rows) {
        return new SheetStructure()
                .setName(name)
                .setTabular(true)
                .setHeaders(headers)
                .setRows(rows);
    }

    /**
     * The first column of every section holds the row's own label, not a field.
     */
    private List<String> headers(String... fields) {
        List<String> headers = new ArrayList<>();
        headers.add("Field");
        headers.addAll(Arrays.asList(fields));
        return headers;
    }

    private List<String> row(String... cells) {
        return new ArrayList<>(Arrays.asList(cells));
    }

    private List<String> documentation(String label) {
        return row(label, "…", "…", "…");
    }

    //#endregion
}
