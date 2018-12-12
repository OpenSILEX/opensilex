//******************************************************************************
//                            UserResourceService.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Apr, 2017
// Contact: morgane.vidal@inra.fr,arnaud.charleroy@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
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
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.resources.dto.UserDTO;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.ResourcesUtils;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormUser;

/**
 * Represents the user data service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Morgane Vidal] April, 2017 : no explanation
 * @update [Arnaud Charleroy] 13 September, 2018 : add total count to response object
 */
@Api("/user")
@Path("users")
public class UserResourceService extends ResourceService {
    /**
     * 
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
     * @return liste des utilisateurs correspondant aux critères de recherche 
     *                                  (ou tous les utilisateurs si pas de critères)
     * Le retour (dans "data") est de la forme : 
     *          [
     *              { description du utilisateur1 },
     *              { description du utilisateur2 },
     *               ...
     *          ]
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
    @ApiParam(value = "Search by family name", example = DocumentationAnnotation.EXAMPLE_USER_FAMILY_NAME) @QueryParam("familyName") String familyName,
    @ApiParam(value = "Search by address", example = DocumentationAnnotation.EXAMPLE_USER_ADDRESS) @QueryParam("address") String address,
    @ApiParam(value = "Search by phone", example = DocumentationAnnotation.EXAMPLE_USER_PHONE) @QueryParam("phone") String phone,
    @ApiParam(value = "Search by affiliation", example = DocumentationAnnotation.EXAMPLE_USER_AFFILIATION) @QueryParam("affiliation") String affiliation,
    @ApiParam(value = "Search by orcid", example = DocumentationAnnotation.EXAMPLE_USER_ORCID) @QueryParam("orcid") String orcid,
    @ApiParam(value = "Search by admin", example = DocumentationAnnotation.EXAMPLE_USER_ADMIN) @QueryParam("admin") String admin,
    @ApiParam(value = "Search by available", example = DocumentationAnnotation.EXAMPLE_USER_AVAILABLE) @QueryParam("available") String available,
    @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_USER_URI) @QueryParam("uri") @URL String uri) {
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
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
     * 
     * @param userEmail
     * @param limit
     * @param page
     * @return l'utilisateur correspondant à l'email s'il existe
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
        
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi(userEmail);
        userDao.setPageSize(limit);
        userDao.setPage(page);
        
        userDao.user = userSession.getUser();
        
        return getUsersData(userDao);
    }
    
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
        
        //Si dans les données envoyées il y a au moins un user
        if (users != null && !users.isEmpty()) {
            UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
            if (userDao.remoteUserAdress != null) {
                userDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            userDao.user = userSession.getUser();
            
            //Vérification des users et insertion en BD
            POSTResultsReturn result = userDao.checkAndInsertList(users);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) { //201, projets insérés
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
            UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
            if (userDao.remoteUserAdress != null) {
                userDao.remoteUserAdress = context.getRemoteAddr();
            }
            userDao.user = userSession.getUser();
            
            //Vérification des données et update de la BD
            POSTResultsReturn result = userDao.checkAndUpdateList(users);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) { //200 users modifiés
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
     * Collecte les données issues d'une requête de l'utilisateur (recherche de users)
     * @param userDao UserDaoPhisBrapi
     * @return  la réponse pour l'utilisateur.
     *          Contient la liste des users correspondant à la recherce
     */
    private Response getUsersData(UserDaoPhisBrapi userDao) {
        ArrayList<User> users = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormUser getResponse;
        Integer usersCount = userDao.count();
        
        if (usersCount != null && usersCount == 0) {
            getResponse = new ResponseFormUser((userDao.getPageSize()), userDao.getPage(), users, false);
            return noResultFound(getResponse, statusList);
        } else {
            users = userDao.allPaginate();
            if (users == null) {
                users = new ArrayList<>();
                getResponse = new ResponseFormUser(0, 0, users, true);
                return sqlError(getResponse, statusList);
            } else if (!users.isEmpty() && usersCount != null) {
                getResponse = new ResponseFormUser(userDao.getPageSize(), userDao.getPage(), users, true, usersCount);
                if (getResponse.getResult().dataSize() == 0) {
                    return noResultFound(getResponse, statusList);
                } else {
                    getResponse.setStatus(statusList);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            } else {
                getResponse = new ResponseFormUser(0, 0, users, true);
                return noResultFound(getResponse, statusList);
            }
        }
    }
}
