//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

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

@Tag(name = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID)
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
    @Operation(summary = "Add a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "A variables group is created", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "409", description = "A variables group with the same URI already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)   
    public Response createVariablesGroup(@Parameter(description = "Variables group description") @Valid VariablesGroupCreationDTO dto) throws Exception {
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
    @Operation(summary = "Search variables groups")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return variables groups", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VariablesGroupGetDTO.class))))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariablesGroups(
            @Parameter(description = "Regex pattern for filtering by name") @QueryParam("name") String name ,
            @Parameter(description = "Variable URI") @QueryParam("variableUri") URI variableUri ,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize,
            @Parameter(description = "Shared resource instance") @QueryParam(VariablesGroupAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
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
    @Operation(summary = "Get a variables group")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variables group retrieved", content = @Content(schema = @Schema(implementation = VariablesGroupGetDTO.class))),
            @ApiResponse(responseCode = "404", description = "Unknown variables group URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariablesGroup(@Parameter(description = "Variables group URI", required = true) @PathParam("uri") @NotNull URI uri) throws Exception {
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
    @Operation(summary = "Get variables groups by their URIs")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return variables groups", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VariablesGroupGetDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Variables group not found (if any provided URIs is not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariablesGroupByURIs(
            @Parameter(description = "Variables group URIs", required = true) @QueryParam(VariablesGroupAPI.GET_BY_URIS_URI_PARAM) @NotNull List<URI> uris,
            @Parameter(description = "Shared resource instance") @QueryParam(VariablesGroupAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
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
    @Operation(summary = "Update a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variables group updated", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Unknown variables group URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVariablesGroup(@Parameter(description = "Variables group description") @Valid VariablesGroupUpdateDTO dto) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        VariablesGroupModel model = dao.update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variables group deleted", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Unknown variables group URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariablesGroup(
            @Parameter(description = "Variables group URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(uri).getResponse();
    }
}
