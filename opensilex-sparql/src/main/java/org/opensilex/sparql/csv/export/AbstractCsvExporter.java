package org.opensilex.sparql.csv.export;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.csv.AbstractCsvImporter;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public abstract class AbstractCsvExporter<T extends SPARQLResourceModel> implements CsvExporter<T> {

    public static final char SEMI_COLON_SEPARATOR = ';';
    public static final int COLUMN_OFFSET = 2;

    private final SPARQLService sparql;
    private final Map<String, Function<T, String>> customRelationWriterByProperty;

    protected AbstractCsvExporter(SPARQLService sparql) {
        this.sparql = sparql;
        customRelationWriterByProperty = new HashMap<>();
    }

    @Override
    public void customRelationWrite(String columnURI, Function<T, String> objectRelationWriter) {
        if (StringUtils.isEmpty(columnURI)) {
            throw new IllegalArgumentException("Null or empty column URI");
        }
        Objects.requireNonNull(objectRelationWriter);
        customRelationWriterByProperty.put(SPARQLDeserializers.formatURI(columnURI), objectRelationWriter);
    }

    @Override
    public byte[] exportCSV() throws IOException, SPARQLException {
        CsvExportOption<T> exportOptions = getExportOptions();
        CsvExportHeader header = getHeader(exportOptions);

        int expectedAvgRowSize = 256;

        // estimation of pre-allocate memory for output stream (note this method is only an estimation)
        // a too big value will lead to too much RAM allocation
        // a too big small value will lead to several array copy and extends
        Writer writer = new StringWriter(expectedAvgRowSize * exportOptions.getResults().size());

        CSVWriter csvWriter = new CSVWriter(
                writer,
                exportOptions.lang.equals("fr") ? SEMI_COLON_SEPARATOR : ICSVWriter.DEFAULT_SEPARATOR,
                ICSVWriter.DEFAULT_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END
        );

        writeHeader(header, csvWriter);
        writeBody(header, exportOptions, csvWriter);

        // #TODO find a more optimized way : since internal buffer is converted into a String, and String byte array is copied also

        // use ISO_8859_1 charset for better encoding/decoding of special characters like accents etc
        byte[] content = writer.toString().getBytes(StandardCharsets.ISO_8859_1);
        csvWriter.flush();
        csvWriter.close();

        return content;
    }

    private CsvExportHeader getHeader(CsvExportOption<T> options) throws SPARQLException {

        CsvExportHeader exportHeader = new CsvExportHeader();

        Set<String> columns;

        // use provided column
        if (!options.columns.isEmpty()) {
            columns = options.columns
                    .stream()
                    .map(SPARQLDeserializers::formatURI)
                    .collect(Collectors.toSet());

        } else {
            // use ontology do determine columns to use
            // get all properties which can be applied on the given class URI and on all it's descendant/subclasses
            Objects.requireNonNull(options.classURI);
            columns = SPARQLModule.getOntologyStoreInstance().getOwlRestrictionsUris(options.classURI, true);
        }

        exportHeader.setColumns(columns);
        exportHeader.setColumnNames(getHeaderNames(columns, options.lang));

        return exportHeader;
    }


    // #TODO avoid the use of a request by using the OntologyStore which store class and properties name
    private List<String> getHeaderNames(Set<String> header, String lang) throws SPARQLException {

        // execute a SPARQL request in order to fetch al columns name with the good translation
        SelectBuilder select = new SelectBuilder();
        Var uriVar = makeVar("uri");
        Var nameVar = makeVar("name");
        select.addVar(nameVar);
        select.addWhere(uriVar, RDFS.label, nameVar);
        select.addFilter(SPARQLQueryHelper.langFilterWithDefault(nameVar.getVarName(), lang));

        SPARQLQueryHelper.addWhereUriStringValues(select, uriVar.getVarName(), header.stream(), true, header.size());

        return sparql.executeSelectQueryAsStream(select)
                .map(result -> result.getStringValue(nameVar.getVarName()))
                .collect(Collectors.toList());
    }

    protected void writeHeader(CsvExportHeader header, CSVWriter writer) {

        // first line : writer header
        // (uri,type)
        String[] headerLine = new String[header.getColumns().size() + COLUMN_OFFSET];
        headerLine[0] = AbstractCsvImporter.CSV_URI_KEY;
        headerLine[1] = AbstractCsvImporter.CSV_TYPE_KEY;

        // write all column in header
        int i = COLUMN_OFFSET;
        for (String column : header.getColumns()) {
            headerLine[i++] = column;
        }
        writer.writeNext(headerLine);

        // 2nd line : writer header description
        // (uri,type)
        i = COLUMN_OFFSET;
        for (String columnName : header.getHeaderNames()) {
            headerLine[i++] = columnName;
        }
        writer.writeNext(headerLine);
    }

    private void writeBody(CsvExportHeader header, CsvExportOption<T> options, CSVWriter writer) {

        String[] lineBuffer = new String[header.getColumns().size() + COLUMN_OFFSET];

        // use a String builder for cell write, in case of multivalued relation
        StringBuilder cellBuffer = new StringBuilder();

        // loop over model/object, one row per model
        for (T model : options.getResults()) {

            // write URI and type
            lineBuffer[0] = model.getUri().toString();
            lineBuffer[1] = model.getType().toString();

            int colIdx = COLUMN_OFFSET;

            // loop over column
            for (String column : header.getColumns()) {
                // one cell per relation
                writeRelations(colIdx, column, model, lineBuffer, cellBuffer);
                colIdx++;
            }

            writer.writeNext(lineBuffer);
        }
    }


    private void writeRelations(int colIdx, String column, T object, String[] lineBuffer, StringBuilder cellBuffer) {

        Function<T, String> customValueGenerator = customRelationWriterByProperty.get(column);
        if (customValueGenerator != null) {
            String customValue = customValueGenerator.apply(object);
            lineBuffer[colIdx] = customValue;
        } else {
            boolean valueWritten = false;

            // #TODO find a better way to loop between models and relation, here complexity is O(n.m.k) with n object, m relations and k column
            // indeed, for each column, all relations are read

            // loop over relation and build corresponding String
            // in case of multivalued relation, several instance of SPARQLModelRelation have the same value
            for (SPARQLModelRelation relation : object.getRelations()) {
                if (SPARQLDeserializers.compareURIs(relation.getProperty().toString(), column)) {

                    // a value has already been written (in case of multivalued relation), then append separator
                    if (valueWritten) {
                        cellBuffer.append(getExportOptions().getMultiValuedCellSeparator());
                    } else {
                        valueWritten = true;
                    }

                    // juste write relation value
                    cellBuffer.append(relation.getValue());
                }
            }

            // write value to the line
            if (!valueWritten) {
                lineBuffer[colIdx] = null;
            } else {
                lineBuffer[colIdx] = cellBuffer.toString();
            }

            // reset buffer after each cell write, less memory allocation than re-creating a new StringBuilder for each row/column
            // #see https://stackoverflow.com/questions/5192512/how-can-i-clear-or-empty-a-stringbuilder
            cellBuffer.setLength(0);
        }
    }
}