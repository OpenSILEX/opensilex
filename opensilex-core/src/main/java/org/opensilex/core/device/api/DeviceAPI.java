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
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

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
@Path("/core/device")
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
    
    @POST
    //@Path("create")
    @ApiOperation("Create a device")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DEVICE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DEVICE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a devcie", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A device with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createDevice(
            @ApiParam("Device description") @Valid DeviceDTO deviceDTO,
            @ApiParam(value = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {       
        DeviceDAO deviceDAO = new DeviceDAO(sparql);
        ErrorResponse error = check(deviceDTO, false);
        if (error != null) {
            return error.getResponse();
        }
        if (!checkOnly){
            URI uri = deviceDAO.create(deviceDTO, currentUser);
            return new ObjectUriResponse(Response.Status.CREATED, uri).getResponse();
        } else {
            return new ObjectUriResponse().getResponse();
        }
    }
    
    @GET
    //@Path("search")
    @ApiOperation("Search list of devices")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of devices corresponding to the given search parameters", response = DeviceDTO.class, responseContainer = "List")
    })    
    public Response searchDevices(
            @ApiParam(value = "RDF type filter", example = "vocabulary:SensingDevice") @QueryParam("rdfTypes") @ValidURI List<URI> rdfTypes,
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("namePattern") String namePattern,
            @ApiParam(value = "Search by year", example = "2017") @QueryParam("year")  @Min(999) @Max(10000) Integer year,
            @ApiParam(value = "Regex pattern for filtering by brand", example = ".*") @DefaultValue("") @QueryParam("brandPattern") String brandPattern,
            @ApiParam(value = "Regex pattern for filtering by model", example = ".*") @DefaultValue("") @QueryParam("modelPattern") String modelPattern,
            @ApiParam(value = "Regex pattern for filtering by serial number", example = ".*") @DefaultValue("") @QueryParam("serialNumberPattern") String snPattern,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DeviceDAO dao = new DeviceDAO(sparql);
        ListWithPagination<DeviceModel> devices = dao.search(
            namePattern,
            rdfTypes,
            year,
            brandPattern,
            modelPattern,
            snPattern,
            currentUser,
            page,
            pageSize);

        ListWithPagination<DeviceDTO> dtoList = devices.convert(DeviceDTO.class, DeviceDTO::getDTOFromModel);

        return new PaginatedListResponse<DeviceDTO>(dtoList).getResponse();
    }
    
    @GET
    @Path("{uri}")
    @ApiOperation("Get device detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return device details corresponding to the devcie URI", response = DeviceDTO.class)
    })
    public Response getDevice(
            @ApiParam(value = "device URI", example = "http://example.com/", required = true)
            @PathParam("uri") URI uri
    ) throws Exception {

        DeviceDAO dao = new DeviceDAO(sparql);

        DeviceModel model = dao.getDeviceByURI(uri, currentUser);

        Response response;
        if (model != null) {
            response = new SingleObjectResponse<>(DeviceDTO.getDTOFromModel(model)).getResponse();
        } else {
            response = Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        
        return response;
    }
    
    @PUT
    //@Path("update")
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
            @Valid DeviceDTO dto
    ) throws Exception {

        URI devType = dto.getType();

        DeviceDAO dao = new DeviceDAO(sparql);

        URI devURI = dao.update(devType, dto.getUri(), dto.getName(), dto.getRelations(), currentUser);

        return new ObjectUriResponse(devURI).getResponse();
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
        DeviceDAO dao = new DeviceDAO(sparql);
        
        dao.delete(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        
    }
    
    private ErrorResponse check(DeviceDTO deviceDTO, boolean update) throws Exception {

        if (!update) {
            // check if germplasm URI already exists
            if (sparql.uriExists(DeviceModel.class, deviceDTO.getUri())) {
                // Return error response 409 - CONFLICT if URI already exists
                return new ErrorResponse(
                        Response.Status.CONFLICT,
                        "device URI already exists",
                        "Duplicated URI: " + deviceDTO.getUri()
                );
            }

            AskBuilder askQuery = new AskBuilder()
                .from(sparql.getDefaultGraph(DeviceModel.class).toString())
                .addWhere("?uri", RDF.type, SPARQLDeserializers.nodeURI(deviceDTO.getType()))
                .addWhere("?uri", RDFS.label, deviceDTO.getName());//boolean exists = germplasmDAO.labelExistsCaseInsensitive(germplasmDTO.getLabel(),germplasmDTO.getRdfType());
            boolean exists = sparql.executeAskQuery(askQuery);
            if (exists) {
                // Return error response 409 - CONFLICT if label already exists
                return new ErrorResponse(
                        Response.Status.PRECONDITION_FAILED,
                        "Device label already exists for this type",
                        "Duplicated label: " + deviceDTO.getName()
                );
            }
        }

        //Check that the given fromAccession, fromVariety or fromSpecies exist in DB
        if (deviceDTO.getPersonInCharge()!= null) {
            if (!sparql.uriExists(UserModel.class,deviceDTO.getPersonInCharge())) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given person doesn't exist in the database",
                        "unknown person : " + deviceDTO.getPersonInCharge().toString()
                );
            }
        }

        return null;
    }
}
