
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import io.swagger.annotations.*;
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
@Api(SecurityModule.REST_SECURITY_API_ID)
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
    @ApiOperation("Add a group")
    @ApiResponses({
        @ApiResponse(code = 201, message = "A group is created")
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GROUP_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GROUP_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroup(
            @ApiParam("Group description") @Valid GroupCreationDTO dto
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
    @ApiOperation("Update a group")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GROUP_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GROUP_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Group updated", response = String.class),
        @ApiResponse(code = 400, message = "Invalid parameters")
    })
    public Response updateGroup(
            @ApiParam("Group description")
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
    @ApiOperation("Delete a group")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GROUP_DELETE_ID,
            credentialLabelKey = CREDENTIAL_GROUP_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(
            @ApiParam(value = "Group URI", example = "http://example.com/", required = true)
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
    @ApiOperation("Get a group")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Group retrieved", response = GroupDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Group not found", response = ErrorDTO.class)
    })
    public Response getGroup(
            @ApiParam(value = "Group URI", example = "dev-groups:admin_group", required = true)
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
    @ApiOperation("Search groups")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return groups", response = GroupDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchGroups(
            @ApiParam(value = "Regex pattern for filtering list by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String pattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
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
    @ApiOperation("Get groups by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return groups", response = GroupDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Group not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getGroupsByURI(
            @ApiParam(value = "Groups URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
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
