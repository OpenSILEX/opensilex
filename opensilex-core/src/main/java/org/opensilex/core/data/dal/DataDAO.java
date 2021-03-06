//******************************************************************************
//                          DataDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import com.opencsv.CSVWriter;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.opensilex.core.data.api.DataExportDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sammy
 */
public class DataDAO {

    protected final URI RDFTYPE_VARIABLE;
    private final URI RDFTYPE_SCIENTIFICOBJECT;
    public static final String DATA_COLLECTION_NAME = "data";
    public static final String FILE_COLLECTION_NAME = "file";
    public final static String FS_FILE_PREFIX = "datafile";

    protected final MongoDBService nosql;
    protected final SPARQLService sparql;
    protected final FileStorageService fs; 
    
    private final static Logger LOGGER = LoggerFactory.getLogger(DataDAO.class);
    
    public DataDAO(MongoDBService nosql, SPARQLService sparql, FileStorageService fs) throws URISyntaxException {
        this.RDFTYPE_VARIABLE = new URI(Oeso.Variable.toString());
        this.RDFTYPE_SCIENTIFICOBJECT = new URI(Oeso.ScientificObject.toString());

        this.nosql = nosql;
        this.sparql = sparql;
        this.fs = fs;
    }

    public void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection dataCollection = nosql.getDatabase()
                .getCollection(DATA_COLLECTION_NAME, DataModel.class);
        dataCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("variable", "provenance", "scientificObject", "date"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("variable", "scientificObject", "date"));

        MongoCollection fileCollection = nosql.getDatabase()
                .getCollection(FILE_COLLECTION_NAME, DataModel.class);
        fileCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
        fileCollection.createIndex(Indexes.ascending("path"), unicityOptions);
        fileCollection.createIndex(Indexes.ascending("provenance", "scientificObject", "date"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("scientificObject"
                + "", "date"));

    }

    public DataModel create(DataModel instance) throws Exception, MongoWriteException {
        createIndexes();
        nosql.create(instance, DataModel.class, DATA_COLLECTION_NAME, "id/data");
        return instance;
    }

    public DataFileModel createFile(DataFileModel instance) throws Exception, MongoBulkWriteException {
        createIndexes();
        nosql.create(instance, DataFileModel.class, FILE_COLLECTION_NAME, "id/file");
        return instance;
    }

    public List<DataModel> createAll(List<DataModel> instances) throws Exception {
        createIndexes(); 
        nosql.createAll(instances, DataModel.class, DATA_COLLECTION_NAME, "id/data");
        return instances;
    } 

    public List<DataFileModel> createAllFiles(List<DataFileModel> instances) throws Exception {
        createIndexes();
        nosql.createAll(instances, DataFileModel.class, FILE_COLLECTION_NAME, "id/file");
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
            Instant startDate,
            Instant endDate,
            Float confidenceMin,
            Float confidenceMax,
            Document metadata,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        Document filter = searchFilter(user, experiments, objects, variables, provenances, startDate, endDate, confidenceMin, confidenceMax, metadata);

        ListWithPagination<DataModel> datas = nosql.searchWithPagination(DataModel.class, DATA_COLLECTION_NAME, filter, orderByList, page, pageSize);

        return datas;

    }
    
     public int count(
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

        Document filter = searchFilter(user, experiments, objects, variables, provenances, startDate, endDate, confidenceMin, confidenceMax, metadata);
        int count = nosql.count(DataModel.class, DATA_COLLECTION_NAME, filter );

        return count;

    }
    
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
        
        Document filter = searchByDeviceFilter(deviceURI, user, experiments, objects, variables, provenances, startDate, endDate, confidenceMin, confidenceMax, metadata);

        ListWithPagination<DataModel> datas = nosql.searchWithPagination(DataModel.class, DATA_COLLECTION_NAME, filter, orderByList, page, pageSize);  

        return datas;

    }
    
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
        
        Document filter = searchByDeviceFilter(deviceURI, user, experiments, objects, variables, provenances, startDate, endDate, confidenceMin, confidenceMax, metadata);

        int count = nosql.count(DataModel.class, DATA_COLLECTION_NAME, filter );

        return count;

    }
    
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
        
        Document filter = searchByDeviceFilter(deviceURI, user, experiments, objects, null, provenances, startDate, endDate, null, null, metadata);

        if (rdfType != null) {
            filter.put("rdfType", rdfType);
        }      
        ListWithPagination<DataFileModel> datas = nosql.searchWithPagination(DataFileModel.class, FILE_COLLECTION_NAME, filter, orderByList, page, pageSize);  

        return datas;

    }
    
    private Document searchByDeviceFilter(
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
        
        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);
        List<URI> agents = new ArrayList<>();
        agents.add(deviceURI);
        Set<URI> deviceProvenances = provDAO.getProvenancesURIsByAgents(agents);        

        Document filter = searchFilter(user, experiments, objects, variables, provenances, startDate, endDate, confidenceMin, confidenceMax, metadata);

        //Get all data that have :
        //    provenance.provUsed.uri = deviceURI 
        // OR ( provenance.uri IN deviceProvenances list && provenance.provUsed.uri isEmpty or not exists)

        Document directProvFilter = new Document("provenance.provUsed.uri", deviceURI);

        Document globalProvUsed = new Document("provenance.uri", new Document("$in", deviceProvenances));
        globalProvUsed.put("$or", Arrays.asList(
            new Document("provenance.provUsed", new Document("$exists", false)),
            new Document("provenance.provUsed", new ArrayList())
            )
        );

        filter.put("$or", Arrays.asList(directProvFilter, globalProvUsed));

        return filter;
    }
    
    public List<DataModel> search(
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
            List<OrderBy> orderByList) throws Exception {

        Document filter = searchFilter(user, experiments, objects, variables, provenances, startDate, endDate, confidenceMin, confidenceMax, metadata);

        List<DataModel> datas = nosql.search(DataModel.class, DATA_COLLECTION_NAME, filter, orderByList);

        return datas;

    }
    
    private Document searchFilter(UserModel user, List<URI> experiments, List<URI> objects, List<URI> variables, List<URI> provenances, Instant startDate, Instant endDate, Float confidenceMin, Float confidenceMax, Document metadata) throws Exception {   
                
        Document filter = new Document();
        
        filter = appendExperimentUserAccessFilter(filter, user, experiments);                
        
        if (objects != null && !objects.isEmpty()) {
            Document inFilter = new Document(); 
            inFilter.put("$in", objects);
            filter.put("scientificObject", inFilter);
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

    public ListWithPagination<VariableModel> getVariablesByExperiment(UserModel user, URI xpUri, Integer page, Integer pageSize) throws Exception {
        List<URI> experiments = new ArrayList();
        experiments.add(xpUri);                
        Document filter = searchFilter(user, experiments, null, null, null, null, null, null, null, null);
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

    public Set<URI> getProvenancesByExperiment(UserModel user, URI xpUri) throws Exception {
        List<URI> experiments = new ArrayList();
        experiments.add(xpUri);
        Document filter = searchFilter(user, experiments, null, null, null, null, null, null, null, null);
        return nosql.distinct("provenance.uri", URI.class, DATA_COLLECTION_NAME, filter);
    }
    
    public List<ProvenanceModel> getProvenancesByScientificObject(UserModel user, URI uri, String collectionName) throws Exception {
        List<URI> scientificObjects = new ArrayList();
        scientificObjects.add(uri);
        Document filter = searchFilter(user, null, scientificObjects, null, null, null, null, null, null, null);
        Set<URI> provenancesURIs = nosql.distinct("provenance.uri", URI.class, collectionName, filter);
        return nosql.findByURIs(ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, new ArrayList(provenancesURIs));
    }
    
    public List<ProvenanceModel> getProvenancesByDevice(UserModel user, URI uri, String collectionName) throws Exception {
        Document filter = searchByDeviceFilter(uri, user, null, null, null, null, null, null, null, null, null);
        Set<URI> provenancesURIs = nosql.distinct("provenance.uri", URI.class, collectionName, filter);
        return nosql.findByURIs(ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, new ArrayList(provenancesURIs));
    }

    public <T extends DataFileModel> void insertFile(DataFileModel model, File file) throws URISyntaxException, Exception {
        //generate URI
        nosql.generateUniqueUriIfNullOrValidateCurrent(model, "id/file", FILE_COLLECTION_NAME);

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
            Instant startDate,
            Instant endDate,
            Document metadata,
            List<OrderBy> orderBy,
            int page,
            int pageSize) throws Exception {

        Document filter = searchFilter(user, experiments, objects, null, provenances, startDate, endDate, null, null, metadata);
                
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
        
        Document filter = searchFilter(user, experiments, objects, variables, provenances, null, null, null, null, null);
        DeleteResult result = nosql.deleteOnCriteria(DataModel.class, DATA_COLLECTION_NAME, filter);
        return result;
    }

    public List<VariableModel> getUsedVariables(UserModel user, List<URI> experiments, List<URI> objects) throws Exception {             
        Document filter = searchFilter(user, experiments, objects, null, null, null, null, null, null, null);
        Set<URI> variableURIs = nosql.distinct("variable", URI.class, DATA_COLLECTION_NAME, filter);
        return sparql.getListByURIs(VariableModel.class, variableURIs, user.getLanguage());
    }
    
    public Response prepareCSVWideExportResponse(List<DataModel> resultList, UserModel user) throws Exception {
        Instant data = Instant.now();

        List<URI> variables = new ArrayList<>();

        Map<URI, ScientificObjectModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> dataByIndexAndInstant = new HashMap<>();
        Map<URI, List<DataModel>> dataByExp = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap();
        
        for (DataModel dataModel : resultList) {
            if (dataModel.getScientificObject() != null && !objects.containsKey(dataModel.getScientificObject())) {
                objects.put(dataModel.getScientificObject(), null);
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
                            dataModel.getScientificObject()
                    );

                    if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                        dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                    } 
                    dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, exp));
                }                
            }
            
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();

        // first static columns

        defaultColumns.add("Experiment");        
        defaultColumns.add("Scientific Object");
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

        VariableDAO variableDao = new VariableDAO(sparql);
        List<VariableModel> variablesModelList = variableDao.getList(variables);

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
            variableUriIndex.put(variableModel.getUri(), (defaultColumns.size() - 1));
        }
        // static supplementary columns
        defaultColumns.add("Data Description");
        for (int i=0; i<experiments.size(); i++) {
           defaultColumns.add("Experiment URI"); 
        }
        defaultColumns.add("Scientific Object URI");
        defaultColumns.add("Data Description URI");

        Instant variableTime = Instant.now();
        LOGGER.debug("Get " + variables.size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        ScientificObjectDAO scientificObjectDao = new ScientificObjectDAO(sparql, nosql);
        List<ScientificObjectModel> listScientificObjectDao = scientificObjectDao.searchByURIs(sparql.getDefaultGraphURI(ScientificObjectModel.class), new ArrayList<>(objects.keySet()), user);
        for (ScientificObjectModel scientificObjectModel : listScientificObjectDao) {
            objects.put(scientificObjectModel.getUri(), scientificObjectModel);
        }
        Instant scientificObjectTime = Instant.now();
        LOGGER.debug("Get " + listScientificObjectDao.size() + " scientificObject(s) " + Long.toString(Duration.between(variableTime, scientificObjectTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDAO provenanceDao = new ProvenanceDAO(nosql);
        List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(scientificObjectTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        ExperimentDAO expDAO = new ExperimentDAO(sparql);
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

                    ArrayList<String> csvRow = new ArrayList<>();;
                    //first is used to have value with the same dates on the same line
                    boolean first = true;

                    for (DataExportDTO dataGetDTO : val) {
                                            

                        if (first) {

                            csvRow = new ArrayList<>();

                            // experiment
                            ExperimentModel experiment = null;
                            if (!dataGetDTO.getProvenance().getExperiments().isEmpty()) {
                                experiment = experiments.get(dataGetDTO.getExperiment());
                            } 

                            if (experiment != null) {
                                csvRow.add(experiment.getName());
                            } else {                        
                                csvRow.add("");
                            }                            

                            // object
                            ScientificObjectModel os = null;
                            if(dataGetDTO.getScientificObject() != null){
                               os = objects.get(dataGetDTO.getScientificObject());
                            }

                            if(os != null){
                                csvRow.add(os.getName());
                            }else{
                                csvRow.add("");
                            }

                            // date
                            csvRow.add(dataGetDTO.getDate());
                            // write blank columns
                            for (int i = 0; i < variables.size(); i++) {
                                csvRow.add("");
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

                            // object URI
                             if(os != null){
                                csvRow.add(os.getUri().toString());
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
                    }
                    

                    String[] row = csvRow.toArray(new String[csvRow.size()]);
                    writer.writeNext(row);                        
                    
                }
            }
            
            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_wide_format" + dtf.format(date);

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(provenancesTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    public Response prepareCSVLongExportResponse(List<DataModel> resultList, UserModel user) throws Exception {
        Instant data = Instant.now();

        Map<URI, VariableModel> variables = new HashMap<>();
        Map<URI, ScientificObjectModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap();

        HashMap<Instant, List<DataGetDTO>> dataByInstant = new HashMap<>();
        for (DataModel dataModel : resultList) {
            if (dataModel.getScientificObject() != null && !objects.containsKey(dataModel.getScientificObject())) {
                objects.put(dataModel.getScientificObject(), null);
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
        defaultColumns.add("Scientific Object");
        defaultColumns.add("Date");
        defaultColumns.add("Variable");
        defaultColumns.add("Method");
        defaultColumns.add("Unit");
        defaultColumns.add("Value");
        defaultColumns.add("Data Description");
        defaultColumns.add("");
        defaultColumns.add("Experiment URI"); 
        defaultColumns.add("Scientific Object URI");
        defaultColumns.add("Variable URI");
        defaultColumns.add("Data Description URI");

        Instant variableTime = Instant.now();
        VariableDAO variableDao = new VariableDAO(sparql);
        List<VariableModel> variablesModelList = variableDao.getList(new ArrayList<>(variables.keySet()));
        for (VariableModel variableModel : variablesModelList) {
            variables.put(variableModel.getUri(), variableModel);
        }
        LOGGER.debug("Get " + variables.keySet().size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        ScientificObjectDAO scientificObjectDao = new ScientificObjectDAO(sparql, nosql);
        List<ScientificObjectModel> listScientificObjectDao = scientificObjectDao.searchByURIs(sparql.getDefaultGraphURI(ScientificObjectModel.class), new ArrayList<>(objects.keySet()), user);
        for (ScientificObjectModel scientificObjectModel : listScientificObjectDao) {
            objects.put(scientificObjectModel.getUri(), scientificObjectModel);
        }
        Instant scientificObjectTime = Instant.now();
        LOGGER.debug("Get " + listScientificObjectDao.size() + " scientificObject(s) " + Long.toString(Duration.between(variableTime, scientificObjectTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDAO provenanceDao = new ProvenanceDAO(nosql);
        List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(scientificObjectTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        ExperimentDAO expDAO = new ExperimentDAO(sparql);
        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        LOGGER.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");

        // See defaultColumns order
        //        Object
        //        Date
        //        Variable
        //        Method
        //        Unit
        //        Value
        //        Data Description
        //        
        //        Object URI
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
                    
                        ScientificObjectModel os = null;
                        if(dataGetDTO.getScientificObject() != null){
                           os = objects.get(dataGetDTO.getScientificObject());
                        }
                        // object
                        if(os != null){
                            csvRow.add(os.getName());
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

                        // object uri
                        if (os != null) {
                            csvRow.add(os.getUri().toString());
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
            String fileName = "export_data_long_format" + dtf.format(date);

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(provenancesTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

}
