
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

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
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.group.dal.GroupDAO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.response.CreatedUriResponse;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vidalmor
 */
@Tag(name = SecurityModule.REST_SECURITY_API_ID)
@Path("/security/groups")
@ApiCredentialGroup(
        groupId = GroupAPI.CREDENTIAL_GROUP_GROUP_ID,
        groupLabelKey = GroupAPI.CREDENTIAL_GROUP_GROUP_LABEL_KEY
)
public class GroupAPI {

    public static final String CREDENTIAL_GROUP_GROUP_ID = "Groups";
    public static final String CREDENTIAL_GROUP_GROUP_LABEL_KEY = "credential-groups.groups";

    public static final String CREDENTIAL_GROUP_MODIFICATION_ID = "group-modification";
    public static final String CREDENTIAL_GROUP_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_GROUP_DELETE_ID = "group-delete";
    public static final String CREDENTIAL_GROUP_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel currentUser;

    /**
     * Create a group and return it's URI
     *
     * @see org.opensilex.security.group.dal.GroupDAO
     * @param dto group model to create
     * @return Group URI
     * @throws Exception if creation failed
     */
    @POST
    @Operation(summary = "Add a group")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "A group is created")
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GROUP_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GROUP_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroup(
            @Parameter(description = "Group description") @Valid GroupCreationDTO dto
    ) throws Exception {
        GroupDAO dao = new GroupDAO(sparql);

        // check if group URI already exists
        if (sparql.uriExists(GroupModel.class, dto.getUri())) {
            // Return error response 409 - CONFLICT if user URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Group already exists",
                    "Duplicated URI: " + dto.getUri()
            ).getResponse();
        }

        // check if group name already exists
        if (dao.groupNameExists(dto.getName())) {
            // Return error response 409 - CONFLICT if profile name already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Group already exists",
                    "Duplicated name: " + dto.getName()
            ).getResponse();
        }

        // create new group
        GroupModel group = dto.newModel();
        group.setPublisher(currentUser.getUri());
        dao.create(group);

        // return group URI
        return new CreatedUriResponse(group.getUri()).getResponse();
    }

    @PUT
    @Operation(summary = "Update a group")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GROUP_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GROUP_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Group updated", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public Response updateGroup(
            @Parameter(description = "Group description")
            @Valid GroupUpdateDTO dto
    ) throws Exception {
        GroupDAO dao = new GroupDAO(sparql);

        GroupModel model = dao.get(dto.getUri());

        Response response;
        if (model != null) {
            GroupModel group = dto.newModel();
            group = dao.update(group);

            response = new ObjectUriResponse(Response.Status.OK, group.getUri()).getResponse();
        } else {
            response = new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Group not found",
                    "Unknown group URI: " + dto.getUri()
            ).getResponse();
        }

        return response;
    }

    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete a group")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GROUP_DELETE_ID,
            credentialLabelKey = CREDENTIAL_GROUP_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(
            @Parameter(description = "Group URI", example = "http://example.com/", required = true)
            @PathParam("uri")
            @NotNull
            @ValidURI URI uri
    ) throws Exception {
        GroupDAO dao = new GroupDAO(sparql);
        dao.delete(uri);
        Response response = new ObjectUriResponse(Response.Status.OK, uri).getResponse();

        return response;
    }

    /**
     * Return a group by URI
     *
     * @see org.opensilex.security.group.dal.GroupDAO
     * @param uri URI of the group
     * @return Corresponding group
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("{uri}")
    @Operation(summary = "Get a group")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Group retrieved", content = @Content(schema = @Schema(implementation = GroupDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response getGroup(
            @Parameter(description = "Group URI", example = "dev-groups:admin_group", required = true)
            @PathParam("uri")
            @NotNull URI uri
    ) throws Exception {
        GroupDAO dao = new GroupDAO(sparql);
        GroupModel model = dao.get(uri);

        // Check if group is found
        if (model != null) {
            // Return group converted in GroupGetDTO
            return new SingleObjectResponse<>(
                    GroupDTO.getDTOFromModel(model)
            ).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Group not found",
                    "Unknown group URI: " + uri.toString()
            ).getResponse();
        }
    }

    /**
     * Search groups
     *
     * @see org.opensilex.security.group.dal.GroupDAO
     * @param pattern Regex pattern for filtering list by names or email
     * @param orderByList List of fields to sort as an array of fieldName=asc|desc
     * @param page Page number
     * @param pageSize Page size
     * @return filtered, ordered and paginated list
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Operation(summary = "Search groups")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return groups", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GroupDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response searchGroups(
            @Parameter(description = "Regex pattern for filtering list by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String pattern,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        GroupDAO dao = new GroupDAO(sparql);
        ListWithPagination<GroupModel> resultList = dao.search(
                pattern,
                currentUser.getLanguage(),
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<GroupDTO> resultDTOList = resultList.convert(
                GroupDTO.class,
                GroupDTO::getDTOFromModel
        );

        // Return paginated list of user DTO
        Response response = new PaginatedListResponse<>(resultDTOList).getResponse();
        return response;
    }

    /**
     * *
     * Return a list of groups corresponding to the given URIs
     *
     * @param uris list of groups uri
     * @return Corresponding list of groups
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("by_uris")
    @Operation(summary = "Get groups by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return groups", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GroupDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Group not found (if any provided URIs is not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response getGroupsByURI(
            @Parameter(description = "Groups URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        GroupDAO dao = new GroupDAO(sparql);
        List<GroupModel> models = dao.getList(uris);

        if (!models.isEmpty()) {
            List<GroupDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                resultDTOList.add(GroupDTO.getDTOFromModel(result));
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Groups not found",
                    "Unknown group URIs"
            ).getResponse();
        }
    }
}
