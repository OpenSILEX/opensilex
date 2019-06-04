//******************************************************************************
//                           MethodResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 17 November 2017
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
import opensilex.service.dao.MethodDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.MethodDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Method;

/**
 * Method resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/methods")
@Path("methods")
public class MethodResourceService extends ResourceService {
    
    /**
     * Method POST service.
     * @param methods
     * @param context
     * @return 
     */
    @POST
    @ApiOperation(value = "Post method(s)",
                  notes = "Register new method(s) in the data base")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Method(s) saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postMethod(@ApiParam(value = DocumentationAnnotation.METHOD_POST_DATA_DEFINITION) @Valid ArrayList<MethodDTO> methods,
                              @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        if (methods != null && !methods.isEmpty()) {
            MethodDAO methodDao = new MethodDAO();
            if (context.getRemoteAddr() != null) {
                methodDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            methodDao.user = userSession.getUser();
            
            POSTResultsReturn result = methodDao.checkAndInsert(methods);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                //Code 201, methodes insérées
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty method(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Method PUT service.
     * @param methods
     * @param context
     * @return 
     */
    @PUT
    @ApiOperation(value = "Update method")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Method updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Method not found"),
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
    public Response putMethod(
        @ApiParam(value = DocumentationAnnotation.METHOD_POST_DATA_DEFINITION) @Valid ArrayList<MethodDTO> methods,
        @Context HttpServletRequest context) {
        AbstractResultForm response = null;
        if (methods != null && !methods.isEmpty()) {
            MethodDAO methodDao = new MethodDAO();
            if (context.getRemoteAddr() != null) {
                methodDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            methodDao.user = userSession.getUser();
            
            POSTResultsReturn result = methodDao.checkAndUpdate(methods);
            
            if (result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.CREATED)) {
                response = new ResponseFormPOST(result.statusList);
                response.getMetadata().setDatafiles(result.createdResources);
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                response = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(response).build();
        } else {
            response = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty method(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }

    /**
     * Gets method data.
     * @param methodDao
     * @return the methods found
     * SILEX:todo
     * Add other filters than URI and label
     * \SILEX:todo
     */
    private Response getMethodsData(MethodDAO methodDao) {
        ArrayList<Method> methods;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Method> getResponse;
        
        methods = methodDao.allPaginate();
        
        if (methods == null) {
            getResponse = new ResultForm<>(0, 0, methods, true);
            return noResultFound(getResponse, statusList);
        } else if (!methods.isEmpty()) {
            getResponse = new ResultForm<>(methodDao.getPageSize(), methodDao.getPage(), methods, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResultForm<>(0, 0, methods, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     *  Method GET service.
     * @param limit
     * @param page
     * @param uri
     * @param label
     * @return
     */
    @GET
    @ApiOperation(value = "Get all methods corresponding to the searched params given",
                  notes = "Retrieve all methods authorized for the user corresponding to the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all methods", response = Method.class, responseContainer = "List"),
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
    public Response getMethodsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_METHOD_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_METHOD_LABEL) @QueryParam("label") String label
    ) {
        MethodDAO methodDao = new MethodDAO();
        
        if (uri != null) {
            methodDao.uri = uri;
        }
        if (label != null) {
            methodDao.label = label;
        }
        
        methodDao.user = userSession.getUser();
        methodDao.setPage(page);
        methodDao.setPageSize(limit);
        
        return getMethodsData(methodDao);
    }
    
    /**
     * Single method GET service from URI.
     * @param method
     * @param limit
     * @param page
     * @return the method found
     */
    @GET
    @Path("{method}")
    @ApiOperation(value = "Get a method", 
                  notes = "Retrieve a method. Need URL encoded method URI (Unique resource identifier).")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a method.", response = Method.class, responseContainer = "List"),
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
    public Response getMethodDetails(
        @ApiParam(value = DocumentationAnnotation.METHOD_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_METHOD_URI, required = true) @PathParam("method") @URL @Required String method,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        
        if (method == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty method URI");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        MethodDAO methodDao = new MethodDAO();
        methodDao.uri = method;
        methodDao.setPageSize(limit);
        methodDao.setPage(page);
        methodDao.user = userSession.getUser();
        
        return getMethodsData(methodDao);
    }
}
