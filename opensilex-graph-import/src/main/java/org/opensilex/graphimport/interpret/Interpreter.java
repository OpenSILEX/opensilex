//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.graph.scope.DataScope;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.HeaderReading;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Interprets a workbook in a fixed order, so that the same file and decisions always give the same
 * answer: first the identifier column of every sheet, then the class of every sheet, then the columns
 * of every sheet, which may then rely on the classes and identifiers of all the sheets.
 *
 * @author Arnaud Charleroy
 */
public class Interpreter {

    private static final URI SCIENTIFIC_OBJECT = URI.create(Oeso.ScientificObject.getURI());
    private static final URI DATA = DataScope.DATA;
    private static final URI VARIABLE = URI.create(Oeso.Variable.getURI());
    private static final URI VALUE = URI.create(RDF.value.getURI());
    private static final URI LABEL = URI.create(RDFS.label.getURI());

    private final InterpretationServices services;
    private final ColumnInterpreter columns;

    public Interpreter(InterpretationServices services, Collection<EvidenceSource> sources) {
        this.services = services;
        List<EvidenceSource> sorted = new ArrayList<>(sources);
        sorted.sort(Comparator.comparing(EvidenceSource::id));
        this.columns = new ColumnInterpreter(services, sorted);
    }

    /**
     * @param decisions  what the user decided so far
     * @param experiment the experiment chosen, null when none is
     */
    public Interpretation interpret(WorkbookStructure workbook, ImportDecisions decisions, URI experiment)
            throws Exception {
        List<SheetDraft> drafts = new ArrayList<>();
        for (SheetStructure sheet : workbook.getSheets()) {
            drafts.add(draft(sheet, decisions.of(sheet.getName())));
        }
        List<SheetFacts> facts = drafts.stream().map(SheetDraft::facts).toList();

        List<SheetInterpretation> sheets = new ArrayList<>();
        boolean scientificObjects = false;
        for (SheetDraft draft : drafts) {
            List<ColumnInterpretation> interpreted = draft.sheet().isTabular()
                    ? columns.interpret(draft.sheet(), draft.readings(), draft.classView(),
                    draft.identifier().chosen(), facts, decisions.of(draft.sheet().getName()).properties(),
                    decisions.of(draft.sheet().getName()).leftOut())
                    : List.of();
            List<ReportMessage> notes = draft.sheet().isTabular() ? List.of() : List.of(Reasons.notATable());
            Map<Integer, MeasureChoice> measures = Map.of();
            if (isData(draft.sheetClass().choice().chosen())) {
                measures = measures(draft.sheet(), interpreted, decisions.of(draft.sheet().getName()));
                interpreted = asMeasures(interpreted, measures);
            }
            sheets.add(new SheetInterpretation(draft.sheet().getName(), draft.sheet().getDataRowCount(),
                    draft.sheetClass().choice(), draft.sheetClass().rowClasses(), draft.identifier(), interpreted,
                    draft.classView(), notes, measures));
            URI chosenClass = draft.sheetClass().choice().chosen();
            scientificObjects |= chosenClass != null && services.catalog().isA(chosenClass, SCIENTIFIC_OBJECT);
        }
        List<ReportMessage> notes = scientificObjects && experiment == null
                ? List.of(Reasons.noExperiment())
                : List.of();
        return new Interpretation(sheets, notes);
    }

    private boolean isData(URI classUri) {
        return classUri != null && (DATA.equals(classUri) || services.catalog().isA(classUri, DATA));
    }

    /**
     * The variable each column of a sheet of data measures: the one the user chose, else the one whose name
     * the header is, when exactly one visible variable has it. A column that holds another property, the
     * identifier, the type, or is left out measures none.
     */
    private Map<Integer, MeasureChoice> measures(SheetStructure sheet, List<ColumnInterpretation> columns,
                                                 ImportDecisions.SheetDecisions decided) throws Exception {
        Map<Integer, MeasureChoice> measures = new LinkedHashMap<>();
        Map<URI, String> labels = labelsOf(decided.measures().values());
        decided.measures().forEach((column, variable) -> measures.put(column,
                new MeasureChoice(variable, Choice.Status.CHOSEN, labels.getOrDefault(variable, variable.toString()))));
        Map<Integer, String> headers = new LinkedHashMap<>();
        for (ColumnInterpretation column : columns) {
            Choice.Status status = column.property().status();
            boolean free = status != Choice.Status.CHOSEN && status != Choice.Status.DECIDED
                    && status != Choice.Status.LEFT_OUT;
            if (free && !column.typeColumn() && !column.identifier() && !measures.containsKey(column.index())
                    && column.header() != null && !column.header().isBlank()) {
                headers.put(column.index(), column.header());
            }
        }
        if (headers.isEmpty()) {
            return measures;
        }
        Map<String, Set<URI>> found = services.instances().lookUp(VARIABLE, headers.values());
        headers.forEach((column, header) -> {
            Set<URI> variables = found.getOrDefault(InstanceIndex.key(header), Set.of());
            if (variables.size() == 1) {
                measures.put(column, new MeasureChoice(variables.iterator().next(), Choice.Status.SUGGESTED, header));
            }
        });
        return measures;
    }

    /** The columns, those of measures holding the value of each datum. */
    private static List<ColumnInterpretation> asMeasures(List<ColumnInterpretation> columns,
                                                         Map<Integer, MeasureChoice> measures) {
        return columns.stream().map(column -> {
            MeasureChoice measure = measures.get(column.index());
            return measure == null ? column : column.withProperty(new Choice<>(VALUE, measure.status(),
                    column.property().candidates(), column.property().notes()));
        }).toList();
    }

    /** The names of variables the user chose, read only among those the user may see. */
    private Map<URI, String> labelsOf(Collection<URI> variables) throws Exception {
        Map<URI, String> labels = new HashMap<>();
        if (variables.isEmpty()) {
            return labels;
        }
        services.instances().existing(variables);
        services.instances().storedValues(variables, List.of(LABEL)).forEach((variable, values) ->
                values.getOrDefault(LABEL, List.of()).stream().findFirst()
                        .ifPresent(label -> labels.put(variable, label.lexical())));
        return labels;
    }

    /** A sheet with its identifier column and class settled, before its columns are. */
    private record SheetDraft(SheetStructure sheet, List<HeaderReading> readings, Choice<Integer> identifier,
                              SheetClassSuggester.SheetClass sheetClass, ClassView classView, SheetFacts facts) {
    }

    private SheetDraft draft(SheetStructure sheet, ImportDecisions.SheetDecisions decided) throws Exception {
        List<HeaderReading> readings = sheet.getHeaders().stream().map(services.headers()::read).toList();
        if (!sheet.isTabular()) {
            Choice<Integer> none = Choice.undecided(List.of(), List.of());
            SheetClassSuggester.SheetClass noClass = new SheetClassSuggester.SheetClass(
                    Choice.undecided(List.of(), List.of()), Set.of());
            return new SheetDraft(sheet, readings, none, noClass, null, new SheetFacts(sheet.getName(), null, Set.of()));
        }

        Choice<Integer> identifier = IdentifierDetector.detect(sheet, readings);
        Integer chosenColumn = decided.identifierColumn();
        if (chosenColumn != null && chosenColumn >= 0 && chosenColumn < readings.size()) {
            identifier = new Choice<>(chosenColumn, Choice.Status.CHOSEN, identifier.candidates(), List.of());
        }

        Integer typeColumn = null;
        for (int index = 0; index < readings.size() && typeColumn == null; index++) {
            if (readings.get(index).form() == HeaderReading.Form.TYPE) {
                typeColumn = index;
            }
        }
        SheetClassSuggester.SheetClass sheetClass = new SheetClassSuggester(services)
                .suggest(sheet, readings, typeColumn, identifier);
        if (decided.classUri() != null && services.catalog().contains(decided.classUri())) {
            Choice<URI> suggested = sheetClass.choice();
            sheetClass = new SheetClassSuggester.SheetClass(new Choice<>(decided.classUri(), Choice.Status.CHOSEN,
                    suggested.candidates(), suggested.notes()), sheetClass.rowClasses());
        }

        URI classUri = sheetClass.choice().chosen();
        ClassView classView = classUri == null ? null : services.views().get(classUri);
        return new SheetDraft(sheet, readings, identifier, sheetClass, classView,
                new SheetFacts(sheet.getName(), classView, identifierKeys(sheet, identifier.chosen())));
    }

    private static Set<String> identifierKeys(SheetStructure sheet, Integer column) {
        Set<String> keys = new HashSet<>();
        if (column != null) {
            ColumnValues.all(sheet, column).stream()
                    .filter(Objects::nonNull)
                    .map(InstanceIndex::key)
                    .forEach(keys::add);
        }
        return keys;
    }
}
