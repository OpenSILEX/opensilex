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
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.User;
import opensilex.service.resource.dto.UserDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User resource service.
 *
 * @update [Arnaud Charleroy] 13 Sept. 2018: add total count to response object
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/user")
@Path("users")
@Deprecated
public class UserResourceService extends ResourceService {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserResourceService.class);

    @Inject
    public UserResourceService(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;

    /**
     * Inject Authentication service
     */
    @Inject
    private AuthenticationService authentication;

    /**
     * User GET service.
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
    @ApiProtected
    @Deprecated
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
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_USER_URI) @QueryParam("uri") @URL String uri) throws Exception {
        UserDAO userDao = new UserDAO(sparql);

        if (uri != null && !uri.isEmpty()) {
            UserModel user = userDao.get(new URI(uri));
            return getUsersData(user);
        } else {
            return getUsersData(userDao.search(email, firstName, familyName, page, limit));
        }
    }

    /**
     * Single user GET service by email
     *
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
    @ApiProtected
    @Deprecated
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserDetails(
            @ApiParam(value = DocumentationAnnotation.USER_EMAIL_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_USER_EMAIL) @PathParam("userEmail") @Email @Required String userEmail,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) throws Exception {
        if (userEmail == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty User email");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }

        UserDAO userDao = new UserDAO(sparql);
        UserModel user = userDao.getByEmail(new InternetAddress(userEmail));

        return getUsersData(user);
    }

    /**
     * User POST service.
     *
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
    @ApiProtected
    @Deprecated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postUser(
            @ApiParam(value = DocumentationAnnotation.USER_POST_DATA_DEFINITION) @Valid ArrayList<UserDTO> users,
            @Context HttpServletRequest context) throws Exception {
        AbstractResultForm postResponse = null;
        // At least one user in the data sent
        if (users != null && !users.isEmpty()) {
            UserDAO userDao = new UserDAO(sparql);

            List<String> createdURIs = new ArrayList<>();
            try {
                sparql.startTransaction();
                for (UserDTO user : users) {
                    UserModel model = userDao.create(
                            null,
                            new InternetAddress(user.getEmail()),
                            user.getFirstName(),
                            user.getFamilyName(),
                            user.getAdmin().equals("true"),
                            authentication.getPasswordHash(user.getPassword()),
                            null
                    );
                    createdURIs.add(model.getUri().toString());
                }
                sparql.commitTransaction();
            } catch (Exception ex) {
                LOGGER.error("Error while creating users", ex);
                sparql.rollbackTransaction();
            }

            postResponse = new ResponseFormPOST();
            postResponse.getMetadata().setDatafiles(createdURIs);
            return Response.status(Response.Status.CREATED).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty user(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * User PUT service.
     *
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
    @ApiProtected
    @Deprecated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putUser(
            @ApiParam(value = DocumentationAnnotation.USER_POST_DATA_DEFINITION) @Valid ArrayList<UserDTO> users,
            @Context HttpServletRequest context) throws Exception {
        AbstractResultForm postResponse = null;
        // At least one user in the data sent
        if (users != null && !users.isEmpty()) {
            UserDAO userDao = new UserDAO(sparql);

            try {
                sparql.startTransaction();
                for (UserDTO user : users) {
                    UserModel model = userDao.getByEmail(new InternetAddress(user.getEmail()));
                    userDao.update(
                            model.getUri(),
                            new InternetAddress(user.getEmail()),
                            user.getFirstName(),
                            user.getFamilyName(),
                            user.getAdmin().equals("true"),
                            user.getPassword(),
                            null
                    );
                }
                sparql.commitTransaction();
            } catch (Exception ex) {
                sparql.rollbackTransaction();
            }

            postResponse = new ResponseFormPOST();
            return Response.status(Response.Status.OK).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty user(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    private Response getUsersData(UserModel user) {
        ArrayList<User> users = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<User> getResponse;

        if (user == null) {
            getResponse = new ResultForm<>(0, 0, users, false);
            return noResultFound(getResponse, statusList);
        } else {
            users.add(getOldUserFromModel(user));
            getResponse = new ResultForm<>(0, 1, users, true, 1);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }

    private Response getUsersData(ListWithPagination<UserModel> usersModels) {
        ArrayList<User> users = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<User> getResponse;

        if (usersModels == null || usersModels.getTotal() == 0) {
            getResponse = new ResultForm<>(0, 0, users, false);
            return noResultFound(getResponse, statusList);
        } else {
            usersModels.getList().forEach(model -> {
                users.add(getOldUserFromModel(model));
            });
            getResponse = new ResultForm<>(usersModels.getPage(), usersModels.getPageSize(), users, true, usersModels.getTotal());
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }

    public static User getOldUserFromModel(UserModel model) {
        User userDTO = new User(model.getEmail().toString());
        if (model.getUri() != null) {
            userDTO.setUri(model.getUri().toString());
        }
        userDTO.setFirstName(model.getFirstName());
        userDTO.setFamilyName(model.getLastName());
        userDTO.setAdmin(model.isAdmin().toString());

        return userDTO;
    }
}
