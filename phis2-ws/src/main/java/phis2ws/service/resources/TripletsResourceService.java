//******************************************************************************
//                                       TripletsResourceService.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 7 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  7 mars 2018
// Subject: Represents the triplets service.
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
import javax.validation.Valid;
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
import phis2ws.service.dao.sesame.TripletDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.TripletDTO;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;

/**
 * Represents the triplets service.
 * A triplet corresponds to an rdf triplet.
 * it is composed of a subject, a predicate and an object
 * @see https://www.w3.org/wiki/JSON_Triple_Sets
 * for the moment, the following keys have been implemented :
 * "s"
 * "p"
 * "o"
 * "o_type" = "literal" or "uri" ("bnode" is not implemented yet)
 * "o_lang" = "en-US" for example
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/triplets")
@Path("/triplets")
public class TripletsResourceService extends ResourceService {
    /**
     * insert given triplets in the triplestore
     * @param triplets triplets list to save. triplets is a list of list of triplets.
     * Each list corresponds to a set of triplets linked each other. 
     * If the uri of the subject is unknown, the "s" field must be equals to "?"
     * the post service will generate an uri for the subject. 
     * e.g of query : 
     * [
     *      {
     *          "s": "?",
     *          "p": "rdf:type",
     *          "o": "http://www.opensilex.org/vocabulary/oeso#Experiment",
     *          "o_type": "uri",
     *          "g": "http://www.phenome-fppn.fr/phis_field/DIA2017-2"
     *      },
     *      {
     *        "s": "?",
     *        "p": "http://www.opensilex.org/vocabulary/oeso#hasDocument",
     *        "o": "http://www.phenome-fppn.fr/phis_field/documents/documente597f57ba71d421a86277d830f4b9885",
     *        "o_type": "uri" ,
     *        "g": "http://www.phenome-fppn.fr/phis_field/DIA2017-2"
     *      }
     * ]
     * @param context
     * @return 
     */
    @POST
    @ApiOperation(value = "Post triplets",
                  notes = "Register new triplets in the triplestore")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Triplets saved", response = ResponseFormPOST.class),
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
    public Response postTriplets(
            @ApiParam(value = DocumentationAnnotation.TRIPLET_POST_DATA_DEFINITION, required = true) @Required @Valid ArrayList<ArrayList<TripletDTO>> triplets,
            @Context HttpServletRequest context) {
        //SILEX:warning
        //blank nodes are not implemented yet
        //\SILEX:warning

        AbstractResultForm postResponse = null;
        
        //If there are at least one list of triplets
        if (triplets != null && !triplets.isEmpty()) {
            if (canUserAddTriplets()) { //If the user has the rights to insert triplets
                TripletDAOSesame tripletDAOSesame = new TripletDAOSesame();
                if (context.getRemoteAddr() != null) {
                    tripletDAOSesame.remoteUserAdress = context.getRemoteAddr();
                }
                tripletDAOSesame.user = userSession.getUser();
                
                String graphUri = PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "baseURI") + Long.toString(new Timestamp(System.currentTimeMillis()).getTime());
                
                POSTResultsReturn insertResult = tripletDAOSesame.checkAndInsert(triplets, graphUri);
                
                //triplets inserted
                if (insertResult.getHttpStatus().equals(Response.Status.CREATED)){
                    postResponse = new ResponseFormPOST(insertResult.statusList);
                    postResponse.getMetadata().setDatafiles(insertResult.getCreatedResources());
                } else if (insertResult.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                        || insertResult.getHttpStatus().equals(Response.Status.OK)
                        || insertResult.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                    postResponse = new ResponseFormPOST(insertResult.statusList);
                }
                return Response.status(insertResult.getHttpStatus()).entity(postResponse).build();
            } else {
                postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, StatusCodeMsg.ADMINISTRATOR_ONLY));
                return Response.status(Response.Status.FORBIDDEN).entity(new ResponseFormPOST()).build();
            }
            
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty triplets to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST()).build();
        }
    }
    
    /**
     * check if the user can insert triplets in the triplestore. Only admins can
     * insert triplets
     * @return true if the user can insert triplets, 
     *         false if user cannot insert triplets
     */
    private boolean canUserAddTriplets() {
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
        return userDao.isAdmin(userSession.getUser());
    }
}
