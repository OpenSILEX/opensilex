//******************************************************************************
//                           UnitResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 18 November 2017
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
import opensilex.service.dao.UnitDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.UnitDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Unit;

/**
 * Unit resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/units")
@Path("units")
public class UnitResourceService extends ResourceService {
    
    /**
     * Unit POST service.
     * @param units
     * @param context
     * @return the POST result
     */
    @POST
    @ApiOperation(value = "Post unit(s)",
                  notes = "Register new unit(s) in the data base")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Unit(s) saved", response = ResponseFormPOST.class),
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
    public Response postUnit(@ApiParam(value = DocumentationAnnotation.UNIT_POST_DATA_DEFINITION, required = true) @Valid ArrayList<UnitDTO> units,
                              @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        if (units != null && !units.isEmpty()) {
            UnitDAO unitDao = new UnitDAO();
            if (context.getRemoteAddr() != null) {
                unitDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            unitDao.user = userSession.getUser();
            
            POSTResultsReturn result = unitDao.checkAndInsert(units);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                //Code 201: units inserted
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty unit(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Unit PUT service.
     * @param units
     * @param context
     * @return thePUT result
     */
    @PUT
    @ApiOperation(value = "Update unit")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Unit updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Unit not found"),
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
    public Response putUnit(
        @ApiParam(value = DocumentationAnnotation.UNIT_POST_DATA_DEFINITION) @Valid ArrayList<UnitDTO> units,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        if (units != null && !units.isEmpty()) {
            UnitDAO unitDao = new UnitDAO();
            if (context.getRemoteAddr() != null) {
                unitDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            unitDao.user = userSession.getUser();
            
            POSTResultsReturn result = unitDao.checkAndUpdate(units);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) {
                //Code 200: units updated
                postResponse = new ResponseFormPOST(result.statusList);
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty unit(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Get units data.
     * @param unitDao
     * @return la réponse pour l'utilisateur. Contient la liste des unités
     *         correspondant à la recherche
     * SILEX:TODO
     * on ne peut chercher que par uri et label. Il faudra ajouter d'autres critères
     * \SILEX:TODO
     */
    private Response getUnitsData(UnitDAO unitDao) {
        ArrayList<Unit> units;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Unit> getResponse;
        
        units = unitDao.allPaginate();
        
        if (units == null) {
            getResponse = new ResultForm<>(0, 0, units, true);
            return noResultFound(getResponse, statusList);
        } else if (!units.isEmpty()) {
            getResponse = new ResultForm<>(unitDao.getPageSize(), unitDao.getPage(), units, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResultForm<>(0, 0, units, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     * Unit GET service.
     * @param limit
     * @param page
     * @param uri
     * @param label
     * @return the GET result
     */
    @GET
    @ApiOperation(value = "Get all units corresponding to the searched params given",
                  notes = "Retrieve all units authorized for the user corresponding to the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all units", response = Unit.class, responseContainer = "List"),
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
    public Response getUnitsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_UNIT_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_UNIT_LABEL) @QueryParam("label") String label
    ) {
        UnitDAO unitDao = new UnitDAO();
        
        if (uri != null) {
            unitDao.uri = uri;
        }
        if (label != null) {
            unitDao.label = label;
        }
        
        unitDao.user = userSession.getUser();
        unitDao.setPage(page);
        unitDao.setPageSize(limit);
        
        return getUnitsData(unitDao);
    }
    /**
     * Single unit GET service from URI
     * @param unit
     * @param limit
     * @param page
     * @return the unit found
     */
    @GET
    @Path("{unit}")
    @ApiOperation(value = "Get a unit", 
                  notes = "Retrieve a unit. Need URL encoded unit URI (Unique resource identifier).")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a unit.", response = Unit.class, responseContainer = "List"),
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
    public Response getUnitDetails(
        @ApiParam(value = DocumentationAnnotation.UNIT_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_UNIT_URI, required = true) @PathParam("unit") @Required @URL String unit,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        
        if (unit == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty unit URI");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        UnitDAO unitDao = new UnitDAO();
        unitDao.uri = unit;
        unitDao.setPageSize(limit);
        unitDao.setPage(page);
        unitDao.user = userSession.getUser();
        
        return getUnitsData(unitDao);
    }  
}
