//**********************************************************************************************
//                                       ImageResourceService.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 8 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 8 2017
// Subject: Represents the images data service
//***********************************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.ImageMetadataDTO;

public class ImageResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(ImageResourceService.class);
    
    //Session Utilisateur
    @SessionInject
    Session userSession;
    
    @POST
    @ApiOperation(value = "Save a file", notes = DocumentationAnnotation.ADMIN_ONLY_NOTES) 
    @ApiResponses(value = {
        @ApiResponse(code = 202, message = "Metadata verified and correct", response = ImageDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)})
    @ApiImplicitParams({
       @ApiImplicitParam(name = "Authorization", required = true,
                         dataType = "string", paramType = "header",
                         value = DocumentationAnnotation.ACCES_TOKEN,
                         example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postImagesMetadata(@Context HttpHeaders headers,
            @ApiParam(value = "JSON Image metadata", required = true) List<ImageMetadataDTO> imagesAnnotations) throws RepositoryException {
        //TODO - metadonnées images
    }
}
