package org.opensilex.core.device.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.geojson.GeoJsonObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.utils.URIsListPostDTO;
import org.opensilex.core.csv.api.CSVValidationDTO;
import org.opensilex.core.csv.api.CsvImportDTO;
import org.opensilex.core.data.api.DataFileGetDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.api.DataGetSearchDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.device.dal.*;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.geospatial.api.GeometryDTO;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.location.dal.LocationObservationSearchFilter;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.metadata.MetaDataDaoV2;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.nosql.mongodb.metadata.MetadataSearchFilter;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.*;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvImporter;
import org.opensilex.sparql.csv.DefaultCsvImporter;
import org.opensilex.sparql.csv.validation.CachedCsvImporter;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.pagination.StreamWithPagination;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Collectors;
import static java.lang.Integer.max;
import static org.opensilex.core.data.api.DataAPI.*;

/**
 *
 * @author sammy
 */

@Tag(name = DeviceAPI.CREDENTIAL_DEVICE_GROUP_ID)
@Path(DeviceAPI.PATH)
@ApiCredentialGroup(
        groupId = DeviceAPI.CREDENTIAL_DEVICE_GROUP_ID,
        groupLabelKey = DeviceAPI.CREDENTIAL_DEVICE_GROUP_LABEL_KEY
)
public class DeviceAPI {

    public static final String PATH = "/core/devices";

    public static final String CREDENTIAL_DEVICE_GROUP_ID = "Devices";
    public static final String CREDENTIAL_DEVICE_GROUP_LABEL_KEY = "credential-groups.device";

    public static final String CREDENTIAL_DEVICE_MODIFICATION_ID = "device-modification";
    public static final String CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_DEVICE_DELETE_ID = "device-delete";
    public static final String CREDENTIAL_DEVICE_DELETE_LABEL_KEY = "credential.default.delete";

    public static final String DEVICE_EXAMPLE_TYPE = "vocabulary:SensingDevice";
    public static final String DEVICE_EXAMPLE_VARIABLE = "test:set/variables#air_temperature_thermocouple_degree-celsius";
    public static final String DEVICE_EXAMPLE_YEAR = "2017";
    public static final String DEVICE_EXAMPLE_METADATA = "{ \"Group\" : \"weather station\",\n" +"\"Group2\" : \"A\"}";
    public static final String DEVICE_EXAMPLE_RELATIONS = "{ \"vocabulary:hasShapeLength\" : \"87.0\" }";
    public static final String DEVICE_EXAMPLE_URI = "http://opensilex.dev/set/device/sensingdevice-sensor_01";

    public static final String LINKED_DEVICE_ERROR = "LINKED_DEVICE_ERROR";

    public static final String METADATA_COLLECTION_NAME = "deviceAttribute";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBService nosql;
    @Inject
    private FileStorageService fs;


    @POST
    @Operation(summary = "Create a device")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "A device is created", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "409", description = "A device with the same URI already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })

    public Response createDevice(
            @Parameter(description = "Device description") @Valid DeviceCreationDTO deviceDTO,
            @Parameter(description = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);

        PersonModel personInCharge = null;
        if (Objects.nonNull(deviceDTO.getPersonInChargeURI())){
                PersonDAO personDAO = new PersonDAO(sparql);
                personInCharge = personDAO.get(deviceDTO.getPersonInChargeURI());
            if (Objects.isNull(personInCharge)){
                throw new NotFoundURIException("Person in charge must be an existing person", deviceDTO.getPersonInChargeURI());            }
        }
        if (!checkOnly){
            try {
                DeviceModel devModel = new DeviceModel();
                deviceDTO.toModel(devModel);
                deviceDAO.initDevice(devModel, deviceDTO.getRelations(), currentUser);
                devModel.setPersonInCharge(personInCharge);
                devModel.setPublisher(currentUser.getUri());
                URI uri = new SparqlMongoTransaction(sparql,nosql.getServiceV2()).execute(session -> {
                    URI transactionResult = deviceDAO.create(devModel, currentUser);
                    if(devModel.getMetaDataModel() != null){
                        //Set the metaDataModel's uri to be the same as the device
                        MetaDataModel metaDataModel = devModel.getMetaDataModel();
                        metaDataModel.setUri(transactionResult);
                        MetaDataDaoV2 metaDataDao = new MetaDataDaoV2(nosql, DeviceAPI.METADATA_COLLECTION_NAME);
                        metaDataDao.create(session, metaDataModel);
                    }
                    return transactionResult;
                });
                return new CreatedUriResponse(uri).getResponse();
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
    @Operation(summary = "Search devices")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return devices corresponding to the given search parameters", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeviceGetDTO.class))))
    })
    public Response searchDevices(
            @Parameter(description = "RDF type filter", example = DEVICE_EXAMPLE_TYPE) @QueryParam("rdf_type") @ValidURI URI rdfType,
            @Parameter(description = "Set this param to true when filtering on rdf_type to also retrieve sub-types") @DefaultValue("false") @QueryParam("include_subtypes") boolean includeSubTypes,
            @Parameter(description = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
            @Parameter(description = "Variable", example = DEVICE_EXAMPLE_VARIABLE) @QueryParam("variable") @ValidURI URI variable,
            @Parameter(description = "Search by year", example = DEVICE_EXAMPLE_YEAR) @QueryParam("year")  @Min(999) @Max(10000) Integer year,
            @Parameter(description = "Date to filter device existence") @QueryParam("existence_date") LocalDate existenceDate,
            @Parameter(description = "Search by facility", example = "http://example.com") @QueryParam("facility") @ValidURI URI facility,
            @Parameter(description = "Regex pattern for filtering by brand", example = ".*") @DefaultValue("") @QueryParam("brand") String brand,
            @Parameter(description = "Regex pattern for filtering by model", example = ".*") @DefaultValue("") @QueryParam("model") String model,
            @Parameter(description = "Regex pattern for filtering by serial number", example = ".*") @DefaultValue("") @QueryParam("serial_number") String serialNumber,
            @Parameter(description = "Search by metadata", example = DEVICE_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @Parameter(description = "Search by RDF relations", example = DEVICE_EXAMPLE_RELATIONS) @QueryParam("relations") String relations,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        List<URI> filteredDeviceUris = null;
        if(metadata != null){
            MetadataSearchFilter metadataFilter = new MetadataSearchFilter();
            Document docVersionOfMetaFilter = Document.parse(metadata);
            metadataFilter.setAttributes(docVersionOfMetaFilter);
            MetaDataDaoV2 metaDataDaoV2 = new MetaDataDaoV2(nosql, DeviceAPI.METADATA_COLLECTION_NAME);
            filteredDeviceUris = metaDataDaoV2.distinctUris(metadataFilter);
            if(CollectionUtils.isEmpty(filteredDeviceUris)){
                return new PaginatedListResponse<>().getResponse();
            }
        }

        Map<URI, String> relationFilters = parseRelationFilters(relations);

        DeviceDAO dao = new DeviceDAO(sparql, nosql, fs);

        DeviceSearchFilter filter = new DeviceSearchFilter()
                .setRdfType(rdfType)
                .setIncludeSubTypes(includeSubTypes)
                .setNamePattern(name)
                .setVariable(variable)
                .setYear(year)
                .setExistenceDate(existenceDate)
                .setBrandPattern(brand)
                .setModelPattern(model)
                .setSnPattern(serialNumber)
                .setRelations(relationFilters)
                .setCurrentUser(currentUser);
        filter.setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize)
                .setIncludedUris(filteredDeviceUris);

        ListWithPagination<DeviceModel> devices = dao.search(filter);

        if (facility != null) {
            List<DeviceModel> resultList = new ArrayList<>();

            devices.getList().forEach((device) -> {
                try {
                    FacilityModel facilityModel = dao.getAssociatedFacility(device.getUri(), currentUser);
                    if (facilityModel != null) {
                        if (SPARQLDeserializers.compareURIs(facility, facilityModel.getUri())) {
                            resultList.add(device);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            devices = new ListWithPagination<>(resultList);
        }

        ListWithPagination<DeviceGetDTO> dtoList = devices.convert(DeviceGetDTO.class, DeviceGetDTO::getDTOFromModel);

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    private Map<URI, String> parseRelationFilters(String relations) {
        if (relations == null || relations.trim().isEmpty()) {
            return null;
        }

        Document document = Document.parse(relations);

        Map<URI, String> relationFilters = new HashMap<>();

        for (String property : document.keySet()) {
            Object value = document.get(property);

            if (value == null || value.toString().trim().isEmpty()) {
                continue;
            }

            URI propertyURI = URI.create(SPARQLDeserializers.formatURI(property));
            relationFilters.put(propertyURI, value.toString().trim());
        }

        return relationFilters;
    }

    @GET
    @Path("{uri}")
    @Operation(summary = "Get device detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return device details corresponding to the device URI", content = @Content(schema = @Schema(implementation = DeviceGetDetailsDTO.class)))
    })
    public Response getDevice(
            @Parameter(description = "device URI", example = DEVICE_EXAMPLE_URI, required = true)
            @PathParam("uri") URI uri
    ) throws Exception {

        DeviceDAO dao = new DeviceDAO(sparql, nosql, fs);
        MetaDataDaoV2 metaDataDao = new MetaDataDaoV2(nosql, DeviceAPI.METADATA_COLLECTION_NAME);

            DeviceModel model = dao.getDeviceByURI(uri, currentUser);
            if (model != null) {
                //Handle creation of MetaDataModel
                MetaDataModel metaDataModel = null;
                try{
                    metaDataModel = metaDataDao.get(uri);
                    model.setMetaDataModel(metaDataModel);
                }catch (NoSQLInvalidURIException ignore){}
                DeviceGetDetailsDTO dto = DeviceGetDetailsDTO.getDTOFromModel(model);
                if (Objects.nonNull(model.getPublisher())) {
                    dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
                }
                return new SingleObjectResponse<>(dto).getResponse();
            } else {
                throw new NotFoundURIException(uri);
            }
    }

    @GET
    @Path("by_uris")
    @Operation(summary = "Get devices by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return devices", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeviceGetDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Device not found (if any provided URIs is not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response getDeviceByUris(
            @Parameter(description = "Device URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql, fs);
        List<DeviceModel> models = dao.getList(uris,currentUser);

        if (!models.isEmpty()) {
            List<DeviceGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(model -> {
                //No need to fetch metadata here as DeviceGetDTO doesn't have metadata information
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
    @Operation(summary = "Update a device")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device updated", content = @Content(schema = @Schema(implementation = URI.class)))
    })
    public Response updateDevice(
            @Parameter(description = "Device description", required = true)
            @NotNull
            @Valid DeviceCreationDTO dto
    ) throws Exception {
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        DeviceModel deviceModel = dto.newModel();
        deviceDAO.initDevice(deviceModel, dto.getRelations(), currentUser);

        if (Objects.nonNull(dto.getPersonInChargeURI())){
                PersonDAO personDAO = new PersonDAO(sparql);
                PersonModel personInCharge =  personDAO.get(dto.getPersonInChargeURI());
                deviceModel.setPersonInCharge(personInCharge);
            if (Objects.isNull(personInCharge)){
                return new ErrorResponse(
                        Response.Status.NOT_FOUND,
                        "Person in charge must be an existing person",
                        "person in charge not found with URI : " + dto.getPersonInChargeURI()
                ).getResponse();
            }
        }

        //Transaction to update device and it's metadata
        URI uri = new SparqlMongoTransaction(sparql,nosql.getServiceV2()).execute(session -> {
            URI deviceUri = deviceModel.getUri();
            MetaDataDaoV2 metaDataDao = new MetaDataDaoV2(nosql, DeviceAPI.METADATA_COLLECTION_NAME);
            MetaDataModel newMetaData = deviceModel.getMetaDataModel();
            if(newMetaData == null){
                try{
                    metaDataDao.delete(session, deviceUri);
                } catch (NoSQLInvalidURIException ignored){}

            }else{
                newMetaData.setUri(deviceUri);
                metaDataDao.upsert(session, newMetaData);
            }
            deviceDAO.update(deviceModel);
            return deviceUri;
        });

        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete a device")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device deleted", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "400", description = "Device is linked to some data, datafile or provenance and could not be deleted {result.title: 'LINKED_DEVICE_ERROR'}.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Device URI not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteDevice(
            @Parameter(description = "Device URI", example = DEVICE_EXAMPLE_URI, required = true)
            @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql, fs);
        MetaDataDaoV2 metaDataDao = new MetaDataDaoV2(nosql, DeviceAPI.METADATA_COLLECTION_NAME);
            new SparqlMongoTransaction(sparql,nosql.getServiceV2()).execute(session -> {
                try{
                    metaDataDao.delete(session, uri);
                }catch (NoSQLInvalidURIException ignore){}
                dao.delete(uri, currentUser);
                return 0;
            });
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @POST
    @Path("import")
    @Operation(summary = "Import a CSV file with one device per line")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Device(s) imported with success", content = @Content(schema = @Schema(implementation = CSVValidationDTO.class)))
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSV(
            @Parameter(description = "CSV import settings", required = true) @NotNull @Valid @FormDataParam("description") CsvImportDTO importDTO,
            @Parameter(description = "Device file", required = true, schema = @Schema(type = "file")) @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        CsvImporter<DeviceModel> csvImporter = new CachedCsvImporter<>(
                new DefaultCsvImporter<>(sparql, DeviceModel.class, DeviceModel::new, currentUser.getUri()),
                importDTO.getValidationToken()
        );

        CSVValidationModel validationModel = csvImporter.importCSV(file, false);
        return new SingleObjectResponse<>(new CSVValidationDTO(validationModel)).getResponse();
    }

    @POST
    @Path("import_validation")
    @Operation(summary = "Validate the import of a CSV file with one device per line")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Device(s) checked", content = @Content(schema = @Schema(implementation = CSVValidationDTO.class)))
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @Parameter(description = "CSV import settings", required = true) @NotNull @Valid @FormDataParam("description") CsvImportDTO importDTO,
            @Parameter(description = "Device file", required = true, schema = @Schema(type = "file")) @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        CsvImporter<DeviceModel> csvImporter = new CachedCsvImporter<>(
                new DefaultCsvImporter<>(sparql, DeviceModel.class, DeviceModel::new, currentUser.getUri()),
                importDTO.getValidationToken()
        );

        CSVValidationModel validationModel = csvImporter.importCSV(file, true);
        return new SingleObjectResponse<>(new CSVValidationDTO(validationModel)).getResponse();
    }

    @GET
    @Path("export")
    @Operation(summary = "export devices")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return a csv file with device list"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response exportDevices(
            @Parameter(description = "RDF type filter", example = DEVICE_EXAMPLE_TYPE) @QueryParam("rdf_type") @ValidURI URI rdfType,
            @Parameter(description = "Set this param to true when filtering on rdf_type to also retrieve sub-types") @DefaultValue("false") @QueryParam("include_subtypes") boolean includeSubTypes,
            @Parameter(description = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
            @Parameter(description = "Search by year", example = DEVICE_EXAMPLE_YEAR) @QueryParam("year")  @Min(999) @Max(10000) Integer year,
            @Parameter(description = "Date to filter device existence") @QueryParam("existence_date") LocalDate existenceDate,
            @Parameter(description = "Regex pattern for filtering by brand", example = ".*") @DefaultValue("") @QueryParam("brand") String brand,
            @Parameter(description = "Regex pattern for filtering by model", example = ".*") @DefaultValue("") @QueryParam("model") String model,
            @Parameter(description = "Regex pattern for filtering by serial number", example = ".*") @DefaultValue("") @QueryParam("serial_number") String serialNumber,
            @Parameter(description = "Search by metadata", example = DEVICE_EXAMPLE_METADATA) @QueryParam("metadata") String metadata
    ) throws Exception {
        // Search device with device DAO and metaDataDao for metaData
        DeviceDAO dao = new DeviceDAO(sparql, nosql, fs);
        MetaDataDaoV2 metaDataDaoV2 = new MetaDataDaoV2(nosql, DeviceAPI.METADATA_COLLECTION_NAME);

        //Handle metadata filter
        List<URI> filteredDeviceUris = null;
        if(metadata != null){
            MetadataSearchFilter metadataFilter = new MetadataSearchFilter();
            Document docVersionOfMetaFilter = Document.parse(metadata);
            metadataFilter.setAttributes(docVersionOfMetaFilter);
            filteredDeviceUris = metaDataDaoV2.distinctUris(metadataFilter);
            if(CollectionUtils.isEmpty(filteredDeviceUris)){
                return buildCSV(Collections.emptyList());
            }
        }

        DeviceSearchFilter filter = new DeviceSearchFilter()
                .setNamePattern(name)
                .setRdfType(rdfType)
                .setIncludeSubTypes(includeSubTypes)
                .setYear(year)
                .setExistenceDate(existenceDate)
                .setBrandPattern(brand)
                .setModelPattern(model)
                .setSnPattern(serialNumber)
                .setCurrentUser(currentUser);
        filter.setIncludedUris(filteredDeviceUris);

        List<DeviceModel> resultList = dao.searchForExport(filter);

        //Handle fetching metadata
        loadMetaData(resultList, metaDataDaoV2);

        return buildCSV(resultList);
    }

    /**
     * Modifies a list of device models by loading their mongo-stored metadata
     * @param devices, device list to load metaData into
     */
    private void loadMetaData(List<DeviceModel> devices, MetaDataDaoV2 metaDataDaoV2) {
        Map<URI,DeviceModel> deviceByUris = new HashMap<>();
        devices.forEach(e -> deviceByUris.put(e.getUri(), e));

        MetadataSearchFilter metadataSearchFilter = new MetadataSearchFilter();
        metadataSearchFilter.setIncludedUris(deviceByUris.keySet());
        metadataSearchFilter.setPage(0);
        metadataSearchFilter.setPageSize(0);
        StreamWithPagination<MetaDataModel> metadataStream = metaDataDaoV2.searchAsStreamWithPagination(metadataSearchFilter);

        metadataStream.forEach(metaDataModel -> {
            if(deviceByUris.containsKey(metaDataModel.getUri())){
                deviceByUris.get(metaDataModel.getUri()).setMetaDataModel(metaDataModel);
            }

        });
    }

    @POST
    @Path("export_by_uris")
    @Operation(summary = "export devices")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return a csv file with device list"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response exportList(
            @Parameter(description = "List of device URI", example = "dev:set/sensor_01") URIsListPostDTO dto
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql, fs);
        List<DeviceModel> resultList = dao.getDevicesByURI(dto.getUris(), currentUser);
        //Handle metadata loading
        loadMetaData(resultList, new MetaDataDaoV2(nosql, DeviceAPI.METADATA_COLLECTION_NAME));
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
                //Remove publication metadata : publisher, publication_date, last_updated_date
                objectNode.remove("publisher");
                objectNode.remove("publication_date");
                objectNode.remove("last_updated_date");
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
        firstObject.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);
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
    @Operation(summary = "Search device data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return data list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataGetDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response searchDeviceData(
            @Parameter(description = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @Parameter(description = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @Parameter(description = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @Parameter(description = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @Parameter(description = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") List<URI> experiments,
            @Parameter(description = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variable") List<URI> variables,
            @Parameter(description = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @Parameter(description = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @Parameter(description = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenance") List<URI> provenances,
            @Parameter(description = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @Parameter(description = "Search by operators", example = DATA_EXAMPLE_OPERATOR) @QueryParam("operators") List<URI> operators,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
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
                Collections.singletonList(uri),
                startInstant,
                endInstant,
                confidenceMin,
                confidenceMax,
                metadataFilter,
                operators,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<DataGetSearchDTO> resultDTOList = dao.modelListToDTO(resultList);

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
    @Operation(summary = "Count device data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the number of data", content = @Content(schema = @Schema(implementation = Integer.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response countDeviceData(
            @Parameter(description = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @Parameter(description = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @Parameter(description = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @Parameter(description = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @Parameter(description = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") List<URI> experiments,
            @Parameter(description = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variable") List<URI> variables,
            @Parameter(description = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @Parameter(description = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @Parameter(description = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenance") List<URI> provenances,
            @Parameter(description = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @Parameter(description = "Search by operators", example = DATA_EXAMPLE_OPERATOR) @QueryParam("operators") List<URI> operators

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
                Collections.singletonList(uri),
                startInstant,
                endInstant,
                confidenceMin,
                confidenceMax,
                metadataFilter,
                operators
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
    @Operation(summary = "Search device datafiles descriptions")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return datafiles list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataGetDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response searchDeviceDatafiles(
            @Parameter(description = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @Parameter(description = "Search by rdf type uri") @QueryParam("rdf_type") URI rdfType,
            @Parameter(description = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @Parameter(description = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @Parameter(description = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @Parameter(description = "Search by experiments", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") List<URI> experiments,
            @Parameter(description = "Search by object uris list", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("scientific_objects") List<URI> objects,
            @Parameter(description = "Search by provenance uris list", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @Parameter(description = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
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

        ListWithPagination<DataFileModel> resultList = dao.searchFiles(
                currentUser,
                rdfType == null ? null : Collections.singletonList(rdfType),
                experiments,
                objects,
                provenances,
                Collections.singletonList(uri),
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
    @Operation(summary = "Get variables linked to the device")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return variables list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = NamedResourceDTO.class))))
    })
    public Response getDeviceVariables(
            @Parameter(description = "Device URI", example = DeviceAPI.DEVICE_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql, fs);
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
    @Operation(summary = "Get provenances of data that have been measured on this device")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return provenances list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProvenanceGetDTO.class))))
    })
    public Response getDeviceDataProvenances(
            @Parameter(description = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
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
    @Operation(summary = "Get provenances of datafiles linked to this device")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return provenances list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProvenanceGetDTO.class))))
    })
    public Response getDeviceDataFilesProvenances(
            @Parameter(description = "Device URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        List<ProvenanceModel> provenances = dataDAO.getProvenancesByDevice(currentUser, uri, DataDAO.FILE_COLLECTION_NAME);
        List<ProvenanceGetDTO> dtoList = provenances.stream().map(ProvenanceGetDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}/facility")
    @Operation(summary = "Get devices by facility")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return devices by facility", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeviceGetDTO.class))))
    })
    public Response getDevicesByFacility(
            @Parameter(description = "target URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI facilityUri
    ) throws Exception {

        DeviceDAO dao = new DeviceDAO(sparql, nosql, fs);

        //TODO these next few lines that call other logic classes should be in a DeviceLogic class in the future
        //TODO No control on devices being in two facilities at once, plus once a device has been moved somewhere else,
        // it will still always be included in facilities that used to have it

        //First fetch the correct LocationObservations, whose Location's 'to' field is our Facility
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2(), sparql);
        LocationObservationSearchFilter locationObservationSearchFilter = new LocationObservationSearchFilter();
        locationObservationSearchFilter.setTo(facilityUri);
        final int pageSizePerIter = 50;
        int currentPage = 0;
        boolean done = false;
        locationObservationSearchFilter.setPageSize(pageSizePerIter);
        ListWithPagination<LocationObservationModel> nextLot = null;
        List<URI> featuresOfInterest = new ArrayList<>();
        while (!done) {
            locationObservationSearchFilter.setPage(currentPage);
            nextLot = locationObservationLogic.searchLocationObservations(locationObservationSearchFilter);
            if(nextLot.getList().size() < pageSizePerIter) {
                done = true;
            }
            featuresOfInterest.addAll(nextLot.getList().stream().map(LocationObservationModel::getFeatureOfInterest).toList());
            currentPage++;
        }

        //Then use the feature of interests to run a Device search
        List<DeviceModel> results = dao.getDevicesByURI(featuresOfInterest, currentUser);

        if (results == null) {
            return new PaginatedListResponse<>().getResponse();
        }

        ListWithPagination<DeviceModel> devices = new ListWithPagination<>(results);
        ListWithPagination<DeviceGetDTO> dtoList = devices.convert(DeviceGetDTO.class, DeviceGetDTO::getDTOFromModel);

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @POST
    @Path("export_geospatial")
    @Operation(summary = "Export a given list of devices URIs to shapefile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data shapefile exported")
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportGeospatial(
            @Parameter(description = "Devices") List<GeometryDTO> selectedObjects,
            @Parameter(description = "properties selected", example = "test") @QueryParam("selected_props") List<URI> selectedProps,
            @Parameter(description = "export format (shp/geojson)", example = "shp") @QueryParam("format") String format,
            @Parameter(description = "Page size limited to 10,000 objects", example = "10000") @QueryParam("pageSize") @Max(10000) int pageSize

    ) throws Exception {

        DeviceDAO dao = new DeviceDAO(sparql, nosql,fs);
        Map<URI, GeoJsonObject> selectedObjectsMap = new HashMap<>();

        //Get device exported URI
        selectedObjects.forEach(o ->{
            selectedObjectsMap.put(URI.create(SPARQLDeserializers.getExpandedURI(o.getUri())), o.getGeometry());
        });

        // Search exported device detail according the selected uris
        List<DeviceModel> objDetailList = dao.getDevicesByURI(new ArrayList<>(selectedObjectsMap.keySet()),currentUser);

        //Convert
        DeviceGeospatialExporter shpExport = new DeviceGeospatialExporter();
        Map<String, byte[]> result = shpExport.exportFormat(selectedProps, objDetailList, selectedObjectsMap,format);

        return Response.ok(result.entrySet().stream().findFirst().get().getValue(), MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + result.entrySet().stream().findFirst().get().getValue() + "\"")
                .build();
    }

}
