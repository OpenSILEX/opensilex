package org.opensilex.sparql.csv.header;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.sparql.csv.AbstractCsvImporter;
import org.opensilex.sparql.deserializer.URIDeserializer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Contains all information about a CSV header during import.
 * @author rcolin
 */
public class CsvHeader{

    private final List<String> columns;
    private final Map<Integer, URI> uriColumns;

    /**
     * A Map which associates each column index (except uri and type) to the string column title. If the column is a
     * rdf property then we associate to the string SHORT URI.
     * <br>Example : <br>
     * <b>header</b> : (uri, type, rdfs:label, rdfs:comment, :my_custom_property, someString) <br>
     * <b>columnIndexes</b> : { 2-> rdfs:label, 3-> rdfs:comment, 4 -> my_custom_property, 5 -> someString } <br>
     */
    private final Map<String, List<Integer>> columnIndexes;
    private final boolean allowPropertiesRepeat;
    private final boolean allowExtraStringColumnsRepeat;

    // The effective size of the CSV header, take care of the number of column, + (uri,type)
    private int realCsvHeaderLength;

    //When we add a column, we say if it's an extra non-URI column, then we add to this set so that we know to treat it
    //as a URI or nay when we fetch indexes for column title.
    private final Set<String> extraColumns = new HashSet<>();

    public CsvHeader(boolean allowPropertiesRepeat, boolean allowExtraStringColumnsRepeat) {
        this.allowPropertiesRepeat = allowPropertiesRepeat;
        this.allowExtraStringColumnsRepeat = allowExtraStringColumnsRepeat;
        columnIndexes = new PatriciaTrie<>();
        columns = new ArrayList<>();
        uriColumns = new HashMap<>();
        realCsvHeaderLength = AbstractCsvImporter.CSV_PROPERTIES_BEGIN_INDEX;
    }

    /**
     *
     * Register a column with some associated index
     * @param column CSV column
     * @param index index in header of the column*
     * @param isExtraColumn if true then the column is some extra column that isn't necessarily a URI.
     * @throws IllegalArgumentException if
     * <ul>
     *     <li>index < 0</li>
     *     <li>column is null or empty</li>
     *     <li>column is already saved and allowPropertiesRepeat was set to false on construct </li>
     * </ul>
     * @throws URISyntaxException if URI parsing of header fail
     */
    public void addColumn(String column, int index, boolean isExtraColumn) throws IllegalArgumentException, URISyntaxException {
        if(index < 0){
            throw new IllegalArgumentException("Index must be positive");
        }
        if (StringUtils.isEmpty(column)) {
            throw new IllegalArgumentException("Null or empty header");
        }

        //Throw error if a repeat of sparql property column is detected AND we are not allowing this
        if(! allowPropertiesRepeat && columnIndexes.containsKey(column)){
                throw new IllegalArgumentException("Can't handle duplicate CSV sparql property column in allowPropertiesRepeat:false mode");
        }
        //Throw error if a repeat of extra column is detected AND we are not allowing this
        if(isExtraColumn && !allowExtraStringColumnsRepeat && columnIndexes.containsKey(column)){
            throw new IllegalArgumentException("Can't handle duplicate CSV extra String column in allowExtraStringColumnsRepeat:false mode");
        }

        String actualUsedHeader =  column;
        if(!isExtraColumn){
            //If it's a standard column then it means it corresponds to some rdf property
            URI headerURI = new URI(column);
            URI shortHeader = URIDeserializer.formatURI(headerURI);
            actualUsedHeader = shortHeader.toString();
            uriColumns.put(index, shortHeader);
        }else{
            extraColumns.add(column);
        }

        columns.add(actualUsedHeader);
        columnIndexes.computeIfAbsent(actualUsedHeader, newKey -> new ArrayList<>()).add(index);

        realCsvHeaderLength++;
    }

    /**
     *
     * @param realIndexInCsv The actual index of column, so without removing the starting URI and type columns
     * @param indexFromUniqueColumns The index starting from the column after URI and Type
     */
    public ColumnInfoFromIndex getColumn(int realIndexInCsv, int indexFromUniqueColumns) {
        String correspondingColumn =  columns.get(realIndexInCsv);
        if(extraColumns.contains(correspondingColumn)){
            return new ColumnInfoFromIndex(null, correspondingColumn);
        }else{
            return new ColumnInfoFromIndex(uriColumns.get(indexFromUniqueColumns), correspondingColumn);
        }
    }

    public List<Integer> getIndexes(String header){
        String actualHeader = header;
        if(!extraColumns.contains(header)){
            actualHeader = URIDeserializer.formatURIAsStr(header);
        }
        return this.columnIndexes.get(actualHeader);
    }

    /**
     *
     * @param header string header we are getting index for
     * @return the index of header, or null if this header is not present
     * @throws Exception if it was not a unique header (multiple columns with this header)
     */
    public Integer getIndexOfAUniqueHeader(String header) throws Exception{
        List<Integer> indexes = getIndexes(header);
        if(indexes.size() > 1){
            throw new Exception(String.format("%s is not a unique header, multiple indexes found", header));
        }
        if(CollectionUtils.isEmpty(indexes)){
            return null;
        }
        return indexes.get(0);
    }

    public Stream<String> iterateColumns() {
        return columns.stream();
    }

    public int size() {
        return columns.size();
    }

    public List<String> getColumns() {
        return columns;
    }

    public int getRealCsvHeaderLength() {
        return realCsvHeaderLength;
    }

    /**
     *
     * @return true is the CsvHeader registered some non relation-uri type extra String columns. False otherwise
     */
    public boolean doesContainExtraStringColumns(){
        return !CollectionUtils.isEmpty(extraColumns);
    }

    /**
     * A little class to be able to have a single getColumn function regardless of its URI column or an extra String column
     */
    public static class ColumnInfoFromIndex{
        final URI columnAsUri;
        final String columnAsString;

        public ColumnInfoFromIndex(URI columnAsUri, String columnAsString) {
            this.columnAsUri = columnAsUri;
            this.columnAsString = columnAsString;
        }

        public URI getColumnAsUri() {
            return columnAsUri;
        }

        public String getColumnAsString() {
            return columnAsString;
        }
    }
}
