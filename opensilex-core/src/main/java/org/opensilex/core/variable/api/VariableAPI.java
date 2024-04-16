//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.swagger.annotations.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.core.CoreModule;
import org.opensilex.core.URIsListPostDTO;
import org.opensilex.core.external.opensilex.SharedResourceInstanceService;
import org.opensilex.core.sharedResource.CopyResourceDTO;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.core.variable.api.characteristic.CharacteristicAPI;
import org.opensilex.core.variable.api.characteristic.CharacteristicDetailsDTO;
import org.opensilex.core.variable.api.entity.EntityAPI;
import org.opensilex.core.variable.api.entity.EntityDetailsDTO;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityAPI;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityDetailsDTO;
import org.opensilex.core.variable.api.method.MethodAPI;
import org.opensilex.core.variable.api.method.MethodDetailsDTO;
import org.opensilex.core.variable.api.unit.UnitAPI;
import org.opensilex.core.variable.api.unit.UnitDetailsDTO;
import org.opensilex.core.variable.dal.*;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Api(VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID)
@Path(VariableAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class VariableAPI {

    public static final String PATH = "/core/variables";

    public static final String GET_BY_URIS_PATH = "by_uris";
    public static final String GET_BY_URIS_URI_PARAM = "uris";
    private static final String SHARED_RESOURCE_INSTANCE_PARAM = "sharedResourceInstance";

    public static final String CREDENTIAL_VARIABLE_GROUP_ID = "Variables";
    public static final String CREDENTIAL_VARIABLE_GROUP_LABEL_KEY = "credential-groups.variables";

    public static final String CREDENTIAL_VARIABLE_MODIFICATION_ID = "variable-modification";
    public static final String CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_VARIABLE_DELETE_ID = "variable-delete";
    public static final String CREDENTIAL_VARIABLE_DELETE_LABEL_KEY = "credential.default.delete";


    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBService mongodb;
    @Inject
    private FileStorageService fs;

    @Inject
    private CoreModule coreModule;

    @Context
    protected HttpServletRequest httpRequest;


    @CurrentUser
    AccountModel currentUser;

    private VariableDAO getDao() throws URISyntaxException {
        return new VariableDAO(sparql, mongodb, fs);
    }

    @POST
    @ApiOperation("Add a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A variable is created", response = URI.class),
            @ApiResponse(code = 409, message = "A Variable with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    public Response createVariable(
            @ApiParam("Variable description") @Valid VariableCreationDTO dto
    ) throws Exception {
        try {
            VariableDAO dao = getDao();
            VariableModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());

            model = dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new CreatedUriResponse(shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Variable already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a variable")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variable retrieved", response = VariableDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown variable URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariable(
            @ApiParam(value = "Variable URI", example = "http://opensilex.dev/set/variables/Plant_Height", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Shared resource instance") @QueryParam(VariableAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
    ) throws Exception {
        if (sharedResourceInstance == null) {
            VariableDAO dao = getDao();
            VariableModel variable = dao.get(uri);
            if (variable == null) {
                throw new NotFoundURIException(uri);
            }

            // This code might be reused later if the SRI mechanic is extended to resources other than variables
            // In that case, the logic can be moved into the response object to avoid code duplication
            SharedResourceInstanceDTO sharedResourceInstanceDTO = coreModule.tryGetSharedResourceInstanceDTO(
                    variable.getFromSharedResourceInstance(), currentUser.getLanguage());
            if (variable.getFromSharedResourceInstance() != null && sharedResourceInstanceDTO == null) {
                VariableDetailsDTO dto = new VariableDetailsDTO(variable);
                if (Objects.nonNull(variable.getPublisher())) {
                    dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(variable.getPublisher())));
                }
                return new SingleObjectResponse<>(dto).addMetadataStatus(new StatusDTO(
                        "Missing SRI from configuration : " + variable.getFromSharedResourceInstance(),
                        StatusLevel.WARNING,
                        "server.warnings.shared-resource-instance-unknown",
                        Collections.singletonMap("uri", variable.getFromSharedResourceInstance().toString()))).getResponse();
            } else {
                VariableDetailsDTO dto = new VariableDetailsDTO(variable, sharedResourceInstanceDTO);
                if (Objects.nonNull(variable.getPublisher())) {
                    dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(variable.getPublisher())));
                }
                return new SingleObjectResponse<>(dto).getResponse();
            }
        }

        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage());
        VariableDetailsDTO dto = service.getByURI(VariableAPI.PATH, uri, VariableDetailsDTO.class);
        return new SingleObjectResponse<>(dto).getResponse();
    }


    @PUT
    @ApiOperation("Update a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variable updated", response = URI.class),
            @ApiResponse(code = 404, message = "Unknown variable URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVariable(
            @ApiParam("Variable description") @Valid VariableUpdateDTO dto
    ) throws Exception {
        VariableDAO dao = getDao();

        VariableModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK, shortUri).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variable deleted", response = URI.class),
            @ApiResponse(code = 404, message = "Unknown variable URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariable(
            @ApiParam(value = "Variable URI", example = "http://opensilex.dev/set/variables/Plant_Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        VariableDAO dao = getDao();
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @ApiOperation(
            value = "Search variables",
            notes = "The following fields could be used for sorting : \n\n" +
                    " _entity_name/entityName : the name of the variable entity\n\n" +
                    " _characteristic_name/characteristicName : the name of the variable characteristic\n\n" +
                    " _method_name/methodName : the name of the variable method\n\n" +
                    " _unit_name/unitName : the name of the variable unit\n\n"
    )
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return variables", response = VariableGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariables(
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern,
            @ApiParam(value = "Entity filter") @QueryParam("entity") @ValidURI URI entity,
            @ApiParam(value = "Entity of interest filter") @QueryParam("entity_of_interest") @ValidURI URI interestEntity,
            @ApiParam(value = "Characteristic filter") @QueryParam("characteristic") @ValidURI URI characteristic,
            @ApiParam(value = "Method filter") @QueryParam("method") @ValidURI URI method,
            @ApiParam(value = "Unit filter") @QueryParam("unit") @ValidURI URI unit,
            @ApiParam(value = "Included in group filter") @QueryParam("group_of_variables") @ValidURI URI includedIngroup,
            @ApiParam(value = "Not included in group filter") @QueryParam("not_included_in_group_of_variables") @ValidURI URI notIncluedInGroup,
            @ApiParam(value = "Data type filter") @QueryParam("data_type") @ValidURI URI dataType,
            @ApiParam(value = "Time interval filter") @QueryParam("time_interval") String timeInterval,
            @ApiParam(value = "Species filter") @QueryParam("species") List<URI> species,
            @ApiParam(value = "Set this param to true to get associated data") @DefaultValue("false") @QueryParam("withAssociatedData") boolean withAssociatedData,
            @ApiParam(value = "Experiment filter") @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Scientific object filter") @QueryParam("scientific_objects") List<URI> objects,
            @ApiParam(value = "Device filter") @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "Shared resource instance") @QueryParam(VariableAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
    ) throws Exception {
        if (sharedResourceInstance == null) {
            VariableSearchFilter filter = new VariableSearchFilter()
                    .setNamePattern(namePattern)
                    .setEntity(entity)
                    .setInterestEntity(interestEntity)
                    .setCharacteristic(characteristic)
                    .setMethod(method)
                    .setUnit(unit)
                    .setIncludedInGroup(includedIngroup)
                    .setNotIncludedInGroup(notIncluedInGroup)
                    .setDataType(dataType)
                    .setTimeInterval(timeInterval)
                    .setSpecies(species)
                    .setWithAssociatedData(withAssociatedData)
                    .setDevices(devices)
                    .setExperiments(experiments)
                    .setObjects(objects)
                    .setUserModel(currentUser);

            filter.setPage(page)
                    .setPageSize(pageSize)
                    .setLang(currentUser.getLanguage())
                    .setOrderByList(orderByList);

            ListWithPagination<VariableGetDTO> variables = getDao().search(filter).convert(
                    VariableGetDTO.class,
                    model -> VariableGetDTO.fromModel(model, coreModule.getSharedResourceInstancesFromConfiguration(currentUser.getLanguage()))
            );

            return new PaginatedListResponse<>(variables).getResponse();
        }

        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage());

        Map<String, String[]> searchParams = new HashMap<>(httpRequest.getParameterMap());
        searchParams.remove(VariableAPI.SHARED_RESOURCE_INSTANCE_PARAM);
        ListWithPagination<VariableGetDTO> variableList = service.search(PATH, searchParams, VariableGetDTO.class);

        // Check if variable exists in local instance
        List<URI> variableUriList = variableList.getList().stream().map(VariableGetDTO::getUri).collect(Collectors.toList());
        Set<URI> existingUriList = sparql.getExistingUris(VariableModel.class, variableUriList, true);
        for (VariableGetDTO variable : variableList.getList()) {
            if (existingUriList.contains(variable.getUri())) {
                variable.setOnLocal(true);
            }
        }

        return new PaginatedListResponse<>(variableList).getResponse();
    }

    @GET
    @Path("details")
    @ApiOperation(
            value = "Search detailed variables by name, long name, entity, characteristic, method or unit name",
            notes = "The following fields could be used for sorting : \n\n" +
                    " _entity_name : the name of the variable entity\n\n" +
                    " _characteristic_name : the name of the variable characteristic\n\n" +
                    " _method_name : the name of the variable method\n\n" +
                    " _unit_name : the name of the variable unit\n\n"
    )
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return detailed variables", response = VariableDetailsDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariablesDetails(
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        VariableSearchFilter filter = new VariableSearchFilter()
                .setNamePattern(namePattern)
                .setFetchSpecies(true)
                .setUserModel(currentUser);

        filter.setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize)
                .setLang(currentUser.getLanguage());

        VariableDAO dao = getDao();
        ListWithPagination<VariableModel> resultList = dao.search(filter);

        ListWithPagination<VariableDetailsDTO> resultDTOList = resultList.convert(
                VariableDetailsDTO.class,
                VariableDetailsDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("datatypes")
    @ApiOperation(value = "Get variables datatypes")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return data types", response = VariableDatatypeDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatatypes() throws URISyntaxException {

        List<VariableDatatypeDTO> variablesXsdTypes = Arrays.asList(
                new VariableDatatypeDTO(XSDDatatype.XSDboolean, "datatypes.boolean"),
                new VariableDatatypeDTO(XSDDatatype.XSDdate, "datatypes.date"),
                new VariableDatatypeDTO(XSDDatatype.XSDdateTime, "datatypes.datetime"),
                new VariableDatatypeDTO(XSDDatatype.XSDdecimal, "datatypes.decimal"),
                new VariableDatatypeDTO(XSDDatatype.XSDinteger, "datatypes.number"),
                new VariableDatatypeDTO(XSDDatatype.XSDstring, "datatypes.string")
        );

        return new PaginatedListResponse<>(variablesXsdTypes).getResponse();
    }

    /**
     * * Return a list of variables corresponding to the given URIs
     *
     * @param uris list of variables uri
     * @return Corresponding list of variables
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path(GET_BY_URIS_PATH)
    @ApiOperation("Get detailed variables by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return variables", response = VariableDetailsDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Variable not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getVariablesByURIs(
            @ApiParam(value = "Variables URIs", required = true) @QueryParam(GET_BY_URIS_URI_PARAM) @NotNull List<URI> uris
    ) throws Exception {
        VariableDAO dao = getDao();
        List<VariableModel> models = dao.getList(uris, currentUser.getLanguage());

        if (!models.isEmpty()) {
            List<VariableDetailsDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> resultDTOList.add(new VariableDetailsDTO(result)));

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Variables not found",
                    "Unknown variable URIs"
            ).getResponse();
        }
    }

    private Response buildCSVForClassicExport(List<VariableModel> variableList) throws IOException {
        List<VariableExportDTOClassic> resultDTOList = new ArrayList<>();
        for (VariableModel variable : variableList) {
            VariableExportDTOClassic dto = VariableExportDTOClassic.fromModel(variable);
            resultDTOList.add(dto);
        }

        if (resultDTOList.isEmpty()) {
            resultDTOList.add(new VariableExportDTOClassic()); // to return an empty table
        }

        //Construct manually json to convert it to csv
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);
        List<JsonNode> list = new ArrayList<>();
        if (jsonTree.isArray()) {
            for (JsonNode jsonNode : jsonTree) {
                list.add(jsonNode);
            }
        }

        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance, list);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = arrayNode.elements().next();
        firstObject.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);

        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader().withArrayElementSeparator(" ");
        StringWriter str = new StringWriter();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class).with(csvSchema).writeValue(str, arrayNode);

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_classic_variable" + dtf.format(date) + ".csv";

        return Response.ok(str.toString(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }

    private Response buildCSVForDetailsExport(List<VariableModel> variableList) throws IOException {
        List<VariableExportDTODetails> resultDTOList = new ArrayList<>();
        for (VariableModel variable : variableList) {
            VariableExportDTODetails dto = VariableExportDTODetails.fromModel(variable);
            resultDTOList.add(dto);
        }

        if (resultDTOList.isEmpty()) {
            resultDTOList.add(new VariableExportDTODetails()); // to return an empty table
        }

        //Construct manually json to convert it to csv
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);
        List<JsonNode> list = new ArrayList<>();
        if (jsonTree.isArray()) {
            for (JsonNode jsonNode : jsonTree) {
                list.add(jsonNode);
            }
        }

        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance, list);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = arrayNode.elements().next();
        firstObject.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);

        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader().withArrayElementSeparator(" ");
        StringWriter str = new StringWriter();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class).with(csvSchema).writeValue(str, arrayNode);

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_detailed_variable" + dtf.format(date) + ".csv";

        return Response.ok(str.toString(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }

    @POST
    @Path("export_classic_by_uris")
    @ApiOperation("export variable by list of uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with variable list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response classicExportVariableByURIs(
            @ApiParam(value = "List of variable URI", example = "http://opensilex.dev/set/variables/Plant_Height") URIsListPostDTO dto
    ) throws Exception {
        VariableDAO dao = getDao();
        List<VariableModel> variableList = dao.getList(dto.getUris());

        return buildCSVForClassicExport(variableList);

    }

    @POST
    @Path("export_details_by_uris")
    @ApiOperation("export detailed variable by list of uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with detailed variable list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response detailsExportVariableByURIs(
            @ApiParam(value = "List of variable URI", example = "http://opensilex.dev/set/variables/Plant_Height") URIsListPostDTO dto
    ) throws Exception {
        VariableDAO dao = getDao();
        List<VariableModel> variableList = dao.getListForExport(dto.getUris(), currentUser.getLanguage());

        return buildCSVForDetailsExport(variableList);

    }


    @POST
    @Path("copy_from_shared_resource_instance")
    @ApiOperation("Copy the selected variables from the shared resource instance")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Copy variables", response = VariableCopyResponseDTO.class)
    })
    public Response copyFromSharedResourceInstance(
            @ApiParam(value = "List of variable URI to copy", required = true) CopyResourceDTO dto
    ) throws Exception {
        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(dto.getSharedResourceInstance()), currentUser.getLanguage());

        Set<URI> variableSetToCopy = sparql.getExistingUris(VariableModel.class, dto.getUris(), false);

        if (CollectionUtils.isEmpty(variableSetToCopy)) {
            return new SingleObjectResponse<>(new VariableCopyResponseDTO()).getResponse();
        }

        ListWithPagination<VariableDetailsDTO> variableDetailsList = service.getListByURI(Paths.get(PATH, GET_BY_URIS_PATH).toString(),
                GET_BY_URIS_URI_PARAM, variableSetToCopy, VariableDetailsDTO.class);

        List<URI> entityUris = new ArrayList<>();
        List<URI> entityOfInterestUris = new ArrayList<>();
        List<URI> characteristicUris = new ArrayList<>();
        List<URI> methodUris = new ArrayList<>();
        List<URI> unitUris = new ArrayList<>();

        for (VariableDetailsDTO variable : variableDetailsList.getList()) {
            entityUris.add(variable.getEntity().getUri());
            if (variable.getEntityOfInterest() != null) {
                entityOfInterestUris.add(variable.getEntityOfInterest().getUri());
            }
            characteristicUris.add(variable.getCharacteristic().getUri());
            methodUris.add(variable.getMethod().getUri());
            unitUris.add(variable.getUnit().getUri());
        }

        VariableCopyResponseDTO resultDto = new VariableCopyResponseDTO();

        try {
            sparql.startTransaction();

            resultDto.setEntityUris(new ArrayList<>(
                    createIfMissing(EntityModel.class, EntityDetailsDTO.class, entityUris, service, EntityAPI.PATH)));
            resultDto.setInterestEntityUris(new ArrayList<>(
                    createIfMissing(InterestEntityModel.class, InterestEntityDetailsDTO.class, entityOfInterestUris, service, InterestEntityAPI.PATH)));
            resultDto.setCharacteristicUris(new ArrayList<>(
                    createIfMissing(CharacteristicModel.class, CharacteristicDetailsDTO.class, characteristicUris, service, CharacteristicAPI.PATH)));
            resultDto.setMethodUris(new ArrayList<>(
                    createIfMissing(MethodModel.class, MethodDetailsDTO.class, methodUris, service, MethodAPI.PATH)));
            resultDto.setUnitUris(new ArrayList<>(
                    createIfMissing(UnitModel.class, UnitDetailsDTO.class, unitUris, service, UnitAPI.PATH)));

            createBaseVariable(VariableModel.class, variableDetailsList.getList(), service);

            resultDto.setVariableUris(new ArrayList<>(variableSetToCopy));

            sparql.commitTransaction();

            return new SingleObjectResponse<>(resultDto).getResponse();
        } catch (Exception e) {
            sparql.rollbackTransaction();
            throw e;
        }
    }

    private <T extends BaseVariableModel<T>, U extends BaseVariableDetailsDTO<T>> Set<URI> createIfMissing(Class<T> modelClass, Class<U> detailsClass, Collection<URI> uriCollection, SharedResourceInstanceService service, String apiPath) throws Exception {
        Set<URI> missingUriSet = sparql.getExistingUris(modelClass, uriCollection, false);

        List<U> detailsList = service.getListByURI(Paths.get(apiPath, GET_BY_URIS_PATH).toString(),
                        GET_BY_URIS_URI_PARAM, missingUriSet, detailsClass)
                .getList();

        createBaseVariable(modelClass, detailsList, service);

        return missingUriSet;
    }

    private <T extends BaseVariableModel<T>, U extends BaseVariableDetailsDTO<T>> void createBaseVariable(Class<T> modelClass, Collection<U> detailsCollection, SharedResourceInstanceService service) throws Exception {
        BaseVariableDAO<T> dao = new BaseVariableDAO<>(modelClass, sparql);

        List<T> modelList = detailsCollection.stream().map(detailsDto -> {
            T model = detailsDto.toModel();
            model.setPublisher(currentUser.getUri());
            model.setFromSharedResourceInstance(service.getSharedResourceInstanceURI());
            return model;
        }).collect(Collectors.toList());

        dao.createList(modelList);
    }
}
