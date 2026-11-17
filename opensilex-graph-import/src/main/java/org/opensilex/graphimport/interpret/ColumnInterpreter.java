//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.HeaderReading;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interprets the columns of a sheet whose class and identifier column are settled, then sends back
 * to the user two columns suggested for one single-valued property.
 *
 * @author Arnaud Charleroy
 */
class ColumnInterpreter {

    static final Combination.Bar BAR = new Combination.Bar(0.75, 0.2);

    /** The source id of a header written as a predicate, as {@code HeaderSource} gives it. */
    private static final String HEADER_SOURCE = "header";
    private static final Comparator<URI> BY_URI = Comparator.comparing(URI::toString);

    private final InterpretationServices services;
    private final List<EvidenceSource> sources;

    /** @param sources sorted by id, so that the clues always come in the same order */
    ColumnInterpreter(InterpretationServices services, List<EvidenceSource> sources) {
        this.services = services;
        this.sources = sources;
    }

    /**
     * @param chosen  the property the user chose for each column, by position
     * @param leftOut the columns the user left out, by position
     */
    List<ColumnInterpretation> interpret(SheetStructure sheet, List<HeaderReading> readings, ClassView classView,
                                         Integer identifierColumn, List<SheetFacts> sheets,
                                         Map<Integer, URI> chosen, Set<Integer> leftOut) throws Exception {
        List<ColumnInterpretation> columns = new ArrayList<>();
        for (int index = 0; index < readings.size(); index++) {
            HeaderReading reading = readings.get(index);
            String header = sheet.getHeaders().get(index);
            boolean identifier = Integer.valueOf(index).equals(identifierColumn);
            if (reading.form() == HeaderReading.Form.TYPE) {
                columns.add(new ColumnInterpretation(index, header, reading, identifier, true,
                        Choice.undecided(List.of(), List.of())));
                continue;
            }
            Choice<URI> property = classView == null
                    ? withoutClass(reading)
                    : withClass(new ColumnContext(sheet, index, header, reading,
                    ColumnValues.distinct(sheet, index, ColumnValues.EVIDENCE_SAMPLE), classView, identifier,
                    sheets, services));
            URI choice = chosen.get(index);
            if (choice != null && (classView == null || classView.getProperty(choice) != null)) {
                property = new Choice<>(choice, Choice.Status.CHOSEN, property.candidates(), property.notes());
            } else if (leftOut.contains(index)) {
                // The candidates stay, so that the user can take the column back.
                property = new Choice<>(null, Choice.Status.LEFT_OUT, property.candidates(), property.notes());
            }
            columns.add(new ColumnInterpretation(index, header, reading, identifier, false, property));
        }
        return classView == null ? columns : reopenDuplicates(columns, classView);
    }

    /** Before the class is known, only a header written as a predicate tells anything. */
    private Choice<URI> withoutClass(HeaderReading reading) {
        List<ReportMessage> notes = notes(reading, null);
        if (!reading.names()) {
            return Choice.undecided(List.of(), notes);
        }
        Evidence evidence = new Evidence(HEADER_SOURCE, 1, Reasons.header(reading.property().toString()));
        return new Choice<>(reading.property(), Choice.Status.DECIDED,
                List.of(new Candidate<>(reading.property(), 1, List.of(evidence))), notes);
    }

    /**
     * The sources that settle a column alone run first; when one does, the others are not asked, so a
     * column whose header names its predicate costs no lookup.
     */
    private Choice<URI> withClass(ColumnContext column) throws Exception {
        List<ReportMessage> notes = notes(column.reading(), column.classView());
        List<Clue<URI>> decisive = new ArrayList<>();
        for (EvidenceSource source : sources) {
            if (source.role() == EvidenceSource.Role.DECIDES) {
                decisive.addAll(source.gather(column));
            }
        }
        if (!decisive.isEmpty()) {
            List<Candidate<URI>> ranked = Combination.rank(decisive, List.of(), BY_URI);
            return new Choice<>(ranked.get(0).target(), Choice.Status.DECIDED, ranked, notes);
        }
        List<Clue<URI>> proposals = new ArrayList<>();
        List<Clue<URI>> modulations = new ArrayList<>();
        for (EvidenceSource source : sources) {
            if (source.role() == EvidenceSource.Role.PROPOSES) {
                proposals.addAll(source.gather(column));
            } else if (source.role() == EvidenceSource.Role.MODULATES) {
                modulations.addAll(source.gather(column));
            }
        }
        return Choice.fromEvidence(Combination.rank(proposals, modulations, BY_URI), BAR, notes);
    }

    private List<ReportMessage> notes(HeaderReading reading, ClassView classView) {
        List<ReportMessage> notes = new ArrayList<>();
        if (reading.form() == HeaderReading.Form.UNKNOWN_PREFIX) {
            notes.add(Reasons.unknownPrefix(reading.prefix()));
        }
        if (classView != null && reading.names() && classView.getProperty(reading.property()) == null) {
            notes.add(Reasons.notAccepted(reading.property().toString(),
                    services.catalog().labelOf(classView.getUri(), services.language())));
        }
        return notes;
    }

    /**
     * A single-valued property suggested for a column while another column also gives it: the
     * suggestion goes back to the user. What the file or the user decided stays.
     */
    private List<ColumnInterpretation> reopenDuplicates(List<ColumnInterpretation> columns, ClassView classView) {
        Map<URI, Integer> uses = new HashMap<>();
        for (ColumnInterpretation column : columns) {
            if (column.property().chosen() != null) {
                uses.merge(column.property().chosen(), 1, Integer::sum);
            }
        }
        List<ColumnInterpretation> checked = new ArrayList<>();
        for (ColumnInterpretation column : columns) {
            URI property = column.property().chosen();
            PropertyView view = property == null ? null : classView.getProperty(property);
            boolean duplicated = view != null && !view.cardinality().isMultiValued() && uses.get(property) > 1;
            if (duplicated && column.property().status() == Choice.Status.SUGGESTED) {
                String label = view.labels().in(services.language());
                String name = label != null ? label : ClassCatalog.localName(property);
                checked.add(column.withProperty(column.property().reopened(Reasons.duplicateSingle(name))));
            } else {
                checked.add(column);
            }
        }
        return checked;
    }
}
