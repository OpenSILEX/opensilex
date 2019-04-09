//******************************************************************************
//                            GroupResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.GroupDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.group.GroupDTO;
import opensilex.service.resource.dto.group.GroupPostDTO;
import opensilex.service.resource.validation.interfaces.GroupLevel;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Group;

/**
 * Group resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/group")
@Path("groups")
public class GroupResourceService extends ResourceService {
    
    /**
     * @param limit
     * @param page
     * @param uri
     * @param name
     * @param level
     * @return groups found
     */
    @GET
    @ApiOperation(value = "Get all groups corresponding to the searched params given",
                  notes = "Retrieve all groups authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all groups", response = Group.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                          dataType = "string", paramType = "header",
                          value = DocumentationAnnotation.ACCES_TOKEN,
                          example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupBySearch(
    @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
    @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
    @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_GROUP_URI) @QueryParam("uri") @URL String uri,
    @ApiParam(value = "Search by name", example = DocumentationAnnotation.EXAMPLE_GROUP_NAME) @QueryParam("name") String name,
    @ApiParam(value = "Search by level", example = DocumentationAnnotation.EXAMPLE_GROUP_LEVEL) @QueryParam("level") @GroupLevel String level) {
        GroupDAO groupDao = new GroupDAO();
        
        if (uri != null) {
            groupDao.uri = uri;
        }
        if (name != null) {
            groupDao.name = name;
        }
        if (level != null) {
            groupDao.level = level;
        }
        
        groupDao.setPageSize(limit);
        groupDao.setPage(page);
        
        return getGroupsData(groupDao);
    }
    
    /**
     * Gets group details.
     * @param groupUri
     * @param limit
     * @param page
     * @return the group found
     */
    @GET
    @Path("{groupURI}")
    @ApiOperation(value = "Get a group",
                  notes = "Retrieve a group. Need group name.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a group.", response = Group.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                          dataType = "string", paramType = "header",
                          value = DocumentationAnnotation.ACCES_TOKEN,
                          example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupDetails(
    @ApiParam(value = DocumentationAnnotation.GROUP_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_GROUP_URI) 
        @PathParam("groupURI") 
        @Required 
        @URL String groupUri,
    @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
    @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        
        if (groupUri == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty Group uri");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        GroupDAO groupDao = new GroupDAO(groupUri);
        groupDao.setPageSize(limit);
        groupDao.setPage(page);
        
        groupDao.user = userSession.getUser();
        
        return getGroupsData(groupDao);
    }
    
    /**
     * Group creation service.
     * @param groups
     * @param context
     * @return creation result
     */
    @POST
    @ApiOperation(value = "Post a group",
                  notes = "Register a new group in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Group saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                          dataType = "string", paramType = "header",
                          value = DocumentationAnnotation.ACCES_TOKEN,
                          example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postGroup(
    @ApiParam(value = DocumentationAnnotation.GROUP_POST_DATA_DEFINITION, required = true) @Valid ArrayList<GroupPostDTO> groups,
    @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        // If there is at least one group in the data sent
        if (groups != null && !groups.isEmpty()) {
            GroupDAO groupDao = new GroupDAO();
            if (groupDao.remoteUserAdress != null) {
                groupDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            groupDao.user = userSession.getUser();
            
            // Check and insert groups
            POSTResultsReturn result = groupDao.checkAndInsertGroups(groups);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) { //201: groups inserted
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)
                    || result.getHttpStatus().equals(Response.Status.CONFLICT)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty group(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Groups update service.
     * @param groups
     * @param context
     * @return the update result
     */
    @PUT
    @ApiOperation(value = "Update groups")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Group updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Group not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                          dataType = "string", paramType = "header",
                          value = DocumentationAnnotation.ACCES_TOKEN,
                          example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putGroup(
    @ApiParam(value = DocumentationAnnotation.GROUP_POST_DATA_DEFINITION, required = true) @Valid ArrayList<GroupDTO> groups,
    @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        if (groups != null && !groups.isEmpty()) {
            GroupDAO groupDao = new GroupDAO();
            if (groupDao.remoteUserAdress != null) {
                groupDao.remoteUserAdress = context.getRemoteAddr();
            }
            groupDao.user = userSession.getUser();
            
            // Check and update groups
            POSTResultsReturn result = groupDao.checkAndUpdateList(groups);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) { //200: groups updated
                postResponse = new ResponseFormPOST(result.statusList);
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty group(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Gets groups data.
     * @param groupDao
     * @return the groups found
     */
    private Response getGroupsData(GroupDAO groupDao) {
        ArrayList<Group> groups = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Group> getResponse; 
        Integer groupsCount = groupDao.count();
        
        if (groupsCount != null && groupsCount == 0) {
            getResponse = new ResultForm<>(groupDao.getPageSize(), groupDao.getPage(), groups, true, groupsCount);
            return noResultFound(getResponse, statusList);
        } else {
            groups = groupDao.allPaginate();
            
            if (groups == null || groupsCount == null) { //sql error
                getResponse = new ResultForm<>(0, 0, groups, true, groupsCount);
                return sqlError(getResponse, statusList);
            } else if (groups.isEmpty()) { // no result found
                getResponse = new ResultForm<>(groupDao.getPageSize(), groupDao.getPage(), groups, false, groupsCount);
                return noResultFound(getResponse, statusList);
            } else { //results founded
                getResponse = new ResultForm<Group>(groupDao.getPageSize(), groupDao.getPage(), groups, true, groupsCount);
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }
}   
