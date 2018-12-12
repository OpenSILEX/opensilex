//**********************************************************************************************
//                                       UnitResourceService.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 18 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 18 2017
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.UnitDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.UnitDTO;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormUnit;
import phis2ws.service.view.model.phis.Unit;

@Api("/units")
@Path("units")
public class UnitResourceService extends ResourceService {
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
            UnitDaoSesame unitDaoSesame = new UnitDaoSesame();
            if (context.getRemoteAddr() != null) {
                unitDaoSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            unitDaoSesame.user = userSession.getUser();
            
            POSTResultsReturn result = unitDaoSesame.checkAndInsert(units);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                //Code 201, unités insérés
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
            UnitDaoSesame unitDaoSesame = new UnitDaoSesame();
            if (context.getRemoteAddr() != null) {
                unitDaoSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            unitDaoSesame.user = userSession.getUser();
            
            POSTResultsReturn result = unitDaoSesame.checkAndUpdate(units);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) {
                //Code 200, unités modifiées
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
     * Collecte les données issues d'une requête de l'utilisateur (recherche d'unités)
     * @param unitDaoSesame
     * @return la réponse pour l'utilisateur. Contient la liste des unités
     *         correspondant à la recherche
     * SILEX:TODO
     * on ne peut chercher que par uri et label. Il faudra ajouter d'autres critères
     * \SILEX:TODO
     */
    private Response getUnitsData(UnitDaoSesame unitDaoSesame) {
        ArrayList<Unit> units;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormUnit getResponse;
        
        units = unitDaoSesame.allPaginate();
        
        if (units == null) {
            getResponse = new ResponseFormUnit(0, 0, units, true);
            return noResultFound(getResponse, statusList);
        } else if (!units.isEmpty()) {
            getResponse = new ResponseFormUnit(unitDaoSesame.getPageSize(), unitDaoSesame.getPage(), units, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormUnit(0, 0, units, true);
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
        UnitDaoSesame unitDaoSesame = new UnitDaoSesame();
        
        if (uri != null) {
            unitDaoSesame.uri = uri;
        }
        if (label != null) {
            unitDaoSesame.label = label;
        }
        
        unitDaoSesame.user = userSession.getUser();
        unitDaoSesame.setPage(page);
        unitDaoSesame.setPageSize(limit);
        
        return getUnitsData(unitDaoSesame);
    }
    /**
     * 
     * @param unit
     * @param limit
     * @param page
     * @return l'unité correspondant à l'uri donnée si elle existe
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
        
        UnitDaoSesame unitDaoSesame = new UnitDaoSesame();
        unitDaoSesame.uri = unit;
        unitDaoSesame.setPageSize(limit);
        unitDaoSesame.setPage(page);
        unitDaoSesame.user = userSession.getUser();
        
        return getUnitsData(unitDaoSesame);
    }  
}
