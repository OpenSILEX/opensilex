//******************************************************************************
//                          DataDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import com.opencsv.CSVWriter;
import org.bson.Document;
import org.opensilex.core.data.api.DataExportDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *
 * @author sammy
 */
public class DataDAO {

    public static final String DATA_COLLECTION_NAME = "data";
    public static final String DATA_PREFIX = "data";

    public static final String FILE_COLLECTION_NAME = "file";
    public static final String FILE_PREFIX = "file";

    public static final String FS_FILE_PREFIX = "datafile";

    protected final MongoDBService nosql;
    protected final SPARQLService sparql;
    protected final FileStorageService fs;

    private static final Logger LOGGER = LoggerFactory.getLogger(DataDAO.class);
        
    public DataDAO(MongoDBService nosql, SPARQLService sparql, FileStorageService fs) throws URISyntaxException {
        this.nosql = nosql;
        this.sparql = sparql;
        this.fs = fs;
    }

    public void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection dataCollection = nosql.getDatabase()
                .getCollection(DATA_COLLECTION_NAME, DataModel.class);
        dataCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("variable", "provenance", "target", "date"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("variable", "target", "date"));
        dataCollection.createIndex(Indexes.compoundIndex(Arrays.asList(Indexes.ascending("variable"),Indexes.descending("date"))));
        dataCollection.createIndex(Indexes.ascending("date"));
        dataCollection.createIndex(Indexes.descending("date"));

        MongoCollection fileCollection = nosql.getDatabase()
                .getCollection(FILE_COLLECTION_NAME, DataModel.class);
        fileCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
        fileCollection.createIndex(Indexes.ascending("path"), unicityOptions);
        fileCollection.createIndex(Indexes.ascending("provenance", "target", "date"), unicityOptions);
        fileCollection.createIndex(Indexes.ascending("target", "date"));
        fileCollection.createIndex(Indexes.descending("date"));
        fileCollection.createIndex(Indexes.ascending("date"));
    }

    public DataModel create(DataModel instance) throws Exception {
        createIndexes();
        nosql.create(instance, DataModel.class, DATA_COLLECTION_NAME, DATA_PREFIX);
        return instance;
    }

    public DataFileModel createFile(DataFileModel instance) throws Exception {
        createIndexes();
        nosql.create(instance, DataFileModel.class, FILE_COLLECTION_NAME, FILE_PREFIX);
        return instance;
    }

    public List<DataModel> createAll(List<DataModel> instances) throws Exception {
        createIndexes(); 
        nosql.createAll(instances, DataModel.class, DATA_COLLECTION_NAME, DATA_PREFIX);
        return instances;
    } 

    public List<DataFileModel> createAllFiles(List<DataFileModel> instances) throws Exception {
        createIndexes();
        nosql.createAll(instances, DataFileModel.class, FILE_COLLECTION_NAME, FILE_PREFIX);
        return instances;
    }

    public DataModel update(DataModel instance) throws NoSQLInvalidURIException {
        nosql.update(instance, DataModel.class, DATA_COLLECTION_NAME);
        return instance;
    }

    public DataFileModel updateFile(DataFileModel instance) throws NoSQLInvalidURIException {
        nosql.update(instance, DataFileModel.class, FILE_COLLECTION_NAME);
        return instance;
    }

    public ListWithPagination<DataModel> search(
            UserModel user,
            List<URI> experiments,
            List<URI> objects,
            List<URI> variables,
            List<URI> provenances,
            List<URI> devices,
            Instant startDate,
            Instant endDate,
            Float confidenceMin,
            Float confidenceMax,
            Document metadata,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        Document filter = searchFilter(user, experiments, objects, variables, provenances, devices, startDate, endDate, confidenceMin, confidenceMax, metadata);

        ListWithPagination<DataModel> datas = nosql.searchWithPagination(DataModel.class, DATA_COLLECTION_NAME, filter, orderByList, page, pageSize);

        return datas;

    }
    
     public int count(
            UserModel user,
            List<URI> experiments,
            List<URI> objects,
            List<URI> variables,
            List<URI> provenances,
            List<URI> devices,
            Instant startDate,
            Instant endDate,
            Float confidenceMin,
            Float confidenceMax,
            Document metadata) throws Exception {

        Document filter = searchFilter(user, experiments, objects, variables, provenances,devices, startDate, endDate, confidenceMin, confidenceMax, metadata);
        int count = nosql.count(DataModel.class, DATA_COLLECTION_NAME, filter );

        return count;

    }
    
    /**
     *
     * @param deviceURI
     * @param user
     * @param experiments
     * @param objects
     * @param variables
     * @param provenances
     * @param startDate
     * @param endDate
     * @param confidenceMin
     * @param confidenceMax
     * @param metadata
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     * @deprecated better use the method search
     */
    @Deprecated
    public ListWithPagination<DataModel> searchByDevice(
            URI deviceURI,
            UserModel user,
            List<URI> experiments,
            List<URI> objects,
            List<URI> variables,
            List<URI> provenances,
            Instant startDate,
            Instant endDate,
            Float confidenceMin,
            Float confidenceMax,
            Document metadata,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {
        
        Document filter = searchFilter(user, experiments, objects, variables, provenances, Arrays.asList(deviceURI), startDate, endDate, confidenceMin, confidenceMax, metadata);

        ListWithPagination<DataModel> datas = nosql.searchWithPagination(DataModel.class, DATA_COLLECTION_NAME, filter, orderByList, page, pageSize);  

        return datas;

    }
    
    /**
     *
     * @param deviceURI
     * @param user
     * @param experiments
     * @param objects
     * @param variables
     * @param provenances
     * @param startDate
     * @param endDate
     * @param confidenceMin
     * @param confidenceMax
     * @param metadata
     * @return
     * @throws Exception
     * @deprecated better use the method count
     */
    @Deprecated
    public int countByDevice(
            URI deviceURI,
            UserModel user,
            List<URI> experiments,
            List<URI> objects,
            List<URI> variables,
            List<URI> provenances,
            Instant startDate,
            Instant endDate,
            Float confidenceMin,
            Float confidenceMax,
            Document metadata) throws Exception {
        
        Document filter = searchFilter(user, experiments, objects, variables, provenances, Arrays.asList(deviceURI), startDate, endDate, confidenceMin, confidenceMax, metadata);

        int count = nosql.count(DataModel.class, DATA_COLLECTION_NAME, filter );

        return count;

    }
    
    /**
     *
     * @param deviceURI
     * @param user
     * @param rdfType
     * @param experiments
     * @param objects
     * @param provenances
     * @param startDate
     * @param endDate
     * @param metadata
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     * @deprecated better use the method searchFiles
     */
    @Deprecated
    public ListWithPagination<DataFileModel> searchFilesByDevice(
            URI deviceURI,
            UserModel user,
            URI rdfType,
            List<URI> experiments,
            List<URI> objects,
            List<URI> provenances,
            Instant startDate,
            Instant endDate,
            Document metadata,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {
        
        Document filter = searchFilter(user, experiments, objects, null, provenances, Arrays.asList(deviceURI), startDate, endDate, null, null, metadata);

        if (rdfType != null) {
            filter.put("rdfType", rdfType);
        }      
        ListWithPagination<DataFileModel> datas = nosql.searchWithPagination(DataFileModel.class, FILE_COLLECTION_NAME, filter, orderByList, page, pageSize);  

        return datas;

    }
    
    public List<DataModel> search(
            UserModel user,
            List<URI> experiments,
            List<URI> objects,
            List<URI> variables,
            List<URI> provenances,
            List<URI> devices,
            Instant startDate,
            Instant endDate,
            Float confidenceMin,
            Float confidenceMax,
            Document metadata,
            List<OrderBy> orderByList) throws Exception {

        Document filter = searchFilter(user, experiments, objects, variables, provenances, devices, startDate, endDate, confidenceMin, confidenceMax, metadata);

        List<DataModel> datas = nosql.search(DataModel.class, DATA_COLLECTION_NAME, filter, orderByList);

        return datas;

    }
    
    public Document searchFilter(UserModel user, List<URI> experiments, List<URI> objects, List<URI> variables, List<URI> provenances, List<URI> devices, Instant startDate, Instant endDate, Float confidenceMin, Float confidenceMax, Document metadata) throws Exception {   
                
        Document filter = new Document();

        // handle case some case in which some service/dao must have access to data collection, even if the user don't have direct access to xp
        if(user != null){
            filter = appendExperimentUserAccessFilter(filter, user, experiments);

        }

        if (objects != null && !objects.isEmpty()) {
            Document inFilter = new Document(); 
            inFilter.put("$in", objects);
            filter.put("target", inFilter);
        }

        if (variables != null && !variables.isEmpty()) {
            Document inFilter = new Document(); 
            inFilter.put("$in", variables);
            filter.put("variable", inFilter);
        }

        if (provenances != null && !provenances.isEmpty()) {
            Document inFilter = new Document();            
            inFilter.put("$in", provenances);
            filter.put("provenance.uri", inFilter);
        }

        if (startDate != null || endDate != null) {
            Document dateFilter = new Document();
            if (startDate != null) {            
                dateFilter.put("$gte", startDate);
            }

            if (endDate != null) {
                dateFilter.put("$lt", endDate);

            }
            filter.put("date", dateFilter);
        }    
        
        if (confidenceMin != null || confidenceMax != null) {
            Document confidenceFilter = new Document();
            if (confidenceMin != null) {            
                confidenceFilter.put("$gte", confidenceMin);
            }

            if (confidenceMax != null) {
                confidenceFilter.put("$lte", confidenceMax);

            }
            filter.put("confidence", confidenceFilter);
        }
        
        if (metadata != null) {
            for (String key:metadata.keySet()) {
                filter.put("metadata." + key, metadata.get(key));
            }
        }        
                
        if (devices != null && !devices.isEmpty()) {
            //Get all data that have :
            //    provenance.provUsed.uri IN deviceURIs 
            // OR ( provenance.uri IN deviceProvenances list && provenance.provUsed.uri isEmpty or not exists)
            ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql);
            Set<URI> deviceProvenances = provDAO.getProvenancesURIsByAgents(devices);

            Document directProvFilter = new Document("provenance.provWasAssociatedWith.uri", new Document("$in", devices));

            Document globalProvUsed = new Document("provenance.uri", new Document("$in", deviceProvenances));
            globalProvUsed.put("$or", Arrays.asList(
                new Document("provenance.provWasAssociatedWith", new Document("$exists", false)),
                new Document("provenance.provWasAssociatedWith", new ArrayList())
                )
            );

            filter.put("$or", Arrays.asList(directProvFilter, globalProvUsed));
        }        
                
        return filter;
    }    
    
    private Document appendExperimentUserAccessFilter(Document filter, UserModel user, List<URI> experiments) throws Exception {
        String experimentField = "provenance.experiments";
        
        //user access
        if (!user.isAdmin()) {
            ExperimentDAO expDAO = new ExperimentDAO(sparql);
            Set<URI> userExperiments = expDAO.getUserExperiments(user);                        

            if (experiments != null && !experiments.isEmpty()) {
                
                //Transform experiments and userExperiments in long format to compare the two lists
                Set<URI> longUserExp = new HashSet<>();
                for (URI exp:userExperiments) {
                    longUserExp.add(new URI(SPARQLDeserializers.getExpandedURI(exp)));
                }                
                Set <URI> longExpURIs = new HashSet<>();
                for (URI exp:experiments) {
                    longExpURIs.add(new URI(SPARQLDeserializers.getExpandedURI(exp)));
                }
                longExpURIs.retainAll(longUserExp); //keep in the list only the experiments the user has access to
                
                if (longExpURIs.isEmpty()) {
                    throw new Exception("you can't access to the given experiments");
                } else {
                    Document inFilter = new Document(); 
                    inFilter.put("$in", longExpURIs);
                    filter.put("provenance.experiments", inFilter);
                }
            } else {
                Document filterOnExp = new Document(experimentField, new Document("$in", userExperiments));                
                Document notExistingExpFilter = new Document(experimentField, new Document("$exists", false));
                Document emptyExpFilter = new Document(experimentField, new ArrayList());                
                List<Document> expFilter = Arrays.asList(filterOnExp, notExistingExpFilter, emptyExpFilter);
             
                filter.put("$or", expFilter);  
            }
        } else {
            if (experiments != null && !experiments.isEmpty()) {
                Document inFilter = new Document(); 
                inFilter.put("$in", experiments);
                filter.put("provenance.experiments", inFilter);
            }
        }
        
        return filter;        
    }

    public DataModel get(URI uri) throws NoSQLInvalidURIException {
        DataModel data = nosql.findByURI(DataModel.class, DATA_COLLECTION_NAME, uri);
        return data;
    }

    public DataFileModel getFile(URI uri) throws NoSQLInvalidURIException {
        DataFileModel data = nosql.findByURI(DataFileModel.class, FILE_COLLECTION_NAME, uri);
        return data;
    }
    
    public void delete(URI uri) throws NoSQLInvalidURIException, Exception {
        nosql.delete(DataModel.class, DATA_COLLECTION_NAME, uri);
    }

    public void delete(List<URI> uris) throws NoSQLInvalidURIException, Exception {
        nosql.delete(DataModel.class, DATA_COLLECTION_NAME, uris);
    }

    public void deleteFile(URI uri) throws NoSQLInvalidURIException {
        nosql.delete(DataFileModel.class, FILE_COLLECTION_NAME, uri);
    }

    @Deprecated
    public ListWithPagination<VariableModel> getVariablesByExperiment(UserModel user, URI xpUri, Integer page, Integer pageSize) throws Exception {
        List<URI> experiments = new ArrayList();
        experiments.add(xpUri);                
        Document filter = searchFilter(user, experiments, null, null, null, null, null, null, null, null, null);
        Set<URI> variableURIs = nosql.distinct("variable", URI.class, DATA_COLLECTION_NAME, filter);
        
        if (variableURIs.isEmpty()) {
            return new ListWithPagination(new ArrayList(), page, pageSize, 0);

        } else {
            
            int total = variableURIs.size();

            List<URI> list = new ArrayList<>(variableURIs);
            List<URI> listToSend = new ArrayList<>();
            if (total > 0 && (page * pageSize) < total) {
                if (page == null || page < 0) {
                    page = 0;
                }                
                int fromIndex = page*pageSize;
                int toIndex;
                if (total > fromIndex + pageSize) {
                    toIndex = fromIndex + pageSize;
                } else {
                    toIndex = total;
                }
                listToSend = list.subList(fromIndex, toIndex);
            }

            List<VariableModel> variables = sparql.getListByURIs(VariableModel.class, listToSend, user.getLanguage());
            return new ListWithPagination(variables, page, pageSize, total);
        }
    }    

    /**
     *
     * @param user
     * @param xpUri
     * @return
     * @throws Exception
     * @deprecated better use directly the method getUsedProvenances
     */
    @Deprecated
    public Set<URI> getProvenancesByExperiment(UserModel user, URI xpUri) throws Exception {
        List<URI> experiments = new ArrayList();
        experiments.add(xpUri);
        Document filter = searchFilter(user, experiments, null, null, null, null, null, null, null, null, null);
        return nosql.distinct("provenance.uri", URI.class, DATA_COLLECTION_NAME, filter);
    }
    
    /**
     *
     * @param user
     * @param uri
     * @param collectionName
     * @return
     * @throws Exception
     * @deprecated better use directly the method getUsedProvenances
     */
    @Deprecated
    public List<ProvenanceModel> getProvenancesByScientificObject(UserModel user, URI uri, String collectionName) throws Exception {
        List<URI> scientificObjects = new ArrayList();
        scientificObjects.add(uri);
        Document filter = searchFilter(user, null, scientificObjects, null, null, null, null, null, null, null, null);
        Set<URI> provenancesURIs = nosql.distinct("provenance.uri", URI.class, collectionName, filter);
        return nosql.findByURIs(ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, new ArrayList(provenancesURIs));
    }

    /**
     *
     * @param user
     * @param uri
     * @param collectionName
     * @return
     * @throws Exception
     * @deprecated better use directly the method getUsedProvenances
     */
    @Deprecated
    public List<ProvenanceModel> getProvenancesByDevice(UserModel user, URI uri, String collectionName) throws Exception {
        Document filter = searchFilter(user, null, null, null, null, Arrays.asList(uri), null, null, null, null, null);
        Set<URI> provenancesURIs = nosql.distinct("provenance.uri", URI.class, collectionName, filter);
        return nosql.findByURIs(ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, new ArrayList<>(provenancesURIs));
    }

    public <T extends DataFileModel> void insertFile(DataFileModel model, File file) throws Exception {
        //generate URI
        nosql.generateUniqueUriIfNullOrValidateCurrent(model, FILE_PREFIX, FILE_COLLECTION_NAME);

        final String filename = Base64.getEncoder().encodeToString(model.getUri().toString().getBytes());
        Path filePath = Paths.get(FS_FILE_PREFIX, filename);
        model.setPath(filePath.toString());

        nosql.startTransaction();         
        try {   
            createFile(model);
            fs.writeFile(FS_FILE_PREFIX, filePath, file);
            nosql.commitTransaction();
        } catch (Exception e) {
            nosql.rollbackTransaction();
            fs.deleteIfExists(FS_FILE_PREFIX, filePath);
            throw e;
        } 

    }

    public ListWithPagination<DataFileModel> searchFiles(
            UserModel user,
            List<URI> rdfTypes,
            List<URI> experiments,
            List<URI> objects,
            List<URI> provenances,
            List<URI> devices,
            Instant startDate,
            Instant endDate,
            Document metadata,
            List<OrderBy> orderBy,
            int page,
            int pageSize) throws Exception {

        Document filter = searchFilter(user, experiments, objects, null, provenances, devices, startDate, endDate, null, null, metadata);
                
        if (rdfTypes != null && !rdfTypes.isEmpty()) {
            Document inFilter = new Document(); 
            inFilter.put("$in", rdfTypes);
            filter.put("rdfType", inFilter);
        }

        ListWithPagination<DataFileModel> files = nosql.searchWithPagination(
                DataFileModel.class, FILE_COLLECTION_NAME, filter, orderBy, page, pageSize);

        return files;

    }

    public DeleteResult deleteWithFilter(UserModel user, URI experimentUri, URI objectUri, URI variableUri, URI provenanceUri) throws Exception {
        List<URI> provenances = new ArrayList<>();
        if (provenanceUri != null) {
            provenances.add(provenanceUri);
        }
        
        List<URI> objects = new ArrayList<>();
        if (objectUri != null) {
            objects.add(objectUri);
        }
        
        List<URI> variables = new ArrayList<>();
        if (variableUri != null) {
            variables.add(variableUri);
        }

        List<URI> experiments = new ArrayList<>();
        if (experimentUri != null) {
            experiments.add(experimentUri);
        }
        
        Document filter = searchFilter(user, experiments, objects, variables, provenances, null, null, null, null, null, null);
        DeleteResult result = nosql.deleteOnCriteria(DataModel.class, DATA_COLLECTION_NAME, filter);
        return result;
    }

    public List<VariableModel> getUsedVariables(UserModel user, List<URI> experiments, List<URI> objects, List<URI> provenances) throws Exception {             
        Document filter = searchFilter(user, experiments, objects, null, provenances, null, null, null, null, null, null);
        Set<URI> variableURIs = nosql.distinct("variable", URI.class, DATA_COLLECTION_NAME, filter);
        return new VariableDAO(sparql,nosql,fs).getList(new ArrayList<>(variableURIs));
    }
    
    public Response prepareCSVWideExportResponse(List<DataModel> resultList, UserModel user, boolean withRawData) throws Exception {
        Instant data = Instant.now();

        List<URI> variables = new ArrayList<>();

        Map<URI, SPARQLNamedResourceModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> dataByIndexAndInstant = new HashMap<>();
        Map<URI, List<DataModel>> dataByExp = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap();
        
        for (DataModel dataModel : resultList) {
            if (dataModel.getTarget() != null && !objects.containsKey(dataModel.getTarget())) {
                objects.put(dataModel.getTarget(), null);
            }
            
            if (!variables.contains(dataModel.getVariable())) {
                variables.add(dataModel.getVariable());
            }

            if (!provenances.containsKey(dataModel.getProvenance().getUri())) {
                provenances.put(dataModel.getProvenance().getUri(), null);
            }
            
            if (!dataByIndexAndInstant.containsKey(dataModel.getDate())) {
                dataByIndexAndInstant.put(dataModel.getDate(), new HashMap<>());
            }
            
            if (dataModel.getProvenance().getExperiments() != null) {
                for (URI exp:dataModel.getProvenance().getExperiments()) {           
                    if (!experiments.containsKey(exp)) {
                        experiments.put(exp, null);
                    }
                    
                    ExportDataIndex exportDataIndex = new ExportDataIndex(
                            exp,
                            dataModel.getProvenance().getUri(), 
                            dataModel.getTarget()
                    );

                    if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                        dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                    } 
                    dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, exp));
                }                
            } else {
                ExportDataIndex exportDataIndex = new ExportDataIndex(
                                null,
                                dataModel.getProvenance().getUri(), 
                                dataModel.getTarget()
                        );
                    
                DataExportDTO dataExportDto = DataExportDTO.fromModel(dataModel, null); 
                if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                        dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                    } 
                dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, null));

            }
            
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();

        // first static columns

        defaultColumns.add("Experiment");        
        defaultColumns.add("Target");
        defaultColumns.add("Date");

        List<String> methods = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            methods.add("");
        }
        methods.add("Method");
        List<String> units = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            units.add("");
        }
        units.add("Unit");
        List<String> variablesList = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            variablesList.add("");
        }
        variablesList.add("Variable");

        List<VariableModel> variablesModelList = new VariableDAO(sparql,nosql,fs).getList(variables);

        Map<URI, Integer> variableUriIndex = new HashMap<>();
        for (VariableModel variableModel : variablesModelList) {

            MethodModel method = variableModel.getMethod();

            if (method != null) {
                String methodID = method.getName() + " (" + method.getUri().toString() + ")";
                methods.add(methodID);
            } else {
                methods.add("");
            }
            UnitModel unit = variableModel.getUnit();

            String unitID = unit.getName() + " (" + unit.getUri().toString() + ")";
            units.add(unitID);
            variablesList.add(variableModel.getName() + " (" + variableModel.getUri() + ")");
            defaultColumns.add(variableModel.getName());
            if (withRawData) {
                defaultColumns.add("Raw data");
                methods.add("");
                units.add("");
                variablesList.add("");
                variableUriIndex.put(variableModel.getUri(), (defaultColumns.size() - 2));
            } else {
                variableUriIndex.put(variableModel.getUri(), (defaultColumns.size() - 1));
            }
        }
        // static supplementary columns
        defaultColumns.add("Provenance");
        for (int i=0; i<experiments.size(); i++) {
           defaultColumns.add("Experiment URI"); 
        }

        defaultColumns.add("Target URI");
        defaultColumns.add("Provenance URI");

        Instant variableTime = Instant.now();
        LOGGER.debug("Get " + variables.size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        OntologyDAO ontologyDao = new OntologyDAO(sparql);
        List<SPARQLNamedResourceModel> objectsList = ontologyDao.getURILabels(objects.keySet(), user.getLanguage(), null);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        LOGGER.debug("Get " + objectsList.size() + " target(s) " + Long.toString(Duration.between(variableTime, targetTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDAO provenanceDao = new ProvenanceDAO(nosql, sparql);
            List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(targetTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        LOGGER.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");    
        
        // ObjectURI, ObjectName, Factor, Date, Confidence, Variable n ...
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            // Method
            // Unit
            // Variable 
            writer.writeNext(methods.toArray(new String[methods.size()]));
            writer.writeNext(units.toArray(new String[units.size()]));
            writer.writeNext(variablesList.toArray(new String[variablesList.size()]));
            // empty line
            writer.writeNext(new String[defaultColumns.size()]);
            // headers
            writer.writeNext(defaultColumns.toArray(new String[defaultColumns.size()]));

            // Search in map indexed by date for exp, prov, object and data            
            for (Map.Entry<Instant, Map<ExportDataIndex, List<DataExportDTO>>> instantProvUriDataEntry : dataByIndexAndInstant.entrySet()) {
                Map<ExportDataIndex, List<DataExportDTO>> mapProvUriData = instantProvUriDataEntry.getValue();
                // Search in map indexed by  prov and data
                for (Map.Entry<ExportDataIndex, List<DataExportDTO>> provUriObjectEntry : mapProvUriData.entrySet()) {
                    List<DataExportDTO> val = provUriObjectEntry.getValue();             

                    ArrayList<String> csvRow = new ArrayList<>();
                    //first is used to have value with the same dates on the same line
                    boolean first = true;

                    for (DataExportDTO dataGetDTO : val) {
                                            

                        if (first) {

                            csvRow = new ArrayList<>();

                            // experiment
                            ExperimentModel experiment = null;
                            if (dataGetDTO.getProvenance().getExperiments() != null && !dataGetDTO.getProvenance().getExperiments().isEmpty()) {
                                experiment = experiments.get(dataGetDTO.getExperiment());
                            } 

                            if (experiment != null) {
                                csvRow.add(experiment.getName());
                            } else {                        
                                csvRow.add("");
                            }                            

                            // target
                            SPARQLNamedResourceModel target = null;
                            if(dataGetDTO.getTarget() != null){
                               target = objects.get(dataGetDTO.getTarget());
                            }

                            if(target != null){
                                csvRow.add(target.getName());
                            }else{
                                csvRow.add("");
                            }

                            // date
                            csvRow.add(dataGetDTO.getDate());
                            
                            // write blank columns for value and rawData
                            for (int i = 0; i < variables.size(); i++) {
                                csvRow.add("");
                                if (withRawData) {
                                    csvRow.add("");
                                }
                            }

                            // provenance
                            if (provenances.containsKey(dataGetDTO.getProvenance().getUri())) {
                                csvRow.add(provenances.get(dataGetDTO.getProvenance().getUri()).getName());
                            } else {
                                csvRow.add("");
                            }                            

                            // experiment URI
                            if (experiment != null) {
                                csvRow.add(experiment.getUri().toString());
                            } else {                        
                                csvRow.add("");
                            }

                            // target URI
                             if(target != null){
                                csvRow.add(target.getUri().toString());
                            }else{
                                csvRow.add("");
                            }

                            // provenance Uri
                            csvRow.add(dataGetDTO.getProvenance().getUri().toString());

                            first = false;
                        }

                        // value
                        if (dataGetDTO.getValue() == null){
                            csvRow.set(variableUriIndex.get(dataGetDTO.getVariable()), null);
                        } else {
                            csvRow.set(variableUriIndex.get(dataGetDTO.getVariable()), dataGetDTO.getValue().toString());
                        }
                        
                        // raw data
                        if (withRawData) {
                            if (dataGetDTO.getRawData() == null){
                                csvRow.set(variableUriIndex.get(dataGetDTO.getVariable())+1, null);
                            } else {
                                csvRow.set(variableUriIndex.get(dataGetDTO.getVariable())+1, Arrays.toString(dataGetDTO.getRawData().toArray()).replace("[", "").replace("]", ""));
                            } 
                        }
                        
                    }
                    

                    String[] row = csvRow.toArray(new String[csvRow.size()]);
                    writer.writeNext(row);                        
                    
                }
            }
            
            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_wide_format" + dtf.format(date) + ".csv";

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(provenancesTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=" + fileName )
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    public Response prepareCSVLongExportResponse(List<DataModel> resultList, UserModel user, boolean withRawData) throws Exception {
        Instant data = Instant.now();

        Map<URI, VariableModel> variables = new HashMap<>();
        Map<URI, SPARQLNamedResourceModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap();

        HashMap<Instant, List<DataGetDTO>> dataByInstant = new HashMap<>();
        for (DataModel dataModel : resultList) {
            if (dataModel.getTarget() != null && !objects.containsKey(dataModel.getTarget())) {
                objects.put(dataModel.getTarget(), null);
            }
            if (!variables.containsKey(dataModel.getVariable())) {
                variables.put(dataModel.getVariable(), null);
            }
            if (!provenances.containsKey(dataModel.getProvenance().getUri())) {
                provenances.put(dataModel.getProvenance().getUri(), null);
            }
            if (!dataByInstant.containsKey(dataModel.getDate())) {
                dataByInstant.put(dataModel.getDate(), new ArrayList<>());
            }                        
            dataByInstant.get(dataModel.getDate()).add(DataGetDTO.getDtoFromModel(dataModel));
            if (dataModel.getProvenance().getExperiments() != null) {
                for (URI exp:dataModel.getProvenance().getExperiments()) {
                    if (!experiments.containsKey(exp)) {
                        experiments.put(exp, null);
                    }
                }                
            }
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();

        defaultColumns.add("Experiment"); 
        defaultColumns.add("Target");
        defaultColumns.add("Date");
        defaultColumns.add("Variable");
        defaultColumns.add("Method");
        defaultColumns.add("Unit");
        defaultColumns.add("Value");
        if (withRawData) {
            defaultColumns.add("Raw data");
        }        
        defaultColumns.add("Data Description");
        defaultColumns.add("");
        defaultColumns.add("Experiment URI"); 
        defaultColumns.add("Target URI");
        defaultColumns.add("Variable URI");
        defaultColumns.add("Data Description URI");

        Instant variableTime = Instant.now();
        List<VariableModel> variablesModelList = new VariableDAO(sparql,nosql,fs).getList(new ArrayList<>(variables.keySet()));
        for (VariableModel variableModel : variablesModelList) {
            variables.put(variableModel.getUri(), variableModel);
        }
        LOGGER.debug("Get " + variables.keySet().size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        OntologyDAO ontologyDao = new OntologyDAO(sparql);
        List<SPARQLNamedResourceModel> objectsList = ontologyDao.getURILabels(objects.keySet(), user.getLanguage(), null);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        LOGGER.debug("Get " + objectsList.size() + " target(s) " + Long.toString(Duration.between(variableTime, targetTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDAO provenanceDao = new ProvenanceDAO(nosql, sparql);
        List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(targetTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        LOGGER.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");

        // See defaultColumns order
        //        Target
        //        Date
        //        Variable
        //        Method
        //        Unit
        //        Value
        //        Data Description
        //        
        //        Target URI
        //        Variable URI
        //        Data Description URI
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(defaultColumns.toArray(new String[defaultColumns.size()]));
            for (Map.Entry<Instant, List<DataGetDTO>> entry : dataByInstant.entrySet()) {
                List<DataGetDTO> val = entry.getValue();
                for (DataGetDTO dataGetDTO : val) {
                    
                    //1 row per experiment 
                    int maxRows = 1;
                    if (dataGetDTO.getProvenance().getExperiments() != null && dataGetDTO.getProvenance().getExperiments().size()>1) {
                        maxRows = dataGetDTO.getProvenance().getExperiments().size();
                    }

                    for (int j=0; j<maxRows; j++) {
                        ArrayList<String> csvRow = new ArrayList<>();
                        // experiment
                        ExperimentModel experiment = null;
                        if (dataGetDTO.getProvenance().getExperiments() != null && !dataGetDTO.getProvenance().getExperiments().isEmpty()) {
                            experiment = experiments.get(dataGetDTO.getProvenance().getExperiments().get(j));
                        } 

                        if (experiment != null) {
                            csvRow.add(experiment.getName());
                        } else {                        
                            csvRow.add("");
                        }            
                    
                        SPARQLNamedResourceModel target = null;
                        if(dataGetDTO.getTarget() != null){
                           target = objects.get(dataGetDTO.getTarget());
                        }
                        // target name
                        if(target != null){
                            csvRow.add(target.getName());
                        }else{
                            csvRow.add("");
                        }

                        // date
                        csvRow.add(dataGetDTO.getDate());
                        // variable
                        csvRow.add(variables.get(dataGetDTO.getVariable()).getName());
                        // method
                        if (variables.get(dataGetDTO.getVariable()).getMethod() != null) {
                            csvRow.add(variables.get(dataGetDTO.getVariable()).getMethod().getName());
                        } else {
                            csvRow.add("");
                        }
                        // unit
                        csvRow.add(variables.get(dataGetDTO.getVariable()).getUnit().getName());
                        // value
                        if(dataGetDTO.getValue() == null){
                            csvRow.add(null);
                        }else{
                            csvRow.add(dataGetDTO.getValue().toString());
                        }
                        
                        // rawData
                        if (withRawData) {
                            if(dataGetDTO.getRawData() == null){
                                csvRow.add(null);
                            }else{
                                csvRow.add(Arrays.toString(dataGetDTO.getRawData().toArray()).replace("[", "").replace("]", ""));
                            }
                        }

                        // provenance
                        if (provenances.containsKey(dataGetDTO.getProvenance().getUri())) {
                            csvRow.add(provenances.get(dataGetDTO.getProvenance().getUri()).getName());
                        } else {
                            csvRow.add("");
                        }
                        csvRow.add("");
                    
                        // experiment URI
                        if (experiment != null) {
                            csvRow.add(experiment.getUri().toString());
                        } else {                        
                            csvRow.add("");
                        }

                        // target uri
                        if (target != null) {
                            csvRow.add(target.getUri().toString());
                        } else {
                            csvRow.add("");
                        }

                        // variable uri
                        csvRow.add(dataGetDTO.getVariable().toString());
                        // provenance Uri
                        csvRow.add(dataGetDTO.getProvenance().getUri().toString());

                        String[] row = csvRow.toArray(new String[csvRow.size()]);
                        writer.writeNext(row);
                    }
                }
            }
            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_long_format" + dtf.format(date) + ".csv";

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(provenancesTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=" + fileName)
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    public Set<URI> getUsedProvenances(String collectionName, UserModel user, List<URI> experiments, List<URI> objects, List<URI> variables, List<URI> devices) throws Exception {
        Document filter = searchFilter(user, experiments, objects, variables, null, devices, null, null, null, null, null);
        Set<URI> provenanceURIs = nosql.distinct("provenance.uri", URI.class, collectionName, filter);
        return provenanceURIs;
    }
    
    public Set<URI> getDataProvenances(UserModel user, List<URI> experiments, List<URI> objects, List<URI> variables, List<URI> devices) throws Exception {
        return getUsedProvenances(DATA_COLLECTION_NAME, user, experiments, objects, variables, devices);
    }
    
    public Set<URI> getDatafileProvenances(UserModel user, List<URI> experiments, List<URI> objects, List<URI> devices) throws Exception {
        return getUsedProvenances(FILE_COLLECTION_NAME, user, experiments, objects, null, devices);
    }

}
