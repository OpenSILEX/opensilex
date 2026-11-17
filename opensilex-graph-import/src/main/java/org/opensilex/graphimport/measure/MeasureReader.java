//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.measure;

import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.graph.DateColumn;
import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.graph.StoredValue;
import org.opensilex.graphimport.graph.scope.DataScope;
import org.opensilex.graphimport.interpret.Choice;
import org.opensilex.graphimport.interpret.ColumnInterpretation;
import org.opensilex.graphimport.interpret.ColumnValues;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.SheetInterpretation;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.graphimport.ontology.literal.DateOrder;
import org.opensilex.graphimport.ontology.literal.LiteralReader;
import org.opensilex.graphimport.ontology.literal.LiteralReading;
import org.opensilex.graphimport.ontology.literal.ReadingContext;
import org.opensilex.graphimport.validate.GraphProblem;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reads the sheets of data: one datum per row and column of measures (a column whose variable is chosen
 * or suggested), or per row in a sheet that names its variable in a column and its value in another.
 * <p>
 * Data do not go into the graph of resources: a sheet may give tens of thousands. They are read row by row
 * and handed to a consumer, which counts them for the check or writes them by batches.
 *
 * @author Arnaud Charleroy
 */
public final class MeasureReader {

    static final URI TARGET = URI.create("http://www.opensilex.org/vocabulary/oeso#hasTarget");
    static final URI DATE = URI.create("http://www.opensilex.org/vocabulary/oeso#hasDate");
    static final URI VARIABLE = URI.create("http://www.opensilex.org/vocabulary/oeso#hasVariable");
    static final URI VALUE = URI.create(RDF.value.getURI());
    private static final URI DATA_TYPE = URI.create(Oeso.hasDataType.getURI());
    private static final URI VARIABLE_CLASS = URI.create(Oeso.Variable.getURI());
    private static final URI SCIENTIFIC_OBJECT = URI.create(Oeso.ScientificObject.getURI());
    private static final URI DATE_TIME = URI.create(XSD.dateTime.getURI());
    private static final URI DAY = URI.create(XSD.date.getURI());
    /** The ways a cell says its value is not available, as the platform's data import knows them. */
    private static final Set<String> NOT_AVAILABLE = Set.of("na", "n/a");

    private final InterpretationServices services;

    public MeasureReader(InterpretationServices services) {
        this.services = services;
    }

    /** The data sheets read: what each gives, the problems of their cells, their columns of dates. */
    public record Result(List<DataSheetSummary> sheets, List<GraphProblem> problems, List<DateColumn> dateColumns) {
    }

    /** Whether a sheet holds data, by its class. */
    public boolean isData(SheetInterpretation sheet) {
        URI classUri = sheet.sheetClass().chosen();
        return classUri != null && (DataScope.DATA.equals(classUri) || services.catalog().isA(classUri, DataScope.DATA));
    }

    /**
     * Reads the data without knowing what the instance holds of the rows of the file: a datum measured on
     * a row of the file waits for it.
     *
     * @param graph  the graph of the other sheets, whose rows may be the targets of data
     * @param drafts what receives each datum read
     */
    public Result read(WorkbookStructure workbook, Interpretation interpretation, ImportDecisions decisions,
                       ImportGraph graph, Consumer<DataDraft> drafts) throws Exception {
        return read(workbook, interpretation, decisions, graph, rowKey -> Optional.empty(), drafts);
    }

    /**
     * @param graph  the graph of the other sheets, whose rows may be the targets of data
     * @param held   for the key of a row of the file, the resource of the instance it is, when the instance
     *               holds it already: a datum measured on that row is then ready, else it waits for it
     * @param drafts what receives each datum read
     */
    public Result read(WorkbookStructure workbook, Interpretation interpretation, ImportDecisions decisions,
                       ImportGraph graph, Function<String, Optional<URI>> held, Consumer<DataDraft> drafts)
            throws Exception {
        List<DataSheetSummary> sheets = new ArrayList<>();
        List<GraphProblem> problems = new ArrayList<>();
        List<DateColumn> dateColumns = new ArrayList<>();
        // Two sheets may give the same datum: it is refused the second time, as within a sheet.
        Set<String> seen = new HashSet<>();
        for (SheetInterpretation sheet : interpretation.sheets()) {
            Optional<SheetStructure> structure = workbook.getSheet(sheet.name());
            if (isData(sheet) && structure.isPresent() && structure.get().isTabular()) {
                sheets.add(readSheet(structure.get(), sheet, decisions.of(sheet.name()), graph, held, drafts,
                        new Found(problems, dateColumns, seen)));
            }
        }
        return new Result(sheets, problems, dateColumns);
    }

    /** What the reading of the sheets gathers as it goes. */
    private record Found(List<GraphProblem> problems, List<DateColumn> dateColumns, Set<String> seen) {
    }

    private DataSheetSummary readSheet(SheetStructure structure, SheetInterpretation sheet,
                                       ImportDecisions.SheetDecisions decided, ImportGraph graph,
                                       Function<String, Optional<URI>> held, Consumer<DataDraft> drafts,
                                       Found gathered) throws Exception {
        String name = sheet.name();
        List<GraphProblem> problems = gathered.problems();
        Integer targetColumn = columnOf(sheet, TARGET);
        Integer dateColumn = columnOf(sheet, DATE);
        Integer variableColumn = columnOf(sheet, VARIABLE);
        Integer valueColumn = sheet.measures().isEmpty() ? columnOf(sheet, VALUE) : null;
        Map<Integer, URI> measures = new LinkedHashMap<>();
        sheet.measures().forEach((column, measure) -> measures.put(column, measure.variable()));

        LiteralReader dates = null;
        if (dateColumn != null) {
            DateColumn column = new DateColumn(name, dateColumn, structure.getHeaders().get(dateColumn), DATE_TIME,
                    DateOrder.detect(ColumnValues.all(structure, dateColumn)), decided.readingOf(dateColumn));
            gathered.dateColumns().add(column);
            dates = new LiteralReader(new ReadingContext(structure.isDate1904(), column.effectiveOrder(),
                    column.chosen().zone()));
        }
        LiteralReader values = new LiteralReader(new ReadingContext(structure.isDate1904(), DateOrder.UNDECIDED, null));

        Map<String, Target> targets = targetColumn == null ? Map.of()
                : targets(ColumnValues.all(structure, targetColumn), graph, held);
        Map<String, Set<URI>> namedVariables = variableColumn == null ? Map.of()
                : services.instances().lookUp(VARIABLE_CLASS, new LinkedHashSet<>(ColumnValues.all(structure,
                variableColumn)));
        Set<URI> variables = new HashSet<>(measures.values());
        namedVariables.values().stream().filter(found -> found.size() == 1).forEach(variables::addAll);
        Map<URI, URI> datatypes = datatypes(variables);

        int ready = 0;
        int waiting = 0;
        int notAvailable = 0;
        int refused = 0;
        List<List<String>> rows = structure.getRows();
        for (int row = 0; row < rows.size(); row++) {
            List<String> cells = rows.get(row);
            Map<Integer, URI> measured = new LinkedHashMap<>(measures);
            if (variableColumn != null && valueColumn != null) {
                Optional<URI> variable = variableOf(cells, variableColumn, namedVariables, name, row, problems);
                if (variable.isPresent()) {
                    measured.put(valueColumn, variable.get());
                } else if (ColumnValues.cell(cells, valueColumn) != null) {
                    refused++;
                }
            }
            if (measured.isEmpty()) {
                continue;
            }
            Target target = targetColumn == null ? Target.NONE
                    : targets.getOrDefault(ColumnValues.cell(cells, targetColumn), Target.NONE);
            String rawTarget = targetColumn == null ? null : ColumnValues.cell(cells, targetColumn);
            if (target.problem() != null) {
                problems.add(problem(target.problem(), new CellRef(name, row, targetColumn), rawTarget, rawTarget));
            }
            Optional<String> date = dateOf(cells, dateColumn, dates, name, row, problems, measured.keySet());
            for (Map.Entry<Integer, URI> measure : measured.entrySet()) {
                String raw = valueCell(cells, measure.getKey());
                if (raw == null) {
                    continue;
                }
                CellRef cell = new CellRef(name, row, measure.getKey());
                if (NOT_AVAILABLE.contains(raw.trim().toLowerCase(Locale.ROOT))) {
                    notAvailable++;
                    continue;
                }
                if (target.problem() != null || date.isEmpty()) {
                    refused++;
                    continue;
                }
                URI datatype = datatypes.get(measure.getValue());
                LiteralReading value = values.read(raw, datatype == null ? URI.create(XSD.xstring.getURI()) : datatype);
                if (value.isRefused()) {
                    problems.add(problem(value.problem(), cell, rawTarget, raw));
                    refused++;
                    continue;
                }
                String identity = target.key() + "\u0000" + date.get() + "\u0000" + measure.getValue();
                if (!gathered.seen().add(identity)) {
                    problems.add(problem(MeasureMessages.duplicate(), cell, rawTarget, raw));
                    refused++;
                    continue;
                }
                DataDraft draft = new DataDraft(cell, target.uri(), target.rowKey(), date.get(), measure.getValue(),
                        value.lexical());
                if (draft.waits()) {
                    waiting++;
                } else {
                    ready++;
                }
                drafts.accept(draft);
            }
        }
        return new DataSheetSummary(name, ready, waiting, notAvailable, refused);
    }

    /** What a target cell names: a resource, a row of the file to create, or a problem. */
    private record Target(URI uri, String rowKey, ReportMessage problem) {

        static final Target NONE = new Target(null, null, null);

        String key() {
            return uri != null ? uri.toString() : rowKey;
        }
    }

    /**
     * The targets the cells name: a URI written, else a scientific object of the file, else one of the
     * experiment, looked up by name all at once.
     */
    private Map<String, Target> targets(List<String> cells, ImportGraph graph, Function<String, Optional<URI>> held)
            throws Exception {
        Map<String, Target> targets = new HashMap<>();
        Map<String, GraphNode> nodes = graph.byKey();
        List<String> names = new ArrayList<>();
        for (String cell : new LinkedHashSet<>(cells)) {
            Optional<URI> uri = Uris.asUri(cell);
            GraphNode row = nodes.get(InstanceIndex.key(cell));
            if (uri.isPresent()) {
                targets.put(cell, new Target(uri.get(), null, null));
            } else if (row != null && isObject(row)) {
                // The row of the file is the target: the resource of the instance it is, once it exists.
                Optional<URI> resource = held.apply(row.getKey());
                targets.put(cell, resource.map(existing -> new Target(existing, null, null))
                        .orElseGet(() -> new Target(null, row.getKey(), null)));
            } else {
                names.add(cell);
            }
        }
        Map<String, Set<URI>> found = names.isEmpty() ? Map.of()
                : services.instances().lookUp(SCIENTIFIC_OBJECT, names);
        for (String name : names) {
            Set<URI> objects = found.getOrDefault(InstanceIndex.key(name), Set.of());
            targets.put(name, objects.size() == 1 ? new Target(objects.iterator().next(), null, null)
                    : new Target(null, null, objects.isEmpty() ? MeasureMessages.targetUnfound(name)
                    : MeasureMessages.targetAmbiguous(name, objects.size())));
        }
        return targets;
    }

    private boolean isObject(GraphNode node) {
        return node.getClasses().stream().anyMatch(classUri -> classUri.equals(SCIENTIFIC_OBJECT)
                || services.catalog().isA(classUri, SCIENTIFIC_OBJECT));
    }

    /**
     * The date of a row: a date and time with its offset, else a day, as the platform reads data dates.
     * A missing or unreadable date is said once, on the date cell, for the row's values.
     */
    private Optional<String> dateOf(List<String> cells, Integer dateColumn, LiteralReader dates, String sheet, int row,
                                    List<GraphProblem> problems, Collection<Integer> valueColumns) {
        String raw = dateColumn == null ? null : ColumnValues.cell(cells, dateColumn);
        if (raw == null) {
            boolean anyValue = valueColumns.stream().anyMatch(column -> ColumnValues.cell(cells, column) != null);
            if (anyValue) {
                int column = dateColumn != null ? dateColumn : valueColumns.iterator().next();
                problems.add(problem(MeasureMessages.noDate(), new CellRef(sheet, row, column), null, null));
            }
            return Optional.empty();
        }
        LiteralReading dateTime = dates.read(raw, DATE_TIME);
        if (!dateTime.isRefused()) {
            return Optional.of(dateTime.lexical());
        }
        LiteralReading day = dates.read(raw, DAY);
        if (!day.isRefused()) {
            return Optional.of(day.lexical());
        }
        problems.add(problem(dateTime.problem(), new CellRef(sheet, row, dateColumn), null, raw));
        return Optional.empty();
    }

    private Optional<URI> variableOf(List<String> cells, int variableColumn, Map<String, Set<URI>> named, String sheet,
                                     int row, List<GraphProblem> problems) {
        String raw = ColumnValues.cell(cells, variableColumn);
        if (raw == null) {
            return Optional.empty();
        }
        Optional<URI> written = Uris.asUri(raw);
        if (written.isPresent()) {
            return written;
        }
        Set<URI> found = named.getOrDefault(InstanceIndex.key(raw), Set.of());
        if (found.size() == 1) {
            return Optional.of(found.iterator().next());
        }
        problems.add(problem(MeasureMessages.variableUnfound(raw), new CellRef(sheet, row, variableColumn), null, raw));
        return Optional.empty();
    }

    /** The datatype of each variable, as the platform holds it; read only for variables the user may see. */
    private Map<URI, URI> datatypes(Collection<URI> variables) throws Exception {
        Map<URI, URI> datatypes = new HashMap<>();
        if (variables.isEmpty()) {
            return datatypes;
        }
        services.instances().existing(variables);
        services.instances().storedValues(variables, List.of(DATA_TYPE)).forEach((variable, values) -> {
            List<StoredValue> types = values.getOrDefault(DATA_TYPE, List.of());
            types.stream().findFirst().map(type -> type.resource() != null ? type.resource()
                    : Uris.asUri(type.lexical()).orElse(null)).ifPresent(type -> datatypes.put(variable, type));
        });
        return datatypes;
    }

    /** The column holding a property, by the user, the file or the evidence. */
    private static Integer columnOf(SheetInterpretation sheet, URI property) {
        for (ColumnInterpretation column : sheet.columns()) {
            Choice.Status status = column.property().status();
            boolean held = status == Choice.Status.CHOSEN || status == Choice.Status.DECIDED
                    || status == Choice.Status.SUGGESTED;
            if (held && property.equals(column.property().chosen()) && !sheet.measures().containsKey(column.index())) {
                return column.index();
            }
        }
        return null;
    }

    /**
     * A cell of values, cleaned; null when empty. Read apart from the other cells, which take a value
     * written {@code NA} as missing: here it is counted as not available.
     */
    private static String valueCell(List<String> cells, int column) {
        if (column >= cells.size()) {
            return null;
        }
        String value = ExcelValueParser.clean(cells.get(column));
        return value == null || value.isBlank() ? null : value;
    }

    private static GraphProblem problem(ReportMessage message, CellRef cell, String target, String raw) {
        return new GraphProblem(GraphProblem.Severity.ERROR, message, List.of(cell), false,
                target == null ? cell.sheet() + " #" + (cell.row() + 1) : target, raw, List.of());
    }
}
