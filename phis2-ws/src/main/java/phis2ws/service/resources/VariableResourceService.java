//**********************************************************************************************
//                                       VariableResourceService.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 14 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 14 2017
// Subject: Represents the variable data service
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.VariableDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.VariableDTO;
import phis2ws.service.resources.dto.validation.interfaces.Required;
import phis2ws.service.resources.dto.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormVariable;
import phis2ws.service.view.model.phis.Variable;

@Api("/variables")
@Path("variables")
public class VariableResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(VariableResourceService.class);
    
    //Session de l'utilisateur
    @SessionInject
    Session userSession;
    
    /**
     * 
     * @param variables la liste des variables à enregistrer
     * @param context
     * @return 
     */
    @POST
    @ApiOperation(value = "Post variable(s)",
                  notes = "Register new variable(s) in the data base")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Variable(s) saved", response = ResponseFormPOST.class),
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
    public Response postVariable(@ApiParam(value = DocumentationAnnotation.VARIABLE_POST_DATA_DEFINITION) @Valid ArrayList<VariableDTO> variables,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        //Si dans les données il y a au moins une variable
        if (variables != null && !variables.isEmpty()) {
            VariableDaoSesame variableDao = new VariableDaoSesame();
            if (context.getRemoteAddr() != null) {
                variableDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            variableDao.user = userSession.getUser();
            
            //Vérification et insertion des variables
            POSTResultsReturn result = variableDao.checkAndInsert(variables);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                //Code 201, variables insérés
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty variable(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    } 
    
    @PUT
    @ApiOperation(value = "Update variable")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Variable updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Variable not found"),
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
    public Response putVariable(
        @ApiParam(value = DocumentationAnnotation.VARIABLE_POST_DATA_DEFINITION) @Valid ArrayList<VariableDTO> variables,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        if (variables != null && !variables.isEmpty()) {
            VariableDaoSesame variableDaoSesame = new VariableDaoSesame();
            if (context.getRemoteAddr() != null) {
                variableDaoSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            variableDaoSesame.user = userSession.getUser();
            
            POSTResultsReturn result = variableDaoSesame.checkAndUpdate(variables);
            
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
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty variable(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    private Response noResultFound(ResponseFormVariable getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the variables"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    /**
     * Collecte les données issues d'une requête de l'utilisateur (recherche de traits)
     * @param variableDaoSesame
     * @return la réponse pour l'utilisateur. Contient la liste des traits
     *         correspondant à la recherche
     * SILEX:TODO
     * on ne peut chercher que par uri et label. Il faudra ajouter d'autres critères
     * \SILEX:TODO
     */
    private Response getVariablesData(VariableDaoSesame variableDaoSesame) {
        ArrayList<Variable> variables;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormVariable getResponse;
        
        variables = variableDaoSesame.allPaginate();
        
        if (variables == null) {
            getResponse = new ResponseFormVariable(0, 0, variables, true);
            return noResultFound(getResponse, statusList);
        } else if (!variables.isEmpty()) {
            getResponse = new ResponseFormVariable(variableDaoSesame.getPageSize(), variableDaoSesame.getPage(), variables, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormVariable(0, 0, variables, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     *
     * @param limit
     * @param page
     * @param uri
     * @param label
     * @param trait
     * @param method
     * @param unit
     * @return
     */
    @GET
    @ApiOperation(value = "Get all variables corresponding to the searched params given",
                  notes = "Retrieve all variables authorized for the user corresponding to the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all variables", response = Variable.class, responseContainer = "List"),
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
    public Response getVariablesBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_VARIABLE_LABEL) @QueryParam("label") String label,
        @ApiParam(value = "Search by trait", example = DocumentationAnnotation.EXAMPLE_TRAIT_URI) @QueryParam("trait") @URL String trait,
        @ApiParam(value = "Search by method", example = DocumentationAnnotation.EXAMPLE_METHOD_URI) @QueryParam("method") @URL String method,
        @ApiParam(value = "Search by unit", example = DocumentationAnnotation.EXAMPLE_UNIT_URI) @QueryParam("unit") @URL String unit
    ) {
        VariableDaoSesame variableDaoSesame = new VariableDaoSesame();
        
        if (uri != null) {
            variableDaoSesame.uri = uri;
        }
        if (label != null) {
            variableDaoSesame.label = label;
        }
        if (trait != null) {
            variableDaoSesame.trait = trait;
        }
        if (method != null) {
            variableDaoSesame.method = method;
        }
        if (unit != null) {
            variableDaoSesame.unit = unit;
        }
        
        variableDaoSesame.user = userSession.getUser();
        variableDaoSesame.setPage(page);
        variableDaoSesame.setPageSize(limit);
        
        return getVariablesData(variableDaoSesame);
    }
    
    /**
     * 
     * @param variable
     * @param limit
     * @param page
     * @return la variable correspondant à l'uri donnée si elle existe
     */
    @GET
    @Path("{variable}")
    @ApiOperation(value = "Get a variable", 
                  notes = "Retrieve a variable. Need URL encoded variable URI (Unique resource identifier).")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a variable.", response = Variable.class, responseContainer = "List"),
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
    public Response getVariableDetail(
        @ApiParam(value = DocumentationAnnotation.VARIABLE_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @PathParam("variable") @URL @Required String variable,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {
        
        if (variable == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty variable URI");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        VariableDaoSesame variableDaoSesame = new VariableDaoSesame();
        variableDaoSesame.uri = variable;
        variableDaoSesame.setPageSize(limit);
        variableDaoSesame.setPage(page);
        variableDaoSesame.user = userSession.getUser();
        
        return getVariablesData(variableDaoSesame);
    }
}
