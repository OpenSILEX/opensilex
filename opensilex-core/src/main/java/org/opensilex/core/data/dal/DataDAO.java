//******************************************************************************
//                          DataDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import org.opensilex.core.exception.DataTypeException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.exception.NoVariableDataTypeException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
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
        dataCollection.createIndex(Indexes.ascending("scientificObject", "date"));

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
        
        ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql);
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
            URI rdfType,
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
        
        if (rdfType != null) {
            filter.put("rdfType", rdfType);
        }        

        ListWithPagination<DataFileModel> files = nosql.searchWithPagination(
                DataFileModel.class, FILE_COLLECTION_NAME, filter, orderBy, page, pageSize);

        return files;

    }

    public DeleteResult deleteWithFilter(UserModel user, URI experimentUri, URI objectUri, URI variableUri, URI provenanceUri) throws Exception {
        List<URI> provenances = new ArrayList<>();
        provenances.add(provenanceUri);
        List<URI> objects = new ArrayList<>();
        objects.add(objectUri);
        List<URI> variables = new ArrayList<>();
        variables.add(variableUri);
        List<URI> experiments = new ArrayList<>();
        experiments.add(experimentUri);
        
        Document filter = searchFilter(user, experiments, objects, variables, provenances, null, null, null, null, null);
        DeleteResult result = nosql.deleteOnCriteria(DataModel.class, DATA_COLLECTION_NAME, filter);
        return result;
    }

    public List<VariableModel> getUsedVariables(UserModel user, List<URI> experiments, List<URI> objects) throws Exception {             
        Document filter = searchFilter(user, experiments, objects, null, null, null, null, null, null, null);
        Set<URI> variableURIs = nosql.distinct("variable", URI.class, DATA_COLLECTION_NAME, filter);
        return sparql.getListByURIs(VariableModel.class, variableURIs, user.getLanguage());
    }

}
