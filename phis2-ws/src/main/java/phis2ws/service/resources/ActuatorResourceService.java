//******************************************************************************
//                                       ActuatorResourceService.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 17 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.ActuatorDAO;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.actuator.ActuatorDTO;
import phis2ws.service.resources.dto.actuator.ActuatorPostDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.model.phis.Actuator;

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
}
