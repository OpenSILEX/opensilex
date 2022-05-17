package org.opensilex.sparql.csv;

import com.univocity.parsers.csv.CsvParser;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.uri.generation.ClassURIGenerator;
import org.opensilex.utils.ClassUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Supplier;

public abstract class AbstractCsvImporter<T extends SPARQLResourceModel & ClassURIGenerator> implements CsvImporter {

    public static final String CSV_URI_KEY = "uri";
    public static final int CSV_URI_INDEX = 0;

    public static final String CSV_TYPE_KEY = "type";
    public static final int CSV_TYPE_INDEX = 1;
    private static final int PROPERTIES_BEGIN_INDEX = 2;

    private final SPARQLService sparql;
    private final OntologyStore ontologyStore;

    protected final URI classURI;
    protected final Class<T> objectClass;
    protected final ClassModel rootClassModel;
    protected final URI graph;
    protected final String generationPrefix;
    protected final Supplier<T> objectConstructor;

    private final int batchSize;
    private final int errorNbLimit;

    protected AbstractCsvImporter(SPARQLService sparql, Class<T> objectClass, URI graph, Supplier<T> objectConstructor) throws SPARQLException {
        this.sparql = sparql;
        this.objectClass = objectClass;
        this.graph = graph;
        this.generationPrefix = sparql.getDefaultGenerationURI(objectClass).toString();
        this.objectConstructor = objectConstructor;

        this.ontologyStore = SPARQLModule.getOntologyStoreInstance();
        this.batchSize = 4096;
        this.errorNbLimit = 16;

        try {
            this.classURI = URIDeserializer.formatURI(sparql.getMapperIndex().getForClass(objectClass).getRDFType().getURI());
        } catch (SPARQLMapperNotFoundException | SPARQLInvalidClassDefinitionException e) {
            throw new RuntimeException(e);
        }

        this.rootClassModel = ontologyStore.getClassModel(classURI, null, null);
    }


    @Override
    public Map<Integer, URI> readHeader(Iterator<String[]> rowIterator, CSVValidationModel csvValidationModel) {

        String[] headers = rowIterator.next();
        Set<String> customHeaders = getCustomColumns();

        if (headers == null || headers.length < PROPERTIES_BEGIN_INDEX) {
            csvValidationModel.addEmptyHeader(0);
            return Collections.emptyMap();
        }

        // no additional properties
        if (headers.length == PROPERTIES_BEGIN_INDEX) {
            return Collections.emptyMap();
        }

        Map<Integer,URI> columnByIndex = new HashMap<>();

        for (int i = 0; i < headers.length - PROPERTIES_BEGIN_INDEX; i++) {
            String header = headers[i + PROPERTIES_BEGIN_INDEX];
            try {
                if (StringUtils.isEmpty(header)) {
                    csvValidationModel.addEmptyHeader(i);
                }else{
                    if(! customHeaders.contains(header)){
                        columnByIndex.put(i+PROPERTIES_BEGIN_INDEX,new URI(URIDeserializer.getShortURI(header)));
                    }
                }
            } catch (URISyntaxException e) {
                csvValidationModel.addInvalidHeaderURI(i, header);
            }
        }

        // skip help line
        rowIterator.next();

        return columnByIndex;
    }

    @Override
    public void readBody(Iterator<String[]> rowIterator, Map<Integer, URI> columnByIndex, CsvOwlRestrictionValidator restrictionValidator, boolean validOnly) throws Exception {

        try {
            if (!validOnly) {
                sparql.startTransaction();
            }

            boolean allOk = true;
            int totalRowIdx = 0;
            Node graphNode = graph != null ? NodeFactory.createURI(graph.toString()) : null;

            while (rowIterator.hasNext() && allOk) {

                // read csv file by batch : perform validation and insertion by batch (by using transaction)
                List<T> modelChunk = new ArrayList<>(batchSize);
                Map<String, Integer> filledUrisToIndexesInChunk = new PatriciaTrie<>();
                Map<String, Integer> generatedUrisToIndexesInChunk = new PatriciaTrie<>();

                int chunkRowIdx = 0;

                // continue while batch size or max error limit is not reached
                while ((chunkRowIdx++ < batchSize) && (restrictionValidator.getNbError() < errorNbLimit) && (rowIterator.hasNext())) {
                    String[] row = rowIterator.next();
                    T model = getModel(totalRowIdx, row, columnByIndex, restrictionValidator);
                    modelChunk.add(model);

                    generateURI(model, totalRowIdx, restrictionValidator.getValidationModel(), filledUrisToIndexesInChunk, generatedUrisToIndexesInChunk);
                    totalRowIdx++;
                }

                checkUrisUniqueness(filledUrisToIndexesInChunk, restrictionValidator.getValidationModel());
                checkGeneratedUrisUniqueness(generatedUrisToIndexesInChunk,modelChunk,restrictionValidator.getValidationModel());

                restrictionValidator.validateValuesByType();
                allOk = restrictionValidator.isValid();

                if (allOk && !validOnly) {
                    sparql.create(graphNode, modelChunk, batchSize, false);
                } else if (! allOk) {
                    sparql.rollbackTransaction();
                }
            }
            if (allOk && !validOnly) {
                sparql.commitTransaction();
                restrictionValidator.getValidationModel().setNbObjectImported(totalRowIdx);
            }

        } catch (Exception e) {
            if (!validOnly) {
                sparql.rollbackTransaction();
            }
            throw e;
        }

    }

    private void checkGeneratedUrisUniqueness(final Map<String, Integer> generatedUrisToIndexes, List<T> models, CSVValidationModel validationModel) throws SPARQLException {

        if(generatedUrisToIndexes.isEmpty()){
            return;
        }

        Set<String> urisToCheck = new HashSet<>(generatedUrisToIndexes.keySet());

        // store the number of duplicate for a row
        Map<Integer,Integer> duplicateCountByRowIdx = new HashMap<>();

        while(! urisToCheck.isEmpty()){

            // query used to check existence of n URIs (return false/true for each given URI)
            SelectBuilder checkUrisQuery = sparql.getCheckUriListExistQuery(classURI.toString(), urisToCheck.stream(), urisToCheck.size());

            // iterator on uri <-> row, used to loop over results by mapping result without input URI set
            // Indeed query only return true/false in order to minimize I/O)
            Iterator<String> urisIterator = urisToCheck.iterator();

            Set<String> duplicates = new HashSet<>();

            sparql.executeSelectQueryAsStream(checkUrisQuery).forEach((SPARQLResult result) -> {

                boolean uriExists = Boolean.parseBoolean(result.getStringValue(SPARQLService.EXISTING_VAR));
                if (uriExists) {

                    // retrieve corresponding uri and  corresponding model
                    String duplicate = urisIterator.next();
                    int rowIdx = generatedUrisToIndexes.get(duplicate);
                    T duplicateModel = models.get(rowIdx);

                    // regenerate a new URI
                    try{
                        int retryCount = duplicateCountByRowIdx.computeIfAbsent(rowIdx, key -> 1);
                        URI generated;
                        do{
                            generated = duplicateModel.generateURI(generationPrefix,duplicateModel,retryCount++);
                        }while(generatedUrisToIndexes.containsKey(generated.toString()));

                        // add URI to the set of uri to re-check at next turn and update duplicate count
                        duplicateCountByRowIdx.put(rowIdx,retryCount);
                        duplicateModel.setUri(generated);

                        duplicates.add(generated.toString());

                        generatedUrisToIndexes.remove(duplicate);
                        generatedUrisToIndexes.put(generated.toString(),rowIdx);

                    }catch (URISyntaxException e){
                        validationModel.addInvalidURIError(new CSVCell(rowIdx, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
                    }
                } else {
                    urisIterator.next();
                }
            });
            urisToCheck = duplicates;
        }
    }

    private void checkUrisUniqueness(Map<String, Integer> filledUrisToIndexesInChunk, CSVValidationModel validationModel) throws SPARQLException {

        Set<String> urisToCheck = filledUrisToIndexesInChunk.keySet();
        if (!urisToCheck.isEmpty()) {

            // iterator on uri <-> row
            Iterator<Map.Entry<String, Integer>> entryIterator = filledUrisToIndexesInChunk.entrySet().iterator();

            // query used to check existence of n URIs (return false/true)
            SelectBuilder checkUrisQuery = sparql.getCheckUriListExistQuery(classURI.toString(), urisToCheck.stream(), urisToCheck.size());
            sparql.executeSelectQueryAsStream(checkUrisQuery).forEach((SPARQLResult result) -> {

                boolean uriExists = Boolean.parseBoolean(result.getStringValue(SPARQLService.EXISTING_VAR));
                if (uriExists) {
                    Map.Entry<String, Integer> entry = entryIterator.next();
                    validationModel.addAlreadyExistingURIError(new CSVCell(entry.getValue(), CSV_URI_INDEX, entry.getKey(), CSV_URI_KEY));
                } else {
                    entryIterator.next();
                }
            });
        }

    }

    private void generateURI(T model, int rowIdx, CSVValidationModel validation, Map<String, Integer> filledUrisToIndexesInChunk, Map<String, Integer> generatedUrisToIndexes) {

        // generate URI after relations building
        if (model.getUri() == null) {
            try {

                // regenerate URI while there are local conflict
                int retryCount = 1;
                URI generated;
                do{
                    generated = model.generateURI(generationPrefix,model,retryCount++);
                }while(generatedUrisToIndexes.containsKey(generated.toString()));

                model.setUri(generated);
                generatedUrisToIndexes.put(generated.toString(), rowIdx);

            } catch (URISyntaxException e) {
                validation.addInvalidURIError(new CSVCell(rowIdx, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
            }
        } else {
            filledUrisToIndexesInChunk.put(model.getUri().toString(), rowIdx);
        }
    }


    private ClassModel readUriAndType(int rowIdx, String[] row, T model, CSVValidationModel csvValidationModel) throws SPARQLException {

        String uriStr = row[CSV_URI_INDEX];
        try {
            if (!StringUtils.isEmpty(uriStr)) {
                model.setUri(new URI(uriStr));
            }
        } catch (URISyntaxException e) {
            csvValidationModel.addInvalidURIError(new CSVCell(rowIdx, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
        }

        ClassModel classModel;
        String typeStr = row[CSV_TYPE_INDEX];
        if (!StringUtils.isEmpty(typeStr)) {
            try {
                URI type = new URI(typeStr);
                classModel = ontologyStore.getClassModel(type,classURI,null);
                model.setType(type);
                return classModel;
            } catch (URISyntaxException e) {
                csvValidationModel.addInvalidURIError(new CSVCell(rowIdx, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
                return null;
            }catch (SPARQLInvalidURIException e){
                csvValidationModel.addInvalidURIError(new CSVCell(rowIdx, CSV_TYPE_INDEX, "Unknown type : " + typeStr, CSV_TYPE_KEY));
                return null;
            }
        } else {
            model.setType(classURI);
            return rootClassModel;
        }
    }

    private void readRelations(int rowIdx, String[] row, Map<Integer, URI> columnByIndex, T model, ClassModel classModel, CsvOwlRestrictionValidator restrictionValidator) {

        for (int colIdx = PROPERTIES_BEGIN_INDEX; colIdx < row.length; colIdx++) {

            String value = row[colIdx];

            // non custom header
            if(columnByIndex.containsKey(colIdx)){
                URI property = columnByIndex.get(colIdx);

                OwlRestrictionModel restriction = classModel.getRestrictionsByProperties().get(property);
                restrictionValidator.validateCsvValue(rowIdx, colIdx, classModel, model, value, property, restriction);
            }else{
                // custom validation

            }
        }

        // row based checking (cardinality/list check)
        restrictionValidator.validateModel(classModel, model, () -> {
            CSVCell cell = new CSVCell();
            cell.setRowIndex(rowIdx);
            return new CsvCellValidationContext(cell);
        });
    }

    private T getModel(int rowIdx, String[] row, Map<Integer, URI> columnByIndex, CsvOwlRestrictionValidator restrictionValidator) throws SPARQLException {

        CSVValidationModel validation = restrictionValidator.getValidationModel();

        T model = objectConstructor.get();
        ClassModel classModel = readUriAndType(rowIdx, row, model, validation);
        if(classModel != null){
            readRelations(rowIdx, row, columnByIndex, model, classModel,restrictionValidator);
        }

        return model;
    }

    public CSVValidationModel read(InputStream inputStream, boolean validOnly) throws Exception {

        CSVValidationModel csvValidationModel = new CSVValidationModel();
        CsvOwlRestrictionValidator restrictionValidator = new CsvOwlRestrictionValidator(sparql, ontologyStore, csvValidationModel, graph,errorNbLimit);

        // use try with resource in order to auto close resource in case of IOException
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            CsvParser csvParser = new CsvParser(ClassUtils.getCSVParserDefaultSettings());
            Iterator<String[]> rowIterator = csvParser.iterate(reader).iterator();

            // read header and  compute columns/properties index
            Map<Integer,URI> columnByIndex = readHeader(rowIterator, csvValidationModel);

            if (columnByIndex.isEmpty()) {
                csvValidationModel.addEmptyHeader(0);

            } else {
                readBody(rowIterator, columnByIndex, restrictionValidator, validOnly);
            }
        }

        return csvValidationModel;
    }

}
