//******************************************************************************
//                                       RadiometricTargetResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 26 sept. 2018
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
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.RadiometricTargetDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.radiometricTargets.RadiometricTargetPostDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;

/**
 * Radiometric target service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/radiometricTargets")
@Path("/radiometricTargets")
public class RadiometricTargetResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(RadiometricTargetResourceService.class);
    
    //user session
    @SessionInject
    Session userSession;
    
    /**
     * Service to insert a given list of radiometric targets in the database.
     * @example
     * [
     *  {
     *      "label": "rt1",
     *      "properties": [
     *          {
     *              "rdfType": null,
     *              "relation": "http://www.phenome-fppn.fr/vocabulary/2017#hasvfcShape",
     *              "value": "3"
     *          },
     *          {
     *              "rdfType": null,
     *              "relation": "http://www.w3.org/2000/01/rdf-schema#comment",
     *              "value": "3"
     *          }
     *      ]
     *  }
     * ]
     * @param profiles
     * @param context
     * @return The founded errors
     *         The list of the uris of the created radiometric targets
     */
    @POST
    @ApiOperation(value = "Post radiometric(s) target(s) ",
                  notes = "Register radiometric(s) target(s) in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "radiometric(s) target(s) saved", response = ResponseFormPOST.class),
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
        @ApiParam(value = DocumentationAnnotation.SENSOR_PROFILE_POST_DEFINITION) @Valid ArrayList<RadiometricTargetPostDTO> profiles,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (profiles != null && !profiles.isEmpty()) {
            RadiometricTargetDAOSesame radiometricTargetDAO = new RadiometricTargetDAOSesame();
            
             if (context.getRemoteAddr() != null) {
                radiometricTargetDAO.remoteUserAdress = context.getRemoteAddr();
            }
            
            radiometricTargetDAO.user = userSession.getUser();
            
            POSTResultsReturn result = radiometricTargetDAO.checkAndInsert(profiles);
            
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
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty radiometric(s) target(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
}
