//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.net.URI;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import javax.validation.Valid;
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

import org.opensilex.core.variablesGroup.dal.VariablesGroupDAO;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.core.variable.api.VariableAPI;

import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.account.dal.AccountModel;

import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;

import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
/**
 * @author Hamza IKIOU
 */

@Api(VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID)
@Path(VariablesGroupAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class VariablesGroupAPI {
    
    public static final String PATH = "/core/variables_group";
    
    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;
    
    @POST
    @ApiOperation("Add a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A variables group is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A variables group with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)   
    public Response createVariablesGroup(@ApiParam("Variables group description") @Valid VariablesGroupCreationDTO dto) throws Exception {
        try {
            VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
            VariablesGroupModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED, shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Variables group already exists", duplicateUriException.getMessage()).getResponse();
        }
    }
    
    
    
    @GET
    @ApiOperation(value = "Search variables groups")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return variables groups", response = VariablesGroupGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariablesGroups(
            @ApiParam(value = "Regex pattern for filtering by name") @QueryParam("name") String name ,
            @ApiParam(value = "Variable URI") @QueryParam("variableUri") URI variableUri ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        ListWithPagination<VariablesGroupModel> resultList = dao.search(
                name,
                variableUri,
                orderByList,
                page,
                pageSize,
                currentUser.getLanguage()
        );

        ListWithPagination<VariablesGroupGetDTO> resultDTOList = resultList.convert(
                VariablesGroupGetDTO.class,
                VariablesGroupGetDTO::fromModel
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }    
    
 
    
   
    @GET
    @Path("{uri}")
    @ApiOperation("Get a variables group")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variables group retrieved", response = VariablesGroupGetDTO.class),
            @ApiResponse(code = 404, message = "Unknown variables group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariablesGroup(@ApiParam(value = "Variables group URI", required = true) @PathParam("uri") @NotNull URI uri) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        VariablesGroupModel model = dao.get(uri);
        if (model == null) {
            throw new NotFoundURIException(uri);
        }
        return new SingleObjectResponse<>(VariablesGroupGetDTO.fromModel(model)).getResponse();
    }
    
    
    
    
    @GET
    @Path("by_uris")
    @ApiOperation("Get variables groups by their URIs")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables groups", response = VariablesGroupGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Variables group not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariablesGroupByURIs(@ApiParam(value = "Variables group URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        List<VariablesGroupModel> models = dao.getList(uris);

        if (!models.isEmpty()) {
            List<VariablesGroupGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> resultDTOList.add(VariablesGroupGetDTO.fromModel(result)));

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Variables group not found", "Unknown variables group URIs or variables URIs").getResponse();
        }
    }
   
    
    
    
    @PUT
    @ApiOperation("Update a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variables group updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown variables group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVariablesGroup(@ApiParam("Variables group description") @Valid VariablesGroupUpdateDTO dto) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        VariablesGroupModel model = dao.update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }
    
    
    
    
    
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variables group deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown variables group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariablesGroup(
            @ApiParam(value = "Variables group URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(uri).getResponse();
    }
}
