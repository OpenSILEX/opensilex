package org.opensilex.sparql.csv.header;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.sparql.deserializer.URIDeserializer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

public class CsvHeader{

    private final List<String> columns;
    private final List<URI> uriColumns;

    /**
     * a Map which associated to each column index (except uri and type), the URI of the column property
     * <br>Example : <br>
     * <b>header</b> : (uri, type, rdfs:label, rdfs:comment, :my_custom_property) <br>
     * <b>columnIndexes</b> : { 2-> rdfs:label, 3-> rdfs:comment, 4 -> my_custom_property } <br>
     */
    private final Map<String, List<Integer>> columnIndexes;
    private final boolean allowPropertiesRepeat;

    public CsvHeader(boolean allowPropertiesRepeat) {
        this.allowPropertiesRepeat = allowPropertiesRepeat;
        columnIndexes = new PatriciaTrie<>();
        columns = new ArrayList<>();
        uriColumns = new ArrayList<>();
    }

    /**
     *
     * Register a column with some associated index
     * @param column CSV column
     * @param index index in header of the column
     * @throws IllegalArgumentException if
     * <ul>
     *     <li>index < 0</li>
     *     <li>column is null or empty</li>
     *     <li>column is already saved and allowPropertiesRepeat was set to false on construct </li>
     * </ul>
     * @throws URISyntaxException if URI parsing of header fail
     */
    public void addColumn(String column, int index) throws IllegalArgumentException, URISyntaxException {
        if(index < 0){
            throw new IllegalArgumentException("Index must be positive");
        }
        if (StringUtils.isEmpty(column)) {
            throw new IllegalArgumentException("Null or empty header");
        }

        if(! allowPropertiesRepeat && columnIndexes.containsKey(column)){
            throw new IllegalArgumentException("Can't handle duplicate CSV column in allowPropertiesRepeat:false mode");
        }

        URI headerURI = new URI(column);
        URI shortHeader = URIDeserializer.formatURI(headerURI);

        columns.add(shortHeader.toString());
        columnIndexes.computeIfAbsent(shortHeader.toString(), newKey -> new ArrayList<>()).add(index);
        uriColumns.add(shortHeader);
    }


    public String getColumn(int index) {
        return columns.get(index);
    }


    public URI getUriColumn(int index) {
        return uriColumns.get(index);
    }

    public List<Integer> getIndexes(String header){
        String shortHeader = URIDeserializer.formatURIAsStr(header);
        return this.columnIndexes.get(shortHeader);
    }

    public Stream<String> iterateColumns() {
        return columns.stream();
    }

    public int size() {
        return columns.size();
    }
}
