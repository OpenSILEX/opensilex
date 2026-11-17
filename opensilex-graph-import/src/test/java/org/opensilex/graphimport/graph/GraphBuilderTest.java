//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.apache.jena.vocabulary.XSD;
import org.junit.Test;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.binding.ColumnReading;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.interpret.ClassViews;
import org.opensilex.graphimport.interpret.InterpretFixtures;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.alias.LearnedAliases;
import org.opensilex.graphimport.ontology.Cardinality;
import org.opensilex.graphimport.ontology.ClassHierarchy;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.HeaderResolver;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.RestrictionRow;
import org.opensilex.graphimport.ontology.literal.DateOrder;
import org.opensilex.graphimport.validate.GraphProblem;
import org.opensilex.graphimport.validate.ValidationReport;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.graphimport.graph.GraphFixtures.SYRAH;
import static org.opensilex.graphimport.graph.GraphFixtures.SheetSpec;
import static org.opensilex.graphimport.graph.GraphFixtures.graph;
import static org.opensilex.graphimport.graph.GraphFixtures.services;
import static org.opensilex.graphimport.graph.GraphFixtures.validate;
import static org.opensilex.graphimport.graph.GraphFixtures.workbook;
import static org.opensilex.graphimport.interpret.InterpretFixtures.COMMENT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.CREATION_DATE;
import static org.opensilex.graphimport.interpret.InterpretFixtures.GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.HAS_GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.IS_PART_OF;
import static org.opensilex.graphimport.interpret.InterpretFixtures.NAME;
import static org.opensilex.graphimport.interpret.InterpretFixtures.PLOT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.SO;

/**
 * Workbooks turned into a graph and checked: rows merged across sheets, links resolved, values read
 * for their datatype, the restrictions of the ontology applied on the merged resources.
 *
 * @author Arnaud Charleroy
 */
public class GraphBuilderTest {

    private static final SheetSpec PLOTS = SheetSpec.of("Parcelles", List.of("nom", "Date de création", "germplasme"),
            new String[]{"P1", "03/04/2026", "Syrah"},
            new String[]{"P2", "25/04/2026", "Syrha"});
    private static final SheetSpec MORE = SheetSpec.of("Compléments", List.of("nom", "remarque", "date"),
            new String[]{"P1", "bord", "2026-04-03"},
            new String[]{"P2", "centre", "2026-05-01"});
    private static final SheetSpec SUBPLOTS = SheetSpec.of("Sous-parcelles", List.of("nom", "parent"),
            new String[]{"SP1", "P1"},
            new String[]{"SP2", "Parcelle 404"});

    private final InterpretationServices services = services();

    @Test
    public void threeSheetsGiveMergedResourcesTheirLinksAndTheirProblems() throws Exception {
        WorkbookStructure workbook = workbook(PLOTS, MORE, SUBPLOTS);
        Map<String, ImportDecisions.SheetDecisions> decisions = new LinkedHashMap<>();
        decisions.put("Parcelles", sheet(PLOT, Map.of(0, NAME, 1, CREATION_DATE, 2, HAS_GERMPLASM)));
        decisions.put("Compléments", sheet(PLOT, Map.of(0, NAME, 1, COMMENT, 2, CREATION_DATE)));
        decisions.put("Sous-parcelles", sheet(PLOT, Map.of(0, NAME, 1, IS_PART_OF)));

        ImportGraph graph = graph(services, workbook, new ImportDecisions(decisions));
        ValidationReport report = validate(services, graph);

        assertEquals(List.of("p1", "p2", "sp1", "sp2"), graph.nodes().stream().map(GraphNode::getKey).toList());
        GraphNode p1 = graph.byKey().get("p1");
        assertEquals("one resource for the rows of two sheets", 2, p1.getRows().size());
        assertEquals("the same date written two ways is given by both cells", 2, p1.getValues().get(CREATION_DATE).size());
        assertEquals(SYRAH, p1.getValues().get(HAS_GERMPLASM).get(0).link().instances().iterator().next());
        LinkTarget parent = graph.byKey().get("sp1").getValues().get(IS_PART_OF).get(0).link();
        assertEquals(LinkTarget.Status.ROW, parent.status());
        assertEquals("p1", parent.nodeKey());

        assertEquals(List.of("problem.tooManyValues", "problem.linkUnfound", "problem.linkUnfound"),
                report.problems().stream().map(problem -> problem.message().getKey()).toList());
        GraphProblem conflict = report.problems().get(0);
        assertEquals(GraphProblem.Severity.ERROR, conflict.severity());
        assertEquals("both cells of the conflict are shown", 2, conflict.cells().size());
        assertEquals("P2", conflict.nodeName());
        GraphProblem typo = report.problems().get(1);
        assertEquals("Syrha", typo.value());
        assertEquals(SYRAH, typo.suggestions().get(0).uri());
        assertTrue(report.problems().get(2).suggestions().isEmpty());
        assertEquals(3, report.total());
        assertEquals(4, report.nodeCount());
        assertTrue("the conflict is also counted on the second sheet's column", report.counts().stream()
                .anyMatch(count -> count.sheet().equals("Compléments") && Integer.valueOf(2).equals(count.column())));

        DateColumn dates = graph.dateColumns().get(0);
        assertEquals("Parcelles", dates.sheet());
        assertEquals("25/04 settles the column: day first", DateOrder.DAY_FIRST, dates.detected());
    }

    @Test
    public void unrelatedClassesForOneIdentifierAreAConflictButASubclassIsNot() throws Exception {
        // The platform's reader takes a sheet of one column for text: each sheet has a second one.
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "note"), new String[]{"X1", "a"});
        SheetSpec germplasm = SheetSpec.of("Matériel", List.of("nom", "note"), new String[]{"X1", "b"});
        SheetSpec objects = SheetSpec.of("Objets", List.of("nom", "note"), new String[]{"X1", "a"});

        ValidationReport conflict = check(workbook(plots, germplasm), Map.of("Parcelles", sheet(PLOT, Map.of(0, NAME)),
                "Matériel", sheet(GERMPLASM, Map.of(0, NAME))));
        ValidationReport subclass = check(workbook(plots, objects), Map.of("Parcelles", sheet(PLOT, Map.of(0, NAME)),
                "Objets", sheet(SO, Map.of(0, NAME))));

        assertEquals(List.of("problem.classConflict"), keys(conflict));
        assertTrue(conflict.problems().get(0).aboutRow());
        assertEquals(2, conflict.problems().get(0).cells().size());
        assertEquals(List.of(), keys(subclass));
    }

    @Test
    public void aNameThatIsARowAndAnInstanceKeepsBothAnswers() throws Exception {
        SheetSpec germplasm = SheetSpec.of("Matériel", List.of("nom", "note"), new String[]{"Syrah", "rouge"});
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "germplasme"), new String[]{"P1", "Syrah"});

        ImportGraph graph = graph(services, workbook(germplasm, plots), new ImportDecisions(Map.of(
                "Matériel", sheet(GERMPLASM, Map.of(0, NAME)), "Parcelles", sheet(PLOT, Map.of(0, NAME, 1, HAS_GERMPLASM)))));

        LinkTarget target = graph.byKey().get("p1").getValues().get(HAS_GERMPLASM).get(0).link();
        assertEquals(LinkTarget.Status.ROW_AND_INSTANCE, target.status());
        assertEquals("syrah", target.nodeKey());
        assertEquals(List.of(SYRAH), List.copyOf(target.instances()));
        assertEquals(List.of(), keys(validate(services, graph)));
    }

    @Test
    public void aNameOfSeveralInstancesOrOfARowOfAnotherClassIsAProblem() throws Exception {
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "germplasme"),
                new String[]{"P1", "Grenache"},
                new String[]{"P2", "P3"},
                new String[]{"P3", null});

        ValidationReport report = check(workbook(plots), Map.of("Parcelles", sheet(PLOT, Map.of(0, NAME, 1, HAS_GERMPLASM))));

        assertEquals(List.of("problem.linkAmbiguous", "problem.linkWrongClass"), keys(report));
        assertEquals("2", report.problems().get(0).message().getParams().get("count"));
        assertEquals("Parcelle", report.problems().get(1).message().getParams().get("classes"));
        assertEquals("Matériel génétique", report.problems().get(1).message().getParams().get("expected"));
    }

    @Test
    public void twoLanguagesOfASingleValuedPropertyAreTwoValues() throws Exception {
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("code", "rdfs:label@fr", "rdfs:label@en"),
                new String[]{"P1", "Parcelle un", "Plot one"});

        ImportGraph graph = graph(services, workbook(plots), new ImportDecisions(Map.of(
                "Parcelles", sheet(PLOT, Map.of(1, NAME, 2, NAME)))));

        assertEquals("fr", graph.nodes().get(0).getValues().get(NAME).get(0).language());
        assertEquals(List.of("problem.tooManyValues"), keys(validate(services, graph)));
    }

    @Test
    public void aMissingRequiredValueOnlyWarnsSinceTheResourceMayExist() throws Exception {
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("code", "remarque"), new String[]{"P1", "bord"});

        ValidationReport report = check(workbook(plots), Map.of("Parcelles", sheet(PLOT, Map.of(1, COMMENT))));

        assertEquals(List.of("problem.requiredMissing"), keys(report));
        assertEquals(GraphProblem.Severity.WARNING, report.problems().get(0).severity());
        assertNull(report.counts().get(0).column());
    }

    @Test
    public void whatIsLeftOutOfTheGraphIsSaid() throws Exception {
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "hauteur"),
                new String[]{"P1", "1"},
                new String[]{null, "2"});
        SheetSpec unknown = SheetSpec.of("Divers", List.of("truc", "machin"), new String[]{"x", "y"});
        Map<String, ImportDecisions.SheetDecisions> decisions = new LinkedHashMap<>();
        decisions.put("Parcelles", sheet(PLOT, Map.of(0, NAME)));

        ImportGraph graph = graph(services, workbook(plots, unknown), new ImportDecisions(decisions));

        List<String> notes = graph.notes().stream().map(ReportMessage::getKey).toList();
        assertTrue(notes.toString(), notes.containsAll(List.of("graph.columnsLeftOut", "graph.emptyIdentifiers",
                "graph.sheetLeftOut")));
        assertEquals("a row without identifier is a resource of its own", 2, graph.nodes().size());
        assertFalse(graph.nodes().get(1).isIdentified());
    }

    @Test
    public void aColumnLeftOutGivesNoValueAndIsNotNamedAsToDecide() throws Exception {
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "remarque", "code"), new String[]{"P1", "a", "x"});

        ImportGraph graph = graph(services, workbook(plots), new ImportDecisions(Map.of("Parcelles",
                new ImportDecisions.SheetDecisions(PLOT, 0, Map.of(0, NAME), Map.of(), Set.of(1)))));

        assertFalse(graph.byKey().get("p1").getValues().containsKey(COMMENT));
        graph.notes().stream()
                .filter(note -> note.getKey().equals("graph.columnsLeftOut"))
                .forEach(note -> assertFalse(note.getParams().get("headers").contains("remarque")));
    }

    @Test
    public void sheetsWithoutClassAreSaidInOneNoteFirst() throws Exception {
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "note"), new String[]{"P1", "a"});
        SheetSpec first = SheetSpec.of("Divers", List.of("truc", "machin"), new String[]{"x", "y"});
        SheetSpec second = SheetSpec.of("Autres", List.of("bidule", "chose"), new String[]{"z", "w"});

        ImportGraph graph = graph(services, workbook(plots, first, second),
                new ImportDecisions(Map.of("Parcelles", sheet(PLOT, Map.of(0, NAME)))));

        ReportMessage note = graph.notes().get(0);
        assertEquals("graph.sheetsLeftOut", note.getKey());
        assertEquals("Divers, Autres", note.getParams().get("sheets"));
        assertEquals("2", note.getParams().get("count"));
        assertEquals(1, graph.notes().stream().filter(n -> n.getKey().startsWith("graph.sheet")).count());
    }

    @Test
    public void theOrderTheUserChoseReadsDatesTheValuesLeaveAmbiguous() throws Exception {
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "Date de création"), new String[]{"P1", "03/04/2026"});
        ImportDecisions.SheetDecisions undecided = sheet(PLOT, Map.of(0, NAME, 1, CREATION_DATE));

        ValidationReport ambiguous = check(workbook(plots), Map.of("Parcelles", undecided));
        ImportGraph read = graph(services, workbook(plots), new ImportDecisions(Map.of("Parcelles",
                undecided.withReadings(Map.of(1, new ColumnReading(DateOrder.MONTH_FIRST, null))))));

        assertEquals(List.of("literal.ambiguousDate"), keys(ambiguous));
        assertEquals("2026-03-04", read.nodes().get(0).getValues().get(CREATION_DATE).get(0).literal().lexical());
        assertEquals(DateOrder.MONTH_FIRST, read.dateColumns().get(0).effectiveOrder());
    }

    @Test
    public void anIdentifierWrittenAsAUriIsOneResourceWhateverItsForm() throws Exception {
        String uri = "http://test.opensilex.org/id/so/x1";
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("uri", "nom"), new String[]{uri, "X1"});
        SheetSpec more = SheetSpec.of("Compléments", List.of("uri", "remarque"), new String[]{"<" + uri + ">", "bord"});

        ImportGraph graph = graph(services, workbook(plots, more), new ImportDecisions(Map.of(
                "Parcelles", sheet(PLOT, Map.of(1, NAME)), "Compléments", sheet(PLOT, Map.of(1, COMMENT)))));

        assertEquals(1, graph.nodes().size());
        assertEquals(URI.create(uri), graph.nodes().get(0).getUri());
    }

    @Test
    public void aTypeThatNamesNoClassIsCountedAndTheRowTakesTheSheetClass() throws Exception {
        SheetSpec objects = SheetSpec.of("Objets", List.of("nom", "a"),
                new String[]{"P1", "vocabulary:Plot"},
                new String[]{"P2", "Vigne"});

        ImportGraph graph = graph(services, workbook(objects), new ImportDecisions(Map.of(
                "Objets", sheet(SO, Map.of(0, NAME)))));

        assertEquals(List.of(PLOT), List.copyOf(graph.byKey().get("p1").getClasses()));
        assertEquals(List.of(SO), List.copyOf(graph.byKey().get("p2").getClasses()));
        ReportMessage note = graph.notes().stream().filter(message -> "graph.unresolvedTypes".equals(message.getKey()))
                .findFirst().orElseThrow();
        assertEquals("1", note.getParams().get("count"));
    }

    @Test
    public void aRowFitsARangeAboveTheRootsOfTheCatalog() throws Exception {
        URI thing = URI.create("http://www.w3.org/2002/07/owl#Thing");
        URI linksTo = URI.create("http://test.opensilex.org/ontology#linksTo");
        ClassHierarchy hierarchy = new ClassHierarchy().link(PLOT, SO).link(SO, thing);
        InterpretationServices aboveRoots = new InterpretationServices(InterpretFixtures.CATALOG,
                new ClassViews(classUri -> ClassView.assemble(classUri, hierarchy, List.of(
                        new RestrictionRow(SO, NAME, PropertyKind.DATA, URI.create(XSD.xstring.getURI()),
                                new Cardinality(1, 1)),
                        new RestrictionRow(SO, linksTo, PropertyKind.OBJECT, thing, Cardinality.ANY)), Map.of())),
                GraphFixtures.instance(), LearnedAliases.NONE, new HeaderResolver(Map.of()), InterpretFixtures.OWNER, "fr");
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "lien"),
                new String[]{"P1", "P2"},
                new String[]{"P2", "P1"});

        ImportGraph graph = graph(aboveRoots, workbook(plots), new ImportDecisions(Map.of(
                "Parcelles", sheet(PLOT, Map.of(0, NAME, 1, linksTo)))));

        assertEquals(LinkTarget.Status.ROW, graph.byKey().get("p1").getValues().get(linksTo).get(0).link().status());
    }

    private ValidationReport check(WorkbookStructure workbook, Map<String, ImportDecisions.SheetDecisions> decisions)
            throws Exception {
        return validate(services, graph(services, workbook, new ImportDecisions(decisions)));
    }

    private static ImportDecisions.SheetDecisions sheet(URI classUri, Map<Integer, URI> properties) {
        return new ImportDecisions.SheetDecisions(classUri, 0, properties);
    }

    private static List<String> keys(ValidationReport report) {
        return report.problems().stream().map(problem -> problem.message().getKey()).toList();
    }
}
