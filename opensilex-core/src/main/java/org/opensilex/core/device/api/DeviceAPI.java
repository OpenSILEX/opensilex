package org.opensilex.core.device.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.Document;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_CONFIDENCE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_MAXIMAL_DATE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_METADATA;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_MINIMAL_DATE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_OBJECTURI;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_PROVENANCEURI;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_TIMEZONE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_VARIABLEURI;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import static org.opensilex.core.experiment.api.ExperimentAPI.EXPERIMENT_EXAMPLE_URI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author sammy
 */

@Api(DeviceAPI.CREDENTIAL_DEVICE_GROUP_ID)
@Path("/core/devices")
@ApiCredentialGroup(
        groupId = DeviceAPI.CREDENTIAL_DEVICE_GROUP_ID,
        groupLabelKey = DeviceAPI.CREDENTIAL_DEVICE_GROUP_LABEL_KEY
)
public class DeviceAPI {
    public static final String CREDENTIAL_DEVICE_GROUP_ID = "Devices";
    public static final String CREDENTIAL_DEVICE_GROUP_LABEL_KEY = "credential-groups.device";

    public static final String CREDENTIAL_DEVICE_MODIFICATION_ID = "device-modification";
    public static final String CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY = "credential.device.modification";

    public static final String CREDENTIAL_DEVICE_DELETE_ID = "device-delete";
    public static final String CREDENTIAL_DEVICE_DELETE_LABEL_KEY = "credential.device.delete";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBService nosql;
    
    @POST
    @ApiOperation("Create a device")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "A device is created", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A device with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createDevice(
            @ApiParam("Device description") @Valid DeviceCreationDTO deviceDTO,
            @ApiParam(value = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {       
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql);
        ErrorResponse error = check(deviceDTO, deviceDAO);
        if (error != null) {
            return error.getResponse();
        }
        if (!checkOnly){
            try{
                DeviceModel devModel = new DeviceModel();
                deviceDTO.toModel(devModel);
                URI uri = deviceDAO.create(devModel, deviceDTO.getRelations(), currentUser);
                return new ObjectUriResponse(Response.Status.CREATED, uri).getResponse();
            }catch(SPARQLAlreadyExistingUriException ex){
                return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Device URI already exists",
                    "Duplicated URI: " + deviceDTO.getUri()
                ).getResponse();
            }
        } else {
            return new ObjectUriResponse().getResponse();
        }
    }
    
    @GET
    @ApiOperation("Search devices")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return devices corresponding to the given search parameters", response = DeviceGetDTO.class, responseContainer = "List")
    })    
    public Response searchDevices(
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("namePattern") String namePattern,
            @ApiParam(value = "RDF type filter", example = "vocabulary:SensingDevice") @QueryParam("rdfType") @ValidURI URI rdfType,
            @ApiParam(value = "Search by year", example = "2017") @QueryParam("year")  @Min(999) @Max(10000) Integer year,
            @ApiParam(value = "Date to filter device existence") @QueryParam("existence_date") LocalDate existenceDate,
            @ApiParam(value = "Regex pattern for filtering by brand", example = ".*") @DefaultValue("") @QueryParam("brandPattern") String brandPattern,
            @ApiParam(value = "Regex pattern for filtering by model", example = ".*") @DefaultValue("") @QueryParam("modelPattern") String modelPattern,
            @ApiParam(value = "Regex pattern for filtering by serial number", example = ".*") @DefaultValue("") @QueryParam("serialNumberPattern") String snPattern,
            @ApiParam(value = "Search by metadata", example = "{ \"Group\" : \"station meteo\",\n" +"\"group2\" : \"parcelle A\"}") @QueryParam("metadata") String metadataParam,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "namePattern=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        ListWithPagination<DeviceModel> devices = dao.search(
            namePattern,
            rdfType,
            year,
            existenceDate,
            brandPattern,
            modelPattern,
            snPattern,
            currentUser,
            orderByList,
            page,
            pageSize);

        ListWithPagination<DeviceGetDTO> dtoList = devices.convert(DeviceGetDTO.class, DeviceGetDTO::getDTOFromModel);

        return new PaginatedListResponse<DeviceGetDTO>(dtoList).getResponse();
    }
    
    @GET
    @Path("{uri}")
    @ApiOperation("Get device detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return device details corresponding to the device URI", response = DeviceGetDetailsDTO.class)
    })
    public Response getDevice(
            @ApiParam(value = "device URI", example = "http://example.com/", required = true)
            @PathParam("uri") URI uri
    ) throws Exception {

        DeviceDAO dao = new DeviceDAO(sparql, nosql);

        DeviceModel model = dao.getDeviceByURI(uri, currentUser);

        Response response;
        if (model != null) {
            response = new SingleObjectResponse<>(DeviceGetDetailsDTO.getDTOFromModel(model)).getResponse();
        } else {
            response = Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        
        return response;
    }
    
    @PUT
    @ApiOperation("Update a device")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Device updated", response = ObjectUriResponse.class)
    })
    public Response updateDevice(
            @ApiParam(value = "Device description", required = true)
            @NotNull
            @Valid DeviceCreationDTO dto
    ) throws Exception {
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql);
        DeviceModel DeviceModel = dto.newModel();
        deviceDAO.update(DeviceModel, dto.getRelations(), currentUser);
        return new ObjectUriResponse(Response.Status.OK, DeviceModel.getUri()).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a device")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Device deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Device URI not found", response = ErrorResponse.class)
    })
    public Response deleteDevice(
            @ApiParam(value = "Device URI", example = "http://example.com/", required = true)
            @PathParam("uri")
            @NotNull
            @ValidURI URI uri
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        
        dao.delete(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        
    }
    
    private ErrorResponse check(DeviceDTO deviceDTO, DeviceDAO deviceDAO) throws Exception {

        // check if device URI already exists
        if (sparql.uriExists(DeviceModel.class, deviceDTO.getUri())) {
            // Return error response 409 - CONFLICT if URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Device URI already exists",
                    "Duplicated URI: " + deviceDTO.getUri()
            );
        }

        AskBuilder askQuery = new AskBuilder()
            .from(sparql.getDefaultGraph(DeviceModel.class).toString())
            .addWhere("?uri", RDF.type, SPARQLDeserializers.nodeURI(deviceDTO.getType()))
            .addWhere("?uri", RDFS.label, deviceDTO.getName());
        boolean exists = sparql.executeAskQuery(askQuery);
        if (exists) {
            // Return error response 409 - CONFLICT if label already exists
            return new ErrorResponse(
                    Response.Status.PRECONDITION_FAILED,
                    "Device label already exists for this type",
                    "Duplicated label: " + deviceDTO.getName()
            );
        }

        //Check that the given person exist in DB
        if (deviceDTO.getPersonInCharge()!= null) {
            if (!sparql.uriExists(UserModel.class,deviceDTO.getPersonInCharge())) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given person doesn't exist in the database",
                        "unknown person : " + deviceDTO.getPersonInCharge().toString()
                );
            }
        }
        
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(deviceDTO.getType(), new URI(Oeso.Device.getURI()), currentUser.getLanguage());

        DeviceModel device = new DeviceModel();
        
        if (deviceDTO.getRelations() != null) {
            for (RDFObjectRelationDTO relation : deviceDTO.getRelations()) {
                URI prop = relation.getProperty();
                if (!ontologyDAO.validateObjectValue(sparql.getDefaultGraphURI(DeviceModel.class), model, prop, relation.getValue(), device)) {
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "Invalid relation value",
                            "Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

        return null;
    }
        
    /*@GET
    @Path("export")
    @ApiOperation("export devices")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with device list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportDevices(
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("namePattern") String namePattern,
            @ApiParam(value = "RDF type filter", example = "vocabulary:SensingDevice") @QueryParam("rdfType") @ValidURI URI rdfType,
            @ApiParam(value = "Search by year", example = "2017") @QueryParam("year")  @Min(999) @Max(10000) Integer year,
            @ApiParam(value = "Date to filter device existence") @QueryParam("existence_date") LocalDate existenceDate,
            @ApiParam(value = "Regex pattern for filtering by brand", example = ".*") @DefaultValue("") @QueryParam("brandPattern") String brandPattern,
            @ApiParam(value = "Regex pattern for filtering by model", example = ".*") @DefaultValue("") @QueryParam("modelPattern") String modelPattern,
            @ApiParam(value = "Regex pattern for filtering by serial number", example = ".*") @DefaultValue("") @QueryParam("serialNumberPattern") String snPattern,
            @ApiParam(value = "Search by metadata", example = "{}") @QueryParam("metadata") String metadataParam
    ) throws Exception {
        Document metadataFilter = null;
        if (metadataParam != null) {
            try {
                metadataFilter = Document.parse(metadataParam);
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();                
            }
        }

        // Search germplasm with germplasm DAO
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        List<DeviceModel> resultList = dao.searchForExport(
            namePattern,
            rdfType,
            year,
            existenceDate,
            brandPattern,
            modelPattern,
            snPattern,
            null,
            currentUser
        );

        // Convert list to DTO
        List<DeviceGetDetailsDTO> resultDTOList = new ArrayList<>();
        Set metadataKeys = new HashSet();
        for (DeviceModel device : resultList) {
            DeviceGetDetailsDTO dto = DeviceGetDetailsDTO.getDTOFromModel(device);
            resultDTOList.add(dto);
            Map metadata = dto.getMetadata();
            if (metadata != null) {
                 metadataKeys.addAll(metadata.keySet());
            }           
        }

        if (resultDTOList.isEmpty()) {
            resultDTOList.add(new DeviceGetDetailsDTO()); // to return an empty table
        }
        
        //Construct manually json with metadata to convert it to csv
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);

        List<JsonNode> list = new ArrayList<>();
        if(jsonTree.isArray()) {
            for(JsonNode jsonNode : jsonTree) {
                ObjectNode objectNode = jsonNode.deepCopy();
                JsonNode metadata = objectNode.get("metadata");
                objectNode.remove("metadata");
                JsonNode value = null;
                for (Object key:metadataKeys) {
                    try {
                        value = metadata.get(key.toString());

                    } catch (Exception e) {

                    } finally {
                        if (value != null) {
                            objectNode.put(key.toString(), value.asText());
                        } else {
                            objectNode.putNull(key.toString());
                        }                            
                    }
                }
                
                list.add(objectNode);
               
            }
         }
        
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance, list);

        Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = arrayNode.elements().next();
        firstObject.fieldNames().forEachRemaining(fieldName -> {
            csvSchemaBuilder.addColumn(fieldName);
        });
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
        StringWriter str = new StringWriter();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class)
                .with(csvSchema)
                .writeValue(str, arrayNode);

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_device" + dtf.format(date) + ".csv";

        return Response.ok(str.toString(), MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();

    }*/
    
    @GET
    @Path("{uri}/data")
    @ApiOperation("Search device data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return data list", response = DataGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchDeviceData(
            @ApiParam(value = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") List<URI> experiments,            
            @ApiParam(value = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variable") List<URI> variables,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenance") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, null);
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

        ListWithPagination<DataModel> resultList = dao.searchByDevice(
                uri,
                currentUser,
                experiments,
                null,
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
    
    @GET
    @Path("{uri}/variables")
    @ApiOperation("Get variables measured by th")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables list", response = NamedResourceDTO.class, responseContainer = "List")
    })
    public Response getDeviceVariables(
            @ApiParam(value = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {        
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        List<VariableModel> variables = dao.getDeviceVariables(uri, currentUser.getLanguage());
        List<NamedResourceDTO> dtoList = variables.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }
}
