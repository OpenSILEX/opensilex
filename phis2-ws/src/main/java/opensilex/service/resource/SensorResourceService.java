//******************************************************************************
//                             SensorResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 14 March 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import com.mongodb.BasicDBObjectBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import opensilex.service.configuration.DateFormat;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.DataDAO;
import opensilex.service.dao.ProvenanceDAO;
import opensilex.service.dao.SensorDAO;
import opensilex.service.dao.SensorProfileDAO;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Data;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Sensor;
import opensilex.service.resource.dto.data.DataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.resource.dto.sensor.SensorDTO;
import opensilex.service.resource.dto.sensor.SensorDetailDTO;
import opensilex.service.resource.dto.sensor.SensorPostDTO;
import opensilex.service.resource.dto.sensor.SensorProfileDTO;
import opensilex.service.view.model.provenance.Provenance;

/**
 * Sensor resource service.
 * @update [Andreas Garcia] 15 Apr. 2019: handle DAO persistence exceptions thrown by property DAO functions.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/sensors")
@Path("/sensors")
public class SensorResourceService extends ResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(SensorResourceService.class);
    
    /**
     * Searches sensors profile corresponding to the given sensor URI.
     * @param sensorDao
     * @return the sensor profile of the given sensor URI
     */
    private Response getSensorProfileData(SensorProfileDAO sensorDao) {
        ArrayList<SensorProfileDTO> sensorsProfiles;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<SensorProfileDTO> getResponse;
        
        sensorsProfiles = sensorDao.allPaginate();
        
        if (sensorsProfiles == null) {
            getResponse = new ResultForm<>(0, 0, sensorsProfiles, true);
            return noResultFound(getResponse, statusList);
        } else if (sensorsProfiles.isEmpty()) {
            getResponse = new ResultForm<>(0, 0, sensorsProfiles, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResultForm<>(sensorDao.getPageSize(), sensorDao.getPage(), sensorsProfiles, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } 
    }
    
    /**
     * Searches sensors by URI, rdfType, label, brand, in service date, 
     * date of purchase and date of last calibration. 
     * @param pageSize
     * @param page
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param serialNumber
     * @param model
     * @param inServiceDate
     * @param dateOfPurchase
     * @param dateOfLastCalibration
     * @param personInCharge
     * @return list of the sensors corresponding to the search params given
     * @example
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
     *                  "rdfType": "http://www.opensilex.org/vocabulary/oeso#LevelMeasurementRainGauge",
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
        @ApiResponse(code = 200, message = "Retrieve all sensors", response = SensorDetailDTO.class, responseContainer = "List"),
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
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @QueryParam("uri") String uri,
            @ApiParam(value = "Search by type uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_RDF_TYPE) @QueryParam("rdfType") @URL String rdfType,
            @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_SENSOR_LABEL) @QueryParam("label") String label,
            @ApiParam(value = "Search by brand", example = DocumentationAnnotation.EXAMPLE_SENSOR_BRAND) @QueryParam("brand") String brand,
            @ApiParam(value = "Search by serial number", example = DocumentationAnnotation.EXAMPLE_SENSOR_SERIAL_NUMBER) @QueryParam("serialNumber") String serialNumber,
            @ApiParam(value = "Search by model", example = DocumentationAnnotation.EXAMPLE_SENSOR_MODEL) @QueryParam("model") String model,
            @ApiParam(value = "Search by service date", example = DocumentationAnnotation.EXAMPLE_SENSOR_IN_SERVICE_DATE) @QueryParam("inServiceDate") @Date(DateFormat.YMD) String inServiceDate,
            @ApiParam(value = "Search by date of purchase", example = DocumentationAnnotation.EXAMPLE_SENSOR_DATE_OF_PURCHASE) @QueryParam("dateOfPurchase") @Date(DateFormat.YMD) String dateOfPurchase,
            @ApiParam(value = "Search by date of last calibration", example = DocumentationAnnotation.EXAMPLE_SENSOR_DATE_OF_LAST_CALIBRATION) @QueryParam("dateOfLastCalibration") @Date(DateFormat.YMD) String dateOfLastCalibration,
            @ApiParam(value = "Search by person in charge", example = DocumentationAnnotation.EXAMPLE_USER_EMAIL) @QueryParam("personInCharge") String personInCharge) {
        
        SensorDAO sensorDAO = new SensorDAO();
        //1. Get count
        Integer totalCount = sensorDAO.count(uri, rdfType, label, brand, serialNumber, model, inServiceDate, dateOfPurchase, dateOfLastCalibration, personInCharge);
        ArrayList<Sensor> sensorsFounded = new ArrayList<>();
         //2. Get sensors
        if(totalCount > 0){
            sensorsFounded = sensorDAO.find(page, pageSize, uri, rdfType, label, brand, serialNumber, model, inServiceDate, dateOfPurchase, dateOfLastCalibration, personInCharge);
        }
       
        //3. Return result
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<SensorDTO> sensorsToReturn = new ArrayList<>();
        ResultForm<SensorDTO> getResponse;
        if (sensorsFounded == null) { //Request failure
            getResponse = new ResultForm<>(0, 0, sensorsToReturn, true);
            return noResultFound(getResponse, statusList);
        } else if (sensorsFounded.isEmpty()) { //No result found
            getResponse = new ResultForm<>(0, 0, sensorsToReturn, true);
            return noResultFound(getResponse, statusList);
        } else { //Results
            //Convert all objects to DTOs
            sensorsFounded.forEach((sensor) -> {
                sensorsToReturn.add(new SensorDTO(sensor));
            });
            
            getResponse = new ResultForm<>(pageSize, page, sensorsToReturn, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }

    /**
     * Gets the information about a sensor.
     * @param uri
     * @param pageSize
     * @param page
     * @return the informations about the sensor if it exists
     * @example
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
     *                 "rdfType": "http://www.opensilex.org/vocabulary/oeso#HumiditySensor",
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
        @ApiResponse(code = 200, message = "Retrieve a sensor", response = SensorDetailDTO.class, responseContainer = "List"),
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
        ArrayList<SensorDetailDTO> sensors = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<SensorDetailDTO> getResponse;
        
        try {
            SensorDAO sensorDAO = new SensorDAO();
            
            SensorDetailDTO sensor = new SensorDetailDTO(sensorDAO.findById(uri));
            
            sensors.add(sensor);

            getResponse = new ResultForm<>(pageSize, page, sensors, true, 1);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } catch (NotFoundException ex) {
            getResponse = new ResultForm<>(0, 0, sensors, true);
            return noResultFound(getResponse, statusList);
        } catch (Exception ex) {
            statusList.add(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, ex.getMessage()));
            getResponse = new ResultForm<>(0, 0, sensors, true);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
        }
    }
    
    /**
     * Searches sensor's profile by its URI
     * @param pageSize
     * @param page
     * @param uri
     * @return list of the sensor's profile corresponding to the search params given
     * @example
     * {
     *  "metadata": {
     *    "pagination": {
     *        "pageSize": 20,
     *        "currentPage": 0,
     *        "totalCount": 3,
     *        "totalPages": 1
     *    },
     *    "status": [],
     *    "datafiles": []
     *  },
     *  "result": {
     *    "data": [
     *      {
     *        "uri": "http://www.phenome-fppn.fr/diaphen/2018/s18560",
     *        "properties": [
     *            {
     *               "rdfType": null,
     *               "relation": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
     *               "value": "http://www.opensilex.org/vocabulary/oeso#MultispectralCamera"
     *             },
     *             {
     *               "rdfType": null,
     *               "relation": "http://www.w3.org/2000/01/rdf-schema#label",
     *               "value": "testMS-final-peut-etre"
     *             },
     *        ]
     *      }
     *    ]
     *  }
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
        
        SensorProfileDAO sensorDAO = new SensorProfileDAO();
        sensorDAO.uri = uri;
        sensorDAO.setPage(page);
        sensorDAO.setPageSize(pageSize);
        sensorDAO.user = userSession.getUser();
        
        return getSensorProfileData(sensorDAO);
    }
    
    /**
     * Generates a Sensor list from a given list of SensorPostDTO
     * @param sensorDTOs
     * @return the list of sensors
     */
    private List<Sensor> sensorPostDTOsToSensors(List<SensorPostDTO> sensorDTOs) {
        ArrayList<Sensor> sensors = new ArrayList<>();
        
        for (SensorPostDTO sensorPostDTO : sensorDTOs) {
            sensors.add(sensorPostDTO.createObjectFromDTO());
        }
        
        return sensors;
    }
    
    /**
     * Inserts sensors in the storage.
     * @param sensors list of sensors to insert.
     * @example
     * {
     *      "rdfType": "http://www.opensilex.org/vocabulary/oeso#Thermocouple",
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
        @ApiParam (value = DocumentationAnnotation.SENSOR_POST_DEFINITION) @Valid ArrayList<SensorPostDTO> sensors,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (sensors != null && !sensors.isEmpty()) {
            SensorDAO sensorDAO = new SensorDAO();
            
            if (context.getRemoteAddr() != null) {
                sensorDAO.remoteUserAdress = context.getRemoteAddr();
            }
            
            sensorDAO.user = userSession.getUser();
            
            POSTResultsReturn result = sensorDAO.checkAndInsert(sensorPostDTOsToSensors(sensors));
            
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
     * Generates a Sensor list from a given list of SensorPostDTO
     * @param sensorDTOs
     * @return the list of sensors
     */
    private List<Sensor> sensorDTOsToSensors(List<SensorDTO> sensorDTOs) {
        ArrayList<Sensor> sensors = new ArrayList<>();
        
        for (SensorDTO sensorPostDTO : sensorDTOs) {
            sensors.add(sensorPostDTO.createObjectFromDTO());
        }
        
        return sensors;
    }
    
    /**
     * Updates the given sensors.
     * @example
     * [
     *   {
     *       "uri": "http://www.phenome-fppn.fr/diaphen/2018/s18142",
     *       "rdfType": "http://www.opensilex.org/vocabulary/oeso#Thermocouple",
     *       "label": "testNewLabel",
     *       "brand": "Skye Instrdfgduments",
     *       "serialNumber": "A1E34qsf5F32",
     *       "inServiceDate": "2017-06-15",
     *       "dateOfPurchase": "2017-06-15",
     *       "dateOfLastCalibration": "2017-06-15",
     *       "personInCharge": "morgane.vidal@inra.fr"
     *   }
     * ]
     * @param sensors
     * @param context
     * @return the post result with the founded errors or the URIs of the updated sensors
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
        @ApiParam(value = DocumentationAnnotation.SENSOR_POST_DEFINITION) @Valid ArrayList<SensorDTO> sensors,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (sensors != null && !sensors.isEmpty()) {
            try {
                SensorDAO sensorDAO = new SensorDAO();
                if (context.getRemoteAddr() != null) {
                    sensorDAO.remoteUserAdress = context.getRemoteAddr();
                }
                
                sensorDAO.user = userSession.getUser();
                
                POSTResultsReturn result = sensorDAO.checkAndUpdate(sensorDTOsToSensors(sensors));
                
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
            } catch (Exception ex) {
                postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.ERR, StatusCodeMsg.ERR, "An internal server error occurred."));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postResponse).build();
            }
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty sensors(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Sensor POST service.
     * @param profiles
     * @param context
     * @return 
     */
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
            try {
                SensorProfileDAO sensorProfileDAO = new SensorProfileDAO();
                
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
            } catch (DAOPersistenceException ex) {
                LOGGER.error(ex.getMessage(), ex);
                return getResponseWhenPersistenceError(ex);
            }
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty sensor(s) profile(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Sensor PUT service.
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
            @ApiParam(value = DocumentationAnnotation.SENSOR_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_SENSOR_URI, required = true) 
                @PathParam("uri") 
                @Required 
                @URL String uri,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        SensorDAO sensorDAO = new SensorDAO();
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
    
    /**
     * Sensor Data GET service.
     * @param pageSize
     * @param page
     * @param provenanceUri
     * @param variablesUri
     * @param startDate
     * @param endDate
     * @param object
     * @param uri
     * @return list of the data corresponding to the search params given
     * @example
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
     *               {
     *                 "uri": "http://www.phenome-fppn.fr/diaphen/id/data/d2plf65my4rc2odiv2lbjgukc2zswkqyoddh25jtoy4b5pf3le3q4ec5c332f5cd44ce82977e404cebf83c",
     *                 "provenanceUri": "http://www.phenome-fppn.fr/mtp/2018/pv181515071552",
     *                 "objectUri": "http://www.phenome-fppn.fr/diaphen/2018/o18001199",
     *                 "variableUri": "http://www.phenome-fppn.fr/diaphen/id/variables/v009",
     *                 "date": "2017-06-15T00:00:00+0200",
     *                 "value": 2.4
     *               },
     *               {
     *                 "uri": "http://www.phenome-fppn.fr/diaphen/id/data/pttdrrqybxoyku4img323dyrhmpp267mhnpiw3vld2wm6tap3vwq93b344c429ec45bb9b185edfe5bc2b64",
     *                 "provenanceUri": "http://www.phenome-fppn.fr/mtp/2018/pv181515071552",
     *                 "objectUri": "http://www.phenome-fppn.fr/diaphen/2018/o18001199",
     *                 "variableUri": "http://www.phenome-fppn.fr/diaphen/id/variables/v009",
     *                 "date": "2017-06-16T00:00:00+0200",
     *                 "value": "2017-06-15T00:00:00+0200"
     *               }
     *          ]
     *      }
     * }
     */
    @GET
    @Path("{uri}/data")
    @ApiOperation(value = "Get data mesured by a sensor")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data mesured by a sensor", response = ResponseFormPOST.class),
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
    public Response getSensorData(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by variable", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("variable") @Required String variablesUri,
            @ApiParam(value = "Search by provenance", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI) @QueryParam("provenance") String provenanceUri,
            @ApiParam(value = "Search by minimal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("startDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String startDate,
            @ApiParam(value = "Search by maximal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("endDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String endDate,
            @ApiParam(value = "Search by object uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @QueryParam("object")  @URL String object,
            @PathParam("uri") @Required @URL String uri
        ) {
        DataDAO dataDAO = new DataDAO();
        dataDAO.setPage(page);
        dataDAO.setPageSize(pageSize);

        ProvenanceDAO provenanceDAO  = new ProvenanceDAO();
        provenanceDAO.setPage(1);
        provenanceDAO.setPageSize(500);
        
        ArrayList<String> provenanceUrisAssociatedToSensor = new ArrayList<>();
        if (provenanceUri != null){
            provenanceUrisAssociatedToSensor.add(provenanceUri);
        }else{
            Provenance searchProvenance = new Provenance();
            String jsonFilter = BasicDBObjectBuilder.start("metadata.prov:Agent.oeso:SensingDevice",Arrays.asList(uri)).get().toString();
            ArrayList<Provenance> provenances = provenanceDAO.getProvenances(searchProvenance, jsonFilter);

            for (Provenance provenance : provenances) {
                provenanceUrisAssociatedToSensor.add(provenance.getUri());
            }
        }
        List<String> objectsUris = new ArrayList<>();
        if(object != null){
           objectsUris.add(object);
        }
        
        //1. Get sensor data count
        Integer totalCount = dataDAO.count(variablesUri, startDate, endDate, objectsUris, provenanceUrisAssociatedToSensor);
        List<Data> dataFounded = new ArrayList<>();
        //2. Get sensor data
        if(totalCount > 0){
            dataFounded = dataDAO.find(page, pageSize, variablesUri,  startDate, endDate, objectsUris, provenanceUrisAssociatedToSensor);
        }
        
        //3. Return result
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<DataDTO> sensorsToReturn = new ArrayList<>();
        ResultForm<DataDTO> getResponse;
        if (dataFounded == null) { //Request failure
            getResponse = new ResultForm<>(0, 0, sensorsToReturn, true);
            return noResultFound(getResponse, statusList);
        } else if (dataFounded.isEmpty()) { //No result found
            getResponse = new ResultForm<>(0, 0, sensorsToReturn, true);
            return noResultFound(getResponse, statusList);
        } else { //Results
            //Convert all objects to DTOs
            dataFounded.forEach((data) -> {
                sensorsToReturn.add(new DataDTO(data));
            });
            
            getResponse = new ResultForm<>(pageSize, page, sensorsToReturn, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}
