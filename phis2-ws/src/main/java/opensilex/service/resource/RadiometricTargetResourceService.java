//******************************************************************************
//                                       RadiometricTargetResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 26 Sept. 2018
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
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.PropertyDAO;
import opensilex.service.dao.RadiometricTargetDAO;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.radiometricTarget.RadiometricTargetDTO;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import opensilex.service.resource.dto.radiometricTarget.RadiometricTargetPostDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.RadiometricTarget;
import org.slf4j.LoggerFactory;

/**
 * Radiometric target resource service.
 * @update [Andreas Garcia] 15 Apr. 2019: handle DAO persistence exceptions thrown by property DAO functions.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/radiometricTargets")
@Path("/radiometricTargets")
public class RadiometricTargetResourceService extends ResourceService {
    final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RadiometricTargetResourceService.class);
    
    /**
     * Generates a RadiometricTarget list from a given list of RadiometricTargetPostDTO
     * @param radiometricTargetsPostDTO
     * @return the list of radiometric targets
     */
    private List<RadiometricTarget> radiometricTargetPostDTOsToRadiometricTargets(List<RadiometricTargetPostDTO> radiometricTargetsPostDTO) {
        ArrayList<RadiometricTarget> radiometricTargets = new ArrayList<>();
        
        radiometricTargetsPostDTO.forEach((radiometricTargetPostDTO) -> {
            radiometricTargets.add(radiometricTargetPostDTO.createObjectFromDTO());
        });
        
        return radiometricTargets;
    }
    
    /**
     * Generates a RadiometricTarget list from a given list of RadiometricTargetDTO.
     * @param radiometricTargetsDTO
     * @return the list of radiometric targets
     */
    private List<RadiometricTarget> radiometricTargetDTOsToRadiometricTargets(List<RadiometricTargetDTO> radiometricTargetsDTO) {
        ArrayList<RadiometricTarget> radiometricTargets = new ArrayList<>();
        
        radiometricTargetsDTO.forEach((radiometricTargetPostDTO) -> {
            radiometricTargets.add(radiometricTargetPostDTO.createRadiometricTargetFromDTO());
        });
        
        return radiometricTargets;
    }
    
    /**
     * Service to insert a given list of radiometric targets in the database.
     * @example
     * [
     *  {
     *    "label": "rt1",
     *    "properties": [
     *        {
     *            "rdfType": null,
     *            "relation": "http://www.opensilex.org/vocabulary/oeso#hasvfcShape",
     *            "value": "3"
     *        },
     *        {
     *            "rdfType": null,
     *            "relation": "http://www.w3.org/2000/01/rdf-schema#comment",
     *            "value": "3"
     *        }
     *    ]
     *  }
     * ]
     * @param radiometricTargets
     * @param context
     * @return The founded errors
     *         The list of the URIs of the created radiometric targets
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
    public Response postRadiometricTargets(
        @ApiParam(value = DocumentationAnnotation.RADIOMETRIC_TARGET_POST_DEFINITION) @Valid ArrayList<RadiometricTargetPostDTO> radiometricTargets,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (radiometricTargets != null && !radiometricTargets.isEmpty()) {
            RadiometricTargetDAO radiometricTargetDAO = new RadiometricTargetDAO();
            
             if (context.getRemoteAddr() != null) {
                radiometricTargetDAO.remoteUserAdress = context.getRemoteAddr();
            }
            
            radiometricTargetDAO.user = userSession.getUser();
            
            POSTResultsReturn result;
            try {
                result = radiometricTargetDAO.checkAndInsert(radiometricTargetPostDTOsToRadiometricTargets(radiometricTargets));
            
                if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                    postResponse = new ResponseFormPOST(result.statusList);
                    postResponse.getMetadata().setDatafiles(result.getCreatedResources());
                } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                        || result.getHttpStatus().equals(Response.Status.OK)
                        || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                    postResponse = new ResponseFormPOST(result.statusList);
                }
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } catch (DAOPersistenceException ex) {
                LOGGER.error(ex.getMessage(), ex);
                // Request error
                postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Error in the persistence system"));
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postResponse).build();
            }
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty radiometric(s) target(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Radiometric target PUT service.
     * @param radiometricTargets
     * @param context
     * @return 
     */
    @PUT
    @ApiOperation(value = "Update radiometric targets")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Radiometric target(s) updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Radiometric target(s) not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(
        @ApiParam(value = DocumentationAnnotation.RADIOMETRIC_TARGET_POST_DEFINITION) @Valid ArrayList<RadiometricTargetDTO> radiometricTargets,
        @Context HttpServletRequest context) {
        AbstractResultForm putResponse = null;

        RadiometricTargetDAO radiometricTargetDAO = new RadiometricTargetDAO();
        if (context.getRemoteAddr() != null) {
            radiometricTargetDAO.remoteUserAdress = context.getRemoteAddr();
        }

        radiometricTargetDAO.user = userSession.getUser();

        POSTResultsReturn result;
        try {
            result = radiometricTargetDAO.checkAndUpdate(radiometricTargetDTOsToRadiometricTargets(radiometricTargets));
            if (result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.CREATED)) {
                putResponse = new ResponseFormPOST(result.statusList);
                putResponse.getMetadata().setDatafiles(result.createdResources);
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                putResponse = new ResponseFormPOST(result.statusList);
            }
        } catch (DAOPersistenceException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return getResponseWhenPersistenceError(ex);
        }

        return Response.status(result.getHttpStatus()).entity(putResponse).build();
    }
    
    /**
     * Searches radiometric target by URI and label.
     * @param pageSize
     * @param page
     * @param uri
     * @param label
     * @return  list of the radiometric targets corresponding to the search params given
     * @example
     * {
     *    "metadata": {
     *        "pagination": {
     *            "pageSize": 20,
     *            "currentPage": 0,
     *            "totalCount": 3,
     *            "totalPages": 1
     *        },
     *        "status": [],
     *        "datafiles": []
     *    },
     *    "result": {
     *        "data": [
     *            {
     *                "uri": "http://www.phenome-fppn.fr/id/radiometricTargets/rt001",
     *                "label": "radiometric target name",
     *                "properties": []
     *            },
     *        ]
     *    }
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
        // 1. Initialize radiometricTargetDAO with parameters
        RadiometricTargetDAO radiometricTargetDAO = new RadiometricTargetDAO();
        
        radiometricTargetDAO.uri = uri;
        radiometricTargetDAO.label = label;
        radiometricTargetDAO.user = userSession.getUser();
        radiometricTargetDAO.setPage(page);
        radiometricTargetDAO.setPageSize(pageSize);

        // 2. Get radiometric target count
        Integer totalCount = radiometricTargetDAO.count();
        
        // 3. Get radiometric target page list
        ArrayList<RadiometricTarget> radiometricTargets = radiometricTargetDAO.allPaginate();
        
        // 4. Initialize return variables
        ArrayList<RdfResourceDefinitionDTO> list = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<RdfResourceDefinitionDTO> getResponse;
        
        if (radiometricTargets == null) {
            // Request failure
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (radiometricTargets.isEmpty()) {
            // No results
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else {
            // Convert all RadiometricTarget object to DTO's
            radiometricTargets.forEach((radiometricTarget) -> {
                list.add(new RadiometricTargetDTO(radiometricTarget));
            });
            
            // Return list of DTO
            getResponse = new ResultForm<>(radiometricTargetDAO.getPageSize(), radiometricTargetDAO.getPage(), list, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
    
    /**
     * Searches radiometric target details for a given URI.
     * @param uri
     * @return list of the radiometric target's detail corresponding to the search uri
     * @example
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
     *            {
     *              "rdfType": "http://www.w3.org/2002/07/owl#Class",
     *              "relation": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
     *              "value": "http://www.opensilex.org/vocabulary/oeso#RadiometricTarget"
     *            },
     *            {
     *              "rdfType": null,
     *              "relation": "http://www.w3.org/2000/01/rdf-schema#label",
     *              "value": "Test circulaire"
     *            },
     *            {
     *              "rdfType": null,
     *              "relation": "http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration",
     *              "value": "6-09-07"
     *            },
     *            {
     *              "rdfType": null,
     *              "relation": "http://www.opensilex.org/vocabulary/oeso#dateOfPurchase",
     *              "value": "6-08-10"
     *            }
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
        @ApiParam(
                value = DocumentationAnnotation.INFRASTRUCTURE_URI_DEFINITION, 
                required = true, 
                example = DocumentationAnnotation.EXAMPLE_INFRASTRUCTURE_URI) 
            @PathParam("uri") @URL @Required String uri) { 
        
        try {
            // 1. Initialize propertyDAO with parameters
            PropertyDAO propertyDAO = new PropertyDAO();
            
            propertyDAO.user = userSession.getUser();
            
            // 2. Initialize result variable
            ArrayList<Status> statusList = new ArrayList<>();
            ResultForm<RdfResourceDefinitionDTO> getResponse;
            ArrayList<RdfResourceDefinitionDTO> list = new ArrayList<>();
            
            // Get all properties in the given language and fill them in RadiometricTarget object
            RadiometricTarget radiometricTarget = new RadiometricTarget();
            radiometricTarget.setUri(uri);
            if (propertyDAO.getAllPropertiesWithLabels(radiometricTarget, null)) {
                // Convert the radiometricTarget to a RadiometricTargetDTO
                list.add(new RadiometricTargetDTO(radiometricTarget));
                
                // Return it
                getResponse = new ResultForm<>(propertyDAO.getPageSize(), propertyDAO.getPage(), list, true, list.size());
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            } else {
                // No result found
                getResponse = new ResultForm<>(0, 0, list, true, 0);
                return noResultFound(getResponse, statusList);
            }
        } catch (DAOPersistenceException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return getResponseWhenPersistenceError(ex);
        }
    }
}
