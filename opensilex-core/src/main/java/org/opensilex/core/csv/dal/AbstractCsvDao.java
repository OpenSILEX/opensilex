/*******************************************************************************
 *                         AbstractCsvDao.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.core.csv.dal;


import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.csv.dal.error.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public abstract class AbstractCsvDao<T extends SPARQLNamedResourceModel> implements CsvDao<T> {

    protected final SPARQLService sparql;

    protected AbstractCsvDao(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public static final char SEMI_COLON_SEPARATOR = ';';
    public static final String CSV_URI_KEY = "uri";
    public static final String CSV_TYPE_KEY = "type";
    public static final String CSV_NAME_KEY = "name";

    public static final int CSV_URI_INDEX = 0;
    public static final int CSV_TYPE_INDEX = 1;

    public static final int CSV_NAME_INDEX = 2;

    public static final int CSV_HEADER_COL_OFFSET = 3;
    public static final int CSV_HEADER_ROWS_NB = 2;

    @Override
    public CSVValidationModel validateCSV(URI graph, URI parentClass, InputStream file, int firstRow, String lang, Map<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators, List<String> customColumns) {
        return null;
    }

    private Map<String, String> getColumnNames(Collection<String> columnUris, String lang) throws SPARQLException, URISyntaxException {

        Map<String, String> columnsNames = new HashMap<>();

        // filter columns which are not parsable as a URI
        List<URI> columnWithAbsoluteUris = new ArrayList<>();
        columnWithAbsoluteUris.add(new URI(RDFS.label.toString()));

        columnUris.forEach(columnId -> {
            try {
                URI columnURI = new URI(columnId);
                if (columnURI.isAbsolute()) {
                    columnWithAbsoluteUris.add(columnURI);
                }
            } catch (Exception ex) {
                // Ignore invalid column IDs
            }
        });

        SelectBuilder propertyNamesRequest = new SelectBuilder();
        Var uriVar = makeVar("uri");
        Var nameVar = makeVar("name");
        propertyNamesRequest.addVar(uriVar);
        propertyNamesRequest.addVar(nameVar);
        propertyNamesRequest.addWhere(uriVar, RDFS.label, nameVar);
        propertyNamesRequest.addFilter(SPARQLQueryHelper.langFilter(nameVar.getVarName(), lang));
        SPARQLQueryHelper.inURI(propertyNamesRequest, uriVar.getVarName(), columnWithAbsoluteUris);

        sparql.executeSelectQuery(propertyNamesRequest, (result) -> {
            String uri = SPARQLDeserializers.formatURI(result.getStringValue(uriVar.getVarName()));
            columnsNames.put(uri, result.getStringValue(nameVar.getVarName()));
        });

        return columnsNames;
    }

    @Override
    public String exportCSV(List<T> objects, URI parentClass, String lang, BiFunction<String, T, String> customValueGenerator, List<String> customColumns, Set<String> strictlyAllowedColumns, Comparator<String> columnSorter) throws Exception {
        boolean sortColumns = columnSorter != null;
        boolean restrictionOnColumn = strictlyAllowedColumns != null;

        Map<String, Integer> columnsIndexes = new HashMap<>();
        columnsIndexes.put(CSV_URI_KEY, CSV_URI_INDEX);
        columnsIndexes.put(CSV_TYPE_KEY, CSV_TYPE_INDEX);
        columnsIndexes.put(CSV_NAME_KEY, CSV_NAME_INDEX);

        customColumns.stream()
                .filter(column -> !restrictionOnColumn || strictlyAllowedColumns.contains(column))
                .map(SPARQLDeserializers::formatURI)
                .forEach(columnId -> columnsIndexes.put(columnId, columnsIndexes.size()));

        List<Map<Integer, String>> rows = new ArrayList<>();

        int initialOffset = CSV_HEADER_COL_OFFSET;

        // read each object, and update row map and column index
        for (T object : objects) {
            Map<Integer, String> row = new HashMap<>();

            // fill URI, type and name columns
            String typeURI = URIDeserializer.formatURI(object.getType()).toString();
            row.put(0, URIDeserializer.formatURI(object.getUri()).toString());
            row.put(1, typeURI);
            row.put(2, object.getName());

            // fill row with custom columns values
            for (int i = 0; i < customColumns.size(); i++) {
                String customColumn = customColumns.get(i);
                if (!restrictionOnColumn || strictlyAllowedColumns.contains(customColumn)) {

                    String value = customValueGenerator.apply(customColumn, object);
                    if (!StringUtils.isEmpty(value)) {
                        row.put(initialOffset + i, value);
                    }
                }
            }

            // fill column with objets relations
            for (SPARQLModelRelation relation : object.getRelations()) {
                Property property = relation.getProperty();
                String propertyURIString = property.getURI();

                if (restrictionOnColumn && !strictlyAllowedColumns.contains(propertyURIString)) {
                    continue;
                }

                Integer propertyIndex = columnsIndexes.get(propertyURIString);

                // new property
                if (propertyIndex == null) {
                    propertyIndex = columnsIndexes.size();
                    columnsIndexes.put(SPARQLDeserializers.formatURI(propertyURIString), propertyIndex);
                }

                String strValue = relation.getValue();
                if (customValueGenerator != null) {
                    strValue = customValueGenerator.apply(propertyURIString, object);
                    if (strValue == null) {
                        strValue = relation.getValue();
                    }
                }
                if (!StringUtils.isEmpty(strValue)) {

                    String existingValue = row.get(propertyIndex);
                    if (existingValue == null) {
                        row.put(propertyIndex, strValue);
                    } else {
                        // multiple value for a property => put all in same column
                        row.put(propertyIndex, existingValue + " " + strValue);
                    }
                }
            }

            rows.add(row);
        }

        LinkedHashSet<String> sortedColumns = new LinkedHashSet<>();
        sortedColumns.add(CSV_URI_KEY);
        sortedColumns.add(CSV_TYPE_KEY);
        sortedColumns.add(CSV_NAME_KEY);

        // append each column into
        Stream<String> columnStream = columnsIndexes.keySet()
                .stream()
                .filter(column -> !sortedColumns.contains(column));

        if (sortColumns) {
            columnStream = columnStream.sorted();
        }
        columnStream.forEach(sortedColumns::add);

        int[] sortedPropertyIndexes = new int[columnsIndexes.size()];

        // build index between old and new property index
        if (sortColumns) {
            int newIdx = 0;
            for (String sortedColumn : sortedColumns) {
                int oldIdx = columnsIndexes.get(sortedColumn);
                sortedPropertyIndexes[oldIdx] = newIdx++;
            }
        }


        Map<String, String> columnsNames = getColumnNames(sortedColumns, lang);

        StringWriter strWriter = new StringWriter();
        CSVWriter writer = new CSVWriter(
                strWriter,
                lang.equals("fr") ? SEMI_COLON_SEPARATOR : ICSVWriter.DEFAULT_SEPARATOR,
                ICSVWriter.DEFAULT_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END
        );

        // write property id+name headers
        String[] headerArray = new String[initialOffset + columnsIndexes.size()];
        String[] headerNameArray = new String[initialOffset + columnsIndexes.size()];
        headerArray[0] = CSV_URI_KEY;
        headerNameArray[0] = "URI";
        headerArray[1] = CSV_TYPE_KEY;
        headerNameArray[1] = "Type";
        headerArray[2] = CSV_NAME_KEY;
        headerNameArray[2] = columnsNames.get(SPARQLDeserializers.formatURI(RDFS.label.getURI()));

        int i = 0;
        for (String columnId : sortedColumns) {

            // don't write uri,type,name twice
            if (i >= initialOffset) {
                String formattedColumnId = SPARQLDeserializers.formatURI(columnId);
                headerArray[i] = formattedColumnId;

                String colName = columnsNames.get(columnId);
                if (colName != null) {
                    headerNameArray[i] = colName.substring(0, 1).toUpperCase() + colName.substring(1);
                } else {
                    headerNameArray[i] = formattedColumnId;
                }
            }
            i++;
        }
        writer.writeNext(headerArray);
        writer.writeNext(headerNameArray);

        // write rows
        for (Map<Integer, String> row : rows) {
            String[] rowArray = new String[columnsIndexes.size() + initialOffset];
            rowArray[0] = row.get(0);
            rowArray[1] = row.get(1);
            rowArray[2] = row.get(2);

            row.forEach((colIdx, colValue) -> {
                if (sortColumns) {
                    int sortedIdx = sortedPropertyIndexes[colIdx];
                    rowArray[sortedIdx] = colValue;
                } else {
                    rowArray[colIdx] = colValue;
                }
            });

            writer.writeNext(rowArray);
        }
        writer.close();
        strWriter.close();

        return strWriter.toString();
    }

}
