//******************************************************************************
//                            LayerResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: August 2017
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
import java.io.File;
import java.io.IOException;
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
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.LayerDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.LayerDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.ResourcesUtils;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * Layer resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/layers")
@Path("layers")
public class LayerResourceService extends ResourceService {
    
    /**
     * Layer POST service.
     * @param layers
     * @param context
     * @return 
     * @throws java.io.IOException 
     */
    @POST
    @ApiOperation(value = "Post a layer",
                  notes = "Create a geojson layer file")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Layer geojson file created", response = ResponseFormPOST.class),
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
    public Response postLayer(
        @ApiParam(value = DocumentationAnnotation.LAYER_POST_DATA_DEFINITION) @Valid ArrayList<LayerDTO> layers,
        @Context HttpServletRequest context) throws IOException {
        AbstractResultForm postResponse = null;
        
        /*
        SILEX:todo
        Generate a layer per element and return the URL of all the generared files.
        */
        if (layers !=  null && !layers.isEmpty()) { 
            LayerDAO layerDao = new LayerDAO();
            List<String> urlFilesList = new ArrayList<>();
            if (ResourcesUtils.getStringBooleanValue(layers.get(0).getGenerateFile())) { // Generate file

                /*
                SILEX:todo
                For the moment, done for a single layer (the first of the list sent)
                Then it should be done for the rest.
                */
                POSTResultsReturn resultCreateFile = layerDao.createLayerFile(layers.get(0));

                
                /*
                SILEX:warning
                Not sure of this. To check
                */
                if (resultCreateFile.getHttpStatus().equals(Response.Status.CREATED)) {
                /*
                /SILEX:warning    
                */
                    // Base return. The file URL has to be added.
                    postResponse = new ResponseFormPOST(resultCreateFile.statusList);
                    urlFilesList.add(layerDao.fileWebPath);
                    postResponse.getMetadata().setDatafiles(urlFilesList);
                    return Response.status(resultCreateFile.getHttpStatus()).entity(postResponse).build();
                } else {
                    return Response.status(resultCreateFile.getHttpStatus()).entity(postResponse).build();
                }
            } else { // The file mustn't be generated
                String fileWebPath = layerDao.getObjectURILayerFilePath(layers.get(0).getObjectUri());
                File f = new File(fileWebPath);
                
                if (f.exists()) { // Return the URL is existing
                    urlFilesList.add(layerDao.getObjectURILayerFileWebPath(layers.get(0).getObjectUri()));
                    List<Status> statusList = new ArrayList<>();
                    statusList.add(new Status("File exist", StatusCodeMsg.INFO, fileWebPath));
                    POSTResultsReturn layerFile = new POSTResultsReturn(true, true, true);
                    layerFile.statusList = statusList;
                    postResponse = new ResponseFormPOST(layerFile.statusList);
                    postResponse.getMetadata().setDatafiles(urlFilesList);
                    
                    return Response.status(layerFile.getHttpStatus()).entity(postResponse).build();
                } else { // Otherwise, an error has to be generated to tell the file doesn't exist
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
            }
                //\SILEX:todo
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST()).build();
        }    
        //\SILEX:todo
    }
}
