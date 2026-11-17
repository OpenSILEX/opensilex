//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What a sheet holds: the class of its rows, the column that identifies them, what each column holds.
 *
 * @param name       the sheet's name
 * @param rowCount   how many data rows it has
 * @param sheetClass the class of its rows
 * @param rowClasses the classes its type column gives the rows, empty without one
 * @param identifier the column identifying its rows
 * @param columns    what each column holds, in order
 * @param classView  what the chosen class accepts; null while the class is undecided
 * @param notes      what the user should know about the sheet
 * @param measures   for a sheet of data, the variable each column of measures measures, by column position
 * @author Arnaud Charleroy
 */
public record SheetInterpretation(
        String name,
        int rowCount,
        Choice<URI> sheetClass,
        Set<URI> rowClasses,
        Choice<Integer> identifier,
        List<ColumnInterpretation> columns,
        ClassView classView,
        List<ReportMessage> notes,
        Map<Integer, MeasureChoice> measures) {

    private static final URI VARIABLE = URI.create("http://www.opensilex.org/vocabulary/oeso#hasVariable");
    private static final URI VALUE = URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#value");

    public SheetInterpretation {
        rowClasses = Set.copyOf(rowClasses);
        columns = List.copyOf(columns);
        notes = List.copyOf(notes);
        measures = Map.copyOf(measures);
    }

    public SheetInterpretation(String name, int rowCount, Choice<URI> sheetClass, Set<URI> rowClasses,
                               Choice<Integer> identifier, List<ColumnInterpretation> columns, ClassView classView,
                               List<ReportMessage> notes) {
        this(name, rowCount, sheetClass, rowClasses, identifier, columns, classView, notes, Map.of());
    }

    /**
     * The properties the class requires that no column of the sheet gives, in the order of the class
     * view. Only a hint: another sheet may give them for the same rows, and the check of the graph is
     * the judge.
     */
    public List<URI> unfilledRequired() {
        if (classView == null) {
            return List.of();
        }
        Set<URI> held = columns.stream()
                .filter(column -> !column.typeColumn() && column.property().chosen() != null
                        && column.property().status() != Choice.Status.UNDECIDED
                        && column.property().status() != Choice.Status.LEFT_OUT)
                .map(column -> column.property().chosen())
                .collect(Collectors.toCollection(HashSet::new));
        if (!measures.isEmpty()) {
            // Columns of measures give the variable and the value of each datum.
            held.add(VARIABLE);
            held.add(VALUE);
        }
        return classView.getProperties().stream()
                .filter(property -> property.cardinality().isRequired() && !held.contains(property.uri()))
                .map(PropertyView::uri)
                .toList();
    }
}
