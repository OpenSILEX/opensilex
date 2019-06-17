//******************************************************************************
//                                       VariableResourceService.java 
// Copyright © INRA 2017
// Creation date: 14 November 2017
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
import javax.ws.rs.NotFoundException;
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
import opensilex.service.dao.VariableDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.VariableDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Variable;
import static opensilex.service.resource.SensorResourceService.LOGGER;
import opensilex.service.resource.dto.variable.VariableDetailDTO;

/**
 * Variable resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/variables")
@Path("variables")
public class VariableResourceService extends ResourceService {
    
    /**
     * Variable POST service.
     * @param variables
     * @param context
     * @return the POST result
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
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postVariable(@ApiParam(value = DocumentationAnnotation.VARIABLE_POST_DATA_DEFINITION) @Valid ArrayList<VariableDTO> variables,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        // At least one variable
        if (variables != null && !variables.isEmpty()) {
            VariableDAO variableDao = new VariableDAO();
            if (context.getRemoteAddr() != null) {
                variableDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            variableDao.user = userSession.getUser();
            
            // Check and insert variables
            POSTResultsReturn result = variableDao.checkAndInsert(variables);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                //Code 201: variables inserted
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty variable(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    } 
    
    /**
     * Variables PUT service.
     * @param variables
     * @param context
     * @return 
     */
    @PUT
    @ApiOperation(value = "Update variable")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Variable updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Variable not found"),
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
    public Response putVariable(
        @ApiParam(value = DocumentationAnnotation.VARIABLE_POST_DATA_DEFINITION) @Valid ArrayList<VariableDTO> variables,
        @Context HttpServletRequest context) {
        AbstractResultForm response = null;
        if (variables != null && !variables.isEmpty()) {
            VariableDAO variableDao = new VariableDAO();
            if (context.getRemoteAddr() != null) {
                variableDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            variableDao.user = userSession.getUser();
            
            POSTResultsReturn result = variableDao.checkAndUpdate(variables);
            
            if (result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.CREATED)) {
                response = new ResponseFormPOST(result.statusList);
                response.getMetadata().setDatafiles(result.createdResources);
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                response = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(response).build();
        } else {
            response = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty variable(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }
    
    /**
     * Variable GET service.
     * @param pageSize
     * @param page
     * @param uri
     * @param label
     * @param trait
     * @param traitSKosReference
     * @param method
     * @param unit
     * @return the GET result
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
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariablesBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_VARIABLE_LABEL) @QueryParam("label") String label,
        @ApiParam(value = "Search by trait", example = DocumentationAnnotation.EXAMPLE_TRAIT_URI) @QueryParam("trait") @URL String trait,
        @ApiParam(value = "Search by skos trait reference", example = DocumentationAnnotation.EXAMPLE_SKOS_REFERECENCE_URI) @QueryParam("traitSKosReference") @URL String traitSKosReference,
        @ApiParam(value = "Search by method", example = DocumentationAnnotation.EXAMPLE_METHOD_URI) @QueryParam("method") @URL String method,
        @ApiParam(value = "Search by unit", example = DocumentationAnnotation.EXAMPLE_UNIT_URI) @QueryParam("unit") @URL String unit
    ) {
        return this.search(pageSize, page, uri, label, trait, traitSKosReference, method, unit, false);
    }
    
    @GET
    @Path("details")
    @ApiOperation(value = "Get all variables details corresponding to the searched params given",
                  notes = "Retrieve all variables with details authorized for the user corresponding to the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all variables with details ", response = Variable.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariablesDetailsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_VARIABLE_LABEL) @QueryParam("label") String label,
        @ApiParam(value = "Search by trait", example = DocumentationAnnotation.EXAMPLE_TRAIT_URI) @QueryParam("trait") @URL String trait,
        @ApiParam(value = "Search by skos trait reference", example = DocumentationAnnotation.EXAMPLE_SKOS_REFERECENCE_URI) @QueryParam("traitSKosReference") @URL String traitSKosReference,
        @ApiParam(value = "Search by method", example = DocumentationAnnotation.EXAMPLE_METHOD_URI) @QueryParam("method") @URL String method,
        @ApiParam(value = "Search by unit", example = DocumentationAnnotation.EXAMPLE_UNIT_URI) @QueryParam("unit") @URL String unit
    ) {
        return this.search(pageSize, page, uri, label, trait, traitSKosReference, method, unit, true);
    }
    
    /**
     * Get list of variable according to the given search parameters
     * if withDetail is set to true, it will return variables with all traits, metods and units proeprties
     * @param pageSize
     * @param page
     * @param uri
     * @param label
     * @param trait
     * @param traitSKosReference
     * @param method
     * @param unit
     * @param withDetail
     * @return 
     */
    private Response search(
        int pageSize,
        int page,
        String uri,
        String label,
        String trait,
        String traitSKosReference,
        String method,
        String unit,
        boolean withDetail
    ) {
        VariableDAO variableDao = new VariableDAO();
        
        if (uri != null) {
            variableDao.uri = uri;
        }
        if (label != null) {
            variableDao.label = label;
        }
        if (trait != null) {
            variableDao.trait = trait;
        }
        if (traitSKosReference != null) {
            variableDao.traitSKosReference = traitSKosReference;
        }
        if (method != null) {
            variableDao.method = method;
        }
        if (unit != null) {
            variableDao.unit = unit;
        }
        
        variableDao.user = userSession.getUser();
        variableDao.setPage(page);
        variableDao.setPageSize(pageSize);
        
        ArrayList<Variable> variables;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Variable> getResponse;
        
        // 1. Get number of variables corresponding to the search params
        Integer totalCount = variableDao.count();
        
        //2. Get the variables to return
        if (withDetail) {
            variables = variableDao.allPaginateDetails();
        } else {
            variables = variableDao.allPaginate();
        }
        
        //3. Return the result
        if (variables == null) { //Request error
            getResponse = new ResultForm<>(0, 0, variables, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (variables.isEmpty()) { //No result
            getResponse = new ResultForm<>(0, 0, variables, true, 0);
            return noResultFound(getResponse, statusList);
        } else { //Results founded. Return the results
            getResponse = new ResultForm<>(variableDao.getPageSize(), variableDao.getPage(), variables, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
    
    /**
     * Single variable GET service by URI.
     * @param uri
     * @param pageSize
     * @param page
     * @return the variable found
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
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariableDetail(
        @ApiParam(value = DocumentationAnnotation.VARIABLE_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @PathParam("variable") @URL @Required String uri,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        
        if (uri == null) {
            final Status status = new Status(StatusCodeMsg.ACCESS_ERROR, StatusCodeMsg.ERR, "Empty variable URI");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        ArrayList<VariableDetailDTO> variables = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<VariableDetailDTO> getResponse;
        
        try {
            VariableDAO variableDao = new VariableDAO();
            
            VariableDetailDTO variable = new VariableDetailDTO(variableDao.findById(uri));
            
            LOGGER.debug("variable : " + variable.getLabel() + " " + variable.getUri());
            
            variables.add(variable);

            getResponse = new ResultForm<>(pageSize, page, variables, true, 1);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } catch (NotFoundException ex) {
            getResponse = new ResultForm<>(0, 0, variables, true);
            return noResultFound(getResponse, statusList);
        } catch (Exception ex) {
            statusList.add(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, ex.getMessage()));
            getResponse = new ResultForm<>(0, 0, variables, true);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
        }
    }
}
