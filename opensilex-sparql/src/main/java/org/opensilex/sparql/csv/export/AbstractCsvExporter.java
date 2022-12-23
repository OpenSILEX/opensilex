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
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;

import java.io.*;
import java.net.URI;
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
        byte[] content =  writer.toString().getBytes();

        csvWriter.flush();
        csvWriter.close();

        return content;
    }

    private CsvExportHeader getHeader(CsvExportOption<T> options) throws SPARQLException {

        CsvExportHeader exportHeader = new CsvExportHeader();

        LinkedHashSet<String> columns = new LinkedHashSet<>();

        // use columns provided
        if (!options.columns.isEmpty()) {
            options.columns
                    .stream().map(SPARQLDeserializers::formatURI)
                    .forEach(columns::add);

        } else {
            // use ontology do determine columns to use
            ClassModel classModel = SPARQLModule.getOntologyStoreInstance().getClassModel(options.classURI, null, options.lang);
            classModel.getRestrictionsByProperties()
                    .keySet()
                    .stream().map(URI::toString)
                    .forEach(columns::add);
        }

        exportHeader.setColumns(columns);
        exportHeader.setColumnNames(getHeaderNames(columns, options.lang));

        return exportHeader;
    }


    private List<String> getHeaderNames(LinkedHashSet<String> header, String lang) throws SPARQLException {

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
        StringBuilder valueWriteBuffer = new StringBuilder();

        for (T object : options.getResults()) {
            writeUriAndType(object, lineBuffer);
            writeRelations(object, header, lineBuffer,valueWriteBuffer);
            writer.writeNext(lineBuffer);
        }
    }

    protected void writeUriAndType(T object, String[] lineBuffer) {
        lineBuffer[0] = object.getUri().toString();
        lineBuffer[1] = object.getType().toString();
    }

    private void writeRelations(T object, CsvExportHeader header, String[] lineBuffer, StringBuilder valueWriteBuffer) {

        int i = COLUMN_OFFSET;

        for (String column : header.getColumns()) {

            Function<T, String> customValueGenerator = customRelationWriterByProperty.get(column);
            if (customValueGenerator != null) {
                String customValue = customValueGenerator.apply(object);
                lineBuffer[i++] = customValue;
            } else {
                boolean valueWritten = false;

                // loop over relation and build corresponding String
                // in case of multivalued relation, several instance of SPARQLModelRelation have the same value
                // here, these values are separated by a character
                for (SPARQLModelRelation relation : object.getRelations()) {
                    if (SPARQLDeserializers.compareURIs(relation.getProperty().toString(), column)) {
                        valueWriteBuffer.append(relation.getValue()).append(" ");
                        valueWritten = true;
                        break;
                    }
                }

                // write value to the line
                if (!valueWritten) {
                    lineBuffer[i++] = null;
                }else{
                    // delete last space character
                    valueWriteBuffer.setLength(valueWriteBuffer.length()-1);
                    lineBuffer[i++] = valueWriteBuffer.toString();
                }

                // reset buffer, less memory allocation than re-creating a new StringBuilder for each row/column
                // #see https://stackoverflow.com/questions/5192512/how-can-i-clear-or-empty-a-stringbuilder
                valueWriteBuffer.setLength(0);
            }
        }
    }

}
