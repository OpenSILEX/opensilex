//**********************************************************************************************
//                                       GroupResourceService.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  April, 2017
// Subject: Represents the group data service
//***********************************************************************************************
package phis2ws.service.resources;

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
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.GroupDao;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.group.GroupDTO;
import phis2ws.service.resources.dto.group.GroupPostDTO;
import phis2ws.service.resources.validation.interfaces.GroupLevel;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Group;

@Api("/group")
@Path("groups")
public class GroupResourceService extends ResourceService {
    /**
     * 
     * @param limit
     * @param page
     * @param uri
     * @param name
     * @param level
     * @return liste des groupes correspondant aux critères de recherche 
     *                                  (ou tous les groupes si pas de critères)
     * Le retour (dans "data") est de la forme : 
     *          [
     *              { description du groupe1 },
     *              { description du groupe2 },
     *               ...
     *          ]
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
        GroupDao groupDao = new GroupDao();
        
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
     * 
     * @param groupUri
     * @param limit
     * @param page
     * @return le groupe correspondant au nom de groupe s'il existe
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
    @ApiParam(value = DocumentationAnnotation.GROUP_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_GROUP_URI) @PathParam("groupURI") @Required @URL String groupUri,
    @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
    @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        
        if (groupUri == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty Group uri");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        GroupDao groupDao = new GroupDao(groupUri);
        groupDao.setPageSize(limit);
        groupDao.setPage(page);
        
        groupDao.user = userSession.getUser();
        
        return getGroupsData(groupDao);
    }
    
    /**
     * permet l'enregistrement en base de données d'un nouveau groupe
     * @param groups
     * @param context
     * @return le message de retour correspondant à cet ajout (erreur ou ok)
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
        
        //Si dans les données envoyées il y a au moins un groupe
        if (groups != null && !groups.isEmpty()) {
            GroupDao groupDao = new GroupDao();
            if (groupDao.remoteUserAdress != null) {
                groupDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            groupDao.user = userSession.getUser();
            
            //Vérification des groupes et insertion en BD
            POSTResultsReturn result = groupDao.checkAndInsertGroups(groups);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) { //201, projets insérés
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
            GroupDao groupDao = new GroupDao();
            if (groupDao.remoteUserAdress != null) {
                groupDao.remoteUserAdress = context.getRemoteAddr();
            }
            groupDao.user = userSession.getUser();
            
            //Vérification des données et update de la BD
            POSTResultsReturn result = groupDao.checkAndUpdateList(groups);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) { //200 users modifiés
                postResponse = new ResponseFormPOST(result.statusList);
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
            //TODO
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty group(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Collecte les données issues d'une requête de l'utilisateur (recherche de groupes)
     * @param groupDao GroupDao
     * @return la réponse pour l'utilisateur. 
     *         Contient la liste des groups correspondant à la recherche
     */
    private Response getGroupsData(GroupDao groupDao) {
        ArrayList<Group> groups = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Group> getResponse; 
        Integer groupsCount = groupDao.count();
        
        if (groupsCount != null && groupsCount == 0) {
            getResponse = new ResultForm<Group>(groupDao.getPageSize(), groupDao.getPage(), groups, true, groupsCount);
            return noResultFound(getResponse, statusList);
        } else {
            groups = groupDao.allPaginate();
            
            if (groups == null || groupsCount == null) { //sql error
                getResponse = new ResultForm<Group>(0, 0, groups, true, groupsCount);
                return sqlError(getResponse, statusList);
            } else if (groups.isEmpty()) { // no result found
                getResponse = new ResultForm<Group>(groupDao.getPageSize(), groupDao.getPage(), groups, false, groupsCount);
                return noResultFound(getResponse, statusList);
            } else { //results founded
                getResponse = new ResultForm<Group>(groupDao.getPageSize(), groupDao.getPage(), groups, true, groupsCount);
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }
}   
