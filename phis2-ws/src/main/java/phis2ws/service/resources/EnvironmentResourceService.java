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
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.environment.EnvironmentPostDTO;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;

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
     * 
     * @param environments
     * @param context
     * @return 
     */
    @POST
    @ApiOperation(value = "Post environment(s)",
                  notes = "Register environment(s) in the database")
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
    public Response postEnvironment(
        @ApiParam(value = DocumentationAnnotation.ENVIRONMENT_POST_DEFINITION) @Valid ArrayList<EnvironmentPostDTO> environments,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (environments != null && !environments.isEmpty()) {
            EnvironmentDAOMongo environmentDAO = new EnvironmentDAOMongo();
        }
                
    }
}
