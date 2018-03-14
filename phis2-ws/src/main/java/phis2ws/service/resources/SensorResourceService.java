//******************************************************************************
//                                       SensorResourceService.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  14 mars 2018
// Subject: represents the sensor service
//******************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.SensorDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormSensor;
import phis2ws.service.view.model.phis.Sensor;

/**
 * sensor service 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(SensorResourceService.class);
    
    //user session
    @SessionInject
    Session userSession;
    
    /**
     * 
     * @param getResponse
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(ResponseFormSensor getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS, StatusCodeMsg.INFO, "No results for the images metadata"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    /**
     * Search sensors corresponding to search params given by a user
     * @param sensorDAOSesame
     * @return the sensors corresponding to the search
     */
    private Response getSensorsData(SensorDAOSesame sensorDAOSesame) {
        ArrayList<Sensor> sensors;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormSensor getResponse;
        
        sensors = sensorDAOSesame.allPaginate();
        
        if (sensors == null) {
            getResponse = new ResponseFormSensor(0, 0, sensors, true);
            return noResultFound(getResponse, statusList);
        } else if (sensors.isEmpty()) {
            getResponse = new ResponseFormSensor(0, 0, sensors, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormSensor(sensorDAOSesame.getPageSize(), sensorDAOSesame.getPage(), sensors, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }    
    
    @GET
    @ApiOperation(value = "Get all sensors corresponding to the search params given",
                  notes = "Retrieve all sensors authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all sensors", response = Sensor.class, responseContainer = "List"),
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
    public Response getSensorsBySearch(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @PathParam("uri") String uri,
            @ApiParam(value = "Search by type uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_RDF_TYPE) @PathParam("rdfType") String rdfType,
            @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_SENSOR_ALIAS) @PathParam("label") String label,
            @ApiParam(value = "Search by brand", example = DocumentationAnnotation.EXAMPLE_SENSOR_BRAND) @PathParam("brand") String brand,
            @ApiParam(value = "Search by variable", example = DocumentationAnnotation.EXAMPLE_SENSOR_VARIABLE) @PathParam("variable") String variable,
            @ApiParam(value = "Search by service date", example = DocumentationAnnotation.EXAMPLE_SENSOR_IN_SERVICE_DATE) @PathParam("inServiceDate") String inServiceDate,
            @ApiParam(value = "Search by date of purchase", example = DocumentationAnnotation.EXAMPLE_SENSOR_DATE_OF_PURCHASE) @PathParam("dateOfPurchase") String dateOfPurchase,
            @ApiParam(value = "Search by date of last calibration", example = DocumentationAnnotation.EXAMPLE_SENSOR_DATE_OF_LAST_CALIBRATION) @PathParam("dateOfLastCalibration") String dateOfLastCalibration) {
        
        SensorDAOSesame sensorDAO = new SensorDAOSesame();
        if (uri != null) {
            sensorDAO.uri = uri;
        }
        if (rdfType != null) {
            sensorDAO.rdfType = rdfType;
        }
        if (label != null) {
            sensorDAO.label = label;
        }
        if (brand != null) {
            sensorDAO.brand = brand;
        }
        if (variable != null) {
            sensorDAO.variable = variable;
        }
        if (inServiceDate != null) {
            sensorDAO.inServiceDate = inServiceDate;
        }
        if (dateOfPurchase != null) {
            sensorDAO.dateOfPurchase = dateOfPurchase;
        }
        if (dateOfLastCalibration != null) {
            sensorDAO.dateOfLastCalibration = dateOfLastCalibration;
        }
        
        sensorDAO.user = userSession.getUser();
        sensorDAO.setPage(page);
        sensorDAO.setPageSize(pageSize);
        
        return getSensorsData(sensorDAO);
    }

    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get a sensor",
                  notes = "Retrieve a sensor. Need URL encoded sensor URI")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a sensor", response = Sensor.class, responseContainer = "List"),
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
    public Response getSensorDetails(
        @ApiParam(value = DocumentationAnnotation.SENSOR_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @PathParam("uri") String uri,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {

        if (uri == null) {
            final Status status = new Status(StatusCodeMsg.ACCESS_ERROR, StatusCodeMsg.ERR, "Empty sensor uri");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }

        SensorDAOSesame sensorDAO = new SensorDAOSesame();
        sensorDAO.uri = uri;
        sensorDAO.setPage(page);
        sensorDAO.setPageSize(pageSize);
        sensorDAO.user = userSession.getUser();

        return getSensorsData(sensorDAO);
    }
}
