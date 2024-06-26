//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import io.swagger.annotations.*;
import org.opensilex.core.CoreModule;
import org.opensilex.core.external.opensilex.SharedResourceInstanceService;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variablesGroup.dal.VariablesGroupDAO;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.response.*;
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
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

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
    public static final String GET_BY_URIS_PATH = "by_uris";
    public static final String GET_BY_URIS_URI_PARAM = "uris";
    private static final String SHARED_RESOURCE_INSTANCE_PARAM = "sharedResourceInstance";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private CoreModule coreModule;

    @Context
    protected HttpServletRequest httpRequest;

    @POST
    @ApiOperation("Add a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A variables group is created", response = URI.class),
            @ApiResponse(code = 409, message = "A variables group with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)   
    public Response createVariablesGroup(@ApiParam("Variables group description") @Valid VariablesGroupCreationDTO dto) throws Exception {
        try {
            VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
            VariablesGroupModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());

            model = dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new CreatedUriResponse(shortUri).getResponse();

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
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "Shared resource instance") @QueryParam(VariablesGroupAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
    ) throws Exception {
        if (sharedResourceInstance == null) {
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

        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage()
        );

        Map<String, String[]> searchParams = new HashMap<>(httpRequest.getParameterMap());
        searchParams.remove(VariablesGroupAPI.SHARED_RESOURCE_INSTANCE_PARAM);
        return new PaginatedListResponse<>(service.search(VariablesGroupAPI.PATH, searchParams, VariablesGroupGetDTO.class))
                .getResponse();
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
        VariablesGroupGetDTO dto = VariablesGroupGetDTO.fromModel(model);
        if (Objects.nonNull(model.getPublisher())) {
            dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path(VariablesGroupAPI.GET_BY_URIS_PATH)
    @ApiOperation("Get variables groups by their URIs")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables groups", response = VariablesGroupGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Variables group not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariablesGroupByURIs(
            @ApiParam(value = "Variables group URIs", required = true) @QueryParam(VariablesGroupAPI.GET_BY_URIS_URI_PARAM) @NotNull List<URI> uris,
            @ApiParam(value = "Shared resource instance") @QueryParam(VariablesGroupAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
    ) throws Exception {
        if (sharedResourceInstance == null) {
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

        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage()
        );

        ListWithPagination<VariablesGroupGetDTO> detailsList = service.getListByURI(Paths.get(VariablesGroupAPI.PATH, VariablesGroupAPI.GET_BY_URIS_PATH).toString(),
                VariablesGroupAPI.GET_BY_URIS_URI_PARAM,
                uris, VariablesGroupGetDTO.class);
        return new PaginatedListResponse<>(detailsList).getResponse();
    }

    @PUT
    @ApiOperation("Update a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variables group updated", response = URI.class),
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
            @ApiResponse(code = 200, message = "Variables group deleted", response = URI.class),
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
