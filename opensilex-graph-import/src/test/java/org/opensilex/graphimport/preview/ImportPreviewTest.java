//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.apache.jena.vocabulary.XSD;
import org.junit.Test;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.GraphImportModule;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.graph.GraphBuilder;
import org.opensilex.graphimport.graph.GraphFixtures;
import org.opensilex.graphimport.graph.GraphFixtures.SheetSpec;
import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.graph.StoredValue;
import org.opensilex.graphimport.graph.Suggestion;
import org.opensilex.graphimport.interpret.ClassViews;
import org.opensilex.graphimport.interpret.InterpretFixtures;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.Interpreter;
import org.opensilex.graphimport.interpret.alias.LearnedAliases;
import org.opensilex.graphimport.ontology.Cardinality;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.HeaderResolver;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.RestrictionRow;
import org.opensilex.graphimport.validate.GraphProblem;
import org.opensilex.graphimport.validate.GraphValidation;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.graphimport.graph.GraphFixtures.SYRAH;
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
 * The preview of an import: each resource matched to what exists, each value compared with what the
 * resource holds, the grid of a sheet, the map of the sheets, the Turtle export.
 *
 * @author Arnaud Charleroy
 */
public class ImportPreviewTest {

    private static final URI P1 = URI.create("http://test.opensilex.org/id/so/p1");
    private static final URI SENSOR = URI.create("http://test.opensilex.org/id/sensor/s1");
    private static final URI SENSOR_CLASS = URI.create("http://test.opensilex.org/ontology#Sensor");
    private static final URI DATE = URI.create(XSD.date.getURI());
    private static final URI DECIMAL = URI.create(XSD.decimal.getURI());
    private static final URI DOUBLE = URI.create(XSD.xdouble.getURI());
    private static final URI STRING = URI.create(XSD.xstring.getURI());
    private static final URI HEIGHT = URI.create("http://test.opensilex.org/ontology#hasHeight");
    private static final URI WEIGHT = URI.create("http://test.opensilex.org/ontology#hasWeight");

    private static final SheetSpec PLOTS = SheetSpec.of("Parcelles", List.of("nom", "Date de création", "germplasme"),
            new String[]{"P1", "03/04/2026", "Syrah"},
            new String[]{"P2", "25/04/2026", "Syrha"});
    private static final SheetSpec MORE = SheetSpec.of("Compléments", List.of("nom", "remarque", "date"),
            new String[]{"P1", "bord", "2026-04-03"},
            new String[]{"P2", "centre", "2026-05-01"});
    private static final SheetSpec SUBPLOTS = SheetSpec.of("Sous-parcelles", List.of("nom", "parent"),
            new String[]{"SP1", "P1"},
            new String[]{"SP2", "Parcelle 404"});

    /**
     * The germplasm of {@link GraphFixtures#instance()}, a plot P1, which holds the values it is given, and a
     * sensor; a plot the user may not see is not there.
     */
    private static final class Instance implements InstanceIndex {

        private final InstanceIndex germplasm = GraphFixtures.instance();
        private final Map<URI, List<StoredValue>> p1Held;

        Instance(Map<URI, List<StoredValue>> p1Held) {
            this.p1Held = p1Held;
        }

        @Override
        public Map<String, Set<URI>> lookUp(URI expectedClass, Collection<String> names) throws Exception {
            if (!PLOT.equals(expectedClass) && !SO.equals(expectedClass)) {
                return germplasm.lookUp(expectedClass, names);
            }
            Map<String, Set<URI>> found = new LinkedHashMap<>();
            names.stream().map(InstanceIndex::key).filter("p1"::equals).forEach(key -> found.put(key, Set.of(P1)));
            return found;
        }

        @Override
        public Map<String, List<Suggestion>> near(URI expectedClass, Collection<String> names) throws Exception {
            return germplasm.near(expectedClass, names);
        }

        @Override
        public Map<URI, Set<URI>> existing(Collection<URI> uris) {
            Map<URI, Set<URI>> found = new LinkedHashMap<>();
            for (URI uri : uris) {
                if (P1.equals(uri)) {
                    found.put(uri, Set.of(PLOT));
                } else if (SENSOR.equals(uri)) {
                    found.put(uri, Set.of(SENSOR_CLASS));
                }
            }
            return found;
        }

        @Override
        public Map<URI, Map<URI, List<StoredValue>>> storedValues(Collection<URI> instances, Collection<URI> properties) {
            if (!instances.contains(P1)) {
                return Map.of();
            }
            Map<URI, List<StoredValue>> held = new LinkedHashMap<>(p1Held);
            held.keySet().retainAll(properties);
            return Map.of(P1, held);
        }
    }

    /** The plot P1 holding its name, untyped, and a creation date. */
    private static Instance instance(String p1Date) {
        Map<URI, List<StoredValue>> held = new LinkedHashMap<>();
        held.put(NAME, List.of(StoredValue.literal("P1", null, null)));
        held.put(CREATION_DATE, List.of(StoredValue.literal(p1Date, DATE, null)));
        return new Instance(held);
    }

    @Test
    public void threeSheetsComparedWithTheInstance() throws Exception {
        ImportPreview preview = preview(instance("2026-04-03"), workbook(PLOTS, MORE, SUBPLOTS), threeSheets());

        assertEquals(new PreviewCounts(3, 1, 0, 0, 2), preview.getCounts());
        NodeComparison p1 = node(preview, "P1");
        assertEquals(NodeState.COMPLETED, p1.state());
        assertEquals(P1, p1.existing());
        assertEquals("03/04/2026 written, 2026-04-03 stored: one date", List.of(ValueState.PRESENT, ValueState.PRESENT),
                states(p1, CREATION_DATE));
        assertEquals("an untyped name and a string are one value, given by two sheets",
                List.of(ValueState.PRESENT, ValueState.PRESENT), states(p1, NAME));
        assertEquals(List.of(ValueState.NEW), states(p1, COMMENT));
        assertEquals(List.of(ValueState.NEW), states(p1, HAS_GERMPLASM));

        NodeComparison sp1 = node(preview, "SP1");
        assertEquals(NodeState.CREATED, sp1.state());
        ValueComparison parent = sp1.values().stream().filter(value -> value.property().equals(IS_PART_OF)).findFirst()
                .orElseThrow();
        assertEquals("the row P1 is the existing plot: the link points to it", P1, parent.target());

        NodeComparison p2 = node(preview, "P2");
        assertEquals(NodeState.CREATED, p2.state());
        assertTrue("two dates for one: the file must be corrected", p2.toFix());
        assertEquals(List.of(ValueState.UNUSABLE), states(p2, HAS_GERMPLASM));
        assertTrue(node(preview, "SP2").toFix());
        assertFalse(p1.toFix());
        assertEquals(3, preview.getProblemTotal());

        SheetMap map = preview.getMap();
        assertEquals(List.of("Parcelles", "Compléments", "Sous-parcelles"),
                map.sheets().stream().map(SheetMap.Sheet::name).toList());
        assertTrue(map.links().contains(new SheetMap.Link("Parcelles", "Compléments", SheetMap.Kind.SAME_RESOURCE, null,
                2)));
        assertTrue(map.links().contains(new SheetMap.Link("Sous-parcelles", "Parcelles", SheetMap.Kind.ROW,
                "fait partie de", 1)));
        assertFalse("rows are drawn to the first sheet they are in; Compléments is reached through the shared "
                + "resources", map.links().contains(new SheetMap.Link("Sous-parcelles", "Compléments",
                SheetMap.Kind.ROW, "fait partie de", 1)));
        assertTrue(map.links().contains(new SheetMap.Link("Parcelles", "Matériel génétique", SheetMap.Kind.INSTANCE,
                "has germplasm", 1)));
    }

    @Test
    public void aValueTheResourceHoldsOtherwiseIsAConflict() throws Exception {
        ImportPreview preview = preview(instance("2026-01-01"), workbook(PLOTS, MORE, SUBPLOTS), threeSheets());

        NodeComparison p1 = node(preview, "P1");
        assertEquals(NodeState.CONFLICT, p1.state());
        ValueComparison date = p1.values().stream().filter(value -> value.property().equals(CREATION_DATE)).findFirst()
                .orElseThrow();
        assertEquals(ValueState.CONFLICT, date.state());
        assertEquals(List.of("2026-01-01"), date.held());
        assertEquals(1, preview.getCounts().conflict());
    }

    @Test
    public void aNameThatIsARowAndAnInstanceIsTheInstanceOnlyWhenTheRowIs() throws Exception {
        SheetSpec germplasm = SheetSpec.of("Matériel", List.of("nom", "note"),
                new String[]{"Syrah", "rouge"},
                new String[]{"Grenache", "rouge"});
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "germplasme"),
                new String[]{"P5", "Syrah"},
                new String[]{"P6", "Grenache"});
        ImportPreview preview = preview(instance("2026-04-03"), workbook(germplasm, plots), new ImportDecisions(Map.of(
                "Matériel", sheet(GERMPLASM, Map.of(0, NAME)),
                "Parcelles", sheet(PLOT, Map.of(0, NAME, 1, HAS_GERMPLASM)))));

        assertEquals(SYRAH, node(preview, "Syrah").existing());
        ValueComparison syrah = node(preview, "P5").values().stream()
                .filter(value -> value.property().equals(HAS_GERMPLASM)).findFirst().orElseThrow();
        assertEquals(ValueState.NEW, syrah.state());
        assertEquals(SYRAH, syrah.target());

        NodeComparison grenache = node(preview, "Grenache");
        assertEquals("two germplasm are named Grenache", NodeState.CONFLICT, grenache.state());
        assertEquals("preview.ambiguousMatch", grenache.reason().getKey());
        assertEquals(2, grenache.candidates().size());
        NodeComparison p6 = node(preview, "P6");
        assertEquals(List.of(ValueState.AMBIGUOUS), states(p6, HAS_GERMPLASM));
        assertTrue(p6.toFix());
    }

    @Test
    public void aWrittenUriIsTheResourceOnlyWhenItIsVisibleAndOfAFittingClass() throws Exception {
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("id", "remarque"),
                new String[]{P1.toString(), "bord"},
                new String[]{SENSOR.toString(), "capteur"},
                new String[]{"http://test.opensilex.org/id/so/hidden", "cachée"});
        ImportPreview preview = preview(instance("2026-04-03"), workbook(plots),
                new ImportDecisions(Map.of("Parcelles", sheet(PLOT, Map.of(1, COMMENT)))));

        NodeComparison p1 = node(preview, P1.toString());
        assertEquals(NodeState.COMPLETED, p1.state());
        assertEquals("the name the file lacks is held by the plot", List.of(), p1.missing());

        NodeComparison sensor = node(preview, SENSOR.toString());
        assertEquals(NodeState.CONFLICT, sensor.state());
        assertEquals("preview.classMismatch", sensor.reason().getKey());

        NodeComparison hidden = node(preview, "http://test.opensilex.org/id/so/hidden");
        assertEquals("a resource the user may not see reads as new", NodeState.CREATED, hidden.state());
        assertNull(hidden.existing());
        assertEquals(List.of("label"), hidden.missing());
        assertTrue("a new resource without its required name must be corrected", hidden.toFix());
        assertTrue("the warning of the check is replaced by what the preview found", hidden.problems().stream()
                .noneMatch(problem -> problem.message().getKey().equals("problem.requiredMissing")));
    }

    @Test
    public void theGridShowsEachCellWithWhatTheImportWouldDo() throws Exception {
        WorkbookStructure workbook = workbook(PLOTS, MORE, SUBPLOTS);
        ImportPreview preview = preview(instance("2026-04-03"), workbook, threeSheets());

        PreviewGrid.Page first = PreviewGrid.page(preview, workbook.getSheet("Parcelles").orElseThrow(), null, false, 0, 1);
        assertEquals(2, first.total());
        assertEquals(1, first.rows().size());
        assertEquals(List.of(CellState.PRESENT, CellState.PRESENT, CellState.NEW),
                first.rows().get(0).cells().stream().map(PreviewGrid.Cell::state).toList());
        assertEquals(PreviewColumn.Role.LINK, first.columns().get(2).role());

        PreviewGrid.Page toFix = PreviewGrid.page(preview, workbook.getSheet("Parcelles").orElseThrow(), null, true, 0,
                PreviewGrid.MAX_PAGE_SIZE);
        assertEquals(1, toFix.total());
        PreviewGrid.Row p2 = toFix.rows().get(0);
        assertEquals("P2", p2.node().node().getName());
        assertEquals(List.of(CellState.NEW, CellState.ERROR, CellState.ERROR),
                p2.cells().stream().map(PreviewGrid.Cell::state).toList());

        PreviewGrid.Page completed = PreviewGrid.page(preview, workbook.getSheet("Compléments").orElseThrow(),
                NodeState.COMPLETED, false, 0, 20);
        assertEquals(1, completed.total());
        assertEquals(0, completed.rows().get(0).row());
        PreviewGrid.Page beyond = PreviewGrid.page(preview, workbook.getSheet("Compléments").orElseThrow(), null, false,
                5, 20);
        assertEquals(List.of(), beyond.rows());
    }

    @Test
    public void numbersAndTheMatchedNameAreComparedOnceRead() throws Exception {
        Map<URI, List<StoredValue>> held = new LinkedHashMap<>();
        held.put(NAME, List.of(StoredValue.literal("P1", null, "fr")));
        held.put(HEIGHT, List.of(StoredValue.literal("1.50", DECIMAL, null)));
        held.put(WEIGHT, List.of(StoredValue.literal("1.5E0", DOUBLE, null)));
        InterpretationServices services = new InterpretationServices(InterpretFixtures.CATALOG,
                new ClassViews(ImportPreviewTest::measured), new Instance(held), LearnedAliases.NONE,
                new HeaderResolver(Map.of()), InterpretFixtures.OWNER, "fr");
        SheetSpec plots = SheetSpec.of("Parcelles", List.of("nom", "hauteur", "poids"), new String[]{"p1", "1,5", "1,50"});

        ImportPreview preview = preview(services, workbook(plots),
                new ImportDecisions(Map.of("Parcelles", sheet(PLOT, Map.of(0, NAME, 1, HEIGHT, 2, WEIGHT)))));

        NodeComparison p1 = node(preview, "p1");
        assertEquals("p1 is the name P1@fr it was matched by", List.of(ValueState.PRESENT), states(p1, NAME));
        assertEquals("1,5 written, 1.50 stored", List.of(ValueState.PRESENT), states(p1, HEIGHT));
        assertEquals("1,50 written, 1.5E0 stored", List.of(ValueState.PRESENT), states(p1, WEIGHT));
        assertEquals(NodeState.UNCHANGED, p1.state());
    }

    @Test
    public void theTurtleExportIsTheSameEveryTime() throws Exception {
        ImportPreview preview = preview(instance("2026-04-03"), workbook(PLOTS, MORE, SUBPLOTS), threeSheets());
        Map<String, String> prefixes = Map.of("vocabulary", "http://www.opensilex.org/vocabulary/oeso#");

        String turtle = new PreviewTurtle().write(preview, prefixes);

        assertEquals(turtle, new PreviewTurtle().write(preview, prefixes));
        assertTrue("an existing resource keeps its URI", turtle.contains("<" + P1 + ">"));
        assertTrue("a new resource has an indicative one", turtle.contains(PreviewTurtle.INDICATIVE_BASE + "resource:p2"));
        assertTrue("the link to the row P1 points to the plot", turtle.contains("vocabulary:isPartOf <" + P1 + ">"));
        assertTrue("Syrha and Parcelle 404 give no value", turtle.contains("the link being unclear: 2."));
    }

    private static ImportDecisions threeSheets() {
        Map<String, ImportDecisions.SheetDecisions> decisions = new LinkedHashMap<>();
        decisions.put("Parcelles", sheet(PLOT, Map.of(0, NAME, 1, CREATION_DATE, 2, HAS_GERMPLASM)));
        decisions.put("Compléments", sheet(PLOT, Map.of(0, NAME, 1, COMMENT, 2, CREATION_DATE)));
        decisions.put("Sous-parcelles", sheet(PLOT, Map.of(0, NAME, 1, IS_PART_OF)));
        return new ImportDecisions(decisions);
    }

    private static ImportDecisions.SheetDecisions sheet(URI classUri, Map<Integer, URI> properties) {
        return new ImportDecisions.SheetDecisions(classUri, 0, properties);
    }

    /** What a plot accepts, with a height in decimal and a weight in double. */
    private static ClassView measured(URI classUri) {
        if (GERMPLASM.equals(classUri)) {
            return InterpretFixtures.view(classUri);
        }
        return ClassView.assemble(classUri, InterpretFixtures.HIERARCHY, List.of(
                new RestrictionRow(SO, NAME, PropertyKind.DATA, STRING, new Cardinality(1, 1)),
                new RestrictionRow(SO, HEIGHT, PropertyKind.DATA, DECIMAL, new Cardinality(0, 1)),
                new RestrictionRow(SO, WEIGHT, PropertyKind.DATA, DOUBLE, new Cardinality(0, 1))), Map.of());
    }

    private static ImportPreview preview(InstanceIndex instance, WorkbookStructure workbook, ImportDecisions decisions)
            throws Exception {
        return preview(InterpretFixtures.services(instance, LearnedAliases.NONE), workbook, decisions);
    }

    private static ImportPreview preview(InterpretationServices services, WorkbookStructure workbook,
                                         ImportDecisions decisions) throws Exception {
        GraphImportModule module = new GraphImportModule();
        Interpretation interpretation = new Interpreter(services, module.getEvidenceSources())
                .interpret(workbook, decisions, null);
        ImportGraph graph = new GraphBuilder(services).build(workbook, interpretation, decisions);
        List<GraphProblem> problems = GraphValidation.problems(graph, module.getGraphValidators(), services);
        return new PreviewBuilder(services).build(interpretation, graph, problems);
    }

    private static NodeComparison node(ImportPreview preview, String name) {
        return preview.getNodes().stream().filter(node -> node.node().getName().equals(name)).findFirst().orElseThrow();
    }

    private static List<ValueState> states(NodeComparison node, URI property) {
        return node.values().stream()
                .filter(value -> value.property().equals(property))
                .map(ValueComparison::state)
                .toList();
    }
}
