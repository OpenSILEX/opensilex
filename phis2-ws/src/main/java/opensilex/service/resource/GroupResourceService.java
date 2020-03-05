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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.group.GroupPostDTO;
import opensilex.service.resource.validation.interfaces.GroupLevel;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Group;
import opensilex.service.model.User;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.group.dal.GroupDAO;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.group.dal.GroupUserProfileModel;
import org.opensilex.rest.profile.dal.ProfileDAO;
import org.opensilex.rest.profile.dal.ProfileModel;
import org.opensilex.rest.security.dal.SecurityAccessDAO;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.exceptions.SPARQLTransactionException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group resource service.
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/group")
@Path("groups")
public class GroupResourceService extends ResourceService {

    private final static Logger LOGGER = LoggerFactory.getLogger(GroupResourceService.class);

    @Inject
    public GroupResourceService(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;

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
            @ApiParam(value = "Search by level", example = DocumentationAnnotation.EXAMPLE_GROUP_LEVEL) @QueryParam("level") @GroupLevel String level) throws Exception {
        try (sparql) {
            GroupDAO groupDao = new GroupDAO(sparql);

            if (uri != null && !uri.isEmpty()) {
                GroupModel group = groupDao.get(new URI(uri));
                return getGroupsData(group);
            } else {
                return getGroupsData(groupDao.search(name, null, page, limit));
            }
        }

    }

    /**
     * Gets group details.
     *
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
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) throws Exception {
        try (sparql) {
            if (groupUri == null) {
                final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty Group uri");
                return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
            }

            GroupDAO groupDao = new GroupDAO(sparql);
            GroupModel group = groupDao.get(new URI(groupUri));

            return getGroupsData(group);
        }
    }

    /**
     * Group creation service.
     *
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
            @Context HttpServletRequest context) throws Exception {
        AbstractResultForm postResponse = null;
        try (sparql) {
            List<String> createdURIs = new ArrayList<>();
            // At least one user in the data sent
            if (groups != null && !groups.isEmpty()) {
                GroupDAO groupDao = new GroupDAO(sparql);

                try {
                    for (GroupPostDTO group : groups) {
                        GroupModel model = groupDao.create(dtoToModel(group));
                        createdURIs.add(model.getUri().toString());
                    }
                } catch (Exception ex) {
                    LOGGER.error("Error while creating groups", ex);
                }

                postResponse = new ResponseFormPOST();
                postResponse.getMetadata().setDatafiles(createdURIs);
                return Response.status(Response.Status.CREATED).entity(postResponse).build();
            } else {
                postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty group(s) to add"));
                return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
            }
        }
    }

//    /**
//     * Groups update service.
//     * @param groups
//     * @param context
//     * @return the update result
//     */
//    @PUT
//    @ApiOperation(value = "Update groups")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Group updated", response = ResponseFormPOST.class),
//        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
//        @ApiResponse(code = 404, message = "Group not found"),
//        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
//    })
//    @ApiImplicitParams({
//        @ApiImplicitParam(name = "Authorization", required = true,
//                          dataType = "string", paramType = "header",
//                          value = DocumentationAnnotation.ACCES_TOKEN,
//                          example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
//    })
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response putGroup(
//    @ApiParam(value = DocumentationAnnotation.GROUP_POST_DATA_DEFINITION, required = true) @Valid ArrayList<GroupDTO> groups,
//    @Context HttpServletRequest context) {
//        AbstractResultForm postResponse = null;
//        if (groups != null && !groups.isEmpty()) {
//            GroupDAO groupDao = new GroupDAO();
//            if (groupDao.remoteUserAdress != null) {
//                groupDao.remoteUserAdress = context.getRemoteAddr();
//            }
//            groupDao.user = userSession.getUser();
//            
//            // Check and update groups
//            POSTResultsReturn result = groupDao.checkAndUpdateList(groups);
//            
//            if (result.getHttpStatus().equals(Response.Status.OK)) { //200: groups updated
//                postResponse = new ResponseFormPOST(result.statusList);
//                return Response.status(result.getHttpStatus()).entity(postResponse).build();
//            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
//                    || result.getHttpStatus().equals(Response.Status.OK)
//                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
//                postResponse = new ResponseFormPOST(result.statusList);
//            }
//            return Response.status(result.getHttpStatus()).entity(postResponse).build();
//        } else {
//            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty group(s) to add"));
//            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
//        }
//    }
    /**
     * Gets groups data.
     *
     * @param groupDao
     * @return the groups found
     */
    private Response getGroupsData(GroupModel model) {
        ArrayList<Group> groups = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Group> getResponse;

        if (model == null) {
            getResponse = new ResultForm<>(0, 0, groups, false);
            return noResultFound(getResponse, statusList);
        } else {
            groups.add(getOldGroupFromModel(model));
            getResponse = new ResultForm<>(0, 1, groups, true, 1);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }

    private Response getGroupsData(ListWithPagination<GroupModel> groupsModels) {
        ArrayList<Group> groups = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Group> getResponse;

        if (groupsModels == null || groupsModels.getTotal() == 0) {
            getResponse = new ResultForm<>(0, 0, groups, false);
            return noResultFound(getResponse, statusList);
        } else {
            groupsModels.getList().forEach(model -> {
                groups.add(getOldGroupFromModel(model));
            });
            getResponse = new ResultForm<>(groupsModels.getPage(), groupsModels.getPageSize(), groups, true, groupsModels.getTotal());
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }

    private Group getOldGroupFromModel(GroupModel model) {
        Group group = new Group();
        group.setUri(model.getUri().toString());
        group.setName(model.getName());
        group.setDescription(model.getDescription());
        group.setLevel("Owner");
        ArrayList<User> users = new ArrayList<>();
        model.getUserProfiles().forEach(userProfile -> {
            users.add(UserResourceService.getOldUserFromModel(userProfile.getUser()));
        });
        group.setUserList(users);

        return group;
    }

    private GroupModel dtoToModel(GroupPostDTO group) throws Exception {
        GroupModel model = new GroupModel();
        model.setName(group.getName());
        model.setDescription(group.getDescription());
        List<GroupUserProfileModel> userProfiles = new ArrayList<>();

        UserDAO userDAO = new UserDAO(sparql);
        ProfileDAO profileDAO = new ProfileDAO(sparql);
        URI profileURI = new URI("http://www.opensilex.org/profiles#default");
        ProfileModel profile = profileDAO.get(profileURI);

        if (profile == null) {
            profile = new ProfileModel();
            profile.setUri(profileURI);
            profile.setName("Default profile");
            SecurityAccessDAO securityDAO = new SecurityAccessDAO(sparql);
            profile.setCredentials(securityDAO.getCredentialsIdList());
        }

        for (String email : group.getUsersEmails()) {
            UserModel user = userDAO.getByEmail(new InternetAddress(email));
            GroupUserProfileModel userProfile = new GroupUserProfileModel();
            userProfile.setUser(user);
            userProfile.setProfile(profile);
            userProfiles.add(userProfile);
        };
        model.setUserProfiles(userProfiles);

        return model;
    }
}
