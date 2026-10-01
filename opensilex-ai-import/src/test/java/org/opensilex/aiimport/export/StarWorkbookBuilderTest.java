//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.junit.Test;
import org.opensilex.aiimport.export.ExperimentSnapshot.ExportedObject;
import org.opensilex.aiimport.export.ExperimentSnapshot.Facility;
import org.opensilex.aiimport.export.ExperimentSnapshot.Level;
import org.opensilex.aiimport.export.ExperimentSnapshot.ObjectType;
import org.opensilex.aiimport.export.ExperimentSnapshot.Observation;
import org.opensilex.aiimport.export.ExperimentSnapshot.Variable;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookReader;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The STAR workbook written from an experiment, read back the way any STAR file is: by the
 * module's own reader and the STAR profile. What the profile finds in it is what the export meant.
 *
 * @author Arnaud Charleroy
 */
public class StarWorkbookBuilderTest {

    private static final URI XP = URI.create("http://opensilex.test/id/experiment/ifv30_teisso_2024");
    private static final URI FIELD = URI.create("http://opensilex.test/id/facility/teissonniere");
    private static final URI PLOT_TYPE = URI.create("http://www.opensilex.org/vocabulary/oeso#Plot");
    private static final URI C1 = URI.create("http://opensilex.test/id/so/c1");
    private static final URI TNT3 = URI.create("http://opensilex.test/id/so/tnt3");
    private static final URI SLOPE = URI.create("http://opensilex.test/ontology#slope_percent");
    private static final URI LEAF = URI.create("http://opensilex.test/id/variable/pm_leaf_pc");
    private static final URI RAIN = URI.create("http://opensilex.test/id/variable/rain_mm");

    private ExperimentSnapshot snapshot() {
        ExperimentSnapshot snapshot = new ExperimentSnapshot()
                .setUri(XP)
                .setName("IFV30_teisso_2024")
                .setObjective("Evaluation des biocontrôles sur Mildiou")
                .setDescription("Essai en blocs")
                .setStartDate(LocalDate.of(2024, 1, 1))
                .setEndDate(LocalDate.of(2024, 12, 31))
                .setObservationsWithoutTarget(2);
        snapshot.getProjects().addAll(List.of("CROPS-LIFE", "STAR"));
        snapshot.getOrganizations().add("IFV");
        snapshot.getSuborganizations().add("Unité de Rodilhan");
        snapshot.getEmails().add("contact@example.org");

        snapshot.getFacilities().add(new Facility(FIELD, "teissonniere", "Bellegarde", 43.77175, 4.475683,
                Map.of(StarProfile.COLUMN_ROW_SPACING, "2.5", "soil", "argilo-calcaire")));
        snapshot.getLevels().add(new Level("Traitement", "TNT", "Témoin non traité"));
        snapshot.getLevels().add(new Level("Traitement", "1", "Cuivre tardif"));

        snapshot.getObjectTypes().add(new ObjectType(PLOT_TYPE, "Plot", "Placette", List.of(
                new ExportedObject(C1, "C1", "Grenache blanc", List.of("1"), null, "1", "11", null,
                        Map.of(SLOPE, "3")),
                new ExportedObject(TNT3, "TNT3", "Grenache blanc", List.of("TNT"), null, "1", "2",
                        "bordure", Map.of())),
                Map.of(SLOPE, "slope_percent")));

        snapshot.getVariables().put(LEAF, new Variable("PM_LEAF_PC", "Pourcentage de la feuille atteint",
                "Mildiou", "Notation", "%", XSDDatatype.XSDdecimal.getURI(),
                "https://cropontology.org/term/CO_356:1000220"));
        snapshot.getVariables().put(RAIN, new Variable("rain_mm", "Pluie", "Pluie", "Pluviomètre", "mm",
                XSDDatatype.XSDdecimal.getURI(), RAIN.toString()));

        LocalDateTime june = LocalDateTime.of(2024, 6, 1, 0, 0);
        snapshot.getObservations().add(new Observation(C1, LEAF, june, true, 1.0));
        snapshot.getObservations().add(new Observation(C1, LEAF, june, true, 3.0));
        snapshot.getObservations().add(new Observation(TNT3, LEAF, june, true, 40.5));
        snapshot.getObservations().add(new Observation(FIELD, RAIN, LocalDateTime.of(2024, 5, 1, 0, 0), true, 1.2));
        // A variable the snapshot does not describe is not written: nothing would say what it is.
        snapshot.getObservations().add(new Observation(C1, URI.create("http://opensilex.test/id/variable/x"),
                june, true, 7));
        return snapshot;
    }

    private WorkbookStructure exported(ExperimentSnapshot snapshot) throws Exception {
        byte[] bytes = new StarWorkbookBuilder(LocalDate.of(2026, 9, 28)).build(snapshot);
        Path file = Files.createTempFile("star-export-", ".xlsx");
        try {
            Files.write(file, bytes);
            return new WorkbookReader().read(file.toFile(), "STAR_export.xlsx");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static SheetStructure sheet(WorkbookStructure workbook, String name) {
        return workbook.getSheet(name).orElseThrow(() -> new AssertionError("no sheet " + name));
    }

    @Test
    public void theSheetsAreTheOnesTheFormatDefines() throws Exception {
        WorkbookStructure workbook = exported(snapshot());

        assertEquals(List.of("readme", "expe", "field", "modalite", "ed_plot", "data_plot", "data_meteo",
                        "dictionary_variables", "dictionary_metadata"),
                workbook.getSheets().stream().map(SheetStructure::getName).collect(Collectors.toList()));
        assertTrue("the values nobody could attach are said",
                sheet(workbook, "readme").getText().contains("2 valeur(s)"));
    }

    /**
     * STAR reads each column's distinct values, so two projects are two rows of the same
     * experiment, not one cell holding both names.
     */
    @Test
    public void theExperimentTakesOneRowPerProject() throws Exception {
        SheetStructure expe = sheet(exported(snapshot()), "expe");

        assertEquals(2, expe.getDataRowCount());
        assertEquals(List.of("CROPS-LIFE", "STAR"), expe.distinctValues(StarProfile.COLUMN_PROJECT));
        assertEquals(List.of("IFV30_teisso_2024"), expe.distinctValues(StarProfile.COLUMN_EXPERIMENT));
        assertEquals(List.of("2024-01-01"), expe.distinctValues(StarProfile.COLUMN_START_DATE));
        assertEquals(List.of(XP.toString()), expe.distinctValues(StarWorkbookBuilder.COLUMN_EXPERIMENT_URI));
    }

    @Test
    public void theFieldCarriesItsCentroidAndItsProperties() throws Exception {
        SheetStructure field = sheet(exported(snapshot()), "field");
        List<String> row = field.getRows().get(0);

        assertEquals("teissonniere", field.cell(row, StarProfile.COLUMN_FIELD));
        assertEquals("Bellegarde", field.cell(row, StarProfile.COLUMN_TOWN));
        assertEquals("43.77175", field.cell(row, StarProfile.COLUMN_LATITUDE));
        assertEquals("4.475683", field.cell(row, StarProfile.COLUMN_LONGITUDE));
        assertEquals("2.5", field.cell(row, StarProfile.COLUMN_ROW_SPACING));
        assertEquals("a property STAR does not name follows the standard ones",
                "argilo-calcaire", field.cell(row, "soil"));
        assertEquals("one cultivar for every plot is the field's",
                "Grenache blanc", field.cell(row, StarProfile.COLUMN_CULTIVAR));
    }

    /**
     * One design sheet per type, shaped like STAR's plots: the identifier, the treatment, the
     * position — then what only this instance knows, the type and the URIs, and the type's own
     * properties.
     */
    @Test
    public void aTypeBecomesADesignSheet() throws Exception {
        SheetStructure plots = sheet(exported(snapshot()), "ed_plot");

        assertEquals(List.of("plot_id", "xp_trt_code", "cultivar_name", "plot_x", "plot_y", "plot_desc",
                "object_type", "object_uri", "slope_percent"), plots.getHeaders());
        List<String> c1 = plots.getRows().get(0);
        assertEquals("C1", plots.cell(c1, "plot_id"));
        assertEquals("1", plots.cell(c1, StarProfile.COLUMN_TREATMENT));
        assertEquals(PLOT_TYPE.toString(), plots.cell(c1, StarWorkbookBuilder.COLUMN_OBJECT_TYPE));
        assertEquals("3", plots.cell(c1, "slope_percent"));
    }

    /**
     * Two values of one variable on one plot the same day are two readings, written as two rows —
     * as the reference template writes several leaves read on the same plot.
     */
    @Test
    public void repetitionsBecomeRows() throws Exception {
        WorkbookStructure workbook = exported(snapshot());
        SheetStructure data = sheet(workbook, "data_plot");

        assertEquals("the variable nobody describes is left out",
                List.of("observation_date", "plot_id", "PM_LEAF_PC"), data.getHeaders());
        assertEquals(3, data.getDataRowCount());

        SheetStructure weather = sheet(workbook, "data_meteo");
        assertEquals(List.of("field_id", "meteo_datetime", "rain_mm"), weather.getHeaders());
    }

    @Test
    public void everyColumnIsDescribed() throws Exception {
        WorkbookStructure workbook = exported(snapshot());

        SheetStructure variables = sheet(workbook, "dictionary_variables");
        assertEquals(List.of("PM_LEAF_PC", "rain_mm"), variables.distinctValues("nom"));
        assertEquals("numeric", variables.cell(variables.getRows().get(0), "Rclass"));

        SheetStructure metadata = sheet(workbook, "dictionary_metadata");
        List<String> described = metadata.distinctValues("nom");
        for (String column : List.of("plot_id", "field_id", "expe_id", "object_type", "slope_percent", "soil",
                "observation_date", "meteo_datetime")) {
            assertTrue(column + " is described", described.contains(column));
        }
        List<String> plot = metadata.getRows().stream()
                .filter(row -> "plot_id".equals(metadata.cell(row, "nom"))).findFirst().orElseThrow();
        assertTrue("the standard's own description",
                metadata.cell(plot, "description_fr").startsWith("Code de la parcelle"));
    }

    /**
     * The point of writing STAR: the profile that reads STAR finds the same experiment in it.
     */
    @Test
    public void theStarProfileReadsTheExportBack() throws Exception {
        WorkbookStructure workbook = exported(snapshot());
        StarProfile profile = new StarProfile();

        assertEquals(100, profile.match(workbook));

        ExtractedImportPlan plan = profile.extract(workbook);
        assertEquals(List.of("IFV30_teisso_2024"), plan.getExperimentNames());
        assertEquals(List.of("CROPS-LIFE", "STAR"), plan.getProjectNames());
        assertEquals(List.of("teissonniere"), plan.getFacilityNames());
        assertTrue(plan.getScientificObjectNames().containsAll(List.of("C1", "TNT3")));
        assertEquals(List.of("PM_LEAF_PC", "rain_mm"),
                plan.getVariables().stream().map(VariableCandidate::getColumnKey).collect(Collectors.toList()));
        assertFalse("every measured column is declared a variable: " + plan.getAnomalies(),
                plan.getAnomalyMessages().stream().anyMatch(message -> message.getKey() != null
                        && message.getKey().endsWith("undeclaredVariables")));

        List<ObjectRow> rows = profile.extractObjectRows(workbook);
        assertEquals(2, rows.size());
        assertEquals("Grenache blanc", rows.get(0).getGermplasm());
        assertEquals("teissonniere", rows.get(0).getFacility());

        List<DataPoint> points = profile.extractDataPoints(workbook);
        assertEquals(4, points.size());
        assertTrue(points.stream().anyMatch(point -> point.getTargetKind() == DataPoint.TargetKind.FACILITY
                && "teissonniere".equals(point.getObjectName())
                && LocalDate.of(2024, 5, 1).equals(point.getDate())));
    }

    @Test
    public void anEmptyExperimentStillMakesAWorkbook() throws Exception {
        WorkbookStructure workbook = exported(new ExperimentSnapshot().setName("empty").setUri(XP));

        assertTrue(workbook.getSheet("expe").isPresent());
        assertFalse("no data, no data sheet", workbook.getSheet("data_meteo").isPresent());
    }

    @Test
    public void twoTypesWithTheSameLastSegmentGetTwoSheets() throws Exception {
        ExperimentSnapshot snapshot = new ExperimentSnapshot().setName("xp").setUri(XP);
        ExportedObject object = new ExportedObject(C1, "C1", null, List.of(), null, null, null, null, Map.of());
        snapshot.getObjectTypes().add(new ObjectType(PLOT_TYPE, "Plot", "Plot", List.of(object), Map.of()));
        snapshot.getObjectTypes().add(new ObjectType(URI.create("http://other.test/Plot"), "Plot", "Plot",
                List.of(object), Map.of()));

        WorkbookStructure workbook = exported(snapshot);

        assertTrue(workbook.getSheet("ed_plot").isPresent());
        assertTrue(workbook.getSheet("ed_plot_2").isPresent());
    }

    @Test
    public void theRClassFollowsTheDatatype() {
        assertEquals("numeric", StarWorkbookBuilder.rClassOf(XSDDatatype.XSDdouble.getURI()));
        assertEquals("integer", StarWorkbookBuilder.rClassOf(XSDDatatype.XSDinteger.getURI()));
        assertEquals("date", StarWorkbookBuilder.rClassOf(XSDDatatype.XSDdate.getURI()));
        assertEquals("datetime", StarWorkbookBuilder.rClassOf(XSDDatatype.XSDdateTime.getURI()));
        assertEquals("logical", StarWorkbookBuilder.rClassOf(XSDDatatype.XSDboolean.getURI()));
        assertEquals("character", StarWorkbookBuilder.rClassOf(XSDDatatype.XSDstring.getURI()));
        assertEquals("character", StarWorkbookBuilder.rClassOf(null));
        assertEquals("object", StarWorkbookBuilder.slug("--"));
        assertEquals("sub_plot", StarWorkbookBuilder.slug("Sub Plot"));
    }

    @Test
    public void aRecordedOffsetIsKept() {
        assertEquals(LocalDateTime.of(2024, 5, 1, 8, 0),
                ExperimentSnapshotReader.localDate(Instant.parse("2024-05-01T06:00:00Z"), "+02:00"));
        assertEquals(LocalDateTime.of(2024, 5, 1, 6, 0),
                ExperimentSnapshotReader.localDate(Instant.parse("2024-05-01T06:00:00Z"), "nonsense"));
        assertEquals("Plot", ExperimentSnapshotReader.localName(PLOT_TYPE));
        assertNull(ExperimentSnapshotReader.expanded(null));
    }
}
