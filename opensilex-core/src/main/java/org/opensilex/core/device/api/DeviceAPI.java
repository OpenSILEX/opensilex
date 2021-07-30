package org.opensilex.core.device.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import io.swagger.annotations.*;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.Document;
import org.opensilex.core.URIsListPostDTO;
import org.opensilex.core.data.api.DataFileGetDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Collectors;
import java.time.format.DateTimeParseException;

import static java.lang.Integer.max;
import static org.opensilex.core.data.api.DataAPI.*;
import org.opensilex.core.provenance.dal.ProvenanceDAO;

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
    
    public static final String DEVICE_EXAMPLE_TYPE = "vocabulary:SensingDevice";
    public static final String DEVICE_EXAMPLE_YEAR = "2017";
    public static final String DEVICE_EXAMPLE_METADATA = "{ \"Group\" : \"weather station\",\n" +"\"Group2\" : \"A\"}";
    public static final String DEVICE_EXAMPLE_URI = "http://opensilex.dev/set/device/sensingdevice-sensor_01";

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
            try {
                DeviceModel devModel = new DeviceModel();
                deviceDTO.toModel(devModel);
                URI uri = deviceDAO.create(devModel, deviceDTO.getRelations(), currentUser);
                return new ObjectUriResponse(Response.Status.CREATED, uri).getResponse();
            } catch (SPARQLAlreadyExistingUriException ex) {
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
            @ApiParam(value = "RDF type filter", example = DEVICE_EXAMPLE_TYPE) @QueryParam("rdf_type") @ValidURI URI rdfType,
            @ApiParam(value = "Set this param to true when filtering on rdf_type to also retrieve sub-types") @DefaultValue("false") @QueryParam("include_subtypes") boolean includeSubTypes,
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
            @ApiParam(value = "Search by year", example = DEVICE_EXAMPLE_YEAR) @QueryParam("year")  @Min(999) @Max(10000) Integer year,
            @ApiParam(value = "Date to filter device existence") @QueryParam("existence_date") LocalDate existenceDate,
            @ApiParam(value = "Regex pattern for filtering by brand", example = ".*") @DefaultValue("") @QueryParam("brand") String brand,
            @ApiParam(value = "Regex pattern for filtering by model", example = ".*") @DefaultValue("") @QueryParam("model") String model,
            @ApiParam(value = "Regex pattern for filtering by serial number", example = ".*") @DefaultValue("") @QueryParam("serial_number") String serialNumber,
            @ApiParam(value = "Search by metadata", example = DEVICE_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        Document metadataFilter = null;
        if (metadata != null) {
            try {
                metadataFilter = Document.parse(metadata);
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();                
            }
        }
        
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        ListWithPagination<DeviceModel> devices = dao.search(name,
            rdfType,
            includeSubTypes,
            year,
            existenceDate,
            brand,
            model,
            serialNumber,
            metadataFilter,
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
            @ApiParam(value = "device URI", example = DEVICE_EXAMPLE_URI, required = true)
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

    @GET
    @Path("by_uris")
    @ApiOperation("Get devices by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return devices", response = DeviceGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Device not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getDeviceByUris(
            @ApiParam(value = "Device URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        List<DeviceModel> models = dao.getList(uris,currentUser);

        if (!models.isEmpty()) {
            List<DeviceGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(model -> {
                resultDTOList.add(DeviceGetDTO.getDTOFromModel(model));
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Devices not found",
                    "Unknown device URIs"
            ).getResponse();
        }
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
            @ApiParam(value = "Device URI", example = DEVICE_EXAMPLE_URI, required = true)
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
        //Check format date
        if (deviceDTO.getStartUp() != null) {
            DateTimeFormatter pattern = DateTimeFormatter.ofPattern("YYYY-MM-DD");
            try {
                LocalDate.parse(deviceDTO.getStartUp().toString(), pattern);
            } catch (DateTimeParseException e) {
                return new ErrorResponse(
                Response.Status.BAD_REQUEST,
                "Expected date format : YYYY-MM-DD,DD-MM-YYYY,DD/MM/YYYY",
                "Wrong date format: " + deviceDTO.getStartUp()
                );
            }            
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
        
    @GET
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
            @ApiParam(value = "RDF type filter", example = DEVICE_EXAMPLE_TYPE) @QueryParam("rdf_type") @ValidURI URI rdfType,
            @ApiParam(value = "Set this param to true when filtering on rdf_type to also retrieve sub-types") @DefaultValue("false") @QueryParam("include_subtypes") boolean includeSubTypes,
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
            @ApiParam(value = "Search by year", example = DEVICE_EXAMPLE_YEAR) @QueryParam("year")  @Min(999) @Max(10000) Integer year,
            @ApiParam(value = "Date to filter device existence") @QueryParam("existence_date") LocalDate existenceDate,
            @ApiParam(value = "Regex pattern for filtering by brand", example = ".*") @DefaultValue("") @QueryParam("brand") String brand,
            @ApiParam(value = "Regex pattern for filtering by model", example = ".*") @DefaultValue("") @QueryParam("model") String model,
            @ApiParam(value = "Regex pattern for filtering by serial number", example = ".*") @DefaultValue("") @QueryParam("serial_number") String serialNumber,
            @ApiParam(value = "Search by metadata", example = DEVICE_EXAMPLE_METADATA) @QueryParam("metadata") String metadata            
    ) throws Exception {
        Document metadataFilter = null;
        if (metadata != null) {
            try {
                metadataFilter = Document.parse(metadata);
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();                
            }
        }

        // Search device with device DAO
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        List<DeviceModel> resultList = dao.searchForExport(
            name,
            rdfType,
            includeSubTypes,
            year,
            existenceDate,
            brand,
            model,
            serialNumber,
            metadataFilter,
            currentUser
        );

        return buildCSV(resultList);

    }
    
    @POST
    @Path("export_by_uris")
    @ApiOperation("export devices")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with device list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportList(
            @ApiParam(value = "List of device URI", example = "dev:set/sensor_01") URIsListPostDTO dto
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        List<DeviceModel> resultList = dao.getDevicesByURI(dto.getUris(), currentUser);
        return buildCSV(resultList);
    }
    
    private Response buildCSV(List<DeviceModel> devices) throws Exception {
        // Convert list to DTO
        List<DeviceExportDTO> resultDTOList = new ArrayList<>();
        Set metadataKeys = new HashSet();
        Map<URI,Integer> relationsUsed = new HashMap();
        for (DeviceModel device : devices) {
            DeviceExportDTO dto = DeviceExportDTO.getDTOFromModel(device);
            resultDTOList.add(dto);
            Map metadata = dto.getMetadata();
            if (metadata != null) {
                metadataKeys.addAll(metadata.keySet());
            }
            
            List<RDFObjectRelationDTO> relations = dto.getRelations();
            Map<URI,Integer> relationsUsed_local = new HashMap();
            for(RDFObjectRelationDTO relation : relations){
                URI prop = relation.getProperty();
                if(relationsUsed_local.containsKey(prop)){
                    relationsUsed_local.replace(prop, relationsUsed_local.get(prop)+1);
                }else{
                    relationsUsed_local.put(prop, 1);
                }
            }
            for(URI prop: relationsUsed_local.keySet()){
                if(relationsUsed.containsKey(prop)){
                    relationsUsed.replace(prop, max(relationsUsed_local.get(prop), relationsUsed.get(prop)));
                }else{
                    relationsUsed.put(prop, relationsUsed_local.get(prop));
                }
            }
        }

        if (resultDTOList.isEmpty()) {
            resultDTOList.add(new DeviceExportDTO()); // to return an empty table
        }
        
        //Construct manually json with metadata and type property to convert it to csv
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);

        List<JsonNode> list = new ArrayList<>();
        if(jsonTree.isArray()) {
            for(JsonNode jsonNode : jsonTree) {
                ObjectNode objectNode = jsonNode.deepCopy();
                JsonNode metadata = objectNode.get("metadata");
                JsonNode relations = objectNode.get("relations");
                objectNode.remove("metadata");
                objectNode.remove("relations");
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
                JsonNode property = null;
                JsonNode propertyValue = null;
                for( URI prop: relationsUsed.keySet()){
                    for(int i = 1; i <= relationsUsed.get(prop); i++){
                        objectNode.putNull(prop.toString()+"_"+i);
                    }
                }
                
                Map<String,Integer> relationsUsed_local = new HashMap();
                for(JsonNode relation : relations){
                    property = relation.get("property");
                    if(relationsUsed_local.containsKey(property.asText())){
                        relationsUsed_local.replace(property.asText(), relationsUsed_local.get(property.asText())+1);
                    }else{
                        relationsUsed_local.put(property.asText(), 1);
                    }
                    propertyValue = relation.get("value");
                    objectNode.put(property.asText()+"_"+relationsUsed_local.get(property.asText()), propertyValue.asText());
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

    }

    /**
     *
     * @param uri
     * @param startDate
     * @param endDate
     * @param timezone
     * @param experiments
     * @param variables
     * @param confidenceMin
     * @param confidenceMax
     * @param provenances
     * @param metadata
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     * @deprecated better use directly the service GET data with the parameter devices
     */
    @Deprecated
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

        ListWithPagination<DataModel> resultList = dao.search(
                currentUser,
                experiments,
                null,
                variables,
                provenances,
                Arrays.asList(uri),
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
    
    /**
     *
     * @param uri
     * @param startDate
     * @param endDate
     * @param timezone
     * @param experiments
     * @param variables
     * @param confidenceMin
     * @param confidenceMax
     * @param provenances
     * @param metadata
     * @return
     * @throws Exception
     * @deprecated better use directly the service GET data/count with the parameter devices
     */
    @Deprecated
    @GET
    @Path("{uri}/data/count")
    @ApiOperation("Count device data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the number of data", response = Integer.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response countDeviceData(
            @ApiParam(value = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") List<URI> experiments,            
            @ApiParam(value = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variable") List<URI> variables,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenance") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata
          
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

        int count = dao.count(
                currentUser,
                experiments,
                null,
                variables,
                provenances,
                Arrays.asList(uri),
                startInstant,
                endInstant,
                confidenceMin,
                confidenceMax,
                metadataFilter
        );

        return new SingleObjectResponse<>(count).getResponse();
    }
    
    /**
     *
     * @param uri
     * @param rdfType
     * @param startDate
     * @param endDate
     * @param timezone
     * @param experiments
     * @param objects
     * @param provenances
     * @param metadata
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     * @deprecated better use directly the service GET datafiles with the parameter devices
     */
    @Deprecated
    @GET
    @Path("{uri}/datafiles")
    @ApiOperation("Search device datafiles descriptions")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return datafiles list", response = DataGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchDeviceDatafiles(
            @ApiParam(value = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Search by rdf type uri") @QueryParam("rdf_type") URI rdfType,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiments", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") List<URI> experiments,
            @ApiParam(value = "Search by object uris list", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("scientific_objects") List<URI> objects,
            @ApiParam(value = "Search by provenance uris list", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
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

        ListWithPagination<DataFileModel> resultList = dao.searchFilesByDevice(
                uri,
                currentUser,
                rdfType,
                experiments,
                objects,
                provenances,
                startInstant,
                endInstant,
                metadataFilter,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<DataFileGetDTO> resultDTOList = resultList.convert(DataFileGetDTO.class, DataFileGetDTO::fromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    @GET
    @Path("{uri}/variables")
    @ApiOperation("Get variables linked to the device")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables list", response = NamedResourceDTO.class, responseContainer = "List")
    })
    public Response getDeviceVariables(
            @ApiParam(value = "Device URI", example = DeviceAPI.DEVICE_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {        
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        List<VariableModel> variables = dao.getDeviceVariables(uri, currentUser.getLanguage());
        List<NamedResourceDTO> dtoList = variables.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    /**
     *
     * @param uri
     * @return
     * @throws Exception
     * @deprecated better use directly the service GET data/provenances with the parameter devices
     */
    @Deprecated
    @GET
    @Path("{uri}/data/provenances")
    @ApiOperation("Get provenances of data that have been measured on this device")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getDeviceDataProvenances(
            @ApiParam(value = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
        ) throws Exception {
        
        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        List<ProvenanceModel> provenances = dataDAO.getProvenancesByDevice(currentUser, uri, DataDAO.DATA_COLLECTION_NAME);
        List<ProvenanceGetDTO> resultDTOList = provenances.stream().map(ProvenanceGetDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    /**
     *
     * @param uri
     * @return
     * @throws Exception
     * @deprecated better use directly the service GET datafiles/provenances with the parameter devices
     */
    @Deprecated
    @GET
    @Path("{uri}/datafiles/provenances")
    @ApiOperation("Get provenances of datafiles linked to this device")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getDeviceDataFilesProvenances(
            @ApiParam(value = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
        ) throws Exception {
        
        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        List<ProvenanceModel> provenances = dataDAO.getProvenancesByDevice(currentUser, uri, DataDAO.FILE_COLLECTION_NAME);
        List<ProvenanceGetDTO> dtoList = provenances.stream().map(ProvenanceGetDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

}
