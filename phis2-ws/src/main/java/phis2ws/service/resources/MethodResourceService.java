//**********************************************************************************************
//                                       MethodResourceService.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 17 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 17 2017
// Subject: Represents the method data service
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
import phis2ws.service.dao.sesame.MethodDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.MethodDTO;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Method;

@Api("/methods")
@Path("methods")
public class MethodResourceService extends ResourceService {
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
            MethodDaoSesame methodDaoSesame = new MethodDaoSesame();
            if (context.getRemoteAddr() != null) {
                methodDaoSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            methodDaoSesame.user = userSession.getUser();
            
            POSTResultsReturn result = methodDaoSesame.checkAndInsert(methods);
            
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
        AbstractResultForm postResponse = null;
        if (methods != null && !methods.isEmpty()) {
            MethodDaoSesame methodDaoSesame = new MethodDaoSesame();
            if (context.getRemoteAddr() != null) {
                methodDaoSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            methodDaoSesame.user = userSession.getUser();
            
            POSTResultsReturn result = methodDaoSesame.checkAndUpdate(methods);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) {
                //Code 200, traits modifiés
                postResponse = new ResponseFormPOST(result.statusList);
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty method(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Collecte les données issues d'une requête de l'utilisateur (recherche de methodes)
     * @param methodDaoSesame
     * @return la réponse pour l'utilisateur. Contient la liste des méthodess
     *         correspondant à la recherche
     * SILEX:TODO
     * on ne peut chercher que par uri et label. Il faudra ajouter d'autres critères
     * \SILEX:TODO
     */
    private Response getMethodsData(MethodDaoSesame methodDaoSesame) {
        ArrayList<Method> methods;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Method> getResponse;
        
        methods = methodDaoSesame.allPaginate();
        
        if (methods == null) {
            getResponse = new ResultForm<Method>(0, 0, methods, true);
            return noResultFound(getResponse, statusList);
        } else if (!methods.isEmpty()) {
            getResponse = new ResultForm<Method>(methodDaoSesame.getPageSize(), methodDaoSesame.getPage(), methods, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResultForm<Method>(0, 0, methods, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     *
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
        MethodDaoSesame methodDaoSesame = new MethodDaoSesame();
        
        if (uri != null) {
            methodDaoSesame.uri = uri;
        }
        if (label != null) {
            methodDaoSesame.label = label;
        }
        
        methodDaoSesame.user = userSession.getUser();
        methodDaoSesame.setPage(page);
        methodDaoSesame.setPageSize(limit);
        
        return getMethodsData(methodDaoSesame);
    }
    
    /**
     * 
     * @param method
     * @param limit
     * @param page
     * @return la méthode correspondant à l'uri donnée si elle existe
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
        
        MethodDaoSesame methodDaoSesame = new MethodDaoSesame();
        methodDaoSesame.uri = method;
        methodDaoSesame.setPageSize(limit);
        methodDaoSesame.setPage(page);
        methodDaoSesame.user = userSession.getUser();
        
        return getMethodsData(methodDaoSesame);
    }
}
