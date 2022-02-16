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
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.csv.dal.error.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vince
 * @param <T> SPARQLNamedResourceModel Class to handle for CSV import/export
 */
public abstract class AbstractCsvDao<T extends SPARQLNamedResourceModel> implements CsvDao<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCsvDao.class);

    protected final Class<T> objectClass;
    protected final SPARQLService sparql;

    protected AbstractCsvDao(SPARQLService sparql, Class<T> objectClass) {
        this.sparql = sparql;
        this.objectClass = objectClass;
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
    public CSVValidationModel validateCSV(
            URI graph,
            URI parentClass,
            InputStream file,
            int firstRow,
            String lang,
            Map<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators,
            List<String> customColumns) throws IOException {

        Map<String, Map<String, OwlRestrictionModel>> typeRestrictions = new HashMap<>();
        Map<String, ClassModel> typeModels = new HashMap<>();
        Map<Integer, String> headerByIndex = new HashMap<>();

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import Ontology objects - CSV format => \n '" + csvReader.getDetectedFormat() + "'");

            String[] ids = csvReader.parseNext();

            int uriIndex = -1;
            int typeIndex = -1;
            int nameIndex = -1;
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];

                if (id.equalsIgnoreCase(CSV_URI_KEY)) {
                    uriIndex = i;
                } else if (id.equalsIgnoreCase(CSV_TYPE_KEY)) {
                    typeIndex = i;
                } else if (id.equalsIgnoreCase(CSV_NAME_KEY)) {
                    nameIndex = i;
                }
                headerByIndex.put(i, SPARQLDeserializers.getExpandedURI(id));
            }

            if (firstRow > 2) {
                int skipLines = 0;
                while (skipLines < (firstRow - 2)) {
                    csvReader.parseNext();
                    skipLines++;
                }
            }

            int rowIndex = 1;
            String[] values = null;

            CSVValidationModel csvValidation = new CSVValidationModel();

            List<String> missingHeaders = new ArrayList<>();
            if (uriIndex == -1) {
                missingHeaders.add(CSV_URI_KEY);
            }
            if (typeIndex == -1) {
                missingHeaders.add(CSV_TYPE_KEY);
            }
            if (nameIndex == -1) {
                missingHeaders.add(CSV_NAME_KEY);
            }

            if (missingHeaders.size() > 0) {
                csvValidation.addMissingHeaders(missingHeaders);
            } else {

                Map<URI, Map<URI, Boolean>> checkedClassObjectURIs = new HashMap<>();
                Map<URI, Integer> checkedURIs = new HashMap<>();

                while ((values = csvReader.parseNext()) != null) {
                    try {
                        URI rdfType = new URI(SPARQLDeserializers.getExpandedURI(values[typeIndex].trim()));
                        if (!typeRestrictions.containsKey(rdfType.toString())) {
                            ClassModel model = sparql.getOntologyDao().getClassModel(rdfType, parentClass, lang);

                            Map<String, OwlRestrictionModel> restrictionsByID = new HashMap<>();
                            model.getRestrictions().values().forEach(restriction -> {
                                String propertyURI = SPARQLDeserializers.getExpandedURI(restriction.getOnProperty());
                                restrictionsByID.put(propertyURI, restriction);
                            });

                            typeRestrictions.put(SPARQLDeserializers.getExpandedURI(rdfType.toString()), restrictionsByID);
                            typeModels.put(rdfType.toString(), model);
                        }

                        Map<String, OwlRestrictionModel> restrictionsByID = typeRestrictions.get(rdfType.toString());

                        validateCSVRow(graph, typeModels.get(rdfType.toString()), values, rowIndex, csvValidation, uriIndex, typeIndex, nameIndex, restrictionsByID, headerByIndex, checkedClassObjectURIs, checkedURIs, customValidators);

                    } catch (Exception ex) {
                        CSVCell cell = new CSVCell(rowIndex, 0, "Unhandled error while parsing row: " + ex.getMessage(), "all");
                        csvValidation.addInvalidValueError(cell);
                    }

                    rowIndex++;
                }

            }

            return csvValidation;
        }
    }

    private void validateCSVRow(
            URI graph,
            ClassModel model,
            String[] values,
            int rowIndex,
            CSVValidationModel csvValidation,
            int uriIndex,
            int typeIndex,
            int nameIndex,
            Map<String, OwlRestrictionModel> restrictionsByID,
            Map<Integer, String> headerByIndex,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            Map<URI, Integer> checkedURIs,
            Map<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators
    ) throws Exception {

        SPARQLNamedResourceModel<T> object = objectClass.getConstructor().newInstance();

        String name = null;

        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (colIndex == typeIndex) {
                continue;
            } else if (colIndex == nameIndex) {
                name = values[colIndex];
                if (StringUtils.isEmpty(name)) {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, name, CSV_NAME_KEY);
                    csvValidation.addMissingRequiredValue(cell);
                } else {
                    object.setName(name);
                }
            } else if (colIndex == uriIndex) {
                String value = values[colIndex];
                CSVCell cell = new CSVCell(rowIndex, colIndex, value, CSV_URI_KEY);
                if (!StringUtils.isEmpty(value)) {
                    if (URIDeserializer.validateURI(value)) {
                        URI objectURI = new URI(value);
                        if (checkedURIs.containsKey(objectURI)) {
                            csvValidation.addDuplicateURIError(cell, checkedURIs.get(objectURI));
                        } else if (!sparql.uriExists(SPARQLDeserializers.nodeURI(graph), objectURI)) {
                            object.setUri(objectURI);
                        } else {
                            csvValidation.addAlreadyExistingURIError(cell);
                        }
                        checkedURIs.put(objectURI, rowIndex);
                    } else {
                        csvValidation.addInvalidURIError(cell);
                    }
                }
            } else if (headerByIndex.containsKey(colIndex)) {
                String header = headerByIndex.get(colIndex);
                String value = values[colIndex];
                if (restrictionsByID.containsKey(header)) {
                    OwlRestrictionModel restriction = restrictionsByID.get(header);
                    URI propertyURI = restriction.getOnProperty();
                    BiConsumer<CSVCell, CSVValidationModel> customValidator = null;
                    if (customValidators != null) {
                        customValidator = customValidators.get(SPARQLDeserializers.getExpandedURI(propertyURI));
                    }

                    CSVCell cell = new CSVCell(rowIndex, colIndex, value, header);
                    validateCSVSingleValue(graph, model, propertyURI, cell, restriction, csvValidation, checkedClassObjectURIs, customValidator, object);

                } else {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, value, header);
                    if (customValidators != null) {
                        BiConsumer<CSVCell, CSVValidationModel> customValidator = customValidators.get(header);
                        if (customValidator != null) {
                            customValidator.accept(cell, csvValidation);
                        }
                    }
                }
            }
        }

        if (!csvValidation.hasErrors()) {
            if (object.getUri() == null) {
                String generationUriPrefix = sparql.getDefaultGenerationURI(objectClass).toString();
                int retry = 0;
                URI objectURI = object.generateURI(generationUriPrefix, (T) object, retry);

                // check if URI has not been locally used or if it not already exist into sparql repository
                while (checkedURIs.containsKey(objectURI) || sparql.uriExists(SPARQLDeserializers.nodeURI(graph), objectURI)) {
                    retry++;
                    objectURI = object.generateURI(generationUriPrefix, (T) object, retry);
                }
                checkedURIs.put(objectURI, rowIndex);
                object.setUri(objectURI);
            }

            object.setType(model.getUri());
            csvValidation.addObject(name, object);
        }
    }

    private void validateCSVSingleValue(
            URI graph,
            ClassModel model,
            URI propertyURI,
            CSVCell cell,
            OwlRestrictionModel restriction,
            CSVValidationModel csvValidation,
            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
            BiConsumer<CSVCell, CSVValidationModel> customValidator,
            SPARQLResourceModel object
    ) throws SPARQLException {
        String value = cell.getValue();

        if (restriction.isRequired() && (StringUtils.isEmpty(value))) {
            csvValidation.addMissingRequiredValue(cell);
        } else if (model.isDatatypePropertyRestriction(propertyURI)) {
            try {
                if (!StringUtils.isEmpty(value)) {
                    SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getSubjectURI());
                    if (!deserializer.validate(value)) {
                        csvValidation.addInvalidDatatypeError(cell, restriction.getSubjectURI());
                    } else if (customValidator != null) {
                        customValidator.accept(cell, csvValidation);
                    }

                    if (!csvValidation.hasErrors()) {
                        object.addRelation(graph, propertyURI, deserializer.getClassType(), value);
                    }
                }
            } catch (SPARQLDeserializerNotFoundException ex) {
                csvValidation.addInvalidDatatypeError(cell, restriction.getSubjectURI());
            }
        } else if (model.isObjectPropertyRestriction(propertyURI)) {
            try {
                if (!StringUtils.isEmpty(value)) {
                    if (URIDeserializer.validateURI(value)) {
                        URI objectURI = new URI(value);

                        URI classURI = restriction.getSubjectURI();
                        boolean doesClassObjectUriExist;
                        if (checkedClassObjectURIs.containsKey(classURI) && checkedClassObjectURIs.get(classURI).containsKey(objectURI)) {
                            doesClassObjectUriExist = checkedClassObjectURIs.get(classURI).get(objectURI);
                        } else {
                            doesClassObjectUriExist = sparql.uriExists(classURI, objectURI);
                            if (!checkedClassObjectURIs.containsKey(classURI)) {
                                checkedClassObjectURIs.put(classURI, new HashMap<>());
                            }
                            checkedClassObjectURIs.get(classURI).put(objectURI, doesClassObjectUriExist);
                        }

                        if (!doesClassObjectUriExist) {
                            csvValidation.addURINotFoundError(cell, classURI, objectURI);
                        } else if (customValidator != null) {
                            customValidator.accept(cell, csvValidation);
                        }

                        if (!csvValidation.hasErrors()) {
                            object.addRelation(graph, propertyURI, URI.class, value);
                        }
                    } else {
                        if (customValidator != null) {
                            customValidator.accept(cell, csvValidation);
                        } else {
                            csvValidation.addInvalidURIError(cell);
                        }
                    }
                }
            } catch (URISyntaxException ex) {
                csvValidation.addInvalidURIError(cell);
            }
        }
    }


    @Override
    public String exportCSV(
            List<T> objects,
            URI parentClass,
            String lang,
            BiFunction<String, T, String> customValueGenerator,
            List<String> customColumns,
            Set<String> strictlyAllowedColumns,
            Comparator<String> columnSorter) throws SPARQLException, IOException  {

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

    private Map<String, String> getColumnNames(Collection<String> columnUris, String lang) throws SPARQLException {

        Map<String, String> columnsNames = new HashMap<>();

        // filter columns which are not parsable as a URI
        List<URI> columnWithAbsoluteUris = new ArrayList<>();
        columnWithAbsoluteUris.add(URI.create(RDFS.label.toString()));

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
        propertyNamesRequest.addFilter(SPARQLQueryHelper.langFilterWithDefault(nameVar.getVarName(), lang));
        SPARQLQueryHelper.inURI(propertyNamesRequest, uriVar.getVarName(), columnWithAbsoluteUris);

        sparql.executeSelectQuery(propertyNamesRequest, (result) -> {
            String uri = SPARQLDeserializers.formatURI(result.getStringValue(uriVar.getVarName()));
            columnsNames.put(uri, result.getStringValue(nameVar.getVarName()));
        });

        return columnsNames;
    }


}
