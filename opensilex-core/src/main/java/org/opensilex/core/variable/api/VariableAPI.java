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
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.core.URIsListPostDTO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Api(VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID)
@Path(VariableAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class VariableAPI {

    public static final String PATH = "/core/variables";

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


    @CurrentUser
    UserModel currentUser;

    private VariableDAO getDao() throws URISyntaxException {
        return new VariableDAO(sparql,mongodb,fs);
    }

    @POST
    @ApiOperation("Add a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A variable is created", response = ObjectUriResponse.class),
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
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

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
            @ApiParam(value = "Variable URI", example = "http://opensilex.dev/set/variables/Plant_Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        VariableDAO dao = getDao();
        VariableModel variable = dao.get(uri);
        if (variable == null) {
            throw new NotFoundURIException(uri);
        }
        return new SingleObjectResponse<>(new VariableDetailsDTO(variable)).getResponse();
    }


    @PUT
    @ApiOperation("Update a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variable updated", response = ObjectUriResponse.class),
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
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
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
            @ApiResponse(code = 200, message = "Variable deleted", response = ObjectUriResponse.class),
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
                    " _entity_name/entityName : the name of the variable entity\n\n"+
                    " _characteristic_name/characteristicName : the name of the variable characteristic\n\n"+
                    " _method_name/methodName : the name of the variable method\n\n"+
                    " _unit_name/unitName : the name of the variable unit\n\n"
    )
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables", response = VariableGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariables(
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String name,
            @ApiParam(value = "Entity filter") @QueryParam("entity") @ValidURI URI entity,
            @ApiParam(value = "Entity of interest filter") @QueryParam("entity_of_interest") @ValidURI URI interestEntity,
            @ApiParam(value = "Characteristic filter") @QueryParam("characteristic") @ValidURI URI characteristic,
            @ApiParam(value = "Method filter") @QueryParam("method") @ValidURI URI method,
            @ApiParam(value = "Unit filter") @QueryParam("unit") @ValidURI URI unit,
            @ApiParam(value = "Group filter") @QueryParam("group_of_variables") @ValidURI URI group,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        VariableDAO dao = getDao();
        ListWithPagination<VariableModel> variables = dao.search(
                name,
                entity,
                interestEntity,
                characteristic,
                method,
                unit,
                group,                
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<VariableGetDTO> resultDTOList = variables.convert(
                VariableGetDTO.class,
                VariableGetDTO::fromModel
        );
        
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    @GET
    @Path("details")
    @ApiOperation(
            value = "Search detailed variables by name, long name, entity, characteristic, method or unit name",
            notes = "The following fields could be used for sorting : \n\n" +
                    " _entity_name : the name of the variable entity\n\n"+
                    " _characteristic_name : the name of the variable characteristic\n\n"+
                    " _method_name : the name of the variable method\n\n"+
                    " _unit_name : the name of the variable unit\n\n"
    )
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return detailed variables", response = VariableDetailsDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariablesDetails(
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        VariableDAO dao = getDao();
        ListWithPagination<VariableModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<VariableDetailsDTO> resultDTOList = resultList.convert(
                VariableDetailsDTO.class,
                VariableDetailsDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("datatypes")
    @ApiOperation(value = "Get variables datatypes")
    @ApiProtected
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
    @Path("by_uris")
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
            @ApiParam(value = "Variables URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        VariableDAO dao = getDao();
        List<VariableModel> models = dao.getList(uris);

        if (!models.isEmpty()) {
            List<VariableDetailsDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                resultDTOList.add(new VariableDetailsDTO(result));
            });

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
                // Convert list to DTO
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
        if(jsonTree.isArray()) {
            for(JsonNode jsonNode : jsonTree) {               
                list.add(jsonNode);              
            }
        }
        
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance, list);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = arrayNode.elements().next();
        firstObject.fieldNames().forEachRemaining(fieldName -> {
            csvSchemaBuilder.addColumn(fieldName);
        });
                        
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
        StringWriter str = new StringWriter();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class).with(csvSchema).writeValue(str, arrayNode);

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_classic_variable" + dtf.format(date) + ".csv";

        return Response.ok(str.toString(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }

    private Response buildCSVForDetailsExport(List<VariableModel> variableList) throws IOException {
                // Convert list to DTO
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
        if(jsonTree.isArray()) {
            for(JsonNode jsonNode : jsonTree) {               
                list.add(jsonNode);              
            }
        }
        
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance, list);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = arrayNode.elements().next();
        firstObject.fieldNames().forEachRemaining(fieldName -> {
            csvSchemaBuilder.addColumn(fieldName);
        });
                        
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
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
        List<VariableModel> variableList = dao.getList(dto.getUris());
        
        return buildCSVForDetailsExport(variableList);
        
    }
}

