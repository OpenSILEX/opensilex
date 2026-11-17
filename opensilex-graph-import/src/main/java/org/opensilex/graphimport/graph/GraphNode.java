//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A resource of the import: every row that shares its identifier, whatever the sheet, with the
 * classes those rows give it and the values of their cells. Filled by {@link GraphBuilder}, then
 * only read.
 *
 * @author Arnaud Charleroy
 */
public final class GraphNode {

    private final String key;
    private final String name;
    private final URI uri;
    private final boolean identified;
    private final List<CellRef> rows = new ArrayList<>();
    private final Map<URI, List<CellRef>> classes = new LinkedHashMap<>();
    private final Map<URI, List<GraphValue>> values = new TreeMap<>(Comparator.comparing(URI::toString));

    /**
     * @param key  what the rows of this resource share: its URI in full form, its identifier in
     *             {@link InstanceIndex#key} form, or the row's own place for a row without identifier
     * @param name what the user calls it: the identifier as first written, or the place of the row
     * @param uri  its URI, when its identifier is written as one; null otherwise
     * @param identified false for a row without identifier, which nothing can link to
     */
    GraphNode(String key, String name, URI uri, boolean identified) {
        this.key = key;
        this.name = name;
        this.uri = uri;
        this.identified = identified;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    /** Its URI when the file writes its identifier as one; null otherwise. */
    public URI getUri() {
        return uri;
    }

    /** False for a row without identifier: no other row completes it, no cell links to it. */
    public boolean isIdentified() {
        return identified;
    }

    /** One cell per row of the resource: its identifier cell, or the row's first cell without one. */
    public List<CellRef> getRows() {
        return Collections.unmodifiableList(rows);
    }

    /** The classes its rows give it, in the order they were met. */
    public Set<URI> getClasses() {
        return Collections.unmodifiableSet(classes.keySet());
    }

    /** The rows that give it this class. */
    public List<CellRef> rowsOfClass(URI classUri) {
        return Collections.unmodifiableList(classes.getOrDefault(classUri, List.of()));
    }

    /** Its values, by property in URI order. */
    public Map<URI, List<GraphValue>> getValues() {
        return Collections.unmodifiableMap(values);
    }

    void addRow(CellRef row, URI classUri) {
        rows.add(row);
        classes.computeIfAbsent(classUri, key -> new ArrayList<>()).add(row);
    }

    void addValue(URI property, GraphValue value) {
        values.computeIfAbsent(property, key -> new ArrayList<>()).add(value);
    }
}
