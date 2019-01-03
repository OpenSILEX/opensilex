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
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.SensorDAOSesame;
import phis2ws.service.dao.sesame.SensorProfileDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.SensorDTO;
import phis2ws.service.resources.dto.SensorProfileDTO;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormSensor;
import phis2ws.service.view.brapi.form.ResponseFormSensorProfile;
import phis2ws.service.view.model.phis.Sensor;

/**
 * sensor service 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/sensors")
@Path("/sensors")
public class SensorResourceService extends ResourceService {
    /**
     * Search sensors corresponding to search params given by a user
     * @param sensorDAOSesame
     * @return the sensors corresponding to the search
     */
    private Response getSensorsData(SensorDAOSesame sensorDAOSesame) {
        ArrayList<Sensor> sensors;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormSensor getResponse;
        
        //1. count
        Integer totalCount = sensorDAOSesame.count();
        //2. get sensors
        sensors = sensorDAOSesame.allPaginate();
        
        if (sensors == null) {
            getResponse = new ResponseFormSensor(0, 0, sensors, true);
            return noResultFound(getResponse, statusList);
        } else if (sensors.isEmpty()) {
            getResponse = new ResponseFormSensor(0, 0, sensors, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormSensor(sensorDAOSesame.getPageSize(), sensorDAOSesame.getPage(), sensors, true, totalCount);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }
    
    /**
     * Search sensors profile corresponding to the given sensor uri
     * @param sensorDAOSesame
     * @return the sensor profile of the given sensor uri
     */
    private Response getSensorProfileData(SensorProfileDAOSesame sensorDAOSesame) {
        ArrayList<SensorProfileDTO> sensorsProfiles;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormSensorProfile getResponse;
        
        sensorsProfiles = sensorDAOSesame.allPaginate();
        
        if (sensorsProfiles == null) {
            getResponse = new ResponseFormSensorProfile(0, 0, sensorsProfiles, true);
            return noResultFound(getResponse, statusList);
        } else if (sensorsProfiles.isEmpty()) {
            getResponse = new ResponseFormSensorProfile(0, 0, sensorsProfiles, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormSensorProfile(sensorDAOSesame.getPageSize(), sensorDAOSesame.getPage(), sensorsProfiles, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } 
    }
    
    /**
     * search sensors by uri, rdfType, label, brand, in service date, 
     * date of purchase and date of last calibration. 
     * 
     * @param pageSize
     * @param page
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param inServiceDate
     * @param dateOfPurchase
     * @param dateOfLastCalibration
     * @param personInCharge
     * @return list of the sensors corresponding to the search params given
     * e.g
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 3,
     *              "totalPages": 1
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "uri": "http://www.phenome-fppn.fr/diaphen/2018/s18001",
     *                  "rdfType": "http://www.phenome-fppn.fr/vocabulary/2017#LevelMeasurementRainGauge",
     *                  "label": "alias",
     *                  "brand": "brand",
     *                  "serialNumber": "E1ISHFUSK2345",
     *                  "inServiceDate": null,
     *                  "dateOfPurchase": null,
     *                  "dateOfLastCalibration": null,
     *                  "personInCharge": "username@mail.com"
     *              },
     *          ]
     *      }
     * }
     */
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
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @QueryParam("uri") @URL String uri,
            @ApiParam(value = "Search by type uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_RDF_TYPE) @QueryParam("rdfType") @URL String rdfType,
            @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_SENSOR_LABEL) @QueryParam("label") String label,
            @ApiParam(value = "Search by brand", example = DocumentationAnnotation.EXAMPLE_SENSOR_BRAND) @QueryParam("brand") String brand,
            @ApiParam(value = "Search by serial number", example = DocumentationAnnotation.EXAMPLE_SENSOR_SERIAL_NUMBER) @QueryParam("serialNumber") String serialNumber,
            @ApiParam(value = "Search by service date", example = DocumentationAnnotation.EXAMPLE_SENSOR_IN_SERVICE_DATE) @QueryParam("inServiceDate") @Date(DateFormat.YMD) String inServiceDate,
            @ApiParam(value = "Search by date of purchase", example = DocumentationAnnotation.EXAMPLE_SENSOR_DATE_OF_PURCHASE) @QueryParam("dateOfPurchase") @Date(DateFormat.YMD) String dateOfPurchase,
            @ApiParam(value = "Search by date of last calibration", example = DocumentationAnnotation.EXAMPLE_SENSOR_DATE_OF_LAST_CALIBRATION) @QueryParam("dateOfLastCalibration") @Date(DateFormat.YMD) String dateOfLastCalibration,
            @ApiParam(value = "Search by person in charge", example = DocumentationAnnotation.EXAMPLE_USER_EMAIL) @QueryParam("personInCharge") @Email String personInCharge) {
        
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
        if (serialNumber != null) {
            sensorDAO.serialNumber = serialNumber;
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
        if (personInCharge != null) {
            sensorDAO.personInCharge = personInCharge;
        }
        
        sensorDAO.user = userSession.getUser();
        sensorDAO.setPage(page);
        sensorDAO.setPageSize(pageSize);
        
        return getSensorsData(sensorDAO);
    }

    /**
     * get the informations about a sensor
     * @param uri
     * @param pageSize
     * @param page
     * @return the informations about the sensor if it exists
     * e.g.
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                 "uri": "http://www.phenome-fppn.fr/diaphen/2018/s18025",
     *                 "rdfType": "http://www.phenome-fppn.fr/vocabulary/2017#HumiditySensor",
     *                 "label": "aria_hr1_p",
     *                 "brand": "unknown",
     *                 "serialNumber": null,
     *                 "inServiceDate": null,
     *                 "dateOfPurchase": null,
     *                 "dateOfLastCalibration": null,
     *                 "personInCharge": "user@mail.fr"
     *              }
     *          ]
     *      }
     * }
     */
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
        @ApiParam(value = DocumentationAnnotation.SENSOR_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_SENSOR_URI, required = true) @PathParam("uri") @URL @Required String uri,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

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
    
    /**
     * search sensor's profile by uri
     * 
     * @param pageSize
     * @param page
     * @param uri
     * @return list of the sensor's profile corresponding to the search params given
     * e.g
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 3,
     *              "totalPages": 1
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "uri": "http://www.phenome-fppn.fr/diaphen/2018/s18560",
     *                  "properties": [
     *                      {
     *                         "rdfType": null,
     *                         "relation": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
     *                         "value": "http://www.phenome-fppn.fr/vocabulary/2017#MultispectralCamera"
     *                       },
     *                       {
     *                         "rdfType": null,
     *                         "relation": "http://www.w3.org/2000/01/rdf-schema#label",
     *                         "value": "testMS-final-peut-etre"
     *                       },
     *                  ]
     *              }
     *         ]
     *      }
     * }
     */
    @GET
    @Path("profiles/{uri}")
    @ApiOperation(value = "Get a sensor profile",
                  notes = "Retrieve a sensor profile. Need URL encoded sensor URI")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a sensor profile", response = Sensor.class, responseContainer = "List"),
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
    public Response getSensorProfile(
        @ApiParam(value = DocumentationAnnotation.SENSOR_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @PathParam("uri") @URL @Required String uri,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        
        if (uri == null) {
            final Status status = new Status(StatusCodeMsg.ACCESS_ERROR, StatusCodeMsg.ERR, "Empty sensor uri");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        SensorProfileDAOSesame sensorDAO = new SensorProfileDAOSesame();
        sensorDAO.uri = uri;
        sensorDAO.setPage(page);
        sensorDAO.setPageSize(pageSize);
        sensorDAO.user = userSession.getUser();
        
        return getSensorProfileData(sensorDAO);
    }
    
    /**
     * insert sensors in the database(s)
     * @param sensors list of sensors to insert.
     *                e.g.
     * {
     *      "rdfType": "http://www.phenome-fppn.fr/vocabulary/2017#Thermocouple",
     *      "label": "tcorg0001",
     *      "brand": "Homemade",
     *      "serialNumber": "A1E345F32",
     *      "inServiceDate": "2017-06-15",
     *      "dateOfPurchase": "2017-06-15",
     *      "dateOfLastCalibration": "2017-06-15",
     *      "personInCharge": "morgane.vidal@inra.fr"
     * }
     * @param context
     * @return the post result with the errors or the uri of the inserted sensors
     */
    @POST
    @ApiOperation(value = "Post a sensor",
                  notes = "Register a new sensor in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Sensor saved", response = ResponseFormPOST.class),
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
    public Response post(
        @ApiParam (value = DocumentationAnnotation.SENSOR_POST_DEFINITION) @Valid ArrayList<SensorDTO> sensors,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (sensors != null && !sensors.isEmpty()) {
            SensorDAOSesame sensorDAOSesame = new SensorDAOSesame();
            
            if (context.getRemoteAddr() != null) {
                sensorDAOSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            sensorDAOSesame.user = userSession.getUser();
            
            POSTResultsReturn result = sensorDAOSesame.checkAndInsert(sensors);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty sensor(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * update the given sensors
     * e.g. 
     * [
     *      {
     *          "uri": "http://www.phenome-fppn.fr/diaphen/2018/s18142",
     *          "rdfType": "http://www.phenome-fppn.fr/vocabulary/2017#Thermocouple",
     *          "label": "testNewLabel",
     *          "brand": "Skye Instrdfgduments",
     *          "serialNumber": "A1E34qsf5F32",
     *          "inServiceDate": "2017-06-15",
     *          "dateOfPurchase": "2017-06-15",
     *          "dateOfLastCalibration": "2017-06-15",
     *          "personInCharge": "morgane.vidal@inra.fr"
     *      }
     * ]
     * @param sensors
     * @param context
     * @return the post result with the founded errors or the uris of the updated sensors
     */
    @PUT
    @ApiOperation(value = "Update sensors")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Sensor(s) updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Sensor(s) not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    public Response put(
        @ApiParam(value = DocumentationAnnotation.VECTOR_POST_DEFINITION) @Valid ArrayList<SensorDTO> sensors,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (sensors != null && !sensors.isEmpty()) {
            SensorDAOSesame sensorDAOSesame = new SensorDAOSesame();
            if (context.getRemoteAddr() != null) {
                sensorDAOSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            sensorDAOSesame.user = userSession.getUser();
            
            POSTResultsReturn result = sensorDAOSesame.checkAndUpdate(sensors);
            
            if (result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.CREATED)) {
                //Code 200, traits modifiés
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.createdResources);
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty sensors(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    @POST
    @Path("profiles")
    @ApiOperation(value = "Post sensor(s) profile(s)",
                  notes = "Register sensor(s) profile(s) in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "sensor(s) profile(s) saved", response = ResponseFormPOST.class),
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
    public Response postProfiles(
        @ApiParam(value = DocumentationAnnotation.SENSOR_PROFILE_POST_DEFINITION) @Valid ArrayList<SensorProfileDTO> profiles,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (profiles != null && !profiles.isEmpty()) {
            SensorProfileDAOSesame sensorProfileDAO = new SensorProfileDAOSesame();
            
             if (context.getRemoteAddr() != null) {
                sensorProfileDAO.remoteUserAdress = context.getRemoteAddr();
            }
            
            sensorProfileDAO.user = userSession.getUser();
            
            POSTResultsReturn result = sensorProfileDAO.checkAndInsert(profiles);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty sensor(s) profile(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * 
     * @param variables the list of variables measured by the sensor. This list can be empty but not null.
     * @param uri
     * @param context
     * @return the result
     * @example
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [
     *              {
     *                  "message": "Resources updated",
     *                  "exception": {
     *                      "type": "Info",
     *                      "href": null,
     *                      "details": "The sensor http://www.phenome-fppn.fr/diaphen/2018/s18533 has now 2 linked variables"
     *                  }
     *              }
     *          ],
     *      "datafiles": [
     *          "http://www.phenome-fppn.fr/diaphen/2018/s18533"
     *      ]
     *    }
     * }
     */
    @PUT
    @Path("{uri}/variables")
    @ApiOperation(value = "Update the measured variables of a sensor")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Measured variables of the sensor updated", response = ResponseFormPOST.class),
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
    public Response putMeasuredVariables(
            @ApiParam(value = DocumentationAnnotation.LINK_VARIABLES_DEFINITION) @URL ArrayList<String> variables,
            @ApiParam(value = DocumentationAnnotation.SENSOR_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_SENSOR_URI, required = true) @PathParam("uri") @Required @URL String uri,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        SensorDAOSesame sensorDAO = new SensorDAOSesame();
        if (context.getRemoteAddr() != null) {
            sensorDAO.remoteUserAdress = context.getRemoteAddr();
        }
        
        sensorDAO.user = userSession.getUser();
        
        POSTResultsReturn result = sensorDAO.checkAndUpdateMeasuredVariables(uri, variables);
        
        if (result.getHttpStatus().equals(Response.Status.CREATED)) {
            postResponse = new ResponseFormPOST(result.statusList);
            postResponse.getMetadata().setDatafiles(result.getCreatedResources());
        } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                || result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
            postResponse = new ResponseFormPOST(result.statusList);
        }
        
        return Response.status(result.getHttpStatus()).entity(postResponse).build();
    }
}
