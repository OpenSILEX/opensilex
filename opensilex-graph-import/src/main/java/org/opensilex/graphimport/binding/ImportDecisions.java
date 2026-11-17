//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.binding;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * What the user decided about the sheets of an import. The interpretation takes it as input: a
 * decision replaces the suggestion, the evidence stays shown beside it.
 *
 * @param sheets   the decisions of each sheet, by sheet name
 * @param declared the resources the user described because the files do not, such as the factor a
 *                 sheet of levels belongs to, in the order they were declared
 * @author Arnaud Charleroy
 */
public record ImportDecisions(Map<String, SheetDecisions> sheets, List<DeclaredResource> declared) {

    /** Nothing decided yet. */
    public static final ImportDecisions NONE = new ImportDecisions(Map.of(), List.of());

    public ImportDecisions {
        sheets = Map.copyOf(sheets);
        declared = List.copyOf(declared);
    }

    public ImportDecisions(Map<String, SheetDecisions> sheets) {
        this(sheets, List.of());
    }

    /** The resource declared under this identifier. */
    public Optional<DeclaredResource> declared(String id) {
        return declared.stream().filter(resource -> resource.id().equals(id)).findFirst();
    }

    public SheetDecisions of(String sheet) {
        return sheets.getOrDefault(sheet, SheetDecisions.NONE);
    }

    /**
     * What the user decided about one sheet.
     *
     * @param classUri         the class of its rows, null when not decided
     * @param identifierColumn the column identifying its rows, null when not decided
     * @param properties       the property of each decided column, by column position
     * @param readings         how the dates of some columns are written, by column position
     * @param leftOut          the columns the user left out: they give no value, and do not go back to
     *                         their suggestion
     * @param given            values the user gave for every row of the sheet, by property, for what
     *                         no column says, such as the factor of a sheet of levels
     * @param measures         for a sheet of data, the variable each column of values measures, by column
     *                         position: one datum per row and such column
     */
    public record SheetDecisions(URI classUri, Integer identifierColumn, Map<Integer, URI> properties,
                                 Map<Integer, ColumnReading> readings, Set<Integer> leftOut,
                                 Map<URI, String> given, Map<Integer, URI> measures) {

        public static final SheetDecisions NONE = new SheetDecisions(null, null, Map.of(), Map.of(), Set.of(),
                Map.of(), Map.of());

        /** URIs are kept in their full form, however the client wrote them, so that they compare equal. */
        public SheetDecisions {
            classUri = classUri == null ? null : Uris.expand(classUri);
            Map<Integer, URI> expanded = new HashMap<>();
            properties.forEach((column, property) -> expanded.put(column, Uris.expand(property)));
            properties = Map.copyOf(expanded);
            Map<Integer, ColumnReading> said = new HashMap<>(readings);
            said.values().removeIf(ColumnReading::isNone);
            readings = Map.copyOf(said);
            leftOut = Set.copyOf(leftOut);
            // Sorted by property, so that the columns they become come in the same order every time.
            Map<URI, String> values = new TreeMap<>();
            given.forEach((property, value) -> {
                if (value != null && !value.isBlank()) {
                    values.put(Uris.expand(property), value.strip());
                }
            });
            given = Collections.unmodifiableMap(values);
            Map<Integer, URI> measured = new HashMap<>();
            measures.forEach((column, variable) -> measured.put(column, Uris.expand(variable)));
            measures = Map.copyOf(measured);
        }

        public SheetDecisions(URI classUri, Integer identifierColumn, Map<Integer, URI> properties,
                              Map<Integer, ColumnReading> readings, Set<Integer> leftOut, Map<URI, String> given) {
            this(classUri, identifierColumn, properties, readings, leftOut, given, Map.of());
        }

        public SheetDecisions(URI classUri, Integer identifierColumn, Map<Integer, URI> properties,
                              Map<Integer, ColumnReading> readings, Set<Integer> leftOut) {
            this(classUri, identifierColumn, properties, readings, leftOut, Map.of());
        }

        public SheetDecisions(URI classUri, Integer identifierColumn, Map<Integer, URI> properties,
                              Map<Integer, ColumnReading> readings) {
            this(classUri, identifierColumn, properties, readings, Set.of());
        }

        public SheetDecisions(URI classUri, Integer identifierColumn, Map<Integer, URI> properties) {
            this(classUri, identifierColumn, properties, Map.of(), Set.of());
        }

        /**
         * The same decisions without the column ones, for a sheet whose columns changed. The values given
         * for every row stay: they name properties, not columns.
         */
        public SheetDecisions withoutColumns() {
            return new SheetDecisions(classUri, null, Map.of(), Map.of(), Set.of(), given, Map.of());
        }

        /** The same decisions with these readings; the decisions about properties do not touch them. */
        public SheetDecisions withReadings(Map<Integer, ColumnReading> newReadings) {
            return new SheetDecisions(classUri, identifierColumn, properties, newReadings, leftOut, given, measures);
        }

        /** How the user said the column is written; {@link ColumnReading#NONE} when they said nothing. */
        public ColumnReading readingOf(int column) {
            return readings.getOrDefault(column, ColumnReading.NONE);
        }
    }

    /**
     * A resource the user described in the assistant because no file does. It becomes a row of a sheet
     * the assistant adds, so that rows of the files may point to it by its name.
     *
     * @param id       stable while the user edits it
     * @param classUri its class
     * @param values   its values by property, several for a property that takes several, in order
     */
    public record DeclaredResource(String id, URI classUri, Map<URI, List<String>> values) {

        /** URIs in their full form; blank values left out; properties in the order the user gave them. */
        public DeclaredResource {
            classUri = classUri == null ? null : Uris.expand(classUri);
            Map<URI, List<String>> kept = new LinkedHashMap<>();
            values.forEach((property, list) -> {
                List<String> filled = new ArrayList<>();
                (list == null ? List.<String>of() : list).stream()
                        .filter(value -> value != null && !value.isBlank())
                        .forEach(value -> filled.add(value.strip()));
                if (!filled.isEmpty()) {
                    kept.merge(Uris.expand(property), List.copyOf(filled), (first, more) -> {
                        List<String> all = new ArrayList<>(first);
                        all.addAll(more);
                        return List.copyOf(all);
                    });
                }
            });
            values = Collections.unmodifiableMap(kept);
        }

        /** Its name, by which rows of the files point to it; null when it has none. */
        public String name() {
            List<String> names = values.get(LABEL);
            return names == null || names.isEmpty() ? null : names.get(0);
        }
    }

    /** The property naming a resource, by which the assistant identifies the rows it adds. */
    public static final URI LABEL = URI.create(RDFS.label.getURI());
}
