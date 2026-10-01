//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.create.objects.ObjectSheet;
import org.opensilex.aiimport.create.objects.ObjectSheetPlan;
import org.opensilex.aiimport.create.objects.ObjectSheets;
import org.opensilex.aiimport.create.objects.ObjectValueResolver;
import org.opensilex.aiimport.create.objects.TypeProperties;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The CSV written for the platform's scientific-object importer, from a real STAR workbook.
 * <p>
 * The two lookups that need an instance — the experiment's factor levels and its start date — are
 * answered in memory; everything else runs as in production.
 *
 * @author Arnaud Charleroy
 */
public class ScientificObjectBulkImportTest {

    private static final URI EXPERIMENT = URI.create("http://opensilex.test/id/experiment/star");
    private static final String PLOT_TYPE = "http://www.opensilex.org/vocabulary/oeso#Plot";
    private static final String PLANT_COUNT = "http://opensilex.test/ontology#plant_count";

    /**
     * The one block the instance already has, in the experiment.
     */
    private static final String EXISTING_BLOCK = "B0";

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put(ScientificObjectBulkImport.EXPERIMENT, EXPERIMENT.toString());
        values.put(ScientificObjectBulkImport.OBJECT_TYPE, PLOT_TYPE);
        return values;
    }

    /**
     * An importer whose factor levels are every treatment the workbook names, so rows resolve.
     */
    private ScientificObjectBulkImport importer(Set<String> levels) {
        return new ScientificObjectBulkImport(null, null, null, null) {
            @Override
            protected Map<String, URI> factorLevels(URI experiment) {
                Map<String, URI> byName = new HashMap<>();
                levels.forEach(level -> byName.put(level.toLowerCase(),
                        URI.create("http://opensilex.test/id/level/" + level)));
                return byName;
            }

            @Override
            protected String experimentStart(URI experiment) {
                return "2024-03-01";
            }

            @Override
            protected TypeProperties typeProperties() {
                return plotProperties();
            }

            @Override
            protected ObjectValueResolver valueResolver() {
                return new ObjectValueResolver(null) {
                    @Override
                    public Resolution resolve(String value, URI range, URI graph) {
                        return EXISTING_BLOCK.equals(value)
                                ? new Resolution(URI.create("http://opensilex.test/id/so/" + value), 1)
                                : new Resolution(null, 0);
                    }
                };
            }
        };
    }

    /**
     * A plot as the core ontology describes a scientific object: a name, a comment, a part-of —
     * without an instance to ask.
     */
    static TypeProperties plotProperties() {
        return new TypeProperties("en") {
            @Override
            public boolean isObjectType(URI type) {
                return PLOT_TYPE.equals(String.valueOf(type));
            }

            @Override
            public List<TypeProperties.TypeProperty> of(URI type) {
                return List.of(
                        new TypeProperties.TypeProperty(URI.create(ObjectTargets.NAME), "name", false,
                                URI.create("http://www.w3.org/2001/XMLSchema#string"), true, false),
                        new TypeProperties.TypeProperty(URI.create(ObjectTargets.COMMENT), "comment", false,
                                URI.create("http://www.w3.org/2001/XMLSchema#string"), false, false),
                        new TypeProperties.TypeProperty(URI.create(ObjectTargets.PARENT), "is part of", true,
                                URI.create("http://www.opensilex.org/vocabulary/oeso#ScientificObject"), false, false),
                        new TypeProperties.TypeProperty(URI.create(PLANT_COUNT), "plant count", false,
                                URI.create("http://www.w3.org/2001/XMLSchema#integer"), false, false));
            }
        };
    }

    /**
     * A STAR session whose report found every germplasm and facility the plots name.
     */
    private AiImportSession session() throws Exception {
        AiImportSession session = new AiImportSession("test", URI.create("http://opensilex.test/id/account/a"));
        session.setWorkbook(WorkbookFixture.starStandard()).setProfileId(StarProfile.ID);
        ResolutionReport report = new ResolutionReport();
        for (ObjectRow row : ScientificObjectBulkImport.rowsOf(session)) {
            found(report.getGermplasm(), row.getGermplasm());
            found(report.getFacilities(), row.getFacility());
        }
        session.setReport(report);
        return session;
    }

    private void found(List<ResolvedItem> items, String name) {
        if (name == null || items.stream().anyMatch(item -> item.getSourceValue().equals(name))) {
            return;
        }
        ResolvedItem item = new ResolvedItem(name).setStatus(ResolutionStatus.FOUND);
        item.getMatches().add(new ResourceReference(URI.create("http://opensilex.test/id/r/"
                + Math.abs(name.hashCode())), name));
        items.add(item);
    }

    private Set<String> treatments(AiImportSession session) {
        Set<String> levels = new LinkedHashSet<>();
        ScientificObjectBulkImport.rowsOf(session).forEach(row -> {
            if (row.getFactorLevel() != null) {
                levels.add(row.getFactorLevel());
            }
        });
        return levels;
    }

    @Test
    public void everyPlotOfTheWorkbookBecomesOneLine() throws Exception {
        AiImportSession session = session();
        GeneratedCsv csv = importer(treatments(session)).generate(session, values());

        assertEquals(ScientificObjectBulkImport.rowsOf(session).size(), csv.getDataLineCount());
        assertTrue("the plot sheet has plots", csv.getDataLineCount() > 0);
        assertTrue(csv.getModuleErrors().toString(), csv.getModuleErrors().isEmpty());
    }

    /**
     * The format of the scientific-object screen: URI and type first, a description line, the type
     * the user chose on every row, the name as rdfs:label.
     */
    @Test
    public void theCsvIsTheOneTheObjectScreenImports() throws Exception {
        AiImportSession session = session();
        String csv = importer(treatments(session)).generate(session, values()).render();
        String[] lines = csv.split("\n");

        assertTrue(lines[0], lines[0].startsWith("\"uri\",\"type\",\"http://www.w3.org/2000/01/rdf-schema#label\""));
        assertEquals("the description line repeats the header", lines[0], lines[1]);
        assertTrue(lines[2], lines[2].startsWith("\"\",\"" + PLOT_TYPE + "\","));
    }

    /**
     * A plot's position is a move dated from the experiment's start: the workbook says where, never
     * since when.
     */
    @Test
    public void aPositionIsDatedFromTheExperimentsStart() throws Exception {
        AiImportSession session = session();
        String csv = importer(treatments(session)).generate(session, values()).render();

        assertTrue(csv.split("\n")[0].contains("start_date_of_Location"));
        assertTrue(csv.contains("\"2024-03-01\""));
    }

    /**
     * A treatment the experiment does not have stops the row before the platform is asked: the
     * importer would not refuse an empty factor level, it would lose the information.
     */
    @Test
    public void aTreatmentTheExperimentDoesNotHaveIsRefusedOnItsRow() throws Exception {
        AiImportSession session = session();
        GeneratedCsv csv = importer(Set.of()).generate(session, values());

        assertFalse(csv.getModuleErrors().isEmpty());
        RowError error = csv.getModuleErrors().get(0);
        assertEquals(RowError.Kind.UNRESOLVED, error.getKind());
        assertEquals("treatment", "factorLevel", error.getColumn());
        assertTrue("points at a row of the plot sheet", error.getRow() >= 2);
    }

    /**
     * The module's own refusals come first: the platform is not asked about rows already known to
     * be wrong, so validation answers without an instance here.
     */
    @Test
    public void validationStopsAtTheModulesOwnRefusals() throws Exception {
        AiImportSession session = session();
        BulkOutcome outcome = importer(Set.of()).validate(session, values());

        assertTrue(outcome.isRefused());
        assertEquals(ScientificObjectBulkImport.rowsOf(session).size(), outcome.getRowsChecked());
    }

    //#region one type per sheet, columns mapped to properties

    /**
     * A STAR workbook with two design sheets: blocks, and plots that are part of them.
     */
    private AiImportSession twoSheets() {
        SheetStructure expe = new SheetStructure().setName("expe")
                .setHeaders(List.of("expe_id")).setRows(List.of(List.of("xp")));
        SheetStructure blocks = new SheetStructure().setName("ed_bloc")
                .setHeaders(List.of("bloc_id", "bloc_desc"))
                .setRows(List.of(List.of("B1", "north"), List.of("B2", "south")));
        SheetStructure plots = new SheetStructure().setName("ed_placette")
                .setHeaders(List.of("plot_id", "parent_id", "plot_n", "plot_desc"))
                .setRows(List.of(List.of("P1", EXISTING_BLOCK, "7", "edge"), List.of("P2", EXISTING_BLOCK, "6", "")));
        WorkbookStructure workbook = new WorkbookStructure().setFileName("star.xlsx")
                .setSheets(new ArrayList<>(List.of(expe, blocks, plots)));
        AiImportSession session = new AiImportSession("sheets", URI.create("http://opensilex.test/id/account/a"));
        session.setWorkbook(workbook).setProfileId(StarProfile.ID);
        session.setReport(new ResolutionReport());
        session.getObjectPlans().put("ed_bloc", new ObjectSheetPlan("ed_bloc").setType(URI.create(PLOT_TYPE)));
        session.getObjectPlans().put("ed_placette", new ObjectSheetPlan("ed_placette")
                .setType(URI.create(PLOT_TYPE)).map("plot_n", PLANT_COUNT));
        return session;
    }

    private Map<String, String> experimentOnly() {
        Map<String, String> values = new HashMap<>();
        values.put(ScientificObjectBulkImport.EXPERIMENT, EXPERIMENT.toString());
        return values;
    }

    /**
     * Both sheets go in one CSV, each row under its sheet's type; a column mapped to a property of
     * the type becomes a column of the CSV under that property, the rows of the other sheet leaving
     * it empty; a parent found in the experiment is written as its URI.
     */
    @Test
    public void everySheetGoesInOneCsvWithItsOwnColumns() throws Exception {
        GeneratedCsv csv = importer(Set.of()).generate(twoSheets(), experimentOnly());

        assertTrue(csv.getModuleErrors().toString(), csv.getModuleErrors().isEmpty());
        assertEquals(4, csv.getDataLineCount());
        String rendered = csv.render();
        String header = rendered.split("\n")[0];
        assertTrue(header, header.contains(PLANT_COUNT));
        assertTrue("the description columns are the comment", header.contains(ObjectTargets.COMMENT));
        assertTrue(header.contains(ObjectTargets.PARENT));
        assertTrue(rendered.contains("\"7\""));
        assertTrue(rendered.contains("http://opensilex.test/id/so/" + EXISTING_BLOCK));
        assertEquals("the column the workbook calls plot_n", "plot_n",
                csv.getOrigins().workbookHeaderOf("ed_placette", PLANT_COUNT));
    }

    @Test
    public void aSheetWithoutATypeIsRefusedByName() throws Exception {
        AiImportSession session = twoSheets();
        session.getObjectPlans().get("ed_bloc").setType(null);

        GeneratedCsv csv = importer(Set.of()).generate(session, experimentOnly());

        assertEquals(1, csv.getModuleErrors().size());
        assertEquals("ed_bloc", csv.getModuleErrors().get(0).getMessage().getParams().get("sheet"));
        assertEquals("the other sheet still counts its rows", 2, csv.getDataLineCount());
    }

    @Test
    public void aTypeThatIsNoObjectTypeIsRefused() throws Exception {
        AiImportSession session = twoSheets();
        session.getObjectPlans().get("ed_bloc").setType(URI.create("http://opensilex.test/ontology#NotAnObject"));

        GeneratedCsv csv = importer(Set.of()).generate(session, experimentOnly());

        assertTrue(csv.getModuleErrors().get(0).getMessage().getKey().endsWith("notObjectType"));
    }

    @Test
    public void aColumnMappedToAPropertyTheTypeLacksIsRefused() throws Exception {
        AiImportSession session = twoSheets();
        session.getObjectPlans().get("ed_placette").map("plot_desc", "http://opensilex.test/ontology#unknown");

        GeneratedCsv csv = importer(Set.of()).generate(session, experimentOnly());

        assertTrue(csv.getModuleErrors().toString(), csv.getModuleErrors().stream()
                .anyMatch(error -> error.getMessage().getKey().endsWith("unknownProperty")));
    }

    @Test
    public void twoColumnsCannotFeedASingleValue() throws Exception {
        AiImportSession session = twoSheets();
        session.getObjectPlans().get("ed_placette").map("plot_desc", PLANT_COUNT);

        GeneratedCsv csv = importer(Set.of()).generate(session, experimentOnly());

        assertTrue(csv.getModuleErrors().toString(), csv.getModuleErrors().stream()
                .anyMatch(error -> error.getMessage().getKey().endsWith("singleValued")));
    }

    @Test
    public void aSheetLeftOutIsNotWritten() throws Exception {
        AiImportSession session = twoSheets();
        session.getObjectPlans().get("ed_bloc").setIncluded(false);

        GeneratedCsv csv = importer(Set.of()).generate(session, experimentOnly());

        assertEquals(2, csv.getDataLineCount());
    }

    /**
     * A parent created by another sheet of the same run cannot be linked yet: the platform checks
     * a parent exists before writing anything. Said, with the sheet to create first.
     */
    @Test
    public void aParentCreatedInTheSameRunIsExplained() throws Exception {
        AiImportSession session = twoSheets();
        List<List<String>> rows = List.of(List.of("P1", "B1", "7", ""));
        session.getWorkbook().getSheet("ed_placette").get().setRows(rows);

        GeneratedCsv csv = importer(Set.of()).generate(session, experimentOnly());

        RowError error = csv.getModuleErrors().get(0);
        assertTrue(error.getMessage().getKey().endsWith("parentInSameRun"));
        assertEquals("ed_bloc", error.getMessage().getParams().get("sheet"));
        assertEquals("parent_id", error.getColumn());
    }

    @Test
    public void aParentNobodyHasIsUnresolved() throws Exception {
        AiImportSession session = twoSheets();
        session.getWorkbook().getSheet("ed_placette").get().setRows(List.of(List.of("P1", "ghost", "7", "")));

        GeneratedCsv csv = importer(Set.of()).generate(session, experimentOnly());

        assertEquals("AiImport.proposal.unresolved.parent", csv.getModuleErrors().get(0).getMessage().getKey());
    }

    /**
     * What the view offers before anyone chose anything: the name column locked on the name, the
     * columns STAR names already mapped, the rest unmapped.
     */
    @Test
    public void theSheetsStartFromWhatTheProfileKnows() {
        AiImportSession session = twoSheets();
        session.getObjectPlans().clear();

        List<ObjectSheet> sheets = ObjectSheets.of(session);

        assertEquals(List.of("ed_bloc", "ed_placette"),
                sheets.stream().map(ObjectSheet::getName).collect(Collectors.toList()));
        ObjectSheet plots = sheets.get(1);
        assertEquals("plot_id", plots.getNameColumn());
        assertEquals(ObjectTargets.NAME, plots.getMapping().get("plot_id"));
        assertEquals(ObjectTargets.PARENT, plots.getMapping().get("parent_id"));
        assertEquals(ObjectTargets.COMMENT, plots.getMapping().get("plot_desc"));
        assertEquals(ObjectTargets.NONE, plots.getMapping().get("plot_n"));
        assertEquals("the type is asked, never guessed", null, plots.getType());
        assertTrue(plots.isIncluded());
    }

    //#endregion
}
