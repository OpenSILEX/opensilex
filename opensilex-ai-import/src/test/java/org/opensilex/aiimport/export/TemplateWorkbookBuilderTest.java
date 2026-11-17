//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.opensilex.aiimport.export.ExperimentSnapshot.ExportedObject;
import org.opensilex.aiimport.export.ExperimentSnapshot.Level;
import org.opensilex.aiimport.export.ExperimentSnapshot.ObjectType;
import org.opensilex.aiimport.export.ExperimentSnapshot.Observation;
import org.opensilex.aiimport.export.ExperimentSnapshot.Variable;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.FileDescription;
import org.opensilex.aiimport.profile.custom.SheetDescription;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.profile.custom.UserDefinedProfile;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateFixtures;
import org.opensilex.aiimport.template.TemplateSheet;
import org.opensilex.aiimport.workbook.WorkbookReader;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * An experiment written in the shape of a template: each sheet of the template filled by the roles of
 * its columns, what could not be said written on the readme, and — the point of it — the workbook read
 * back by the module with the same template finds the same objects and the same measures.
 *
 * @author Arnaud Charleroy
 */
public class TemplateWorkbookBuilderTest {

    private static final URI P01 = URI.create("http://opensilex.test/id/so/p01");
    private static final URI P02 = URI.create("http://opensilex.test/id/so/p02");
    private static final URI ALTITUDE = URI.create(TemplateFixtures.ALTITUDE);
    private static final URI HEIGHT = URI.create("http://opensilex.test/id/variable/hauteur_cm");
    private static final URI RAIN = URI.create("http://opensilex.test/id/variable/rain_mm");

    private static ExperimentSnapshot snapshot() {
        ExperimentSnapshot snapshot = new ExperimentSnapshot().setName("Essai vigne").setUri(URI.create("http://x/xp"));
        snapshot.getLevels().add(new Level("Traitement", "T1", null));
        snapshot.getLevels().add(new Level("Traitement", "T2", null));
        snapshot.getLevels().add(new Level("Bloc", "B1", null));
        snapshot.getObjectTypes().add(new ObjectType(TemplateFixtures.PLOT, "Plot", "Plot", List.of(
                new ExportedObject(P01, "P01", "Syrah", List.of("T1", "B1"), null, "1", "2", "bord",
                        Map.of(ALTITUDE, "120")),
                new ExportedObject(P02, "P02", "Grenache", List.of("T2"), null, "1", "3", null, Map.of())),
                Map.of(ALTITUDE, "altitude")));
        snapshot.getVariables().put(HEIGHT, new Variable("hauteur_cm", null, null, null, "cm", "decimal",
                HEIGHT.toString()));
        snapshot.getVariables().put(RAIN, new Variable("rain_mm", null, null, null, "mm", "decimal", RAIN.toString()));
        snapshot.getObservations().add(new Observation(P01, HEIGHT, LocalDate.of(2026, 6, 12).atStartOfDay(), true, 84.0));
        snapshot.getObservations().add(new Observation(P02, HEIGHT, LocalDate.of(2026, 6, 12).atStartOfDay(), true, 79.0));
        snapshot.getObservations().add(new Observation(P01, HEIGHT, LocalDate.of(2026, 7, 3).atStartOfDay(), true, 131.0));
        snapshot.getObservations().add(new Observation(P01, HEIGHT, LocalDate.of(2026, 7, 3).atStartOfDay(), true, 133.0));
        snapshot.getObservations().add(new Observation(P01, RAIN, LocalDate.of(2026, 7, 3).atStartOfDay(), true, 2.0));
        snapshot.setObservationsWithoutTarget(1);
        return snapshot;
    }

    private static Workbook open(byte[] bytes) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private static List<String> rowOf(Sheet sheet, int index) {
        List<String> cells = new ArrayList<>();
        sheet.getRow(index).forEach(cell -> cells.add(cell.toString()));
        return cells;
    }

    private static String cellText(Sheet sheet, int row, int column) {
        return sheet.getRow(row) == null || sheet.getRow(row).getCell(column) == null
                ? "" : sheet.getRow(row).getCell(column).toString();
    }

    private byte[] build(FileTemplate template) throws Exception {
        return new TemplateWorkbookBuilder(template, LocalDate.of(2026, 11, 11)).build(snapshot());
    }

    @Test
    public void everySheetOfTheTemplateIsWrittenWithItsColumnsInOrderAndAReadmeLast() throws Exception {
        try (Workbook book = open(build(TemplateFixtures.planAndMeasures()))) {
            assertEquals(List.of("plan", "mesures", "readme"), java.util.stream.StreamSupport
                    .stream(book.spliterator(), false).map(Sheet::getSheetName).collect(Collectors.toList()));
            assertEquals(List.of("plot_id", "variete", "traitement", "altitude"), rowOf(book.getSheet("plan"), 0));
            assertEquals(List.of("plot_id", "date", "hauteur_cm"), rowOf(book.getSheet("mesures"), 0));
        }
    }

    @Test
    public void aSheetOfObjectsHasARowPerObjectFilledByTheRoleOfEachColumn() throws Exception {
        try (Workbook book = open(build(TemplateFixtures.planAndMeasures()))) {
            Sheet plan = book.getSheet("plan");

            assertEquals(List.of("P01", "Syrah", "T1", "120"), rowOf(plan, 1));
            assertEquals("a level of the factor the header names, and an empty cell for a property the object lacks",
                    List.of("P02", "Grenache", "T2"), rowOf(plan, 2));
            assertEquals(3, plan.getPhysicalNumberOfRows());
        }
    }

    @Test
    public void aSheetOfDataHasARowPerObjectAndDateAndRepeatsMakeMoreRows() throws Exception {
        try (Workbook book = open(build(TemplateFixtures.planAndMeasures()))) {
            Sheet data = book.getSheet("mesures");

            assertEquals(4, data.getPhysicalNumberOfRows() - 1);
            assertEquals("P01", cellText(data, 1, 0));
            assertEquals(84.0, data.getRow(1).getCell(2).getNumericCellValue(), 0.0001);
            assertEquals(131.0, data.getRow(2).getCell(2).getNumericCellValue(), 0.0001);
            assertEquals("the second value of the same day", 133.0, data.getRow(3).getCell(2).getNumericCellValue(),
                    0.0001);
            assertEquals("P02", cellText(data, 4, 0));
        }
    }

    @Test
    public void theTypeOfTheSheetKeepsOtherObjectsOut() throws Exception {
        TemplateSheet plan = TemplateFixtures.planAndMeasures().getSheets().get(0);
        FileTemplate wrongType = FileTemplate.fromSheets("autre", List.of(new TemplateSheet("plan", plan.getHeaders(),
                SheetKind.OBJECTS, plan.roles(), URI.create("http://www.opensilex.org/vocabulary/oeso#Plant"),
                plan.getProperties())));

        try (Workbook book = open(build(wrongType))) {
            assertEquals("no plant in the experiment", 1, book.getSheet("plan").getPhysicalNumberOfRows());
        }
    }

    @Test
    public void aVariableTheTemplateNamesByItsUriBeatsTheOneNamedLikeTheHeader() throws Exception {
        FileTemplate template = FileTemplate.fromSheets("pluie", List.of(new TemplateSheet("mesures",
                List.of("plot_id", "date", "pluie"), SheetKind.DATA,
                Map.of("plot_id", DescribedRole.OBJECT, "date", DescribedRole.DATE, "pluie", DescribedRole.VARIABLE),
                null, Map.of("pluie", RAIN.toString()))));

        try (Workbook book = open(build(template))) {
            Sheet data = book.getSheet("mesures");

            assertEquals(1, data.getPhysicalNumberOfRows() - 1);
            assertEquals(2.0, data.getRow(1).getCell(2).getNumericCellValue(), 0.0001);
        }
    }

    @Test
    public void objectsAndDataInOneSheetRepeatWhatIsKnownOfTheObject() throws Exception {
        FileTemplate flat = FileTemplate.fromSheets("plat", List.of(new TemplateSheet("essai",
                List.of("plot_id", "variete", "date", "hauteur_cm"), SheetKind.OBJECTS_AND_DATA,
                Map.of("plot_id", DescribedRole.OBJECT, "variete", DescribedRole.GERMPLASM,
                        "date", DescribedRole.DATE, "hauteur_cm", DescribedRole.VARIABLE), null, Map.of())));

        try (Workbook book = open(build(flat))) {
            Sheet sheet = book.getSheet("essai");

            assertEquals("Syrah", cellText(sheet, 1, 1));
            assertEquals("Syrah", cellText(sheet, 2, 1));
            assertEquals("Grenache", cellText(sheet, 4, 1));
        }
    }

    @Test
    public void aSheetOfLevelsListsTheFactorsAndTheirLevels() throws Exception {
        FileTemplate factors = FileTemplate.fromSheets("modalites", List.of(new TemplateSheet("modalites",
                List.of("facteur", "modalite", "description", "nom"), SheetKind.FACTORS,
                Map.of("facteur", DescribedRole.FACTOR, "modalite", DescribedRole.FACTOR_LEVEL,
                        "description", DescribedRole.FACTOR_LEVEL_DESCRIPTION, "nom", DescribedRole.FACTOR_LEVEL_NAME),
                null, Map.of())));

        try (Workbook book = open(build(factors))) {
            Sheet sheet = book.getSheet("modalites");

            assertEquals(List.of("Traitement", "T1"), rowOf(sheet, 1));
            assertEquals(List.of("Bloc", "B1"), rowOf(sheet, 3));
            assertTrue("a level's name is kept in its description, so its own column stays empty",
                    cellText(book.getSheet("readme"), 3, 0).contains("nom"));
        }
    }

    @Test
    public void aSheetOfExperimentsIsTheExperimentOnOneRow() throws Exception {
        FileTemplate experiments = FileTemplate.fromSheets("expe", List.of(new TemplateSheet("Expe",
                List.of("essai", "but", "debut", "nom2"), SheetKind.EXPERIMENTS,
                Map.of("essai", DescribedRole.TRIAL, "but", DescribedRole.EXPERIMENT_OBJECTIVE,
                        "debut", DescribedRole.EXPERIMENT_START, "nom2", DescribedRole.EXPERIMENT_ALTERNATIVE_NAME),
                null, Map.of())));
        ExperimentSnapshot snapshot = snapshot().setObjective("Mesurer").setStartDate(LocalDate.of(2026, 3, 1));

        try (Workbook book = open(new TemplateWorkbookBuilder(experiments, LocalDate.of(2026, 11, 13)).build(snapshot))) {
            Sheet sheet = book.getSheet("Expe");
            assertEquals("Essai vigne", cellText(sheet, 1, 0));
            assertEquals("Mesurer", cellText(sheet, 1, 1));
            assertTrue(sheet.getRow(1).getCell(2).getDateCellValue() != null);
            assertTrue("what the snapshot does not hold is said", cellText(book.getSheet("readme"), 3, 0)
                    .contains("nom2"));
        }
    }

    @Test
    public void theReadmeSaysWhatWasNotExported() throws Exception {
        FileTemplate withPerson = FileTemplate.fromSheets("avec observateur", List.of(new TemplateSheet("mesures",
                List.of("plot_id", "date", "hauteur_cm", "observateur"), SheetKind.DATA,
                Map.of("plot_id", DescribedRole.OBJECT, "date", DescribedRole.DATE,
                        "hauteur_cm", DescribedRole.VARIABLE, "observateur", DescribedRole.OBSERVER), null, Map.of())));

        try (Workbook book = open(build(withPerson))) {
            Sheet readme = book.getSheet(TemplateWorkbookBuilder.README_SHEET);
            String text = java.util.stream.IntStream.range(0, readme.getPhysicalNumberOfRows())
                    .mapToObj(i -> cellText(readme, i, 0)).collect(Collectors.joining("\n"));

            assertTrue(text, text.contains("Experiment: Essai vigne"));
            assertTrue(text, text.contains("avec observateur"));
            assertTrue(text, text.contains("2026-11-11"));
            assertTrue("the column nothing fills", text.contains("columns left empty") && text.contains("observateur"));
            assertTrue("the variable the template has no column for", text.contains("1 values of variables the "
                    + "template has no column for were not exported: rain_mm"));
            assertTrue(text.contains("1 values concern neither an object"));
        }
    }

    @Test
    public void aSheetNothingFillsOrNobodyReadsIsSaidOrSkipped() throws Exception {
        FileTemplate other = FileTemplate.fromSheets("autres", List.of(
                new TemplateSheet("sites", List.of("nom"), SheetKind.FACILITIES,
                        Map.of("nom", DescribedRole.LOCATION), null, Map.of()),
                new TemplateSheet("lisezmoi", List.of("a", "b"), SheetKind.IGNORED, Map.of(), null, Map.of())));

        try (Workbook book = open(build(other))) {
            assertNotNull(book.getSheet(TemplateWorkbookBuilder.README_SHEET));
            assertEquals("only the readme", 1, book.getNumberOfSheets());
            assertTrue(cellText(book.getSheet(TemplateWorkbookBuilder.README_SHEET), 3, 0).contains("sites"));
        }
    }

    /**
     * The other direction of an import by template, closed: what the module writes, it reads back with
     * the same template as the same objects and the same measures.
     */
    @Test
    public void theWorkbookReadsBackWithTheSameTemplateAsTheSameObjectsAndMeasures() throws Exception {
        FileTemplate template = TemplateFixtures.planAndMeasures();
        File file = File.createTempFile("exported-", ".xlsx");
        try {
            Files.write(file.toPath(), build(template));
            WorkbookStructure workbook = new WorkbookReader().read(file, "exported.xlsx");
            FileDescription description = new FileDescription();
            for (TemplateSheet sheet : template.getSheets()) {
                SheetDescription said = description.sheet(sheet.getName()).setKind(sheet.kind().orElseThrow())
                        .setType(sheet.getType());
                said.getRoles().putAll(sheet.roles());
                said.getProperties().putAll(sheet.getProperties());
            }
            UserDefinedProfile profile = new UserDefinedProfile(description);

            assertEquals(List.of("P01", "P02"), profile.extract(workbook).getScientificObjectNames());
            assertEquals(List.of("Syrah", "Grenache"), profile.extract(workbook).getGermplasmNames());
            List<DataPoint> points = profile.extractDataPoints(workbook);
            assertEquals(List.of("84", "79", "131", "133"), points.stream().filter(point -> point.getObjectName() != null)
                    .sorted(java.util.Comparator.comparing(DataPoint::getDate).thenComparing(DataPoint::getObjectName))
                    .map(DataPoint::getRawValue).map(value -> value.replaceAll("\\.0$", "")).collect(Collectors.toList()));
            assertEquals(LocalDate.of(2026, 6, 12), points.stream().map(DataPoint::getDate).min(LocalDate::compareTo)
                    .orElseThrow());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}
