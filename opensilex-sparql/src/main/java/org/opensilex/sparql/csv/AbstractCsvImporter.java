package org.opensilex.sparql.csv;

import com.univocity.parsers.csv.CsvParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.csv.error.CSVValidationModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.uri.generation.DefaultURIGenerator;
import org.opensilex.uri.generation.URIGenerator;
import org.opensilex.utils.ClassUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Supplier;

public abstract class AbstractCsvImporter<T extends SPARQLResourceModel> {

    public static final String CSV_URI_KEY = "uri";
    public static final int CSV_URI_INDEX = 0;

    public static final String CSV_TYPE_KEY = "type";
    public static final int CSV_TYPE_INDEX = 1;
    private static final int PROPERTIES_BEGIN_INDEX = 2;

    private final SPARQLService sparql;
    private final OntologyStore ontologyStore;
    private final URIGenerator<T> uriGenerator;

    protected final URI classURI;
    protected final Class<T> objectClass;
    protected final ClassModel rootClassModel;
    protected final URI graph;
    protected final String generationPrefix;
    protected final Supplier<T> objectConstructor;

    private final int batchSize;

    protected AbstractCsvImporter(SPARQLService sparql, Class<T> objectClass, URI graph, Supplier<T> objectConstructor) throws SPARQLException {
        this.sparql = sparql;
        this.objectClass = objectClass;
        this.graph = graph;
        this.uriGenerator = new DefaultURIGenerator<>();
        this.generationPrefix = sparql.getDefaultGenerationURI(objectClass).toString();
        this.objectConstructor = objectConstructor;

        this.ontologyStore = SPARQLModule.getOntologyStoreInstance();
        this.batchSize = 1024;

        try {
            this.classURI = URIDeserializer.formatURI(sparql.getMapperIndex().getForClass(objectClass).getRDFType().getURI());
        } catch (SPARQLMapperNotFoundException | SPARQLInvalidClassDefinitionException e) {
            throw new RuntimeException(e);
        }

        this.rootClassModel = ontologyStore.getClassModel(classURI, null, null);
    }


    private URI[] readHeader(Iterator<String[]> rowIterator, CSVValidationModel csvValidationModel) {

        String[] headers = rowIterator.next();

        if (headers == null || headers.length < PROPERTIES_BEGIN_INDEX) {
            csvValidationModel.addEmptyHeader(0);
            return new URI[0];
        }

        // no additional properties
        if (headers.length == PROPERTIES_BEGIN_INDEX) {
            return new URI[0];
        }

        URI[] columns = new URI[headers.length - PROPERTIES_BEGIN_INDEX];

        for (int i = 0; i < headers.length - PROPERTIES_BEGIN_INDEX; i++) {
            String header = headers[i + PROPERTIES_BEGIN_INDEX];
            try {
                if (StringUtils.isEmpty(header)) {
                    csvValidationModel.addEmptyHeader(i);
                } else {
                    columns[i] = new URI(URIDeserializer.formatURIAsStr(header));
                }
            } catch (URISyntaxException e) {
                csvValidationModel.addInvalidHeaderURI(i, header);
            }
        }

        // skip help line
        rowIterator.next();

        return columns;
    }

    private void readBody(Iterator<String[]> rowIterator, URI[] columns, CsvOwlRestrictionValidator restrictionValidator, boolean validOnly) throws Exception {

        try {
            if (!validOnly) {
                sparql.startTransaction();
            }

            int totalRowIdx = 0;
            Node graphNode = graph != null ? NodeFactory.createURI(graph.toString()) : null;

            while (rowIterator.hasNext()) {

                // read csv file by batch : perform validation and insertion by batch (by using transaction)
                List<T> modelChunk = new ArrayList<>(batchSize);
                int chunkRowIdx = 0;

                while (chunkRowIdx++ < batchSize && rowIterator.hasNext()) {
                    String[] row = rowIterator.next();
                    modelChunk.add(getModel(totalRowIdx, row, columns, restrictionValidator));
                    totalRowIdx++;
                }

                // #TODO check URI uniqueness

                if (restrictionValidator.validateValuesByType() && !validOnly) {
                    sparql.create(graphNode, modelChunk, batchSize, false);
                } else if (!validOnly) {
                    sparql.rollbackTransaction();
                }
            }
            if (!validOnly) {
                sparql.commitTransaction();
                restrictionValidator.getCsvValidationModel().setNbObjectImported(totalRowIdx);
            }

        } catch (Exception e) {
            if (!validOnly) {
                sparql.rollbackTransaction();
            }
            throw e;
        }

    }

    private void readUriAndType(int rowIdx, String[] row, T model, CSVValidationModel csvValidationModel) {

        String uriStr = row[CSV_URI_INDEX];
        try {
            if (!StringUtils.isEmpty(uriStr)) {
                model.setUri(new URI(uriStr));
            } else {
                model.setUri(uriGenerator.generateURI(generationPrefix, model, 0));
            }
        } catch (URISyntaxException e) {
            csvValidationModel.addInvalidURIError(new CSVCell(rowIdx, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
        }

        String typeStr = row[CSV_TYPE_INDEX];
        if (!StringUtils.isEmpty(typeStr)) {
            try {
                URI type = new URI(typeStr);
                if (!ontologyStore.classExist(type, classURI)) {
                    csvValidationModel.addInvalidURIError(new CSVCell(rowIdx, CSV_TYPE_INDEX, "Unknown type :" + typeStr, CSV_TYPE_KEY));
                }
                model.setType(type);
            } catch (URISyntaxException e) {
                csvValidationModel.addInvalidURIError(new CSVCell(rowIdx, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
            }
        } else {
            model.setType(classURI);
        }
    }

    private void readProperties(int rowIdx, String[] row, URI[] columns, T model, CsvOwlRestrictionValidator restrictionValidator) throws SPARQLException {

        // #TODO use local map-based Class model intra-cache
        ClassModel classModel = ontologyStore.getClassModel(model.getType(), classURI, null);

        for (int colIdx = PROPERTIES_BEGIN_INDEX; colIdx < row.length; colIdx++) {
            URI property = columns[colIdx - PROPERTIES_BEGIN_INDEX];
            String value = row[colIdx];
            OwlRestrictionModel restriction = classModel.getRestrictionsByProperties().get(property);
            restrictionValidator.validateCsvValue(rowIdx, colIdx, classModel, model, value, property, restriction);
        }

        //   // row based checking (cardinality/list check)
        restrictionValidator.validateModel(classModel, model, () -> {
            CSVCell cell = new CSVCell();
            cell.setRowIndex(rowIdx);
            return new CsvCellValidationContext(cell);
        });
    }

    private T getModel(int rowIdx, String[] row, URI[] columns, CsvOwlRestrictionValidator restrictionValidator) throws SPARQLException {
        T model = objectConstructor.get();
        readUriAndType(rowIdx, row, model, restrictionValidator.getCsvValidationModel());
        readProperties(rowIdx, row, columns, model, restrictionValidator);
        return model;
    }

    public CSVValidationModel read(InputStream inputStream, boolean validOnly) throws Exception {

        CSVValidationModel csvValidationModel = new CSVValidationModel();
        CsvOwlRestrictionValidator restrictionValidator = new CsvOwlRestrictionValidator(sparql, ontologyStore, csvValidationModel, graph);

        // use try with resource in order to auto close resource in case of IOException
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            CsvParser csvParser = new CsvParser(ClassUtils.getCSVParserDefaultSettings());
            Iterator<String[]> rowIterator = csvParser.iterate(reader).iterator();

            // read header and  compute columns/properties index
            URI[] columns = readHeader(rowIterator, csvValidationModel);

            if (columns.length > 0) {
                readBody(rowIterator, columns, restrictionValidator, validOnly);
            }

        } catch (IOException e) {
            throw e;
        }

        return csvValidationModel;
    }

}
