//******************************************************************************
//                          DataAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.result.DeleteResult;
import com.opencsv.CSVWriter;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import static java.lang.Float.NaN;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import static org.opensilex.core.data.api.DataCreationDTO.NAN_VALUES;
import static org.opensilex.core.data.api.DataCreationDTO.NA_VALUES;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.CSVDataTypeException;
import org.opensilex.core.exception.DateMappingExceptionResponse;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.core.exception.DataTypeException;
import org.opensilex.core.exception.DateValidationException;
import org.opensilex.core.exception.NoVariableDataTypeException;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import static org.opensilex.core.experiment.api.ExperimentAPI.CSV_NB_ERRORS_MAX;
import static org.opensilex.core.experiment.api.ExperimentAPI.EXPERIMENT_EXAMPLE_URI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.experiment.utils.ImportDataIndex;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.exceptions.NoSQLTooLargeSetException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensilex.utils.OrderBy;

/**
 *
 * @author sammy
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path("/core/data")
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class DataAPI {
    protected final static Logger LOGGER = LoggerFactory.getLogger(DataAPI.class);

    public static final String CREDENTIAL_DATA_GROUP_ID = "Data";
    public static final String CREDENTIAL_DATA_GROUP_LABEL_KEY = "credential-groups.data";

    public static final String DATA_EXAMPLE_URI = "http://opensilex.dev/id/data/1598857852858";
    public static final String DATA_EXAMPLE_OBJECTURI = "http://opensilex.dev/opensilex/2020/o20000345";
    public static final String DATA_EXAMPLE_VARIABLEURI = "http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6";
    public static final String DATA_EXAMPLE_CONFIDENCE = "0.5";
    public static final String DATA_EXAMPLE_CONFIDENCE_MAX = "1";
    public static final String DATA_EXAMPLE_PROVENANCEURI = "http://opensilex.dev/provenance/1598001689415";
    public static final String DATA_EXAMPLE_VALUE = "8.6";
    public static final String DATA_EXAMPLE_MINIMAL_DATE  = "2020-08-21T00:00:00+01:00";
    public static final String DATA_EXAMPLE_MAXIMAL_DATE = "2020-09-21T00:00:00+01:00";
    public static final String DATA_EXAMPLE_TIMEZONE = "Europe/Paris";
    public static final String DATA_EXAMPLE_METADATA = "{ \"LabelView\" : \"side90\",\n" +
                                                        "\"paramA\" : \"90\"}";;
    
    public static final String CREDENTIAL_DATA_MODIFICATION_ID = "data-modification";
    public static final String CREDENTIAL_DATA_MODIFICATION_LABEL_KEY = "credential.data.modification";

    public static final String CREDENTIAL_DATA_DELETE_ID = "data-delete";
    public static final String CREDENTIAL_DATA_DELETE_LABEL_KEY = "credential.data.delete";
    public static final int SIZE_MAX = 50000;
    


    @Inject
    private MongoDBService nosql;
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private FileStorageService fs;

    @CurrentUser
    UserModel user;

    @POST
    @ApiProtected
    @ApiOperation("Add data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add data", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
   public Response addListData(
            @ApiParam("Data description") @Valid List<DataCreationDTO> dtoList
    ) throws Exception {            
        DataDAO dao = new DataDAO(nosql, sparql, fs);        

        List<DataModel> dataList = new ArrayList();
        try {
            if (dtoList.size()> SIZE_MAX) {
                throw new NoSQLTooLargeSetException(dtoList.size());
            }
            
            for(DataCreationDTO dto : dtoList ){            
                DataModel model = dto.newModel();
                dataList.add(model);
            }

            validData(dataList);

            dataList = (List<DataModel>) dao.createAll(dataList);
            List<URI> createdResources = new ArrayList<>();
            for (DataModel data : dataList){
                createdResources.add(data.getUri());
            }
            return new ObjectUriResponse(Response.Status.CREATED, createdResources).getResponse();

        } catch (NoSQLTooLargeSetException ex) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DATA_SIZE_LIMIT",
                ex.getMessage()).getResponse();

        } catch (MongoBulkWriteException duplicateError) {
            List<DataCreationDTO> datas = new ArrayList();
            List<BulkWriteError> errors = duplicateError.getWriteErrors();
            for (int i=0 ; i < errors.size() ; i++) {
                int index = errors.get(i).getIndex();
                datas.add(dtoList.get(index));
            }                    
            ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
            String json = mapper.writeValueAsString(datas);

            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", json)
            .getResponse();

        } catch (MongoCommandException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", e.getErrorMessage())
            .getResponse();
        } catch (DateValidationException e) {
            return new DateMappingExceptionResponse().toResponse(e);
        } catch (DataTypeException | NoVariableDataTypeException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DATA_TYPE_ERROR", e.getMessage())
            .getResponse();
        } catch (NoSQLInvalidUriListException e) {
            throw new NotFoundException(e.getMessage());
        }        
    }
        
    
    
    @GET
    @Path("{uri}")
    @ApiOperation("Get data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Data retrieved", response = DataGetDTO.class),
        @ApiResponse(code = 404, message = "Data not found", response = ErrorResponse.class)})
    public Response getData(
            @ApiParam(value = "Data URI", /*example = "platform-data:irrigation",*/ required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        DataDAO dao = new DataDAO(nosql,sparql, fs);
        
        try {
            DataModel model = dao.get(uri);
            return new SingleObjectResponse<>(DataGetDTO.fromModel(model)).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", uri);
        }
        
    }
    
    @GET
    @ApiOperation("Search data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return data list", response = DataGetDTO.class, responseContainer = "List")
    })
    public Response searchDataList(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") List<URI> experiments,
            @ApiParam(value = "Search by objects uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("scientific_objects") List<URI> objects,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE_MAX) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        
        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;
        
        if (startDate != null) {
            try  {
                startInstant = DataValidateUtils.getDateInstant(startDate, timezone, Boolean.FALSE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }          
        }
        
        if (endDate != null) {
            try {
                endInstant = DataValidateUtils.getDateInstant(endDate, timezone, Boolean.TRUE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }
                    
        Document metadataFilter = null;
        if (metadata != null) {
            try {
                metadataFilter = Document.parse(metadata);
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "METADATA_PARAM_ERROR", "unable to parse metadata")
            .getResponse();
            }
        }        
        
        ListWithPagination<DataModel> resultList = dao.search(
                user,
                experiments,
                objects,
                variables,
                provenances,
                startInstant,
                endInstant,
                confidenceMin,
                confidenceMax,
                metadataFilter,
                orderByList,
                page,
                pageSize
        );
               
        ListWithPagination<DataGetDTO> resultDTOList = resultList.convert(DataGetDTO.class, DataGetDTO::fromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete data")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_DELETE_ID, credentialLabelKey = CREDENTIAL_DATA_DELETE_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Data deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Data URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteData(
            @ApiParam(value = "Data URI", example = DATA_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri) 
            throws Exception {
        try {
            DataDAO dao = new DataDAO(nosql,sparql, fs);        
            dao.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse(); 
        } catch (NoSQLInvalidURIException e){
            throw new NotFoundURIException("Invalid or unknown data URI ", uri);
        }
    }
    
    @PUT
    @Path("{uri}/confidence")
    @ApiProtected
    @ApiOperation("Update confidence index")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Confidence update", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response updateConfidence(
            @ApiParam("Data description") @Valid DataConfidenceDTO dto,
            @ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);  
        try {
            DataModel data = dao.get(uri);
            data.setConfidence(dto.getConfidence());
            dao.update(data);
            return new ObjectUriResponse(Response.Status.OK, data.getUri()).getResponse();
        } catch (NoSQLInvalidURIException e){
            throw new NotFoundURIException("Invalid or unknown data URI ", uri);
        }
        
    }
    
    @PUT
    @ApiProtected
    @ApiOperation("Update data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Confidence update", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response update(
            @ApiParam("Data description") @Valid DataUpdateDTO dto
            //@ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        
        DataDAO dao = new DataDAO(nosql, sparql, fs);            

        try {
            DataModel model = dto.newModel();
            validData(Arrays.asList(model));
            dao.update(model);
            return new SingleObjectResponse<>(DataGetDTO.fromModel(model)).getResponse();
        } catch (NoSQLInvalidURIException e){
            throw new NotFoundURIException("Invalid or unknown data URI ", dto.getUri());
        } catch (MongoBulkWriteException e) {                   
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", e.getMessage())
            .getResponse();            
        } catch (MongoCommandException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", e.getErrorMessage())
            .getResponse();
        } catch (DateValidationException e) {
           return new DateMappingExceptionResponse().toResponse(e);
        }
    }
 
    @DELETE
    @ApiOperation("Delete data on criteria")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_DELETE_ID, credentialLabelKey = CREDENTIAL_DATA_DELETE_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Data deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Data URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteDataOnSearch(
            @ApiParam(value = "Search by experiment uri", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") URI experimentUri,
            @ApiParam(value = "Search by object uri", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("scientific_object") URI objectUri,
            @ApiParam(value = "Search by variable uri", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variable") URI variableUri,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenance") URI provenanceUri
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql,sparql, fs);
        DeleteResult result = dao.deleteWithFilter(user, experimentUri, objectUri, variableUri, provenanceUri);
        return new SingleObjectResponse(result).getResponse(); 
    }

    
    /**
     * Check one data type
     *
     * @param data
     * @throws Exception
     */
    public void checkVariableDataTypes(DataModel data) throws Exception {
        ArrayList<DataModel> dataList = new ArrayList();
        dataList.add(data);
        checkVariableDataTypes(dataList);
    }

    /**
     * Check data list type
     *
     * @param datas
     * @throws Exception
     */
    public void checkVariableDataTypes(List<DataModel> datas) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);
        Map<URI, URI> variableTypes = new HashMap();
        int dataIndex = 0;
        for (DataModel data : datas) {
            checkVariableDatumTypes(dao, variableTypes, data, dataIndex);
            dataIndex++;
        }
    }

    private void checkVariableDatumTypes(VariableDAO dao, Map<URI, URI> variableTypes, DataModel data, int dataIndex) throws Exception {
        if (data.getValue() != null) {
            URI variableUri = data.getVariable();
            if (!variableTypes.containsKey(variableUri)) {
                VariableModel variable = dao.get(data.getVariable());
                if (variable.getDataType() == null) {
                    throw new NoVariableDataTypeException(variableUri);
                } else {
                    variableTypes.put(variableUri, variable.getDataType());
                }
            }
            URI dataType = variableTypes.get(variableUri);

            if (!DataValidateUtils.checkTypeCoherence(dataType, data.getValue())) {
                throw new DataTypeException(variableUri, data.getValue(), dataType, dataIndex);
            }
        }
    }
    
    private void validData(List<DataModel> dataList) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);
        Map<URI, URI> variableTypes = new HashMap();
        Set<URI> variableURIs = new HashSet<>();
        Set<URI> notFoundedVariableURIs = new HashSet<>();
        Set<URI> objectURIs = new HashSet<>();
        Set<URI> notFoundedObjectURIs = new HashSet<>();
        Set<URI> provenanceURIs= new HashSet<>();
        Set<URI> notFoundedProvenanceURIs = new HashSet<>();
        Set<URI> expURIs= new HashSet<>();
        Set<URI> notFoundedExpURIs = new HashSet<>();
        
        int dataIndex = 0;
        for (DataModel data : dataList) {
            // check variable uri and datatype
            if (data.getVariable() != null) {
                if (!variableURIs.contains(data.getVariable())) {
                    variableURIs.add(data.getVariable());
                    VariableModel variable = dao.get(data.getVariable());

                    if (variable == null) {
                        notFoundedVariableURIs.add(data.getVariable());
                    } else {
                        checkVariableDatumTypes(dao, variableTypes, data, dataIndex);
                        dataIndex++;
                    }
                }
            }            
            
            //check objects uri
            if (data.getScientificObject() != null) {
                if (!objectURIs.contains(data.getScientificObject())) {
                    objectURIs.add(data.getScientificObject());
                    if (!sparql.uriExists(ScientificObjectModel.class, data.getScientificObject())) {
                        notFoundedObjectURIs.add(data.getScientificObject());
                    }
                }         
            }
        
            //check provenance urii
            ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql);
            if (!provenanceURIs.contains(data.getProvenance().getUri())) {
                provenanceURIs.add(data.getProvenance().getUri());
                if (!provDAO.provenanceExists(data.getProvenance().getUri())) {
                    notFoundedProvenanceURIs.add(data.getProvenance().getUri());
                }
            }  
            
            // check experiments uri
            if (data.getProvenance().getExperiments() != null) {
                for (URI exp:data.getProvenance().getExperiments()) {
                    if (!expURIs.contains(exp)) {
                        expURIs.add(exp);
                        if (!sparql.uriExists(ExperimentModel.class, exp)) {
                            notFoundedExpURIs.add(exp);
                        }    
                    } 
                }
            }
            
        }      
        
        if (!notFoundedVariableURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong variable uris", new ArrayList<>(variableURIs));
        }
        if (!notFoundedObjectURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong scientific_object uris", new ArrayList<>(objectURIs));
        }
        if (!notFoundedProvenanceURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong provenance uris", new ArrayList<>(provenanceURIs));
        }
        if (!notFoundedExpURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong experiments uris", new ArrayList<>(provenanceURIs));
        }

    }
    
    @POST
    @Path("import")
    @ApiOperation(value = "Import a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = DataCSVValidationDTO.class)})
    @ApiProtected
    @ApiCredential(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY,
        credentialId = CREDENTIAL_DATA_MODIFICATION_ID, 
        credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSVData(
            @ApiParam(value = "Experiment URIs", example = EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") @NotNull @ValidURI List<URI> xpUris,
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);

        // test exp
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);        
        Set<URI> userExp = xpDAO.getUserExperiments(user);
        if (!userExp.containsAll(xpUris)) {
            throw new Exception("no access to all the experiments");
        }

        // test prov
        ProvenanceModel provenanceModel = null;
        ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql); 
        try {
            provenanceModel = provDAO.get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        } 

        DataCSVValidationModel validation;
        
        validation = validateWholeCSV(xpUris, provenanceModel, file, user);

        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();

        validation.setInsertionStep(true);
        validation.setValidCSV(!validation.hasErrors()); 
        validation.setNbLinesToImport(validation.getData().size()); 

        if (validation.isValidCSV()) {
            Instant start = Instant.now();
            try {
                dao.createAll(new ArrayList<>(validation.getData().keySet()));
                validation.setNbLinesImported(validation.getData().keySet().size());
            } catch (NoSQLTooLargeSetException ex) {
                validation.setTooLargeDataset(true);

            } catch (MongoBulkWriteException duplicateError) {
                List<BulkWriteError> bulkErrors = duplicateError.getWriteErrors();
                for (int i = 0; i < bulkErrors.size(); i++) {
                    int index = bulkErrors.get(i).getIndex();
                    ArrayList<DataModel> arrayList = new ArrayList<>(validation.getData().keySet());
                    DataGetDTO fromModel = DataGetDTO.fromModel(arrayList.get(index));
                    int variableIndex = validation.getHeaders().indexOf(fromModel.getVariable().toString());
                    String variableName = validation.getHeadersLabels().get(variableIndex) + '(' + validation.getHeaders().get(variableIndex) + ')';
                    CSVCell csvCell = new CSVCell(validation.getData().get(arrayList.get(index)), validation.getHeaders().indexOf(fromModel.getVariable().toString()), fromModel.getValue().toString(), variableName);
                    validation.addDuplicatedDataError(csvCell);
                }
            } catch (MongoCommandException e) {
                CSVCell csvCell = new CSVCell(-1, -1, "Unknown value", "Unknown variable");
                validation.addDuplicatedDataError(csvCell);
            } catch (DataTypeException e) {
                int indexOfVariable = validation.getHeaders().indexOf(e.getVariable().toString());
                String variableName = validation.getHeadersLabels().get(indexOfVariable) + '(' + validation.getHeaders().get(indexOfVariable) + ')';
                validation.addInvalidDataTypeError(new CSVCell(e.getDataIndex(), indexOfVariable, e.getValue().toString(), variableName));
            }
            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();
            LOGGER.debug("Insertion " + Long.toString(timeElapsed) + " milliseconds elapsed");
            
            validation.setValidCSV(!validation.hasErrors());
        }
        csvValidation.setDataErrors(validation); 
        return new SingleObjectResponse<>(csvValidation).getResponse();
    }
    
    @POST
    @Path("import_validation")
    @ApiOperation(value = "Import a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = DataCSVValidationDTO.class)})
    @ApiProtected
    @ApiCredential(
            groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
            groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @ApiParam(value = "Experiment URIs", example = EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") @NotNull @ValidURI List<URI> xpUris,
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        // test exp
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);        
        Set<URI> userExp = xpDAO.getUserExperiments(user);
        if (!userExp.containsAll(xpUris)) {
            throw new Exception("no access to all the experiments");
        }

        // test prov
        ProvenanceModel provenanceModel = null;

        ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql);
        try {
            provenanceModel = provDAO.get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        }

        DataCSVValidationModel validation;

        Instant start = Instant.now();
        validation = validateWholeCSV(xpUris, provenanceModel, file, user);
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        LOGGER.debug("Validation " + Long.toString(timeElapsed) + " milliseconds elapsed");

        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();

        validation.setValidCSV(!validation.hasErrors());
        validation.setValidationStep(true);
        validation.setNbLinesToImport(validation.getData().size());
        csvValidation.setDataErrors(validation);

        return new SingleObjectResponse<>(csvValidation).getResponse();
    }
    
    private DataCSVValidationModel validateWholeCSV(List<URI> experimentURIs, ProvenanceModel provenance, InputStream file, UserModel currentUser) throws Exception {
        DataCSVValidationModel csvValidation = new DataCSVValidationModel();
        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);

        List<URI> scientificObjectsNotInDB = new ArrayList<>();

        Map<Integer, String> headerByIndex = new HashMap<>();

        List<ImportDataIndex> duplicateDataByIndex = new ArrayList<>(); 

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import data - CSV format => \n '" + csvReader.getDetectedFormat()+ "'");

            // Line 1
            String[] ids = csvReader.parseNext();

            // 1. check variables
            HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();
 
            VariableDAO dao = new VariableDAO(sparql);
            if (ids != null) {

                for (int i = 0; i < ids.length; i++) {
                    if (i > 1) {
                        String header = ids[i];
                        try {
                            if (header == null || !URIDeserializer.validateURI(header)) {
                                csvValidation.addInvalidHeaderURI(i, header);
                            } else {
                                VariableModel var = dao.get(URI.create(header));
                                // boolean uriExists = sparql.uriExists(VariableModel.class, URI.create(header));
                                if (var == null) {
                                    csvValidation.addInvalidHeaderURI(i, header);
                                } else {
                                    mapVariableUriDataType.put(var.getUri(), var.getDataType());
                                    // TODO : Validate duplicate variable colonne
                                    headerByIndex.put(i, header);
                                }
                            }
                        } catch (URISyntaxException e) {
                            csvValidation.addInvalidHeaderURI(i, ids[i]);
                        }
                    }
                }

                // 1.1 return error variables
                if (csvValidation.hasErrors()) {
                    return csvValidation;
                }
                csvValidation.setHeadersFromArray(ids);

                int rowIndex = 0;
                String[] values;

                // Line 2
                String[] headersLabels = csvReader.parseNext();
                csvValidation.setHeadersLabelsFromArray(headersLabels);

                // Line 3
                csvReader.parseNext();
                // Line 4
                int nbError = 0;
                boolean validateCSVRow = false;
                while ((values = csvReader.parseNext()) != null) {
                    try {
                        validateCSVRow = validateCSVRow(provenance, values, rowIndex, csvValidation, headerByIndex, experimentURIs, scientificObjectDAO, scientificObjectsNotInDB, mapVariableUriDataType, duplicateDataByIndex);
                    } catch (CSVDataTypeException e) {
                        csvValidation.addInvalidDataTypeError(e.getCsvCell());
                    }
                    rowIndex++;
                    if (!validateCSVRow) {
                        nbError++;
                    }
                    if (nbError >= CSV_NB_ERRORS_MAX) {
                        break;
                    }
                }
            }
        }

        if (csvValidation.getData().keySet().size() >  DataAPI.SIZE_MAX) {
            csvValidation.setTooLargeDataset(true);
        }
        
        return csvValidation;
    }

    private boolean validateCSVRow(ProvenanceModel provenance, String[] values, int rowIndex, DataCSVValidationModel csvValidation, Map<Integer, String> headerByIndex, List<URI> experimentURIs, ScientificObjectDAO scientificObjectDAO, List<URI> scientificObjectsNotInDB, HashMap<URI, URI> mapVariableUriDataType, List<ImportDataIndex> duplicateDataByIndex) throws CSVDataTypeException, TimezoneAmbiguityException, TimezoneException, URISyntaxException, Exception {

        boolean validRow = true;

        ParsedDateTimeMongo parsedDateTimeMongo = null;
        URI objectUri = null;
        for (int colIndex = 0; colIndex < values.length; colIndex++) {            
            
            if (colIndex == 0) {
                String objectUriString = values[colIndex];                
                
                if (objectUriString != null) {
                    objectUri = new URI(objectUriString);

                    if (scientificObjectsNotInDB.contains(objectUri)) {                    
                        CSVCell cell = new CSVCell(rowIndex, colIndex, objectUriString, "OBJECT_ID");
                        csvValidation.addInvalidObjectError(cell);
                        validRow = false;
                        break;  
                        
                    } else {
                        // test not in uri list
                        if (!sparql.uriExists(ScientificObjectModel.class, objectUri)) {   
                            scientificObjectsNotInDB.add(objectUri);
                            CSVCell cell = new CSVCell(rowIndex, colIndex, objectUriString, "OBJECT_ID");
                            csvValidation.addInvalidObjectError(cell);
                            validRow = false;
                            break;                    
                        }

                    }
                }

            } else if (colIndex == 1) {
                // check date
                // TODO : Validate timezone ambiguity
                parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(values[colIndex], null);
                if (parsedDateTimeMongo == null) {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, values[colIndex], "DATE");
                    csvValidation.addInvalidDateError(cell);
                    validRow = false;
                    break;
                }
            } else {
                // If value is not blank and null
                if(!StringUtils.isEmpty(values[colIndex])){
                    
                    DataModel dataModel = new DataModel();
                    DataProvenanceModel provenanceModel = new DataProvenanceModel();
                    provenanceModel.setUri(provenance.getUri());
                    provenanceModel.setExperiments(experimentURIs);
                    dataModel.setDate(parsedDateTimeMongo.getInstant());
                    dataModel.setOffset(parsedDateTimeMongo.getOffset());
                    dataModel.setIsDateTime(parsedDateTimeMongo.getIsDateTime());
                    
                    dataModel.setScientificObject(objectUri);
                    dataModel.setProvenance(provenanceModel);
                    URI varURI = URI.create(headerByIndex.get(colIndex));
                    dataModel.setVariable(varURI);
                    dataModel.setValue(returnValidCSVDatum(varURI, values[colIndex].trim(), mapVariableUriDataType.get(varURI), rowIndex, colIndex, csvValidation));
                    csvValidation.addData(dataModel,rowIndex);
                    // check for duplicate data
                    ImportDataIndex importDataIndex = new ImportDataIndex(parsedDateTimeMongo.getInstant(),varURI, provenance.getUri(), objectUri);
                    if (!duplicateDataByIndex.contains(importDataIndex)) { 
                        duplicateDataByIndex.add(importDataIndex);
                    }else{
                        String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';
                        CSVCell duplicateCell = new CSVCell(rowIndex, colIndex, values[colIndex].trim(), variableName);
                        csvValidation.addDuplicatedDataError(duplicateCell);
                    }    
                } 
            } 
        }
        return validRow;
    }
    
    private Object returnValidCSVDatum(URI variable, Object value, URI dataType, int dataIndex, int colIndex, DataCSVValidationModel csvValidation) throws CSVDataTypeException {
        String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';

        if (Arrays.asList(NAN_VALUES).contains(value)) {
            value = NaN;
        } else if (Arrays.asList(NA_VALUES).contains(value)) {
            return null;
        }

        if (dataType == null) {
            return value;
        } else {
            if (dataType.toString().equals("xsd:integer")) {
                try {
                    return Integer.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                    throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
                }
            }

        }
        if (dataType.toString().equals("xsd:decimal")) {
            try {
                return Double.valueOf(value.toString());
            } catch (NumberFormatException e) {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }
        if (dataType.toString().equals("xsd:boolean")) {
            Boolean toBooleanObject = BooleanUtils.toBooleanObject(value.toString());
            if (Objects.equals(toBooleanObject, Boolean.FALSE) || Objects.equals(toBooleanObject, Boolean.TRUE)) {
                return toBooleanObject;
            } else {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }

        if (dataType.toString().equals("xsd:date")) {
            if (value instanceof String && DataValidateUtils.isDate(value.toString())) {
                return value;
            } else {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }
        if (dataType.toString().equals("xsd:datetime")) {
            if (value instanceof String && DataValidateUtils.isDateTime(value.toString())) {
                return value;
            } else {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }
        if (dataType.toString().equals("xsd:string")) {
            if ((value instanceof String)) {
                return value;
            } else {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }

        return value;
    }
    
        /**
     *
     * @param xpUri experimentUri
     * @param startDate startDate
     * @param endDate endDate
     * @param timezone timezone
     * @param objects objectUri
     * @param variables variableUri
     * @param confidenceMin confidenceMin
     * @param confidenceMax confidenceMax
     * @param provenanceUri provenanceUri
     * @param metadata metadata json filter
     * @param csvFormat long or wide format
     * @param orderByList orderByList
     * @param page page number
     * @param pageSize page size
     * @return
     * @throws Exception
     */
    @GET
    @Path("export")
    @ApiOperation("export data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with data list results in wide or long format"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportData(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by objects", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("scientific_objects") @ValidURI List<URI> objects,
            @ApiParam(value = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") @ValidURI List<URI> variables,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Format wide or long", example = "wide") @DefaultValue("wide") @QueryParam("mode") String csvFormat,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            try {
                startInstant = DataValidateUtils.getDateInstant(startDate, timezone, Boolean.FALSE);
            } catch (UnableToParseDateException e) {
                return new ErrorResponse(e).getResponse();
            } catch (ZoneRulesException e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "WRONG TIMEZONE PARAMETER", e.getMessage())
                        .getResponse();
            }
        }

        if (endDate != null) {
            try {
                endInstant = DataValidateUtils.getDateInstant(endDate, timezone, Boolean.TRUE);
            } catch (UnableToParseDateException e) {
                return new ErrorResponse(e).getResponse();
            } catch (ZoneRulesException e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "WRONG TIMEZONE PARAMETER", e.getMessage())
                        .getResponse();
            }
        }

        Document metadataFilter = null;
        if (metadata != null) {
            try {
                metadataFilter = Document.parse(metadata);
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();
            }
        }
        
        Instant start = Instant.now();
        List<DataModel> resultList = dao.search(user, experiments, objects, variables, provenances, startInstant, endInstant, confidenceMin, confidenceMax, metadataFilter, orderByList);
        Instant data = Instant.now();
        LOGGER.debug(resultList.size() + " observations retrieved " + Long.toString(Duration.between(start, data).toMillis()) + " milliseconds elapsed");

        Response prepareCSVExport = null;

        if (csvFormat.equals("long")) {
            prepareCSVExport = prepareCSVLongExportResponse(resultList);
        } else {
            prepareCSVExport = prepareCSVWideExportResponse(resultList);
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        LOGGER.debug("Export data " + Long.toString(timeElapsed) + " milliseconds elapsed");

        return prepareCSVExport;
    }
        
    private Response prepareCSVWideExportResponse(List<DataModel> resultList) throws Exception {
        Instant data = Instant.now();

        List<URI> variables = new ArrayList<>();

        Map<URI, ScientificObjectModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<Instant, Map<ExportDataIndex, List<DataGetDTO>>> dataByIndexAndInstant = new HashMap<>();

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
            // add export data
            // <Instant => Map<ExportDataIndex(Prov,Object) => List<Data>>
            ExportDataIndex exportDataIndex = new ExportDataIndex(dataModel.getProvenance().getUri(), dataModel.getScientificObject());

            if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
            }
            dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataGetDTO.fromModel(dataModel));
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();

        // first static columns
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

        ProvenanceDAO provenanceDao = new ProvenanceDAO(nosql, sparql);
        List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(scientificObjectTime, provenancesTime).toMillis()) + " milliseconds elapsed");

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

            // Search in map indexed by date for prov, object and data
            // <Instant => Map<ExportDataIndex(Prov,Object) => List<Data>>
            for (Map.Entry<Instant, Map<ExportDataIndex, List<DataGetDTO>>> instantProvUriDataEntry : dataByIndexAndInstant.entrySet()) {
                Map<ExportDataIndex, List<DataGetDTO>> mapProvUriData = instantProvUriDataEntry.getValue();
                // Search in map indexed by  prov and data
                for (Map.Entry<ExportDataIndex, List<DataGetDTO>> provUriObjectEntry : mapProvUriData.entrySet()) {
                    List<DataGetDTO> val = provUriObjectEntry.getValue();

                    // Each row is linked to a provenance 
                    ArrayList<String> csvRow = new ArrayList<>();
                    boolean first = true;
                    for (DataGetDTO dataGetDTO : val) {
                        if (first == true) {
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
                        if(dataGetDTO.getValue() == null){
                            csvRow.set(variableUriIndex.get(dataGetDTO.getVariable()), null);
                        }else{
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

    private Response prepareCSVLongExportResponse(List<DataModel> resultList) throws Exception {
        Instant data = Instant.now();

        HashMap<URI, VariableModel> variables = new HashMap<>();
        HashMap<URI, ScientificObjectModel> objects = new HashMap<>();
        HashMap<URI, ProvenanceModel> provenances = new HashMap<>();

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
            dataByInstant.get(dataModel.getDate()).add(DataGetDTO.fromModel(dataModel));
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();

        defaultColumns.add("Scientific Object");
        defaultColumns.add("Date");
        defaultColumns.add("Variable");
        defaultColumns.add("Method");
        defaultColumns.add("Unit");
        defaultColumns.add("Value");
        defaultColumns.add("Data Description");
        defaultColumns.add("");
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

        ProvenanceDAO provenanceDao = new ProvenanceDAO(nosql, sparql);
        List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(scientificObjectTime, provenancesTime).toMillis()) + " milliseconds elapsed");

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
                    ArrayList<String> csvRow = new ArrayList<>();
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
                    // object uri
                    if(os != null){
                        csvRow.add(os.getUri().toString());
                    }else{
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
