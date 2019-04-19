//******************************************************************************
//                                       ActuatorResourceService.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 17 avr. 2019
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
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.ActuatorDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Actuator;
import opensilex.service.resource.dto.actuator.ActuatorDTO;
import opensilex.service.resource.dto.actuator.ActuatorDetailDTO;
import opensilex.service.resource.dto.actuator.ActuatorPostDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.result.ResultForm;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * Actuator service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/actuators")
@Path("/actuators")
public class ActuatorResourceService extends ResourceService {
    
    /**
     * Generates a Actuator list from a given list of ActuatorPostDTO.
     * @param actuatorsDTOs
     * @return the list of actuators.
     */
    private List<Actuator> actuatorPostDTOsToActuators(List<ActuatorPostDTO> actuatorsDTOs) {
        ArrayList<Actuator> actuators = new ArrayList<>();
        
        for (ActuatorPostDTO actuatorDTO : actuatorsDTOs) {
            actuators.add(actuatorDTO.createObjectFromDTO());
        }
        
        return actuators;
    }
    
    /**
     * Generates an Actuator list form a given list of ActuatoDTOs
     * @param actuatorsDTOs
     * @return 
     */
    private List<Actuator> actuatorDTOsToActuators(List<ActuatorDTO> actuatorsDTOs) {
        ArrayList<Actuator> actuators = new ArrayList<>();
        
        for (ActuatorDTO actuatorDTO : actuatorsDTOs) {
            actuators.add(actuatorDTO.createObjectFromDTO());
        }
        
        return actuators;
    }
    
    /**
     * Create actuators.
     * @param actuators
     * @example
     * [
     *      {
     *          "rdfType": "http://www.opensilex.org/vocabulary/oeso#Actuator",
     *          "label": "par03_p",
     *          "brand": "Skye Instruments",
     *          "serialNumber": "A1E345F32",
     *          "inServiceDate": "2017-06-15",
     *          "dateOfPurchase": "2017-06-15",
     *          "dateOfLastCalibration": "2017-06-15",
     *          "personInCharge": "admin@opensilex.org"
     *      }
     * ]
     * @param context
     * @return the creation result.
     * @example
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [
     *              {
     *                  "message": "Resource(s) created",
     *                  "exception": {
     *                      "type": "Info",
     *                      "href": null,
     *                      "details": "Data inserted"
     *                  }
     *              }
     *          ],
     *          "datafiles": [
     *              "http://www.opensilex.org/opensilex/2019/a19005"
     *          ]
     *      }
     * }
     */
    @POST
    @ApiOperation(value = "Post actuator(s)",
                  notes = "Register actuator(s) in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "actuator(s) saved", response = ResponseFormPOST.class),
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
        @ApiParam (value = DocumentationAnnotation.ACTUATOR_POST_DEFINITION) @Valid ArrayList<ActuatorPostDTO> actuators,
        @Context HttpServletRequest context) throws Exception {
        AbstractResultForm postResponse = null;
        
        if (actuators != null && !actuators.isEmpty()) {
            ActuatorDAO actuatorDAO = new ActuatorDAO();
            actuatorDAO.user = userSession.getUser();
            
            POSTResultsReturn result = actuatorDAO.checkAndInsert(actuatorPostDTOsToActuators(actuators));
            
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
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty actuator(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Update actuators.
     * @param actuators
     * @param context
     * @example
     * 
     * @return the update result.
     * @example
     * 
     */
    @PUT
    @ApiOperation(value = "Put actuator(s)",
                  notes = "Update actuator(s) in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "actuator(s) updated", response = ResponseFormPOST.class),
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
    public Response put(
            @ApiParam(value = DocumentationAnnotation.ACTUATOR_POST_DEFINITION) @Valid ArrayList<ActuatorDTO> actuators,
            @Context HttpServletRequest context) {
        AbstractResultForm putResponse = null;

        ActuatorDAO actuatorDAO = new ActuatorDAO();

        actuatorDAO.user = userSession.getUser();

        POSTResultsReturn result = actuatorDAO.checkAndUpdate(actuatorDTOsToActuators(actuators));

        if (result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.CREATED)) {
            putResponse = new ResponseFormPOST(result.statusList);
            putResponse.getMetadata().setDatafiles(result.createdResources);
        } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                || result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
            putResponse = new ResponseFormPOST(result.statusList);
        }
        return Response.status(result.getHttpStatus()).entity(putResponse).build();
    }
    
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get an actuator",
                  notes = "Retrieve an actuator. Need URL encoded actuator URI")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a sensor", response = ActuatorDetailDTO.class, responseContainer = "List"),
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
    public Response getActuatorDetails(
        @ApiParam(value = DocumentationAnnotation.ACTUATOR_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_ACTUATOR_URI, required = true) @PathParam("uri") @URL @Required String uri,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        ArrayList<ActuatorDetailDTO> actuators = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<ActuatorDetailDTO> getResponse;
        
        try {
            ActuatorDAO actuatorDAO = new ActuatorDAO();
            
            ActuatorDetailDTO actuator = new ActuatorDetailDTO(actuatorDAO.findById(uri));
            
            actuators.add(actuator);

            getResponse = new ResultForm<>(pageSize, page, actuators, true, 1);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } catch (NotFoundException ex) {
            getResponse = new ResultForm<>(0, 0, actuators, true);
            return noResultFound(getResponse, statusList);
        } catch (Exception ex) {
            statusList.add(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, ex.getMessage()));
            getResponse = new ResultForm<>(0, 0, actuators, true);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
        }
    }
}
