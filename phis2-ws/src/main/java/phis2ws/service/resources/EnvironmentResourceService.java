//******************************************************************************
//                                       EnvironmentResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 29 oct. 2018
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.mongo.EnvironmentDAOMongo;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.environment.EnvironmentMeasurePostDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.model.phis.EnvironmentMeasure;

/**
 * Environment resource service
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/environments")
@Path("/environments")
public class EnvironmentResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(EnvironmentResourceService.class);
    
    //user session
    @SessionInject
    Session userSession;
    
    /**
     * Generayes an Environment list from a given list of EnvironmentPostDTO.
     * @param environmentDTOs
     * @return the list of environments
     */
    private List<EnvironmentMeasure> environmentMeasurePostDTOsToEnvironmentMeasure(List<EnvironmentMeasurePostDTO> environmentDTOs) {
        ArrayList<EnvironmentMeasure> environments = new ArrayList<>();
        
        environmentDTOs.forEach((environmentDTO) -> {
            environments.add(environmentDTO.createObjectFromDTO());
        });
        
        return environments;
    }
    
    /**
     * Service to insert environment measures. 
     * @example
     * [
     *  {
     *      "sensorUri": "http://www.phenome-fppn.fr/diaphen/2018/s18521",
     *      "variableUri": "http://www.phenome-fppn.fr/id/variables/v001",
     *      "date": "2017-06-15T10:51:00+0200",
     *      "value": "0.5"
     *  }
     * ]
     * @param environmentMeasures
     * @param context
     * @return the insertion result. 
     */
    @POST
    @ApiOperation(value = "Post environment(s) measures",
                  notes = "Register environment(s) measures in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "environment(s) saved", response = ResponseFormPOST.class),
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
    public Response postEnvironmentMeasures(
        @ApiParam(value = DocumentationAnnotation.ENVIRONMENT_POST_DEFINITION) @Valid ArrayList<EnvironmentMeasurePostDTO> environmentMeasures,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (environmentMeasures != null && !environmentMeasures.isEmpty()) {
            EnvironmentDAOMongo environmentDAO = new EnvironmentDAOMongo();
            
            environmentDAO.user = userSession.getUser();
            
            POSTResultsReturn result = environmentDAO.checkAndInsert(environmentMeasurePostDTOsToEnvironmentMeasure(environmentMeasures));
            
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
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty environment measure(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }       
    }
}
