//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.binding.ColumnReading;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.interpret.Choice;
import org.opensilex.graphimport.interpret.ColumnInterpretation;
import org.opensilex.graphimport.interpret.ColumnValues;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.SheetClassSuggester;
import org.opensilex.graphimport.interpret.SheetInterpretation;
import org.opensilex.graphimport.measure.MeasureReader;
import org.opensilex.graphimport.ontology.PlatformReadings;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.PropertyView;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.graphimport.ontology.literal.DateOrder;
import org.opensilex.graphimport.ontology.literal.LiteralReader;
import org.opensilex.graphimport.ontology.literal.LiteralReading;
import org.opensilex.graphimport.ontology.literal.ReadingContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Turns the interpreted sheets into one graph: each row a resource, merged with every row of any
 * sheet that shares its identifier; each cell of a column whose property is settled a value of it.
 * <p>
 * Two passes: the rows first, so that every resource has all the classes its sheets give it, then the
 * links, which point to a row only when its class fits. A column whose property is only suggested
 * enters the graph as the screen shows it; what is left out is said in the graph's notes.
 *
 * @author Arnaud Charleroy
 */
public class GraphBuilder {

    private static final URI LITERAL = URI.create(RDFS.Literal.getURI());
    private static final Set<String> DATE_TYPES = Set.of(XSD.date.getURI(), XSD.dateTime.getURI());

    /** Starts the key of a row without identifier: no name read from a cell holds this character. */
    private static final String ANONYMOUS = "\u0000";

    private final InterpretationServices services;
    private final SheetClassSuggester classNames;

    public GraphBuilder(InterpretationServices services) {
        this.services = services;
        this.classNames = new SheetClassSuggester(services);
    }

    public ImportGraph build(WorkbookStructure workbook, Interpretation interpretation, ImportDecisions decisions)
            throws Exception {
        Map<String, GraphNode> nodes = new LinkedHashMap<>();
        List<LinkResolver.PendingLink> links = new ArrayList<>();
        List<ReportMessage> notes = new ArrayList<>();
        List<DateColumn> dateColumns = new ArrayList<>();
        List<String> withoutClass = new ArrayList<>();
        for (SheetInterpretation sheet : interpretation.sheets()) {
            Optional<SheetStructure> structure = workbook.getSheet(sheet.name());
            if (structure.isEmpty() || !structure.get().isTabular()) {
                continue;
            }
            if (sheet.classView() == null || sheet.sheetClass().chosen() == null) {
                withoutClass.add(sheet.name());
            } else if (new MeasureReader(services).isData(sheet)) {
                // Data are read apart, row by row: a sheet of data may give tens of thousands.
                continue;
            } else {
                addSheet(structure.get(), sheet, decisions.of(sheet.name()), nodes, links, notes, dateColumns);
            }
        }
        if (!withoutClass.isEmpty()) {
            // One note for every sheet left out, first: a workbook of fifteen sheets would otherwise open on
            // fifteen lines saying the same thing.
            notes.add(0, withoutClass.size() == 1
                    ? GraphNotes.sheetLeftOut(withoutClass.get(0))
                    : GraphNotes.sheetsLeftOut(withoutClass));
        }
        List<LinkTarget> targets = new LinkResolver(services).resolve(links, nodes);
        for (int index = 0; index < links.size(); index++) {
            LinkResolver.PendingLink link = links.get(index);
            link.node().addValue(link.property(), GraphValue.link(link.cell(), link.raw(), targets.get(index)));
        }
        return new ImportGraph(new ArrayList<>(nodes.values()), notes, dateColumns);
    }

    /** The columns of a sheet that give values: a settled property its class accepts. */
    private record SheetColumns(Map<Integer, PropertyView> properties, Map<Integer, LiteralReader> readers,
                                Map<Integer, String> languages, Integer typeColumn) {
    }

    private void addSheet(SheetStructure structure, SheetInterpretation sheet, ImportDecisions.SheetDecisions decided,
                          Map<String, GraphNode> nodes, List<LinkResolver.PendingLink> links,
                          List<ReportMessage> notes, List<DateColumn> dateColumns) {
        URI sheetClass = sheet.sheetClass().chosen();
        SheetColumns columns = columns(structure, sheet, decided, notes, dateColumns);
        Integer identifierColumn = sheet.identifier().chosen();
        if (identifierColumn == null) {
            notes.add(GraphNotes.noIdentifier(sheet.name()));
        }
        int withoutIdentifier = 0;
        int unresolvedTypes = 0;
        List<List<String>> rows = structure.getRows();
        for (int row = 0; row < rows.size(); row++) {
            List<String> cells = rows.get(row);
            String identifier = identifierColumn == null ? null : ColumnValues.cell(cells, identifierColumn);
            if (identifierColumn != null && identifier == null) {
                withoutIdentifier++;
            }
            GraphNode node = nodes.computeIfAbsent(keyOf(sheet.name(), row, identifier),
                    key -> newNode(key, sheet.name(), cells, identifierColumn));
            URI rowClass = rowClass(cells, columns.typeColumn());
            boolean typed = columns.typeColumn() != null && ColumnValues.cell(cells, columns.typeColumn()) != null;
            if (rowClass == null && typed) {
                unresolvedTypes++;
            }
            node.addRow(new CellRef(sheet.name(), row, identifierColumn == null ? 0 : identifierColumn),
                    rowClass == null ? sheetClass : rowClass);
            addCells(node, sheet.name(), row, cells, columns, links);
        }
        if (withoutIdentifier > 0) {
            notes.add(GraphNotes.emptyIdentifiers(sheet.name(), withoutIdentifier));
        }
        if (unresolvedTypes > 0) {
            notes.add(GraphNotes.unresolvedTypes(sheet.name(), unresolvedTypes));
        }
    }

    private SheetColumns columns(SheetStructure structure, SheetInterpretation sheet,
                                 ImportDecisions.SheetDecisions decided, List<ReportMessage> notes,
                                 List<DateColumn> dateColumns) {
        Map<Integer, PropertyView> properties = new LinkedHashMap<>();
        Map<Integer, LiteralReader> readers = new LinkedHashMap<>();
        Map<Integer, String> languages = new LinkedHashMap<>();
        Integer typeColumn = null;
        List<String> leftOut = new ArrayList<>();
        for (ColumnInterpretation column : sheet.columns()) {
            if (column.typeColumn()) {
                typeColumn = column.index();
                continue;
            }
            URI property = column.property().chosen();
            if (property == null) {
                // A column the user left out is not one to decide: the note does not name it.
                if (!column.identifier() && column.property().status() != Choice.Status.LEFT_OUT) {
                    leftOut.add(column.header());
                }
                continue;
            }
            PropertyView view = sheet.classView().getProperty(property);
            if (view == null) {
                notes.add(GraphNotes.propertyNotAccepted(sheet.name(), column.header()));
                continue;
            }
            properties.put(column.index(), view);
            languages.put(column.index(), column.reading().language());
            if (view.kind() == PropertyKind.DATA) {
                readers.put(column.index(), reader(structure, column, view, decided.readingOf(column.index()),
                        dateColumns));
            }
        }
        if (!leftOut.isEmpty()) {
            notes.add(GraphNotes.columnsLeftOut(sheet.name(), leftOut));
        }
        return new SheetColumns(properties, readers, languages, typeColumn);
    }

    /** Reads a column with its sheet's date system, and the order and zone of its dates. */
    private static LiteralReader reader(SheetStructure structure, ColumnInterpretation column, PropertyView view,
                                        ColumnReading chosen, List<DateColumn> dateColumns) {
        DateOrder order = DateOrder.UNDECIDED;
        if (view.range() != null && DATE_TYPES.contains(view.range().toString())) {
            DateColumn dates = new DateColumn(structure.getName(), column.index(), column.header(), view.range(),
                    DateOrder.detect(ColumnValues.all(structure, column.index())), chosen);
            dateColumns.add(dates);
            order = dates.effectiveOrder();
        }
        return new LiteralReader(new ReadingContext(structure.isDate1904(), order, chosen.zone()));
    }

    private static String keyOf(String sheet, int row, String identifier) {
        if (identifier == null) {
            return ANONYMOUS + sheet + ANONYMOUS + row;
        }
        return Uris.asUri(identifier).map(URI::toString).orElseGet(() -> InstanceIndex.key(identifier));
    }

    private static GraphNode newNode(String key, String sheet, List<String> cells, Integer identifierColumn) {
        String identifier = identifierColumn == null ? null : ColumnValues.cell(cells, identifierColumn);
        if (identifier == null) {
            return new GraphNode(key, sheet + " #" + key.substring(key.lastIndexOf(ANONYMOUS) + 1), null, false);
        }
        return new GraphNode(key, identifier, Uris.asUri(identifier).orElse(null), true);
    }

    /** The class a type cell names; null without a type cell, or when it names none. */
    private URI rowClass(List<String> cells, Integer typeColumn) {
        String type = typeColumn == null ? null : ColumnValues.cell(cells, typeColumn);
        return type == null ? null : classNames.classNamed(type);
    }

    private static void addCells(GraphNode node, String sheet, int row, List<String> cells, SheetColumns columns,
                                 List<LinkResolver.PendingLink> links) {
        for (Map.Entry<Integer, PropertyView> column : columns.properties().entrySet()) {
            String raw = ColumnValues.cell(cells, column.getKey());
            if (raw == null) {
                continue;
            }
            PropertyView view = column.getValue();
            CellRef cell = new CellRef(sheet, row, column.getKey());
            if (view.kind() == PropertyKind.OBJECT) {
                links.add(new LinkResolver.PendingLink(node, view.uri(), view.range(), cell, raw));
            } else {
                URI datatype = view.range() == null ? LITERAL : view.range();
                LiteralReader reader = columns.readers().get(column.getKey());
                LiteralReading reading = reader.read(raw, datatype);
                if (reading.isRefused() && PlatformReadings.isMoment(view.uri())) {
                    // A moment of an event given as a day alone is the start of that day.
                    LiteralReading startOfDay = reader.read(raw.trim() + " 00:00", datatype);
                    reading = startOfDay.isRefused() ? reading : startOfDay;
                }
                node.addValue(view.uri(), GraphValue.literal(cell, raw, reading, columns.languages().get(column.getKey())));
            }
        }
    }
}
