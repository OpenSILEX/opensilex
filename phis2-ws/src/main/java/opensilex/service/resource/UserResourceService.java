//******************************************************************************
//                            UserResourceService.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Apr. 2017
// Contact: morgane.vidal@inra.fr,arnaud.charleroy@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
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
import javax.validation.constraints.Email;
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
import opensilex.service.dao.UserDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.User;
import opensilex.service.resource.dto.UserDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.ResourcesUtils;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;

/**
 * User resource service.
 * @update [Arnaud Charleroy] 13 Sept. 2018: add total count to response object
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/user")
@Path("users")
public class UserResourceService extends ResourceService {
    
    /**
     * User GET service.
     * @param limit
     * @param page
     * @param email
     * @param firstName
     * @param familyName
     * @param address
     * @param phone
     * @param affiliation
     * @param orcid
     * @param admin
     * @param available
     * @param uri
     * @return the users found
     */
    @GET
    @ApiOperation(value = "Get all users corresponding to the searched params given",
                  notes = "Retrieve all users authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all users", response = User.class, responseContainer = "List"),
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
    public Response getUserBySearch(
    @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
    @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
    @ApiParam(value = "Search by email", example = DocumentationAnnotation.EXAMPLE_USER_EMAIL) @QueryParam("email") @Email String email,
    @ApiParam(value = "Search by first name", example = DocumentationAnnotation.EXAMPLE_USER_FIRST_NAME) @QueryParam("firstName") String firstName,
    @ApiParam(value = "Search by family name", example = DocumentationAnnotation.EXAMPLE_USER_LASTNAME) @QueryParam("familyName") String familyName,
    @ApiParam(value = "Search by address", example = DocumentationAnnotation.EXAMPLE_USER_ADDRESS) @QueryParam("address") String address,
    @ApiParam(value = "Search by phone", example = DocumentationAnnotation.EXAMPLE_USER_PHONE) @QueryParam("phone") String phone,
    @ApiParam(value = "Search by affiliation", example = DocumentationAnnotation.EXAMPLE_USER_AFFILIATION) @QueryParam("affiliation") String affiliation,
    @ApiParam(value = "Search by orcid", example = DocumentationAnnotation.EXAMPLE_USER_ORCID) @QueryParam("orcid") String orcid,
    @ApiParam(value = "Search by admin", example = DocumentationAnnotation.EXAMPLE_USER_ADMIN) @QueryParam("admin") String admin,
    @ApiParam(value = "Search by available", example = DocumentationAnnotation.EXAMPLE_USER_AVAILABLE) @QueryParam("available") String available,
    @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_USER_URI) @QueryParam("uri") @URL String uri) {
        UserDAO userDao = new UserDAO();
        if (email != null) {
            userDao.email = email;
        }
        if (firstName != null) {
            userDao.firstName = firstName;
        }
        if (familyName != null) {
            userDao.familyName = familyName;
        }
        if (userDao.address != null) {
            userDao.address = address;
        }
        if (userDao.phone != null) {
            userDao.phone = phone;
        }
        if (userDao.affiliation != null) {
            userDao.affiliation = affiliation;
        }
        if (userDao.orcid != null) {
            userDao.orcid = orcid;
        }
        if (admin != null) {
            userDao.admin = ResourcesUtils.getStringBooleanValue(admin);
        }
        if (userDao.available != null) {
            userDao.available = available;
        }
        
        if (userDao.uri != null) {
            userDao.uri = uri;
        }
        
        userDao.setPageSize(limit);
        userDao.setPage(page);
        
        return getUsersData(userDao);
    }
    
    /**
     * Single user GET service by email
     * @param userEmail
     * @param limit
     * @param page
     * @return the user found
     */
    @GET
    @Path("{userEmail}")
    @ApiOperation(value = "Get a user",
                  notes = "Retrieve a user. Need user email")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a user.", response = User.class, responseContainer = "List"),
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
    public Response getUserDetails(
    @ApiParam(value = DocumentationAnnotation.USER_EMAIL_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_USER_EMAIL) @PathParam("userEmail") @Email @Required String userEmail,
    @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
    @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        if (userEmail == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty User email");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        UserDAO userDao = new UserDAO(userEmail);
        userDao.setPageSize(limit);
        userDao.setPage(page);
        
        userDao.user = userSession.getUser();
        
        return getUsersData(userDao);
    }
    
    /**
     * User POST service.
     * @param users
     * @param context
     * @return the POST result
     */
    @POST
    @ApiOperation(value = "Post a user",
                  notes = "Register a new user in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "User saved", response = ResponseFormPOST.class),
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
    public Response postUser(
    @ApiParam(value = DocumentationAnnotation.USER_POST_DATA_DEFINITION) @Valid ArrayList<UserDTO> users,
    @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        // At least one user in the data sent
        if (users != null && !users.isEmpty()) {
            UserDAO userDao = new UserDAO();
            if (userDao.remoteUserAdress != null) {
                userDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            userDao.user = userSession.getUser();
            
            // Check and insert users
            POSTResultsReturn result = userDao.checkAndInsertList(users);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) { //201: users inserted
                postResponse = new ResponseFormPOST(result.statusList);
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty user(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * User PUT service.
     * @param users
     * @param context
     * @return 
     */
    @PUT
    @ApiOperation(value = "Update users")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "User not found"),
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
    public Response putUser(
    @ApiParam(value = DocumentationAnnotation.USER_POST_DATA_DEFINITION) @Valid ArrayList<UserDTO> users,
    @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (users != null && !users.isEmpty()) {
            UserDAO userDao = new UserDAO();
            if (userDao.remoteUserAdress != null) {
                userDao.remoteUserAdress = context.getRemoteAddr();
            }
            userDao.user = userSession.getUser();
            
            // Check and update users
            POSTResultsReturn result = userDao.checkAndUpdateList(users);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) { //200: users updated
                postResponse = new ResponseFormPOST(result.statusList);
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty user(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Get users data.
     * @param userDao
     * @return the users found
     */
    private Response getUsersData(UserDAO userDao) {
        ArrayList<User> users = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<User> getResponse;
        Integer usersCount = userDao.count();
        
        if (usersCount != null && usersCount == 0) {
            getResponse = new ResultForm<>((userDao.getPageSize()), userDao.getPage(), users, false);
            return noResultFound(getResponse, statusList);
        } else {
            users = userDao.allPaginate();
            if (users == null) {
                users = new ArrayList<>();
                getResponse = new ResultForm<>(0, 0, users, true);
                return sqlError(getResponse, statusList);
            } else if (!users.isEmpty() && usersCount != null) {
                getResponse = new ResultForm<>(userDao.getPageSize(), userDao.getPage(), users, true, usersCount);
                if (getResponse.getResult().dataSize() == 0) {
                    return noResultFound(getResponse, statusList);
                } else {
                    getResponse.setStatus(statusList);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            } else {
                getResponse = new ResultForm<>(0, 0, users, true);
                return noResultFound(getResponse, statusList);
            }
        }
    }
}
