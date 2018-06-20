//******************************************************************************
//                                       AnnotationResourceService.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 7 mars 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  7 mars 2018
// Subject: Represents the annotation service.
//******************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.sql.Timestamp;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.dao.sesame.AnnotationDAOSesame;
import phis2ws.service.dao.sesame.TripletDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.resources.dto.AnnotationDTO;
/**
 * Represents the annotation service.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Api("/annotations")
@Path("/annotations")
public class AnnotationResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(AnnotationResourceService.class);
    
    //Session de l'utilisateur
    @SessionInject
    Session userSession;
  
    
    /**
     * insert given annotations in the triplestore
     * @param annotations annotations list to save. 
     * @param context
     * @return 
     */
    @POST
    @ApiOperation(value = "Post annotations",
                  notes = "Register new annotations in the triplestore")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Annotations saved", response = ResponseFormPOST.class),
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
    public Response postAnnotations(
            @ApiParam(value = DocumentationAnnotation.ANNOTATION_POST_DATA_DEFINITION) ArrayList<AnnotationDTO> annotations,
            @Context HttpServletRequest context) {

        AbstractResultForm postResponse = null;
        
        //If there are at least one list of annotations
        if (annotations != null && !annotations.isEmpty()) {
                AnnotationDAOSesame annotationDAOSesame = new AnnotationDAOSesame();
                if (context.getRemoteAddr() != null) {
                    annotationDAOSesame.remoteUserAdress = context.getRemoteAddr();
                }
                annotationDAOSesame.user = userSession.getUser();
                                
                POSTResultsReturn insertResult = annotationDAOSesame.checkAndInsert(annotations);
                
                //annotations inserted
                if (insertResult.getHttpStatus().equals(Response.Status.CREATED)){
                    postResponse = new ResponseFormPOST(insertResult.statusList);
                    postResponse.getMetadata().setDatafiles(insertResult.getCreatedResources());
                } else if (insertResult.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                        || insertResult.getHttpStatus().equals(Response.Status.OK)
                        || insertResult.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                    postResponse = new ResponseFormPOST(insertResult.statusList);
                }
                return Response.status(insertResult.getHttpStatus()).entity(postResponse).build();
//            
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty annotations to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST()).build();
        }
    }
    
//    /**
//     * check if the user can insert triplets in the triplestore. Only admins can
//     * insert triplets
//     * @return true if the user can insert triplets, 
//     *         false if user cannot insert triplets
//     */
//    private boolean canUserAddTriplets() {
//        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
//        return userDao.isAdmin(userSession.getUser());
//    }
}
