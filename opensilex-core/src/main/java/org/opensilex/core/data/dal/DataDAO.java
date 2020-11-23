//******************************************************************************
//                          DataDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.mongodb.BulkWriteError;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.sparql.exceptions.SPARQLException;

/**
 *
 * @author sammy
 */
public class DataDAO {

    protected final URI RDFTYPE_VARIABLE;
    private final URI RDFTYPE_SCIENTIFICOBJECT;
    public static final String DATA_COLLECTION_NAME = "Data";
    public static final String FILE_COLLECTION_NAME = "File";

    protected final MongoDBService nosql;
    protected final SPARQLService sparql;
    protected final FileStorageService fs;
    
    public DataDAO(MongoDBService nosql,SPARQLService sparql, FileStorageService fs) throws URISyntaxException{
        this.RDFTYPE_VARIABLE = new URI(Oeso.Variable.toString());
        this.RDFTYPE_SCIENTIFICOBJECT = new URI(Oeso.ScientificObject.toString());

        this.nosql = nosql;
        this.sparql = sparql;
        this.fs = fs;
    }
    
    public void createIndexes() {        
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection dataCollection = nosql.mongoClient.getDatabase(nosql.dbName)
                .getCollection(DATA_COLLECTION_NAME, DataModel.class);            
        dataCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("variable","provenance","scientificObjects","date"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("variable","scientificObjects","date"));

        MongoCollection fileCollection = nosql.mongoClient.getDatabase(nosql.dbName)
                .getCollection(FILE_COLLECTION_NAME, DataModel.class);            
        fileCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
        fileCollection.createIndex(Indexes.ascending("provenance","scientificObjects","date"), unicityOptions);
        dataCollection.createIndex(Indexes.ascending("scientificObjects","date"));
        
    }
   
    public DataModel create(DataModel instance) throws Exception, MongoWriteException {
        nosql.create(instance, DataModel.class, DATA_COLLECTION_NAME, "id/data");
        return instance;
    }
    
    public DataFileModel createFile(DataFileModel instance) throws Exception, MongoBulkWriteException {
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
        nosql.createAll(instances, DataFileModel.class, FILE_COLLECTION_NAME, "id/data");
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
    
    public ErrorResponse valid(DataModel data) throws SPARQLException, Exception {
        if (data.getVariable() != null) {
            VariableDAO dao = new VariableDAO(sparql);
            VariableModel variable = dao.get(data.getVariable());

            if (variable == null) {
                return new ErrorResponse(
                                Response.Status.BAD_REQUEST,
                                "wrong variable uri",
                                "Variable " + data.getVariable() +" doesn't exist"
                    );
            } else {
                URI dataType = variable.getDataType();
                if (!checkTypeCoherence(dataType, data.getValue())) {
                    return new ErrorResponse(
                                Response.Status.BAD_REQUEST,
                                "wrong value format",
                                "Variable dataType: " + dataType.toString() 
                   );
                }
            }
            
        }

        //check objects uri
        if (data.getScientificObjects() != null) {
            if (!data.getScientificObjects().isEmpty()) {
                if (!sparql.uriListExists(ScientificObjectModel.class, data.getScientificObjects())) {
                    return new ErrorResponse(
                                    Response.Status.BAD_REQUEST,
                                    "wrong object uri",
                                    "At least one scientific object doesn't exist exist" 
                        );
                }
            }
        }
        //check provenance uri
        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);
        if (!provDAO.provenanceExists(data.getProvenance().getUri())) {
            return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "wrong provenance uri",
                            "Provenance "+ data.getProvenance().getUri() +" doesn't exist"
                );
        }
        
        return null;
    }    

    public ListWithPagination<DataModel> search(
            UserModel user,
            URI objectUri,
            URI variableUri,
            URI provenanceUri,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer page,
            Integer pageSize) {
        
        Document filter = new Document();
        
        if (objectUri != null) {
            filter.put("scientificObjects",objectUri);
        }

        if (variableUri != null) {
            filter.put("variable", variableUri);
        }

        if (provenanceUri != null) {
            filter.put("provenance.uri", provenanceUri);
        }

        if (startDate != null) {
            Document greater = new Document();
            greater.put("$gte", startDate);
            filter.put("date", greater);
        }
        
        if (endDate != null) {
            Document less = new Document();
            less.put("$lte", endDate);
            filter.put("date", less);
        }

        ListWithPagination<DataModel> datas = nosql.searchWithPagination(DataModel.class, DATA_COLLECTION_NAME, filter, page, pageSize);
        
        return datas;
       
    }

    public DataModel get(URI uri) throws NoSQLInvalidURIException {
        DataModel data = nosql.findByURI(DataModel.class, DATA_COLLECTION_NAME, uri);
        return data;
    }

    public DataFileModel getFile(URI uri) throws NoSQLInvalidURIException {
        DataFileModel data = nosql.findByURI(DataFileModel.class, FILE_COLLECTION_NAME, uri);
        return data;
    }
    
    public void delete(URI uri) throws NoSQLInvalidURIException {
        nosql.delete(DataModel.class, DATA_COLLECTION_NAME, uri);
    }

    public void deleteFile(URI uri) throws NoSQLInvalidURIException {
        nosql.delete(DataFileModel.class, FILE_COLLECTION_NAME, uri);
    }
        
    public ErrorResponse validList(Set<URI> variables, Set<URI> objects, Set<URI> provenances) throws Exception {
        //check variables uri
        if (!variables.isEmpty()) {
            if (!sparql.uriListExists(VariableModel.class, variables)) {
                return new ErrorResponse(
                                Response.Status.BAD_REQUEST,
                                "wrong variable uri",
                                "A given variable uri doesn't exist"
                    );
            }
        }

        //check objects uri
        if (!objects.isEmpty()) {
            if (!sparql.uriListExists(ScientificObjectModel.class, objects)) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "wrong object uri",
                        "A given object uri doesn't exist"
                );
            }
        }

        //check provenances uri
        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);
        if (!provDAO.provenanceListExists(provenances)) {
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "wrong provenance uri",
                    "At least one provenance uri doesn't exist"
            );
        }

        return null;

    }

    public ZonedDateTime convertDateTime(String strDate) {
        DateFormat[] formats = {DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ};
        ZonedDateTime zdt = null;
        for (DateFormat dateCheckFormat : formats) {
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateCheckFormat.toString());
                zdt = ZonedDateTime.parse(strDate, dtf);
                break;
            } catch (DateTimeParseException e) {
            }
        }
        return zdt;
    }

    public List<VariableModel> getVariablesByExperiment(URI xpUri, String language) throws Exception {
        Set<URI> provenances = getProvenancesByExperiment(xpUri); 
        if (provenances.size() > 0) {
            Document listFilter = new Document();
            listFilter.append("$in", provenances);
            Document filter = new Document();
            filter.append("provenance.uri",listFilter);

            Set<URI> variableURIs = nosql.distinct("variable", URI.class, DATA_COLLECTION_NAME, filter);
 
            return sparql.getListByURIs(VariableModel.class, variableURIs, language);
        } else {
            return new ArrayList<>();
        }
    }

    public Set<URI> getProvenancesByExperiment(URI xpUri) throws Exception {
        Document filter = new Document();       
        filter.append("experiments", xpUri);
        return nosql.distinct("uri", URI.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, filter);        
    }

    public <T extends DataFileModel> void insertFile(DataFileModel model, File file) throws URISyntaxException, Exception {
        //generate URI
        nosql.generateUniqueUriIfNullOrValidateCurrent(model, "id/file", FILE_COLLECTION_NAME);
        
        Path fileStorageDirectory = Paths.get(fs.getStorageBasePath().toString()).toAbsolutePath();
        final String filename = Base64.getEncoder().encodeToString(model.getUri().toString().getBytes());
        model.setPath(fileStorageDirectory.toString() + "/" + filename);    
        
        //copy file to directory
        try {
            fs.createDirectories(fileStorageDirectory);
            fs.writeFile(Paths.get(model.getPath()), file);
            createFile(model);
            
        } catch (IOException e) {            
        }     

    }

    public ListWithPagination<DataFileModel> searchFiles(
            UserModel user, 
            URI objectUri, 
            URI provenanceUri, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            int page, 
            int pageSize) {
        
        Document filter = new Document();
        
         if (objectUri != null) {
            filter.put("scientificObjects",objectUri);
        }

        if (provenanceUri != null) {
            filter.put("provenance.uri", provenanceUri);
        }

        if (startDate != null) {
            Document greater = new Document();
            greater.put("$gte", startDate);
            filter.put("date", greater);
        }
        
        if (endDate != null) {
            Document less = new Document();
            less.put("$lte", endDate);
            filter.put("date", less);
        }

        ListWithPagination<DataFileModel> files = nosql.searchWithPagination(
                DataFileModel.class, DATA_COLLECTION_NAME, filter, page, pageSize);
        
        return files;
            
    }

    public Set<URI> checkVariableDataTypes(List<DataModel> datas) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);
        Set<URI> variables = new HashSet<>();
        Map<URI, URI> variableTypes = new HashMap();        
        
        for (DataModel data:datas) {
            if (data.getValue() != "NA") {                
                URI variableUri = data.getVariable();
                if (!variableTypes.containsKey(variableUri)) {                
                    VariableModel variable = dao.get(data.getVariable());
                    variableTypes.put(variableUri,variable.getDataType());
                }
                URI dataType = variableTypes.get(variableUri);
                if (!checkTypeCoherence(dataType, data.getValue()))  {
                    variables.add(variableUri);
                }
                
            }
        }
        return variables;
    }
    
    private Boolean checkTypeCoherence(URI dataType, Object value) {
        Boolean checkCoherence = false;
        if (dataType == null) {
            checkCoherence = true;
            
        } else {
             switch (dataType.toString()) {
            case "xsd:integer":
                if ((value instanceof Integer)) {
                    checkCoherence = true;
                }   break;
            case "xsd:decimal":
                if ((value instanceof Double)) {
                    checkCoherence = true;
                }   break;
            case "xsd:boolean":
                if ((value instanceof Boolean)) {
                    checkCoherence = true;
                }   break;
            case "xsd:date":
                if ((value instanceof String)) {
                    checkCoherence = true;
                }   break;
            case "xsd:datetime":
                if ((value instanceof String)) {
                    checkCoherence = true;
                }   break;
            case "xsd:string":
                if ((value instanceof String)) {
                    checkCoherence = true;
                }   break;
            default:
                break;        
            }
        }       
        return checkCoherence;
    }
    
}
