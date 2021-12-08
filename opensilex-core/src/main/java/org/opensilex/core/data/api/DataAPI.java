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
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDFS;
import org.bson.Document;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.CoreModule;
import org.opensilex.core.data.dal.*;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.device.api.DeviceAPI;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.*;
import org.opensilex.core.exception.CSVDataTypeException;
import org.opensilex.core.exception.DateMappingExceptionResponse;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.core.exception.DataTypeException;
import org.opensilex.core.exception.DateValidationException;
import org.opensilex.core.exception.DeviceOrTargetToDataException;
import org.opensilex.core.exception.DeviceProvenanceAmbiguityException;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.exception.NoVariableDataTypeException;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ImportDataIndex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.exceptions.NoSQLTooLargeSetException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sammy
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path("/core/data")
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class DataAPI {

    public static final String PATH = "/core/data";

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
    public static final String DATA_EXAMPLE_MINIMAL_DATE = "2020-08-21T00:00:00+01:00";
    public static final String DATA_EXAMPLE_MAXIMAL_DATE = "2020-09-21T00:00:00+01:00";
    public static final String DATA_EXAMPLE_TIMEZONE = "Europe/Paris";
    public static final String DATA_EXAMPLE_METADATA = "{ \"LabelView\" : \"side90\",\n" +
            "\"paramA\" : \"90\"}";

    public static final String CREDENTIAL_DATA_MODIFICATION_ID = "data-modification";
    public static final String CREDENTIAL_DATA_MODIFICATION_LABEL_KEY = "credential.data.modification";

    public static final String CREDENTIAL_DATA_DELETE_ID = "data-delete";
    public static final String CREDENTIAL_DATA_DELETE_LABEL_KEY = "credential.data.delete";
    public static final int SIZE_MAX = 50000;
    
    Map<URI, URI> rootDeviceTypes = null;
    private Map<DeviceModel, List<URI>> variablesToDevices = new HashMap<>();
    
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
            @ApiParam("Data description") @Valid @NotNull @NotEmpty List<DataCreationDTO> dtoList
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        List<DataModel> dataList = new ArrayList<>();
        try {
            if (dtoList.size() > SIZE_MAX) {
                throw new NoSQLTooLargeSetException(SIZE_MAX, dtoList.size());
            }

            for (DataCreationDTO dto : dtoList) {
                DataModel model = dto.newModel();
                dataList.add(model);
            }
            
            dataList = validData(dataList);

            dataList = (List<DataModel>) dao.createAll(dataList);
            if(variablesToDevices.size() > 0) {
               
                DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
                for (Map.Entry variablesToDevice : variablesToDevices.entrySet() ){
                    
                    deviceDAO.associateVariablesToDevice((DeviceModel) variablesToDevice.getKey(),(List<URI>)variablesToDevice.getValue(), user );
                    
                }
            }
            // do the  variable/device associations here ..
            
            List<URI> createdResources = new ArrayList<>();
            for (DataModel data : dataList) {
                createdResources.add(data.getUri());
            }
            return new ObjectUriResponse(Response.Status.CREATED, createdResources).getResponse();

        } catch (NoSQLTooLargeSetException ex) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DATA_SIZE_LIMIT",
                    ex.getMessage()).getResponse();

        } catch (MongoBulkWriteException duplicateError) {
            List<DataCreationDTO> datas = new ArrayList();
            List<BulkWriteError> errors = duplicateError.getWriteErrors();
            for (int i = 0; i < errors.size(); i++) {
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
        DataDAO dao = new DataDAO(nosql, sparql, fs);

        try {
            DataModel model = dao.get(uri);
            return new SingleObjectResponse<>(DataGetDTO.getDtoFromModel(model)).getResponse();
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
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> objects,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE_MAX) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
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
                devices,
                startInstant,
                endInstant,
                confidenceMin,
                confidenceMax,
                metadataFilter,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<DataGetDTO> resultDTOList = resultList.convert(DataGetDTO.class, DataGetDTO::getDtoFromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }


    @GET
    @Path("/count")
    @ApiOperation("Count data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return the number of data ", response = Integer.class)
    })
    public Response countData(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by target uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> objects,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE_MAX) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);

        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            try {
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

        int count = dao.count(
                user,
                experiments,
                objects,
                variables,
                provenances,
                devices,
                startInstant,
                endInstant,
                confidenceMin,
                confidenceMax,
                metadataFilter
        );
        return new SingleObjectResponse<>(count).getResponse();
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
            DataDAO dao = new DataDAO(nosql, sparql, fs);
            dao.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        } catch (NoSQLInvalidURIException e) {
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
        } catch (NoSQLInvalidURIException e) {
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
            validData(Collections.singletonList(model));
            dao.update(model);
            return new SingleObjectResponse<>(DataGetDTO.getDtoFromModel(model)).getResponse();
        } catch (NoSQLInvalidURIException e) {
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
            @ApiParam(value = "Search by target uri", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("target") URI objectUri,
            @ApiParam(value = "Search by variable uri", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variable") URI variableUri,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenance") URI provenanceUri
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        DeleteResult result = dao.deleteWithFilter(user, experimentUri, objectUri, variableUri, provenanceUri);
        return new SingleObjectResponse<>(result).getResponse();
    }



    /** 
     * check that value is coherent with the variable datatype 
     *
     * @param variable
     * @param data
     * @param dataIndex
     * @throws Exception
     */
    private void checkVariableDatumTypes(VariableModel variable, DataModel data, int dataIndex) throws Exception {
        if (data.getValue() != null) {
            URI variableUri = variable.getUri();
            URI dataType = variable.getDataType();
            Object value = data.getValue(); 
            if (!DataValidateUtils.checkTypeCoherence(dataType, value)) {
                throw new DataTypeException(variableUri, value, dataType, dataIndex);
            }
        }
    }
    
    
    /** 
     * First check in the data provenance if there is a device 
     * Then check in the provenance and fill the data provenance with the device
     * If no device and no target return an exception
     *
     * @param provDAO
     * @param data
     * @param hasTarget 
     * @param variableCheckedDevice 
     * @param provenanceToDevice 

     * @throws Exception
     */
    private void variablesDeviceAssociation(ProvenanceDAO provDAO, DataModel data, boolean hasTarget, Map<DeviceModel, URI> variableCheckedDevice, Map<URI, DeviceModel> provenanceToDevice) throws Exception{
        
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        URI provenanceURI = data.getProvenance().getUri();
        DeviceModel deviceFromProvWasAssociated = checkAndReturnDeviceFromDataProvenance(data, deviceDAO); 
        if (deviceFromProvWasAssociated == null) {
            
            DeviceModel device = null;
            if(provenanceToDevice.containsKey(provenanceURI)) {
               device = provenanceToDevice.get(provenanceURI);
               //check
            } else {
                device = checkAndReturnDeviceFromProvenance(deviceDAO, provDAO, data);
                provenanceToDevice.put(provenanceURI,device);
            }
          
            if (device != null) {
                
                if (!variableIsAssociatedToDevice(device, data.getVariable())) {
                    addVariableToDevice(device,data.getVariable()); // add variable/device
                }
                
                if (rootDeviceTypes == null) {
                    rootDeviceTypes = getRootDeviceTypes();
                }

                DataProvenanceModel provMod = data.getProvenance();
                List<ProvEntityModel> agents = null;
                if (provMod.getProvWasAssociatedWith() == null) {
                    agents = new ArrayList<>();
                } else {
                    agents = provMod.getProvWasAssociatedWith();
                }

                ProvEntityModel agent = new ProvEntityModel();
                agent.setUri(device.getUri());
                URI rootType = rootDeviceTypes.get(device.getType()); 
                agent.setType(rootType);
                agents.add(agent);
                provMod.setProvWasAssociatedWith(agents);
                data.setProvenance(provMod);
            } else {
                if(!hasTarget) {
                   throw new DeviceOrTargetToDataException(data);
                }
            } 
           
        } else {
            boolean deviceIsChecked = variableCheckedDevice.containsKey(deviceFromProvWasAssociated) && variableCheckedDevice.get(deviceFromProvWasAssociated) == data.getVariable() ;
            if(!deviceIsChecked){
                if (!variableIsAssociatedToDevice(deviceFromProvWasAssociated, data.getVariable())) {
                    addVariableToDevice(deviceFromProvWasAssociated,data.getVariable()); // add variable/device
                }
                variableCheckedDevice.put(deviceFromProvWasAssociated,data.getVariable());
                
            }
        }
    }
    
    /** 
     * check and return Device from Provenance if no ambiguity
     *
     * @param deviceDAO
     * @param provDAO
     * @param data
     * @throws Exception
     */
    private DeviceModel checkAndReturnDeviceFromProvenance(DeviceDAO deviceDAO, ProvenanceDAO provDAO, DataModel data) throws Exception {

       ProvenanceModel provenance = provDAO.get(data.getProvenance().getUri());

       DeviceModel deviceToReturn = null ;
       List<DeviceModel> devices = new ArrayList<>();
       List<DeviceModel> linkedDevices = new ArrayList<>();
                               
       if (provenance.getAgents() != null && !provenance.getAgents().isEmpty()) {
           
            for (AgentModel agent : provenance.getAgents()) {
                if(agent.getRdfType() == null) {
                     throw new ProvenanceAgentTypeException(agent.getUri().toString());
                }
                if (deviceDAO.isDeviceType(agent.getRdfType())) {
                    DeviceModel device = deviceDAO.getDeviceByURI(agent.getUri(), user);
                    if (device != null) {
                        devices.add(device);
                       
                        if(variableIsAssociatedToDevice(device, data.getVariable())){
                            linkedDevices.add(device);
                        }
                    }
                }
            }
           
            switch (linkedDevices.size()) {
                case 0:
                    if (devices.size() > 1) {
                        throw new DeviceProvenanceAmbiguityException(provenance.getUri().toString());
                    } else {
                        if (!devices.isEmpty()) {
                            deviceToReturn = devices.get(0);
                        }
                    }
                    break;
                case 1:
                    deviceToReturn = linkedDevices.get(0);
                    break;
                default :
                    throw new DeviceProvenanceAmbiguityException(provenance.getUri().toString());

            }    
            
        }
        return deviceToReturn;
    }
    
    /** 
     * check and return Device from Data Provenance if no ambiguity
     * Exception if two devices as provenance agent
     *
     * @param deviceDAO
     * @param provDAO
     * @param data
     * @throws Exception
     */
    private DeviceModel checkAndReturnDeviceFromDataProvenance(DataModel data, DeviceDAO deviceDAO) throws Exception{
        
        
       boolean deviceIsLinked = false; // to test if there are 2 devices
       URI agentToReturn = null;
       DeviceModel device = null;
       if(data.getProvenance().getProvWasAssociatedWith()!= null && !data.getProvenance().getProvWasAssociatedWith().isEmpty()){
            for (ProvEntityModel agent : data.getProvenance().getProvWasAssociatedWith()) {
                
                    if(agent.getType() == null) {
                        throw new ProvenanceAgentTypeException(agent.getUri().toString());
                    }
                    
                    if (deviceDAO.isDeviceType(agent.getType())) {
                        if(!deviceIsLinked) {
                            deviceIsLinked = true;
                            agentToReturn = agent.getUri();

                        } else {
                            throw new DeviceProvenanceAmbiguityException(data.getProvenance().getUri().toString());
                        }
                    }
            }
            if(agentToReturn != null){
                device = deviceDAO.getDeviceByURI(agentToReturn, user);
            }
        }
        return device;
    }
    
    
    /** 
     * check if variable is associated to device
     * @param device
     * @param variable
     * @throws Exception
     */
    private boolean variableIsAssociatedToDevice(DeviceModel device, URI variable){
        List<SPARQLModelRelation> variables = device.getRelations(Oeso.measures).collect(Collectors.toList());

        if (!variables.isEmpty()) {
            if (variables.stream().anyMatch(var -> (SPARQLDeserializers.compareURIs(var.getValue(), variable.toString())))) {
                return true;
            }

        }
        return false;
        
    }
    
     
    public Map<DeviceModel, List<URI>> getVariablesToDevices() {
        return variablesToDevices;
    }

    public void setVariablesToDevices(Map<DeviceModel,  List<URI>> variablesToDevices) {
        this.variablesToDevices = variablesToDevices;
    }
    
    public void addVariableToDevice(DeviceModel device, URI variable) {
        
        if (!variablesToDevices.containsKey(device)) {
            List<URI> list = new ArrayList<>();
            list.add(variable);
            variablesToDevices.put(device, list);
        } else {
            if (!variablesToDevices.get(device).contains(variable)) {
                variablesToDevices.get(device).add(variable);
            }
        }
    }

    
    /**
     * Check variable data list before creation
     * Complete the prov_was_associated_with provenance attribut
     *
     * @param dataList
     * @throws Exception
     */
    private List<DataModel> validData(List<DataModel> dataList) throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql,nosql,fs);

        Map<URI, VariableModel> variableURIs = new HashMap<>();
        Set<URI> notFoundedVariableURIs = new HashSet<>();
        Set<URI> targetURIs = new HashSet<>();
        Set<URI> notFoundedTargetURIs = new HashSet<>();
        Set<URI> provenanceURIs = new HashSet<>();
        Set<URI> notFoundedProvenanceURIs = new HashSet<>();
        Set<URI> expURIs = new HashSet<>();
        Set<URI> notFoundedExpURIs = new HashSet<>();
        Map<DeviceModel, URI> variableCheckedDevice =  new HashMap<>();
        Map<URI, DeviceModel> provenanceToDevice =  new HashMap<>();
        
        List<DataModel> validData = new ArrayList<>();
        int dataIndex = 0;
        for (DataModel data : dataList) {
            
            boolean hasTarget = false;
            // check variable uri and datatype
            if (data.getVariable() != null) {  // and if null ?
                VariableModel variable = null;
                URI variableURI = data.getVariable();
                if (!variableURIs.containsKey(variableURI)) {
                    variable = variableDAO.get(variableURI);  
                    if (variable == null) {
                        notFoundedVariableURIs.add(variableURI);
                    } else {
                        if (variable.getDataType() == null) {
                            throw new NoVariableDataTypeException(variableURI);
                        }
                        variableURIs.put(variableURI, variable);
                    }
                } else {
                    variable = variableURIs.get(variableURI);
                    
                }
                if(!notFoundedVariableURIs.contains(variableURI)) {
                    checkVariableDatumTypes(variable, data, dataIndex); 
                    dataIndex++;
                }
                
              
            }
            

            //check targets uri
            if (data.getTarget() != null) {
                hasTarget = true ;
                if (!targetURIs.contains(data.getTarget())) {
                    targetURIs.add(data.getTarget());
                    if (!sparql.uriExists((Node) null, data.getTarget())) {
                        hasTarget = false ;
                        notFoundedTargetURIs.add(data.getTarget()); 
                    }
                }
            }

            //check provenance uri and variables device association
            ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql);
            if (!provenanceURIs.contains(data.getProvenance().getUri())) {
                provenanceURIs.add(data.getProvenance().getUri());
                if (!provDAO.provenanceExists(data.getProvenance().getUri())) {  
                    notFoundedProvenanceURIs.add(data.getProvenance().getUri());
                } 
            }
            
            if(!notFoundedProvenanceURIs.contains(data.getProvenance().getUri())){
                variablesDeviceAssociation(provDAO, data, hasTarget, variableCheckedDevice, provenanceToDevice);
            }

            // check experiments uri
            if (data.getProvenance().getExperiments() != null) {
                for (URI exp : data.getProvenance().getExperiments()) {
                    if (!expURIs.contains(exp)) {
                        expURIs.add(exp);
                        if (!sparql.uriExists(ExperimentModel.class, exp)) {
                            notFoundedExpURIs.add(exp);
                        }
                    }
                }
            }
            
            validData.add(data);

        }

        if (!notFoundedVariableURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong variable uris: ", new ArrayList<>(notFoundedVariableURIs));// NOSQL Exception ? come from sparql request
        }
        if (!notFoundedTargetURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong target uris", new ArrayList<>(notFoundedTargetURIs)); // NOSQL Exception ? come from sparql request
        }
        if (!notFoundedProvenanceURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong provenance uris: ", new ArrayList<>(notFoundedProvenanceURIs)); 
        }
        if (!notFoundedExpURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong experiments uris: ", new ArrayList<>(notFoundedExpURIs)); // NOSQL Exception ? come from sparql request
        }
        
        return validData;

    }

    /**
     * @param startDate     startDate
     * @param endDate       endDate
     * @param timezone      timezone
     * @param experiments   experimentUris
     * @param objects       objectUris
     * @param variables     variableUris
     * @param confidenceMin confidenceMin
     * @param confidenceMax confidenceMax
     * @param provenances   provenanceUris
     * @param metadata      metadata json filter
     * @param csvFormat     long or wide format
     * @param orderByList   orderByList
     * @param page          page number
     * @param pageSize      page size
     * @return
     * @throws Exception
     */
    @GET
    @Path("export")
    @ApiOperation("Export data")
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
            @ApiParam(value = "Search by targets", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> objects,
            @ApiParam(value = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Format wide or long", example = "wide") @DefaultValue("wide") @QueryParam("mode") String csvFormat,
            @ApiParam(value = "Export also raw_data") @DefaultValue("false") @QueryParam("with_raw_data") boolean withRawData,
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
        List<DataModel> resultList = dao.search(user, experiments, objects, variables, provenances, devices, startInstant, endInstant, confidenceMin, confidenceMax, metadataFilter, orderByList);
        Instant data = Instant.now();
        LOGGER.debug(resultList.size() + " observations retrieved " + Long.toString(Duration.between(start, data).toMillis()) + " milliseconds elapsed");

        Response prepareCSVExport = null;

        if (csvFormat.equals("long")) {
            prepareCSVExport = dao.prepareCSVLongExportResponse(resultList, user, withRawData);
        } else {
            prepareCSVExport = dao.prepareCSVWideExportResponse(resultList, user, withRawData);
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        LOGGER.debug("Export data " + Long.toString(timeElapsed) + " milliseconds elapsed");

        return prepareCSVExport;
    }
    
    @GET
    @Path("provenances")
    @ApiOperation("Get provenances linked to data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getUsedProvenances(
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> objects,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices
    ) throws Exception {
        
        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        Set<URI> provenanceURIs = dataDAO.getDataProvenances(user, experiments, objects, variables, devices);

        ProvenanceDAO provenanceDAO = new ProvenanceDAO(nosql, sparql);
        List<ProvenanceModel> resultList = provenanceDAO.getListByURIs(new ArrayList<>(provenanceURIs));
        List<ProvenanceGetDTO> resultDTOList = new ArrayList<>();
        
        resultList.forEach(result -> {
            resultDTOList.add(ProvenanceGetDTO.fromModel(result));
        });
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    @GET
    @Path("variables")
    @ApiOperation("Get variables linked to data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getUsedVariables(
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> objects,
            @ApiParam(value = "Search by provenance uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("provenances") List<URI> provenances
    ) throws Exception {
        
        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        List<VariableModel> variables = dataDAO.getUsedVariables(user, experiments, objects, provenances);
        List<NamedResourceDTO> dtoList = variables.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @POST
    @Path("import")
    @ApiOperation(value = "Import a CSV file for the given provenanceURI")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data are imported", response = DataCSVValidationDTO.class)})
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
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);

        // test prov
        ProvenanceModel provenanceModel = null;
        ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql); 
        try {
            provenanceModel = provDAO.get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        } 

        DataCSVValidationModel validation;
        
        validation = validateWholeCSV(provenanceModel, file, user);

        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();

        validation.setInsertionStep(true);
        validation.setValidCSV(!validation.hasErrors()); 
        validation.setNbLinesToImport(validation.getData().size()); 

        if (validation.isValidCSV()) {
            Instant start = Instant.now();
            List<DataModel> data = new ArrayList<>(validation.getData().keySet());
            try {
                dao.createAll(data);
                
                if(!validation.getVariablesToDevices().isEmpty()){
                    DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
                    for (Map.Entry variablesToDevice : validation.getVariablesToDevices().entrySet() ){
                        deviceDAO.associateVariablesToDevice((DeviceModel) variablesToDevice.getKey(),(List<URI>)variablesToDevice.getValue(), user );
                    }
                }
                
                validation.setNbLinesImported(data.size());
            } catch (NoSQLTooLargeSetException ex) {
                validation.setTooLargeDataset(true);

            } catch (MongoBulkWriteException duplicateError) {
                List<BulkWriteError> bulkErrors = duplicateError.getWriteErrors();
                for (int i = 0; i < bulkErrors.size(); i++) {
                    int index = bulkErrors.get(i).getIndex();
                    DataGetDTO fromModel = DataGetDTO.getDtoFromModel(data.get(index));
                    int variableIndex = validation.getHeaders().indexOf(fromModel.getVariable().toString());
                    String variableName = validation.getHeadersLabels().get(variableIndex) + '(' + validation.getHeaders().get(variableIndex) + ')';
                    CSVCell csvCell = new CSVCell(validation.getData().get(data.get(index)), validation.getHeaders().indexOf(fromModel.getVariable().toString()), fromModel.getValue().toString(), variableName);
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
    @ApiOperation(value = "Import a CSV file for the given provenanceURI.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data are validated", response = DataCSVValidationDTO.class)})
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
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {

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
        validation = validateWholeCSV(provenanceModel, file, user);
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
    
    private final String expHeader = "experiment";
    private final String targetHeader = "target";
    private final String dateHeader = "date";
    private final String deviceHeader = "device";
    private final String rawdataHeader = "raw_data";
    
    private DataCSVValidationModel validateWholeCSV(ProvenanceModel provenance, InputStream file, UserModel currentUser) throws Exception {       
        DataCSVValidationModel csvValidation = new DataCSVValidationModel();
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        Map<String, SPARQLNamedResourceModel> nameURITargets = new HashMap<>();
        List<String> notExistingTargets = new ArrayList<>();
        List<String> duplicatedTargets = new ArrayList<>();
        
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        Map<String, ExperimentModel> nameURIExperiments = new HashMap<>();
        List<String> notExistingExperiments = new ArrayList<>();
        List<String> duplicatedExperiments = new ArrayList<>();
        
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        Map<String,DeviceModel> nameURIDevices = new HashMap<>();
        List<String> notExistingDevices = new ArrayList<>();
        List<String> duplicatedDevices = new ArrayList<>();
        
        List<AgentModel> agents = provenance.getAgents();
        Boolean hasDevice = false;
        if (agents !=  null) {
            for (AgentModel agent:agents) {
                if (agent.getRdfType() != null && deviceDAO.isDeviceType(agent.getRdfType())) { 
                    hasDevice = true;
                    break;
                }
            }
        }
        
        Map<String,DeviceModel> variableCheckedProvDevice =  new HashMap<>();
        List<String> checkedVariables = new ArrayList<>();
        
        Map<String, DeviceModel> variableCheckedDevice = new HashMap<>();

        Map<Integer, String> headerByIndex = new HashMap<>();

        List<ImportDataIndex> duplicateDataByIndex = new ArrayList<>();

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import data - CSV format => \n '" + csvReader.getDetectedFormat()+ "'");

            // Line 1
            String[] ids = csvReader.parseNext();
            Set<String> headers = Arrays.stream(ids).filter(Objects::nonNull).map(id -> id.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());
            if (!headers.contains(deviceHeader) && !headers.contains(targetHeader) && !hasDevice) {
                csvValidation.addMissingHeaders(Arrays.asList(deviceHeader + " or " + targetHeader));
            }  
            
            // 1. check variables
            HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();
            VariableDAO dao = new VariableDAO(sparql,nosql,fs);

            if (ids != null) {

                for (int i = 0; i < ids.length; i++) {
                    String header = ids[i];
                    if (header == null) {
                        csvValidation.addEmptyHeader(i+1);
                    } else {                       
                    
                        if (header.equalsIgnoreCase(expHeader) || header.equalsIgnoreCase(targetHeader) 
                            || header.equalsIgnoreCase(dateHeader) || header.equalsIgnoreCase(deviceHeader)
                            || header.equalsIgnoreCase(rawdataHeader)) {
                            headerByIndex.put(i, header);                            
                        
                        } else {                        
                            try {
                                if (!URIDeserializer.validateURI(header)) {
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
                        validateCSVRow = validateCSVRow(
                                provenance,
                                hasDevice,
                                variableCheckedDevice,
                                variableCheckedProvDevice,
                                checkedVariables,
                                values, 
                                rowIndex, 
                                csvValidation, 
                                headerByIndex, 
                                xpDAO,
                                notExistingExperiments,
                                duplicatedExperiments,
                                nameURIExperiments,                                
                                ontologyDAO,
                                notExistingTargets,
                                duplicatedTargets,
                                nameURITargets,
                                deviceDAO,
                                notExistingDevices,
                                duplicatedDevices,
                                nameURIDevices,
                                mapVariableUriDataType, 
                                duplicateDataByIndex);
                    } catch (CSVDataTypeException e) {
                        csvValidation.addInvalidDataTypeError(e.getCsvCell());
                    }
                    rowIndex++;
                    if (!validateCSVRow) {
                        nbError++;
                    }
                    if (nbError >= ExperimentAPI.CSV_NB_ERRORS_MAX) {
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

    private boolean validateCSVRow(
            ProvenanceModel provenance, 
            Boolean hasDevice,
            Map<String, DeviceModel> variableCheckedDevice,
            Map<String,DeviceModel> variableCheckedProvDevice,
            List<String> checkedVariables,
            String[] values, 
            int rowIndex, 
            DataCSVValidationModel csvValidation, 
            Map<Integer, String> headerByIndex, 
            ExperimentDAO xpDAO,
            List<String> notExistingExperiments,
            List<String> duplicatedExperiments,
            Map<String, ExperimentModel> nameURIExperiments,
            OntologyDAO ontologyDAO, 
            List<String> notExistingTargets,
            List<String> duplicatedTargets,
            Map<String, SPARQLNamedResourceModel> nameURITargets,
            DeviceDAO deviceDAO, 
            List<String> notExistingDevices,
            List<String> duplicatedDevices,
            Map<String, DeviceModel> nameURIDevices,
            HashMap<URI, URI> mapVariableUriDataType, 
            List<ImportDataIndex> duplicateDataByIndex) 
        throws CSVDataTypeException, TimezoneAmbiguityException, TimezoneException, URISyntaxException, Exception {
        
        boolean validRow = true;

        ParsedDateTimeMongo parsedDateTimeMongo = null;
        
        List<ProvEntityModel> agents = new ArrayList<>();
        List<URI> experiments = new ArrayList<>();
        SPARQLNamedResourceModel target = null;
        
        Boolean missingTargetOrDevice = false;
        int targetColIndex = 0;
        int deviceColIndex = 0;
        
        DeviceModel device = null; 
        for (int colIndex = 0; colIndex < values.length; colIndex++) {  
            if (headerByIndex.get(colIndex).equalsIgnoreCase(expHeader)) {
                //check experiment column
                ExperimentModel exp = null;
                String expNameOrUri = values[colIndex];
                // test in uri list
                if (!StringUtils.isEmpty(expNameOrUri)) {
                    if (nameURIExperiments.containsKey(expNameOrUri)) {
                        exp = nameURIExperiments.get(expNameOrUri);
                    } else {
                        // test not in uri list
                        if (duplicatedExperiments.contains(expNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                            csvValidation.addDuplicateExperimentError(cell);
                            validRow = false;
                            break;
                        }
                        
                        if (!notExistingExperiments.contains(expNameOrUri)) {
                            try {
                                exp = getExperimentByNameOrURI(xpDAO, expNameOrUri);
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                                csvValidation.addDuplicateExperimentError(cell);
                                duplicatedExperiments.add(expNameOrUri);
                                validRow = false;
                                break;
                            }
                        }
                        if (exp == null) {
                            if (!notExistingExperiments.contains(expNameOrUri)) {
                                notExistingExperiments.add(expNameOrUri);
                            }
                           
                            CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                            csvValidation.addInvalidExperimentError(cell);
                            validRow = false;
                            break;
                        } else {
                            nameURIExperiments.put(expNameOrUri, exp);
                        }
                    }
                }
                if (exp != null) {
                    experiments.add(exp.getUri());
                }
                
            
            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(targetHeader)) {
                //check target column
                String targetNameOrUri = values[colIndex];   
                targetColIndex = colIndex;
                
                if (!StringUtils.isEmpty(targetNameOrUri)){
                    if (nameURITargets.containsKey(targetNameOrUri)) {
                        target = nameURITargets.get(targetNameOrUri);
                    } else {
                        // test not in uri list
                        if (duplicatedTargets.contains(targetNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                            csvValidation.addDuplicateObjectError(cell);
                            validRow = false;
                            break;
                        }
                        
                        if (!notExistingTargets.contains(targetNameOrUri)) {
                            try {
                                target = getTargetByNameOrURI(ontologyDAO, targetNameOrUri);
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                                csvValidation.addDuplicateObjectError(cell);
                                duplicatedTargets.add(targetNameOrUri);
                                validRow = false;
                                break;
                            }                            
                        }
                        if (target == null) {
                            if (!notExistingTargets.contains(targetNameOrUri)) {
                               notExistingTargets.add(targetNameOrUri);   
                            }
                           
                            CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                            csvValidation.addInvalidObjectError(cell);
                            validRow = false;
                            break;
                        } else {
                            nameURITargets.put(targetNameOrUri, target);
                        }
                    }
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(dateHeader)) {
                // check date
                // TODO : Validate timezone ambiguity
                parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(values[colIndex], null);
                if (parsedDateTimeMongo == null) {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, values[colIndex], "DATE");
                    csvValidation.addInvalidDateError(cell);
                    validRow = false;
                    break;
                }
            
            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(deviceHeader)){
                // check device column
                String deviceNameOrUri = values[colIndex];
                deviceColIndex = colIndex;
                
                // test in uri list
                if (!StringUtils.isEmpty(deviceNameOrUri)) {
                     if (nameURIDevices.containsKey(deviceNameOrUri)) {                    
                        device = nameURIDevices.get(deviceNameOrUri);                    
                    } else {
                        // test not in uri list
                        if (duplicatedDevices.contains(deviceNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                            csvValidation.addDuplicateDeviceError(cell);
                            validRow = false;
                            break;
                        }
                        
                        if (!notExistingDevices.contains(deviceNameOrUri)) {
                            try {
                                device = getDeviceByNameOrURI(deviceDAO, deviceNameOrUri);
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                                csvValidation.addDuplicateDeviceError(cell);
                                duplicatedDevices.add(deviceNameOrUri);
                                validRow = false;
                                break;
                            }
                        }
                        if (device == null) {
                            if (!notExistingDevices.contains(deviceNameOrUri)) {
                                notExistingDevices.add(deviceNameOrUri);
                            }
                            CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                            csvValidation.addInvalidDeviceError(cell);
                            validRow = false;
                            break;
                        } else {
                            nameURIDevices.put(deviceNameOrUri, device);                        
                        }
                    }    
                }
               
            } else if (!headerByIndex.get(colIndex).equalsIgnoreCase(rawdataHeader)) { // Variable/Value column
                if (headerByIndex.containsKey(colIndex)) {
                    // If value is not blank and null
                    if (!StringUtils.isEmpty(values[colIndex])){ 
                       
                        String variable = headerByIndex.get(colIndex);
                        URI varURI = URI.create(variable);
                        if (!hasDevice && device == null && target == null) {
                            missingTargetOrDevice = true;
                            validRow = false;
                            break;      
                        }
                        if (device != null) {
                            
                            boolean variableIsChecked = variableCheckedDevice.containsKey(variable) && variableCheckedDevice.get(variable) == device ;
                            if(!variableIsChecked){
                                if (!variableIsAssociatedToDevice(device, varURI)) {
                                    csvValidation.addVariableToDevice(device, varURI);
                                }
                                variableCheckedDevice.put(variable, device);
                            }
                    
                        } else if (hasDevice) {
                            if (!checkedVariables.contains(variable)) { // do it one time but write the error on each row if there is one
                                List<DeviceModel> devices = new ArrayList<>();
                                List<DeviceModel> linkedDevice = new ArrayList<>();
                                DeviceModel dev = null;
                                for (AgentModel agent : provenance.getAgents()) {
                                    if (agent.getRdfType() != null && deviceDAO.isDeviceType(agent.getRdfType())) {
                                        dev = deviceDAO.getDeviceByURI(agent.getUri(), user);
                                        if (dev != null) {

                                            if (variableIsAssociatedToDevice(dev, varURI)) {
                                                linkedDevice.add(dev) ;
                                            }
                                            devices.add(dev);
                                        }
                                    }
                                }
                                switch (linkedDevice.size()) {
                                    case 0:
                                        if (devices.size() > 1) {
                                            //wich device to choose ?
                                            CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID");  // add specific exception
                                            csvValidation.addDeviceChoiceAmbiguityError(cell);
                                            validRow = false;
                                        } else {
                                            if (!devices.isEmpty()) {
                                                csvValidation.addVariableToDevice(devices.get(0), varURI);
                                               
                                                variableCheckedProvDevice.put(variable, devices.get(0));
                                            } else {
                                                if(target == null){
                                                    missingTargetOrDevice = true;
                                                    validRow = false;
                                                }
                                                
                                            }

                                        }
                                        break;
                                    case 1:

                                        variableCheckedProvDevice.put(variable, linkedDevice.get(0));
                                        break;
                                        
                                    default :
                                        //witch device to choose ?
                                        CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID"); // add specific exception
                                        csvValidation.addDeviceChoiceAmbiguityError(cell);
                                        validRow = false;
                                        break;

                                }
                                checkedVariables.add(variable);
                                
                            
                            } else {
                                if(!variableCheckedProvDevice.containsKey(variable)){
                                     CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID");  // add specific exception
                                     csvValidation.addDeviceChoiceAmbiguityError(cell);
                                     validRow = false;
                                     break;
                                }
                                
                            }
                            
                        }
                        
                        if (validRow) {
                            DataModel dataModel = new DataModel();
                            DataProvenanceModel provenanceModel = new DataProvenanceModel();
                            provenanceModel.setUri(provenance.getUri());
                            
                            if (!experiments.isEmpty()) {                    
                                provenanceModel.setExperiments(experiments);
                            }
                            
                            if (device != null) {
                                ProvEntityModel agent = new ProvEntityModel();
                                if (rootDeviceTypes == null) {
                                    rootDeviceTypes = getRootDeviceTypes();
                                }
                                URI rootType = rootDeviceTypes.get(device.getType());
                                agent.setType(rootType);
                                agent.setUri(device.getUri());
                                agents.add(agent);
                                provenanceModel.setProvWasAssociatedWith(agents);
                                
                            } else if (hasDevice ) {
                                
                                DeviceModel checkedDevice = variableCheckedProvDevice.get(variable);
                                ProvEntityModel agent = new ProvEntityModel();
                                if (rootDeviceTypes == null) {
                                    rootDeviceTypes = getRootDeviceTypes();
                                }
                                URI rootType = rootDeviceTypes.get(checkedDevice.getType());
                                agent.setType(rootType);
                                agent.setUri(checkedDevice.getUri());
                                agents.add(agent);
                                provenanceModel.setProvWasAssociatedWith(agents);
                                
                            }

                            dataModel.setDate(parsedDateTimeMongo.getInstant());
                            dataModel.setOffset(parsedDateTimeMongo.getOffset());
                            dataModel.setIsDateTime(parsedDateTimeMongo.getIsDateTime());

                            if (target != null) {
                                dataModel.setTarget(target.getUri());
                            }                        
                            dataModel.setProvenance(provenanceModel);
                            dataModel.setVariable(varURI);
                            dataModel.setValue(ExperimentAPI.returnValidCSVDatum(varURI, values[colIndex].trim(), mapVariableUriDataType.get(varURI), rowIndex, colIndex, csvValidation));
                            if (colIndex+1<values.length) {
                                if (headerByIndex.get(colIndex+1).equalsIgnoreCase(rawdataHeader) && values[colIndex+1] != null) {
                                    dataModel.setRawData(ExperimentAPI.returnValidRawData(varURI, values[colIndex+1].trim(), mapVariableUriDataType.get(varURI), rowIndex, colIndex+1, csvValidation));
                                }
                            }

                            // check for duplicate data
                            URI targetUri = null;
                            if (target != null) {
                                targetUri = target.getUri();
                            }
                            ImportDataIndex importDataIndex = new ImportDataIndex(parsedDateTimeMongo.getInstant(),varURI, provenance.getUri(), targetUri);
                            if (!duplicateDataByIndex.contains(importDataIndex)) { 
                                duplicateDataByIndex.add(importDataIndex);
                            } else {
                                String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';
                                CSVCell duplicateCell = new CSVCell(rowIndex, colIndex, values[colIndex].trim(), variableName);
                                csvValidation.addDuplicatedDataError(duplicateCell);
                            }   

                            csvValidation.addData(dataModel,rowIndex);
                        }
                        
                    }
                }
            }
            
            
        }
        
        if (missingTargetOrDevice) {
            //the device or the target is mandatory if there is no device in the provenance
            CSVCell cell1 = new CSVCell(rowIndex, deviceColIndex, null, "DEVICE_ID");
            CSVCell cell2 = new CSVCell(rowIndex, targetColIndex, null, "TARGET_ID");
            csvValidation.addMissingRequiredValue(cell1);
            csvValidation.addMissingRequiredValue(cell2);
        }
        
        return validRow;
    }

    // Map who associate each type with its root type
    private Map<URI, URI> getRootDeviceTypes() throws URISyntaxException, Exception {

        SPARQLTreeListModel<ClassModel> treeList = CoreModule.getOntologyCacheInstance().getSubClassesOf(new URI(Oeso.Device.toString()), null, user.getLanguage(), true);
        List<ResourceTreeDTO> treeDtos = ResourceTreeDTO.fromResourceTree(treeList);

        Map<URI, URI> map = new HashMap<>();

        for (ResourceTreeDTO tree : treeDtos) {
            URI agentRootType = tree.getUri();
            List<ResourceTreeDTO> children = tree.getChildren();
            if (!children.isEmpty()) {
                childrenToRoot(children, map, agentRootType);
            }
        }

        return map;
    }
    private void childrenToRoot( List<ResourceTreeDTO> children,Map<URI, URI> map, URI agentRootType){
        for (ResourceTreeDTO subTree : children) {
            map.put(subTree.getUri(), agentRootType);
            List<ResourceTreeDTO> child = subTree.getChildren();
            if (!child.isEmpty()) {
                childrenToRoot(child, map, agentRootType);
            }
        }
    }

    private ExperimentModel getExperimentByNameOrURI(ExperimentDAO xpDAO, String expNameOrUri) throws Exception {
        ExperimentModel exp;
        if (URIDeserializer.validateURI(expNameOrUri)) {
            URI expUri = URI.create(expNameOrUri);
            exp = xpDAO.get(expUri, user);
        } else {
            exp = xpDAO.getByName(expNameOrUri);         
        }
        return exp;
    }

    private DeviceModel getDeviceByNameOrURI(DeviceDAO deviceDAO, String deviceNameOrUri) throws Exception {
        DeviceModel device;
        if (URIDeserializer.validateURI(deviceNameOrUri)) {
            URI deviceURI = URI.create(deviceNameOrUri);
            device = deviceDAO.getDeviceByURI(deviceURI, user);
        } else {
            device = deviceDAO.getByName(deviceNameOrUri);
        }
        return device;
    }
    
    private SPARQLNamedResourceModel<?> getTargetByNameOrURI(OntologyDAO dao, String targetNameOrUri) throws Exception {
        SPARQLNamedResourceModel<?> target = new SPARQLNamedResourceModel<>();
        if (URIDeserializer.validateURI(targetNameOrUri)) {
            URI targetUri = URI.create(targetNameOrUri);
            if (sparql.executeAskQuery(new AskBuilder()
                    .addWhere(SPARQLDeserializers.nodeURI(targetUri), RDFS.label, "?label")
                    )) {
                target.setUri(targetUri);
            } else {
                target = null;
            }
        } else {
            List<SPARQLNamedResourceModel> results = dao.getByName(targetNameOrUri);
            if (results.size()>1) {
                throw new DuplicateNameException(targetNameOrUri);
            } else {
                target = results.get(0);
            }
        }
        return target;
    }

}
