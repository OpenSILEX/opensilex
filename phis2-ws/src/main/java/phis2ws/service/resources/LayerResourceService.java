//**********************************************************************************************
//                                       LayerResourceService.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: August 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 6 2017
// Subject: Represents the layer data service
//***********************************************************************************************
package phis2ws.service.resources;

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
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.LayerDao;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.LayerDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.ResourcesUtils;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;

@Api("/layers")
@Path("layers")
public class LayerResourceService extends ResourceService {
    /**
     * 
     * @param layers
     * @param context
     * @return 
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
        
        //SILEX:TODO
        //générer une couche par élément de la liste et retourner les url de tous les fichiers générés
        if (layers !=  null && !layers.isEmpty()) { 
            LayerDao layerDao = new LayerDao();
            List<String> urlFilesList = new ArrayList<>();
            if (ResourcesUtils.getStringBooleanValue(layers.get(0).getGenerateFile())) { //On génère le fichier

                //SILEX:test
                //Pour l'instant on le fait que pour une seule couche (la première
                //de la liste envoyée. Il faudra par la suite le faire pour le reste)
                POSTResultsReturn resultCreateFile = layerDao.createLayerFile(layers.get(0));

                if (resultCreateFile.getHttpStatus().equals(Response.Status.CREATED)) { //PAS SURE DE ÇA, À VÉRIFIER
                    //Retour de base à retourner. Il me faut ajouter l'url du fichier
                    postResponse = new ResponseFormPOST(resultCreateFile.statusList);
                    urlFilesList.add(layerDao.fileWebPath);
                    postResponse.getMetadata().setDatafiles(urlFilesList);
                    return Response.status(resultCreateFile.getHttpStatus()).entity(postResponse).build();
                } else {
                    return Response.status(resultCreateFile.getHttpStatus()).entity(postResponse).build();
                }
            } else { //On ne doit pas générer le fichier
                String fileWebPath = layerDao.getObjectURILayerFilePath(layers.get(0).getObjectUri());
                File f = new File(fileWebPath);
                
                if (f.exists()) { //S'il existe, on retourne l'url
                    urlFilesList.add(layerDao.getObjectURILayerFileWebPath(layers.get(0).getObjectUri()));
                    List<Status> statusList = new ArrayList<>();
                    statusList.add(new Status("File exist", StatusCodeMsg.INFO, fileWebPath));
                    POSTResultsReturn layerFile = new POSTResultsReturn(true, true, true);
                    layerFile.statusList = statusList;
                    postResponse = new ResponseFormPOST(layerFile.statusList);
                    postResponse.getMetadata().setDatafiles(urlFilesList);
                    
                    return Response.status(layerFile.getHttpStatus()).entity(postResponse).build();
                } else { //Sinon, il faut retourner une erreur comme quoi le fichier n'existe pas. 
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
            }
            //\SILEX:test
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST()).build();
        }    
        //\SILEX:TODO
    }
}
