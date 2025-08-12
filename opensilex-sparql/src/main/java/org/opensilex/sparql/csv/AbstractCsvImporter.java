package org.opensilex.sparql.csv;

import com.univocity.parsers.csv.CsvParser;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.csv.header.CsvHeader;
import org.opensilex.sparql.csv.validation.CsvCellValidationContext;
import org.opensilex.sparql.csv.validation.CustomCsvValidation;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.*;
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
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author rcolin
 * @param <T>
 */
public abstract class AbstractCsvImporter<T extends SPARQLResourceModel & ClassURIGenerator> implements CsvImporter<T> {

    public static final String CSV_URI_KEY = "uri";
    public static final int CSV_URI_INDEX = 0;

    public static final String CSV_TYPE_KEY = "type";
    public static final int CSV_TYPE_INDEX = 1;

    public static final int CSV_NAME_INDEX = 2;

    public static final int CSV_PROPERTIES_BEGIN_INDEX = 2;

    // Start at 2 since we skip header and header description line (2)
    public static final int CSV_ROW_BEGIN_INDEX = 2;

    // Offset for a human-readable error reporting (array index start at 0 for computer-scientist but not for all human)
    // Here users expect that first column index is 1
    public static final int CSV_HEADER_HUMAN_READABLE_COLUMN_OFFSET = 1;

    // Add one LINE since users expect that the first effective row is the third line
    public static final int CSV_HEADER_HUMAN_READABLE_ROW_OFFSET = CSV_ROW_BEGIN_INDEX+1;

    protected URI publisher;

    protected final SPARQLService sparql;
    protected final OntologyStore ontologyStore;

    protected final URI rootClassURI;
    protected final Class<T> objectClass;
    protected final ClassModel rootClassModel;

    /**
     * URI of the SPARQL graph is which models are managed. This graph
     * can be used for validation and creation
     */
    protected final URI graph;
    protected final Node graphNode;
    protected final String generationPrefix;

    /**
     * Determine how to create a new instance of {@code T}
     */
    protected final Supplier<T> objectConstructor;

    /**
     * Index between RDF property and corresponding custom CSV validation
     * @see #addCustomValidation(CustomCsvValidation)
     */
    private final Map<String, CustomCsvValidation<T>> customValidationByProperty;

    /**
     * Size of chunk during CSV file reading.
     * The importer read a chunk and apply creation and validation on this chunk.
     */
    protected final int batchSize;

    /**
     * Maximum number of error until stop file reading
     */
    protected final int errorNbLimit;

    /**
     *
     * @param sparql SPARQL service
     * @param objectClass object class
     * @param graph object graph
     * @param objectConstructor way to define new model which are instance of the given {@code objectClass}
     */
    protected AbstractCsvImporter(SPARQLService sparql, Class<T> objectClass, URI graph, Supplier<T> objectConstructor, URI publisher) throws SPARQLException {

        Objects.requireNonNull(sparql);
        Objects.requireNonNull(objectClass);
        Objects.requireNonNull(graph);
        Objects.requireNonNull(objectConstructor);
        Objects.requireNonNull(publisher);

        SPARQLConfig sparqlConfig;
        try{
             sparqlConfig = sparql.getOpenSilex().getModuleConfig(SPARQLModule.class,SPARQLConfig.class);
        }catch (OpenSilexModuleNotFoundException e){
            throw new RuntimeException(e);
        }

        if(sparqlConfig.csvBatchSize() <= 0 ){
            throw new IllegalArgumentException("csvBatchSize config value is <= 0");
        }
        this.batchSize = sparqlConfig.csvBatchSize();

        if(sparqlConfig.csvMaxErrorNb() <= 0 ){
            throw new IllegalArgumentException("csvMaxErrorNb config value is <= 0");
        }
        this.errorNbLimit = sparqlConfig.csvMaxErrorNb();

        this.sparql = sparql;
        this.objectClass = objectClass;
        this.graph = graph;
        this.graphNode = NodeFactory.createURI(graph.toString());
        this.generationPrefix = sparql.getDefaultGenerationURI(objectClass).toString();
        this.objectConstructor = objectConstructor;
        this.ontologyStore = SPARQLModule.getOntologyStoreInstance();
        this.publisher = publisher;

        try {
            this.rootClassURI = URIDeserializer.formatURI(sparql.getRDFTypeURI(objectClass));
        } catch (SPARQLMapperNotFoundException | SPARQLInvalidClassDefinitionException e) {
            throw new RuntimeException(e);
        }

        this.rootClassModel = ontologyStore.getClassModel(rootClassURI, null, null);
        customValidationByProperty = new PatriciaTrie<>();
    }


    /**
     * Read CSV file header and update validation in case of invalid or unknown column/property
     *
     * @param rowIterator an Iterator on each CSV row, this iterator is read 2 times (for header and header description)
     * @param csvValidationModel CSV validation
     *
     * @return the extracted {@link CsvHeader}
     */
    protected CsvHeader readHeader(Iterator<String[]> rowIterator, CSVValidationModel csvValidationModel) {

        String[] headers = rowIterator.next();

        if (headers == null || headers.length < CSV_PROPERTIES_BEGIN_INDEX) {
            csvValidationModel.addEmptyHeader(0);
            return null;
        }

        // no additional properties
        if (headers.length == CSV_PROPERTIES_BEGIN_INDEX) {
            return null;
        }

        CsvHeader csvHeader = new CsvHeader(true);

        for (int i = 0; i < headers.length - CSV_PROPERTIES_BEGIN_INDEX; i++) {
            String header = headers[i + CSV_PROPERTIES_BEGIN_INDEX];
            try {
                if (StringUtils.isEmpty(header)) {
                    // add +1 because index start at 0, but error reporting is more intuitive with index which start at 1 (all users are not computer scientist)
                    csvValidationModel.addEmptyHeader(i+ CSV_PROPERTIES_BEGIN_INDEX +1);
                } else {
                    csvHeader.addColumn(header,i + CSV_PROPERTIES_BEGIN_INDEX);
                }
            } catch (URISyntaxException e) {
                csvValidationModel.addInvalidHeaderURI(i, header);
            }
        }

        // skip help line
        rowIterator.next();

        return csvHeader;
    }

    /**
     * Check if the row size is equals to the {@link CsvHeader} size, append the error to the validator else
     * @param row CSV row to check
     * @param totalRowIdx current index in CSV reading
     * @param validator CSV validator (used for error registration)
     * @param header CSV header
     * @return true if row size is OK, false else
     *
     * @see CsvHeader#size()
     * @see CsvOwlRestrictionValidator#addInvalidRowSizeError(CsvCellValidationContext)
     */
    private boolean checkRowSize(String[] row, int totalRowIdx, CsvOwlRestrictionValidator validator, CsvHeader header){

        int maxRowSize = header.size() + CSV_PROPERTIES_BEGIN_INDEX;
        int diff = row.length - maxRowSize;

        if(diff == 0){
            return true;
        }
        else if(diff < 0){
            // row too small -> return an error linked to the first cell which is expected to be defined
            validator.addInvalidRowSizeError(new CsvCellValidationContext(totalRowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, row.length,null, null));
        }else{
            // row too large -> return an error linked to the first illegal/unexpected cell
            validator.addInvalidRowSizeError(new CsvCellValidationContext(totalRowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, row.length, row[maxRowSize], null));
        }
        return false;
    }

    /**
     *
     * @param rowIterator an Iterator on each CSV row
     * @param csvHeader CSV Header
     * @param validator OWL validation which can deal with custom csv validation and errors registering
     * @param validOnly performs validation if {@code true}, performs validation+insertion else
     * @param modelsConsumer a {@link BiConsumer} used to consume each chunk after validation/insertion (optional)
     */
    private void readBody(Iterator<String[]> rowIterator, CsvHeader csvHeader, CsvOwlRestrictionValidator validator, boolean validOnly, BiConsumer<CSVValidationModel, Stream<T>> modelsConsumer) throws Exception {

        boolean allOk = true;
        int totalRowIdx = 0;

        // contains each encountered type which is a subClass of rootClassModel, during whole CSV file reading
        Map<String,ClassModel> localClassesCache = new PatriciaTrie<>();

        while (rowIterator.hasNext() && allOk) {

            // read csv file by batch :
            // perform validation and insertion by batch (by using transaction)
            List<T> modelChunkToCreate = new ArrayList<>(batchSize);
            // perform validation and modification by batch (by using transaction)
            List<T> modelChunkToUpdate = new ArrayList<>(batchSize);

            Map<String, Integer> filledUrisToIndexesInChunk = new PatriciaTrie<>();
            Map<String, Integer> generatedUrisToIndexesInChunk = new PatriciaTrie<>();
            Map<String, Integer> filledUrisToUpdateIndexesInChunk = new PatriciaTrie<>();
            int chunkRowIdx = 0;

            // continue while batch size or max error limit is not reached
            while ((chunkRowIdx++ < batchSize) && (validator.getNbError() < errorNbLimit) && (rowIterator.hasNext())) {
                String[] row = rowIterator.next();

                // Check that row size is coherent with header size
                if (checkRowSize(row, totalRowIdx, validator, csvHeader)) {

                    // read model and performs local validation
                    T model = getModel(totalRowIdx, row, csvHeader, validator,localClassesCache);
                    // handle URI generation or map filled URIs to the model object
                    // add model in modelChunk List
                    handleURIMapping(validator, model, totalRowIdx, modelChunkToCreate, modelChunkToUpdate, generatedUrisToIndexesInChunk, filledUrisToIndexesInChunk, filledUrisToUpdateIndexesInChunk);
                }
                totalRowIdx++;
            }

            checkUrisUniqueness(validator, filledUrisToIndexesInChunk, generatedUrisToIndexesInChunk, modelChunkToCreate);

            // batch validation and custom consumer use
            if(validator.isValid()){
                batchValidation(validator, modelChunkToCreate,totalRowIdx-chunkRowIdx);
                batchValidation(validator, modelChunkToUpdate,totalRowIdx-chunkRowIdx);

                if(modelsConsumer != null){
                    modelsConsumer.accept(validator.getValidationModel(), modelChunkToCreate.stream());
                }
                // map modelChunkToUpdate list in validator
                mapObjectsToUpdate(validator, modelChunkToUpdate);
            }

            // write chunk
            allOk = validator.isValid();
            if (allOk && !validOnly) {
                upsert(validator.getValidationModel(), modelChunkToCreate, modelChunkToUpdate);
            }
        }
        if (allOk) {
            validator.getValidationModel().setNbObjectImported(totalRowIdx);
        }
    }

    protected <T extends SPARQLResourceModel & ClassURIGenerator> void mapObjectsToUpdate(CsvOwlRestrictionValidator validator, List<T> modelChunkToUpdate) {
        // no need of mapping objects to update here
    }

    /**
     * Adds the model objects in the List modelChunkToCreate
     * Handles the URI generation and mapping them into the model object
     *
     * @param modelChunkToCreate the list of models to be created
     * @param modelChunkToUpdate the list of models to be updated
     *
     * <b>override this method</b> to add the models to be updated in the List modelChunkToUpdate and
     * <b>override this method</b> {@link #upsert(CSVValidationModel, List, List)} also for the models in modelChunkToUpdate to be updated
     *
     */
    protected void handleURIMapping(CsvOwlRestrictionValidator validator, T model, int totalRowIdx, List<T> modelChunkToCreate, List<T> modelChunkToUpdate, Map<String, Integer> generatedUrisToIndexesInChunk, Map<String, Integer> filledUrisToIndexesInChunk, Map<String, Integer> filledUrisToUpdateIndexesInChunk) throws SPARQLException {
        addModelInModelChunk(model, modelChunkToCreate);
        // generate new URI and register it to set of URI to check
        if (model.getUri() == null) {
            generateLocallyUniqueUri(model, totalRowIdx, validator.getValidationModel(), generatedUrisToIndexesInChunk);
        } else {
            // register URI to the set of URI to check
            filledUrisToIndexesInChunk.put(model.getUri().toString(), totalRowIdx);
        }
    }

    protected static <T extends SPARQLResourceModel & ClassURIGenerator> void addModelInModelChunk(T model, List<T> modelChunk) {
        modelChunk.add(model);
    }

    protected void checkUrisUniqueness(CsvOwlRestrictionValidator validator, Map<String, Integer> filledUrisToIndexesInChunk, Map<String, Integer> generatedUrisToIndexesInChunk, List<T> modelChunk) throws SPARQLException {
        // check generated and filled URI uniqueness in batch way
        if(validator.isValid()){
            checkUrisUniqueness(filledUrisToIndexesInChunk, validator);
            checkGeneratedUrisUniqueness(generatedUrisToIndexesInChunk, modelChunk, validator);
        }
    }

    /**
     * Apply a custom validation on a chunk. This method is useful is you need to perform some validation
     * not row-by-row, but for row chunk. <br>
     * Example : performs a validation query on a database on n objects, instead of evaluating validation query of each row
     *
     * @param restrictionValidator OWL validator
     * @param models chunk of models
     *
     * @throws IOException if some error occurs during custom validation
     */
    protected void customBatchValidation(CsvOwlRestrictionValidator restrictionValidator, List<T> models, int offset) throws IOException {
        // no custom batch validation
    }

    /**
     * Performs batch validation by applying {@link CsvOwlRestrictionValidator#batchValidation()} and {@link #customBatchValidation(CsvOwlRestrictionValidator, List,int)}
     * @param restrictionValidator OWL validator
     * @param models chunk of models
     * @param offset current index in CSV file
     * @throws IOException if some error occurs during custom validation
     */
    private void batchValidation(CsvOwlRestrictionValidator restrictionValidator, List<T> models, int offset) throws IOException {
        restrictionValidator.batchValidation();
        customBatchValidation(restrictionValidator,models,offset);
    }

    /**
     * 
     * @param urisToCheck a bounded Stream on the URIs to check
     * @param streamSize stream size
     * @return a SPARQL query which indicate for each input URI, if the URI exist or not (boolean representation)
     * 
     * @see SPARQLService#getCheckUriListExistQuery(Stream, int, String, Node)
     *
     * @apiNote the SPARQL query must use the SPARQL variable {@link SPARQLService#EXISTING_VAR}
     */
    protected SelectBuilder getCheckGeneratedUrisUniquenessQuery(Stream<String> urisToCheck, int streamSize){
        return sparql.getCheckUriListExistQuery(urisToCheck, streamSize, rootClassURI.toString(), graphNode);
    }

    /**
     *
     * Generate a unique URI for each {@link SPARQLResourceModel} from {@code models}
     * @param generatedUrisToIndexes index between URI (string representation) and corresponding row index in CSV file
     * @param models models for which we need to generate a unique URI
     * @param validator CSV validation
     * @throws SPARQLException if SPARQL query evaluation fails
     * @throws IllegalArgumentException if the SPARQL query returned by {@link #getCheckGeneratedUrisUniquenessQuery(Stream, int)}
     * doesn't contains the {@link SPARQLService#EXISTING_VAR} variable
     *
     * @apiNote This method evaluate a SPARQL query while there are remaining duplicate URI.
     * By duplicate, we mean, a URI which is already present into the triple-store. <br>
     *
     * This method rely on {@link #getCheckGeneratedUrisUniquenessQuery(Stream, int)} in order to know what is the SPARQL
     * query to evaluate at each generation round. <br>
     *
     * if error occurs during URI parsing, then {@link CSVValidationModel#addInvalidURIError(CSVCell)} is called for error registration
     *
     */
    protected void checkGeneratedUrisUniqueness(final Map<String, Integer> generatedUrisToIndexes, List<T> models, CsvOwlRestrictionValidator validator) throws SPARQLException, IllegalArgumentException {

        if (generatedUrisToIndexes.isEmpty()) {
            return;
        }

        Set<String> urisToCheck = new HashSet<>(generatedUrisToIndexes.keySet());

        // store the number of duplicate for a row
        Map<Integer, Integer> duplicateCountByRowIdx = new HashMap<>();

        while (!urisToCheck.isEmpty()) {

            // query used to check existence of n URIs (return false/true for each given URI)
            SelectBuilder checkUrisQuery = getCheckGeneratedUrisUniquenessQuery(urisToCheck.stream(), urisToCheck.size());

            // ensure var name is recognized, since we expect to use it just after
            boolean useExistingVar = checkUrisQuery.getVars().stream().anyMatch(sparqlVar -> sparqlVar.getVarName().equals(SPARQLService.EXISTING_VAR));
            if (!useExistingVar) {
                throw new IllegalArgumentException("The SPARQL query generated from getCheckGeneratedUrisUniquenessQuery() must contains the "+SPARQLService.EXISTING_VAR+" variable name");
            }

            // iterator on uri <-> row, used to loop over results by mapping result without input URI set
            // Indeed query only return true/false in order to minimize I/O
            Iterator<String> urisIterator = urisToCheck.iterator();

            Set<String> duplicates = new HashSet<>();

            // evaluate query and iterate over results : true or false
            sparql.executeSelectQueryAsStream(checkUrisQuery).forEach((SPARQLResult result) -> {

                boolean uriExists = Boolean.parseBoolean(result.getStringValue(SPARQLService.EXISTING_VAR));
                if (uriExists) {

                    // retrieve corresponding uri and  corresponding model
                    String duplicate = urisIterator.next();
                    int rowIdx = generatedUrisToIndexes.get(duplicate);
                    T duplicateModel = models.get(rowIdx);

                    // regenerate a new URI
                    try {
                        int retryCount = duplicateCountByRowIdx.computeIfAbsent(rowIdx, key -> 1);
                        URI generated;
                        do {
                            generated = duplicateModel.generateURI(generationPrefix, duplicateModel, retryCount++);
                        } while (generatedUrisToIndexes.containsKey(generated.toString()));

                        //  update duplicate count and current model URI
                        duplicateCountByRowIdx.put(rowIdx, retryCount);
                        duplicateModel.setUri(generated);

                        // add URI to the set of uri to re-check at next turn
                        duplicates.add(generated.toString());

                        // update URI <-> row index map
                        generatedUrisToIndexes.remove(duplicate);
                        generatedUrisToIndexes.put(generated.toString(), rowIdx);

                    } catch (URISyntaxException e) {
                        validator.addInvalidURIError(new CsvCellValidationContext(rowIdx+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
                    }
                } else {
                    urisIterator.next();
                }
            });
            urisToCheck = duplicates;
        }
    }

    /**
     * @param urisToCheck a bounded Stream on the URIs to check
     * @param streamSize stream size
     * @return a SPARQL query which indicate for each input URI, if the URI exist or not (boolean representation)
     */
    protected SelectBuilder getCheckUrisUniquenessQuery(Stream<String> urisToCheck, int streamSize) {
        return sparql.getCheckUriListExistQuery(urisToCheck, streamSize, rootClassURI.toString(), graphNode);
    }

    /**
     * Check that each URI from {models} is unique
     * @param filledUrisToIndexesInChunk index between URI (string representation) and corresponding row index in CSV file
     * @param validator CSV validator
     * @throws SPARQLException if SPARQL query evaluation fails
     * @throws IllegalArgumentException if the SPARQL query returned by {@link #getCheckGeneratedUrisUniquenessQuery(Stream, int)}
     * doesn't contains the {@link SPARQLService#EXISTING_VAR} variable
     *
     * @apiNote
     * This method rely on {@link #getCheckUrisUniquenessQuery(Stream, int)} in order to know what is the SPARQL
     * query to evaluate. <br>
     * 
     * If duplicate URI are found, then {@link CSVValidationModel#addAlreadyExistingURIError(CSVCell)} is called for error registration
     */
    private void checkUrisUniqueness(Map<String, Integer> filledUrisToIndexesInChunk, CsvOwlRestrictionValidator validator) throws SPARQLException, IllegalArgumentException {

        Set<String> urisToCheck = filledUrisToIndexesInChunk.keySet();
        if (!urisToCheck.isEmpty()) {

            // iterator on uri <-> row
            Iterator<Map.Entry<String, Integer>> entryIterator = filledUrisToIndexesInChunk.entrySet().iterator();

            // query used to check existence of n URIs (return false/true)
            SelectBuilder checkUrisQuery = getCheckUrisUniquenessQuery(urisToCheck.stream(), urisToCheck.size());

            // ensure var name is recognized, since we expect to use it just after
            boolean useExistingVar = checkUrisQuery.getVars().stream().anyMatch(sparqlVar -> sparqlVar.getVarName().equals(SPARQLService.EXISTING_VAR));
            if (!useExistingVar) {
                throw new IllegalArgumentException("The SPARQL query generated from getCheckUrisUniquenessQuery() must contains the "+SPARQLService.EXISTING_VAR+" variable name");
            }

            Iterator<SPARQLResult> resultIt = sparql.executeSelectQueryAsStream(checkUrisQuery).iterator();
            while (resultIt.hasNext()){
                SPARQLResult result = resultIt.next();

                boolean uriExists = Boolean.parseBoolean(result.getStringValue(SPARQLService.EXISTING_VAR));
                if (uriExists) {
                    Map.Entry<String, Integer> entry = entryIterator.next();
                    validator.addAlreadyExistingURIError(new CsvCellValidationContext(entry.getValue()+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, CSV_URI_INDEX, entry.getKey(), CSV_URI_KEY));

                    if(validator.getNbError() >= errorNbLimit){
                        break;
                    }
                } else {
                    entryIterator.next();
                }
            }
        }

    }

    /**
     * Generate a URI for the given model, by ensuring that the URI is locally unique; that the URI
     * has not been used for model generation before.
     * @param model CSV row model
     * @param rowIdx CSV row index
     * @param validation CSV validation
     * @param generatedUrisToIndexes index between URI (string representation) and corresponding row index in CSV file
     */
    protected void generateLocallyUniqueUri(T model, int rowIdx, CSVValidationModel validation, Map<String, Integer> generatedUrisToIndexes) {

        // generate URI after relations building
        try {

            // regenerate URI while there are local conflict
            int retryCount = 0;
            URI generated;
            do {
                generated = model.generateURI(generationPrefix, model, retryCount++);
            } while (generatedUrisToIndexes.containsKey(generated.toString()));

            model.setUri(generated);
            generatedUrisToIndexes.put(generated.toString(), rowIdx);

        } catch (URISyntaxException e) {
            validation.addInvalidURIError(new CSVCell(rowIdx+ CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, CSV_URI_INDEX+ CSV_HEADER_HUMAN_READABLE_COLUMN_OFFSET, e.getMessage(), CSV_URI_KEY));
        }
    }


    /**
     * Read the uri and the type of the given row and update model
     * @param rowIdx CSV row index
     * @param row row content
     * @param model current row model
     * @param validator CSV validator
     * @param localClassesCache a {@link Map} on each encountered type during CSV file reading. This map is used as a local cache in order to minimize call to {@link OntologyStore#getClassModel(URI, URI, String)}.
     *
     * @return the {@link ClassModel} corresponding with the type declared in row. Return the rootClassModel if no type was set into row
     * @throws SPARQLException if some error occurs during {@link ClassModel} information retrieval
     */
    private ClassModel readUriAndType(int rowIdx, String[] row, T model, CsvOwlRestrictionValidator validator, Map<String,ClassModel> localClassesCache) throws SPARQLException {

        String uriStr = row[CSV_URI_INDEX];
        try {
            if (!StringUtils.isEmpty(uriStr)) {
                model.setUri(new URI(uriStr));
            }
        } catch (URISyntaxException e) {
            validator.addInvalidValueError(new CsvCellValidationContext(rowIdx+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
        }

        String typeStr = row[CSV_TYPE_INDEX];

        // no type specified -> use default root type
        if(StringUtils.isEmpty(typeStr)){
            model.setType(rootClassURI);
            return rootClassModel;
        }

        try {
            String shortTypeStr = URIDeserializer.getShortURI(typeStr);  // ensure prefixed type URI using
            URI type = new URI(shortTypeStr);

            // ensure that the type exists and is a subClassOf of rootClassURI
            ClassModel classModel = localClassesCache.get(shortTypeStr);
            if(classModel == null){
                classModel = ontologyStore.getClassModel(type, rootClassURI, null);
                localClassesCache.put(typeStr,classModel);
            }

            model.setType(type);
            return classModel;
        } catch (URISyntaxException e) {
            validator.addInvalidValueError(new CsvCellValidationContext(rowIdx+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, CSV_URI_INDEX, e.getMessage(), CSV_URI_KEY));
            return null;
        } catch (SPARQLInvalidURIException | SPARQLInvalidUriListException e) { // unknown or incorrect type
            validator.addInvalidValueError(new CsvCellValidationContext(rowIdx+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, CSV_TYPE_INDEX, "Unknown type : " + typeStr, CSV_TYPE_KEY));
            return null;
        }
    }


    /**
     * Read each relation from {@code row} by applying validation on relation value
     * @param rowIdx CSV row index
     * @param row CSV row content
     * @param csvHeader CSV header
     * @param model current model
     * @param classModel {@link ClassModel} witch match with current model
     * @param restrictionValidator OWL validator
     */
    protected void readRelations(int rowIdx, String[] row, CsvHeader csvHeader, T model, ClassModel classModel, CsvOwlRestrictionValidator restrictionValidator) {

        for (int colIdx = CSV_PROPERTIES_BEGIN_INDEX; colIdx < row.length; colIdx++) {

            URI property = csvHeader.getUriColumn(colIdx- CSV_PROPERTIES_BEGIN_INDEX);
            CustomCsvValidation<T> customValidator = customValidationByProperty.get(property.toString());

            // no custom validation or validation which just overload default validation
            if (customValidator == null || customValidator.applyDefaultValidation()) {
                OwlRestrictionModel restriction = classModel.getRestrictionsByProperties().get(property);
                restrictionValidator.validateCsvValue(rowIdx, colIdx, classModel, model, row[colIdx], property, restriction);
            }

            // apply custom validation
            if (customValidator != null) {
                final int finalColIdx = colIdx;

                // pass value, validator and how to generate a validation context (a CSV cell here)
                customValidator.getValidationAction().accept(model, row[colIdx], restrictionValidator, () -> new CsvCellValidationContext(
                        rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                        finalColIdx + CSV_HEADER_HUMAN_READABLE_COLUMN_OFFSET,
                        row[finalColIdx],
                        property.toString())
                );
            }
        }

        // row based checking (cardinality/list check)
        restrictionValidator.validateModel(classModel, model, () -> {
            CsvCellValidationContext cell = new CsvCellValidationContext();
            cell.setRowIndex(rowIdx+ CSV_HEADER_HUMAN_READABLE_ROW_OFFSET);
            return cell;
        });
    }

    /**
     *
     * @param rowIdx CSV row index
     * @param row CSV row
     * @param csvHeader CSV header
     * @param validator OWL validator
     * @param localClassesCache local class cache
     * @return the {@link SPARQLResourceModel} parsed according row content
     * @throws SPARQLException if some error in encountered during row URI and type reading
     */
    private T getModel(int rowIdx, String[] row, CsvHeader csvHeader, CsvOwlRestrictionValidator validator, Map<String,ClassModel> localClassesCache) throws SPARQLException {

        T model = objectConstructor.get();
        ClassModel classModel = readUriAndType(rowIdx, row, model, validator,localClassesCache);
        if (classModel != null) {
            readRelations(rowIdx, row, csvHeader, model, classModel, validator);
        }
        return model;
    }

    @Override
    public final CSVValidationModel importCSV(File file, boolean validOnly, BiConsumer<CSVValidationModel, Stream<T>> modelsConsumer) throws Exception {

        Objects.requireNonNull(file);

        CsvOwlRestrictionValidator restrictionValidator = new CsvOwlRestrictionValidator(sparql, ontologyStore, graph, errorNbLimit);
        CSVValidationModel csvValidationModel = restrictionValidator.getValidationModel();

        // use try with resource in order to auto close resource in case of IOException
        try(BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))){

            CsvParser csvParser = new CsvParser(ClassUtils.getCSVParserDefaultSettings());
            Iterator<String[]> rowIterator = csvParser.iterate(inputStream).iterator();

            // read header and  compute columns/properties index
            CsvHeader csvHeader = readHeader(rowIterator, csvValidationModel);

            // no header -> error
            if (csvHeader == null) {
                csvValidationModel.addEmptyHeader(0);
            } else if(!csvValidationModel.hasErrors()){ // only read body if header is correct
                csvValidationModel.setCsvHeader(csvHeader);
                readBody(rowIterator, csvHeader, restrictionValidator, validOnly, modelsConsumer);
            }
        }

        return csvValidationModel;
    }

    /**
     * <pre>
     * Add a custom validation on some specific rdf property.
     * If this property is found into CSV file header, then the associated custom validation
     * is applied on each row, on the cell which match with the property.
     *
     * Example :
     *  <b>custom validation</b> : rdfs:label ->  {your validation}
     *  <b>header</b> : (uri,type,rdfs:label)
     *  <b>body</b> :
     *       :uri1, :type, "name1",
     *       :uri1, :type, "name2",
     *
     * The custom validation will be applied on "name1" and on "name2" cells.
     * </pre>
     *
     * @param customValidator the Custom validation to apply (required)
     *
     * @apiNote This method rely on {@link CustomCsvValidation#getProperty()} in order to determine on which property apply the validation
     */
    protected final void addCustomValidation(CustomCsvValidation<T> customValidator) {
        Objects.requireNonNull(customValidator);

        String property = URIDeserializer.formatURIAsStr(customValidator.getProperty());
        customValidationByProperty.put(property, customValidator);
    }

    /**
     * Calls the create method to create the models 'modelsToCreate' to be created
     *
     * @param modelsToCreate the list of models to be created
     * @param modelChunkToUpdate the list of models to be updated
     *
     * <b>override this method</b> by adding the update logic to handle both creation and modification for the models in modelChunkToUpdate to be updated
     *
     */
    @Override
    public void upsert(CSVValidationModel validation, List<T> modelsToCreate, List<T> modelChunkToUpdate) throws Exception {
        if (!validation.hasErrors()) {
            if (Objects.nonNull(this.publisher)) {
                for (T model : modelsToCreate) {
                    if (Objects.isNull(model.getPublisher())) {
                        model.setPublisher(this.publisher);
                    }
                }
            }
            sparql.create(NodeFactory.createURI(graph.toString()), modelsToCreate, modelsToCreate.size(), false, true);
        }
    }
}
