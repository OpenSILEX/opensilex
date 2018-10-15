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
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.RadiometricTargetDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.radiometricTargets.RadiometricTargetDTO;
import phis2ws.service.resources.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import phis2ws.service.resources.dto.radiometricTargets.RadiometricTargetPostDTO;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormRdfResourceDefinition;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.RadiometricTarget;

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
     * @param radiometricTargets
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
        @ApiParam(value = DocumentationAnnotation.RADIOMETRIC_TARGET_POST_DEFINITION) @Valid ArrayList<RadiometricTargetPostDTO> radiometricTargets,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (radiometricTargets != null && !radiometricTargets.isEmpty()) {
            RadiometricTargetDAOSesame radiometricTargetDAO = new RadiometricTargetDAOSesame();
            
             if (context.getRemoteAddr() != null) {
                radiometricTargetDAO.remoteUserAdress = context.getRemoteAddr();
            }
            
            radiometricTargetDAO.user = userSession.getUser();
            
            POSTResultsReturn result = radiometricTargetDAO.checkAndInsert(radiometricTargets);
            
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
    
    /**
     * Search radiometric target by uri and label
     * 
     * @param pageSize
     * @param page
     * @param uri
     * @param label
     * @return  list of the radiometric targets corresponding to the search params given
     * e.g
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 3,
     *              "totalPages": 1
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "uri": "http://www.phenome-fppn.fr/id/radiometricTargets/rt001",
     *                  "label": "radiometric target name",
     *                  "properties": []
     *              },
     *          ]
     *      }
     * }
     */
    @GET
    @ApiOperation(value = "Get all radiometric targets corresponding to the search params given",
                  notes = "Retrieve all radiometric targets authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all radiometric targets", response = RdfResourceDefinitionDTO.class, responseContainer = "List"),
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
    public Response getRadiometricTargetsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_INFRASTRUCTURE_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_INFRASTRUCTURE_LABEL) @QueryParam("label") String label
    ) {
        RadiometricTargetDAOSesame rtDAO = new RadiometricTargetDAOSesame();
        
        rtDAO.uri = uri;
        rtDAO.label = label;
        rtDAO.user = userSession.getUser();
        rtDAO.setPage(page);
        rtDAO.setPageSize(pageSize);
        
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormRdfResourceDefinition getResponse;

        Integer totalCount = rtDAO.count();
        
        ArrayList<RadiometricTarget> radiometricTargets = rtDAO.getRadiometricTargets();
        
        ArrayList<RdfResourceDefinitionDTO> list = new ArrayList<>();

        if (radiometricTargets == null) {
            getResponse = new ResponseFormRdfResourceDefinition(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (radiometricTargets.isEmpty()) {
            getResponse = new ResponseFormRdfResourceDefinition(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else {
            radiometricTargets.forEach((radiometricTarget) -> {
                list.add(new RadiometricTargetDTO(radiometricTarget));
            });
            
            getResponse = new ResponseFormRdfResourceDefinition(rtDAO.getPageSize(), rtDAO.getPage(), list, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
    
    /**
     * Search radiometric target details for a given uri
     * 
     * @param uri
     * @return list of the radiometric target's detail corresponding to the search uri
     * e.g
     * {
     *   "metadata": {
     *     "pagination": null,
     *     "status": [],
     *     "datafiles": []
     *   },
     *   "result": {
     *     "data": [
     *       {
     *         "uri": "http://www.phenome-fppn.fr/id/radiometricTargets/rt001",
     *         "properties": [
     *              {
     *                "rdfType": "http://www.w3.org/2002/07/owl#Class",
     *                "relation": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
     *                "value": "http://www.phenome-fppn.fr/vocabulary/2017#RadiometricTarget"
     *              },
     *              {
     *                "rdfType": null,
     *                "relation": "http://www.w3.org/2000/01/rdf-schema#label",
     *                "value": "Test circulaire"
     *              },
     *              {
     *                "rdfType": null,
     *                "relation": "http://www.phenome-fppn.fr/vocabulary/2017#dateOfLastCalibration",
     *                "value": "6-09-07"
     *              },
     *              {
     *                "rdfType": null,
     *                "relation": "http://www.phenome-fppn.fr/vocabulary/2017#dateOfPurchase",
     *                "value": "6-08-10"
     *              }
     *         ]
     *       }
     *     ]
     *   }
     * }
     */
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get all radiometric target's details corresponding to the search uri",
                  notes = "Retrieve all radiometric target's details authorized for the user corresponding to the searched uri")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve radiometric target's details", response = RdfResourceDefinitionDTO.class, responseContainer = "List"),
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
    public Response getRadiometricTargetsDetails(
        @ApiParam(value = DocumentationAnnotation.INFRASTRUCTURE_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_INFRASTRUCTURE_URI) @PathParam("uri") @URL @Required String uri) {            
        
        RadiometricTargetDAOSesame rtDAO = new RadiometricTargetDAOSesame();
        
        rtDAO.user = userSession.getUser();
        rtDAO.uri = uri;
        
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormRdfResourceDefinition getResponse;

        ArrayList<RdfResourceDefinitionDTO> list = new ArrayList<>();
        
        RadiometricTarget radiometricTarget = new RadiometricTarget();
        if (rtDAO.getAllPropertiesWithLabels(radiometricTarget, null)) {
            list.add(new RadiometricTargetDTO(radiometricTarget));
            
            getResponse = new ResponseFormRdfResourceDefinition(rtDAO.getPageSize(), rtDAO.getPage(), list, true, list.size());
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } else {
            getResponse = new ResponseFormRdfResourceDefinition(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        }
    }
    
        
    /**
     * Return a generic response when no result are found
     * @param getResponse
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(ResultForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS, StatusCodeMsg.INFO, "No results for the radiometric target"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
}
