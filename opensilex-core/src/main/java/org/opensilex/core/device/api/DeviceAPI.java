package org.opensilex.core.device.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
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
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
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
            @ApiParam(value = "Regex pattern for filtering by brand", example = ".*") @DefaultValue("") @QueryParam("brandPattern") String brandPattern,
            @ApiParam(value = "Regex pattern for filtering by model", example = ".*") @DefaultValue("") @QueryParam("modelPattern") String modelPattern,
            @ApiParam(value = "Regex pattern for filtering by serial number", example = ".*") @DefaultValue("") @QueryParam("serialNumberPattern") String snPattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "namePattern=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql, nosql);
        ListWithPagination<DeviceModel> devices = dao.search(
            namePattern,
            rdfType,
            year,
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
        @ApiResponse(code = 200, message = "Return device details corresponding to the devcie URI", response = DeviceGetDetailsDTO.class)
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
}
