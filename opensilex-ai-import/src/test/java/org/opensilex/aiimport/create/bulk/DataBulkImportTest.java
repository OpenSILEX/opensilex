//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The CSV written for the platform's data import, and the refusals made before it is asked.
 * <p>
 * Every DAO here is null, so any attempt to write would throw rather than return a refusal: a
 * refusal test that passes has proved both that the refusal happens and that it happens before
 * anything is written — the guarantee the insertion has always given, kept through the platform's
 * import.
 *
 * @author Arnaud Charleroy
 */
public class DataBulkImportTest {

    private static final URI ACCOUNT = URI.create("http://opensilex.test/id/account/alice");
    private static final URI EXPERIMENT = URI.create("http://opensilex.test/id/experiment/cepinnov");
    private static final URI FACILITY = URI.create("http://opensilex.test/id/facility/melgueil");

    private static WorkbookStructure workbook;
    private static ExtractedImportPlan plan;
    private static List<DataPoint> dataPoints;

    private final DataBulkImport importer = new DataBulkImport(null, null, null, null);

    @BeforeClass
    public static void readTheSampleFile() throws Exception {
        VitisExplorerProfile profile = new VitisExplorerProfile();
        workbook = WorkbookFixture.vitis();
        plan = profile.extract(workbook);
        dataPoints = profile.extractDataPoints(workbook);
    }

    //#region refusals, before anything is written

    @Test
    public void oneUnresolvedTargetRefusesTheWholeInsertion() throws Exception {
        AiImportSession session = sessionWithEverythingFound();
        ResolvedItem plot = session.getReport().getScientificObjects().get(0);
        plot.setStatus(ResolutionStatus.MISSING).setMatches(List.of());

        BulkOutcome outcome = importer.importAll(session, values());

        assertTrue("the insertion should have been refused", outcome.isRefused());
        assertEquals(0, outcome.getImported());
        RowError error = outcome.getErrors().get(0);
        assertEquals(RowError.Kind.UNRESOLVED, error.getKind());
        assertEquals("AiImport.proposal.unresolved.object", error.getMessage().getKey());
        assertEquals(plot.getSourceValue(), error.getValue());
        assertNotNull("the refusal has to name the sheet", error.getSheet());
        assertTrue("the refusal has to name the row", error.getRow() > 0);
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

        BulkOutcome outcome = importer.importAll(session, values());

        assertTrue(outcome.isRefused());
        assertEquals("AiImport.proposal.unresolved.variable",
                outcome.getErrors().get(0).getMessage().getKey());
    }

    @Test
    public void anUnknownFacilityRefusesAsAFacility() throws Exception {
        AiImportSession session = sessionWithEverythingFound();
        session.setDataPoints(List.of(weather("Melgueil", "18.4")));

        BulkOutcome outcome = importer.importAll(session, values());

        assertTrue(outcome.isRefused());
        RowError error = outcome.getErrors().get(0);
        assertEquals("AiImport.proposal.unresolved.facility", error.getMessage().getKey());
        assertEquals("data_meteo", error.getSheet());
        assertEquals(4, error.getRow());
    }

    /**
     * Every row in the way is reported, not a sample: the grid shows them on the user's sheets.
     */
    @Test
    public void everyUnplacedObservationIsReported() throws Exception {
        AiImportSession session = sessionWithEverythingFound();
        session.getReport().getScientificObjects()
                .forEach(item -> item.setStatus(ResolutionStatus.MISSING).setMatches(List.of()));

        BulkOutcome outcome = importer.importAll(session, values());

        assertEquals(session.getDataPoints().size(), outcome.getErrors().size());
    }

    /**
     * The platform's format has one cell per variable and line: a second, different value for the
     * same row, target, date and variable is refused rather than silently dropped.
     */
    @Test
    public void twoValuesForTheSameCellAreRefused() {
        AiImportSession session = sessionWithEverythingFound();
        withMelgueil(session);
        session.setDataPoints(List.of(weather("Melgueil", "18.4"), weather("Melgueil", "19.0")));

        GeneratedCsv csv = importer.generate(session, values());

        assertEquals(1, csv.getModuleErrors().size());
        assertEquals(RowError.Kind.DUPLICATE_IN_FILE, csv.getModuleErrors().get(0).getKind());
    }

    //#endregion

    //#region the generated CSV

    /**
     * Three header lines — identifiers, labels, descriptions — then the data: target and date first,
     * one column per variable URI, labelled with the workbook's own header.
     */
    @Test
    public void theCsvIsTheOneTheDataImportReads() {
        AiImportSession session = sessionWithEverythingFound();

        GeneratedCsv csv = importer.generate(session, values());
        String[] lines = csv.render().split("\n");

        assertTrue(csv.getModuleErrors().toString(), csv.getModuleErrors().isEmpty());
        assertTrue(lines[0], lines[0].startsWith("\"target\",\"date\",\"http://opensilex.test/id/variable/"));
        assertTrue(lines[1], lines[1].startsWith("\"target\",\"date\","));
        assertFalse("the labels are the workbook's headers, not URIs", lines[1].contains("http://"));
        assertTrue(lines[3], lines[3].startsWith("\"http://opensilex.test/id/object/plot"));
        assertTrue("an ISO date", lines[3].matches("^\"[^\"]+\",\"\\d{4}-\\d{2}-\\d{2}\",.*"));
        assertTrue("never more lines than observations",
                csv.getDataLineCount() <= session.getDataPoints().size());
    }

    /**
     * Weather is measured at the field, not on a plot: a facility goes in the target column, which
     * accepts any resource, instead of being dropped or refused.
     */
    @Test
    public void aFacilityGoesInTheTargetColumn() {
        AiImportSession session = sessionWithEverythingFound();
        withMelgueil(session);
        session.setDataPoints(List.of(weather("Melgueil", "18.4")));

        GeneratedCsv csv = importer.generate(session, values());

        assertTrue(csv.getModuleErrors().toString(), csv.getModuleErrors().isEmpty());
        assertEquals(1, csv.getDataLineCount());
        assertTrue(csv.render().split("\n")[3].startsWith("\"" + FACILITY + "\",\"2020-05-12\","));
    }

    /**
     * Two variables measured on the same row, target and date share one line; a variable that row
     * did not measure is an empty cell, which the platform skips.
     */
    @Test
    public void oneLinePerRowTargetAndDate() {
        AiImportSession session = sessionWithEverythingFound();
        withMelgueil(session);
        String temperature = variableKey(session, 0);
        String rain = variableKey(session, 1);
        session.setDataPoints(List.of(
                weather("Melgueil", "18.4").setVariableKey(temperature),
                weather("Melgueil", "2.5").setVariableKey(rain),
                weather("Melgueil", "17.9").setVariableKey(temperature).setRowNumber(5)));

        GeneratedCsv csv = importer.generate(session, values());
        String[] lines = csv.render().split("\n");

        assertEquals(2, csv.getDataLineCount());
        assertTrue(lines[3], lines[3].endsWith(",\"18.4\",\"2.5\""));
        assertTrue("row 5 measured the temperature only", lines[4].endsWith(",\"17.9\",\"\""));
    }

    /**
     * A batch starts from its own first line but keeps the three header lines: the platform reads
     * every batch as a file of its own.
     */
    @Test
    public void aBatchKeepsItsHeaders() {
        AiImportSession session = sessionWithEverythingFound();
        GeneratedCsv csv = importer.generate(session, values());

        String[] batch = csv.render(1, 2).split("\n");

        assertEquals(4, batch.length);
        assertEquals(csv.render().split("\n")[4], batch[3]);
    }

    //#endregion

    //#region fixtures

    private AiImportSession sessionWithEverythingFound() {
        AiImportSession session = new AiImportSession("test", ACCOUNT);
        session.setFileName(WorkbookFixture.VITIS_FILE_NAME)
                .setWorkbook(workbook)
                .setPlan(plan)
                .setDataPoints(dataPoints);

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

    private void withMelgueil(AiImportSession session) {
        session.getReport().getFacilities().add(new ResolvedItem("Melgueil")
                .setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(FACILITY, "Melgueil"))));
    }

    private DataPoint weather(String facility, String value) {
        return new DataPoint()
                .setSheet("data_meteo")
                .setRowNumber(4)
                .setObjectName(facility)
                .setTargetKind(DataPoint.TargetKind.FACILITY)
                .setDate(LocalDate.of(2020, 5, 12))
                .setVariableKey(plan.getVariables().get(0).getColumnKey())
                .setRawValue(value);
    }

    private String variableKey(AiImportSession session, int index) {
        return session.getReport().getVariables().get(index).getSourceValue();
    }

    private Map<String, String> values() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(DataBulkImport.EXPERIMENT, EXPERIMENT.toString());
        values.put(DataBulkImport.PROVENANCE_NAME, "test");
        return values;
    }

    //#endregion
}
