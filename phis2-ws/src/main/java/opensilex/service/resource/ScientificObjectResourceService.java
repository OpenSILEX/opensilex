//******************************************************************************
//                      ScientificObjectResourceService.java 
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
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.ScientificObjectRdf4jDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.ScientificObjectDTO;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.ScientificObject;

/**
 * Scientific objects resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/scientificObjects")
@Path("scientificObjects")
public class ScientificObjectResourceService extends ResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(ScientificObjectResourceService.class);
  
    /**
     * Enters a set of scientific objects into the storage and associate them to 
     * an experiment if given.
     * @param scientificObjectsDTO scientific objects to save
     * @param context query context element to get the IP address information of the user
     * @example 
     * [
        {
          "rdfType": "http://www.opensilex.org/vocabulary/oeso#Plant",
          "experiment": "http://www.phenome-fppn.fr/diaphen/DIA2017-2",
          "year": "2018",
          "properties": [
           {
             "relation": "http://www.w3.org/2000/01/rdf-schema#label",
             "value": "plt030"
           }
          ]
        }
     * @return the creation result
     */
    @POST
    @ApiOperation(value = "Post scientific object(s)",
                  notes = "Register new scientific object(s) in the database.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Scientific object(s) saved", response = ResponseFormPOST.class),
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
    public Response postScientificObject(
            @ApiParam(value = DocumentationAnnotation.SCIENTIFIC_OBJECT_POST_DATA_DEFINITION, required = true) @Valid ArrayList<ScientificObjectDTO> scientificObjectsDTO,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        //if there is at least one scientific object
        if (!scientificObjectsDTO.isEmpty()) {
            try {
                ScientificObjectRdf4jDAO scientificObjectRdf4jDao = new ScientificObjectRdf4jDAO();
                if (context.getRemoteAddr() != null) {
                    scientificObjectRdf4jDao.remoteUserAdress = context.getRemoteAddr();
                }
                
                scientificObjectRdf4jDao.user = userSession.getUser();
                
                //Check the scientific objects and insert them in triplestore
                POSTResultsReturn result = scientificObjectRdf4jDao.checkAndInsert(scientificObjectsDTO);
                if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                    //scientific objects inserted (201)
                    postResponse = new ResponseFormPOST(result.statusList);
                    postResponse.getMetadata().setDatafiles(result.getCreatedResources());
                    return Response.status(result.getHttpStatus()).entity(postResponse).build();
                } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                        || result.getHttpStatus().equals(Response.Status.OK)
                        || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                    postResponse = new ResponseFormPOST(result.statusList);
                }
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
            }
        } else {
            postResponse = new ResponseFormPOST(
                    new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty scientific objects list"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Collects data corresponding to the user query (scientific objects research)
     * @param ScientificObjectRdf4jDao
     * @return the response for the user. Contains the list of scientific objects
     */
    private Response getScientificObjectsData(ScientificObjectRdf4jDAO ScientificObjectRdf4jDao) {
        ArrayList<ScientificObject> scientificObjects;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<ScientificObject> getResponse;
        
        scientificObjects = ScientificObjectRdf4jDao.allPaginate();
        
        if (scientificObjects == null) {
            getResponse = new ResultForm<>(0, 0, scientificObjects, true);
            return noResultFound(getResponse, statusList);
        } else if (!scientificObjects.isEmpty()) {
            getResponse = new ResultForm<>(
                    ScientificObjectRdf4jDao.getPageSize(), 
                    ScientificObjectRdf4jDao.getPage(), 
                    scientificObjects, 
                    false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResultForm<>(0, 0, scientificObjects, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    @GET
    @ApiOperation(value = "Get all scientific objects corresponding to the searched params given",
                  notes = "Retrieve all scientific objects authorized for the user corresponding to the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all scientific objects", response = ScientificObject.class, responseContainer = "List"),
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
    public Response getScientificObjectsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by experiment URI", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("experiment") @URL String experimentURI,
        @ApiParam(value = "Search by alias", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_ALIAS) @QueryParam("alias") String alias,
        @ApiParam(value = "Search by rdfType", example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_TYPE) @QueryParam("rdfType") @URL String rdfType
    ) {
        ScientificObjectRdf4jDAO scientificObjectRdf4jDAO = new ScientificObjectRdf4jDAO();
        
        if (uri != null) {
            scientificObjectRdf4jDAO.uri = uri;
        }
        if (experimentURI != null) {
            scientificObjectRdf4jDAO.experiment = experimentURI;
        }
        if (alias != null) {
            scientificObjectRdf4jDAO.alias = alias;
        }
        if (rdfType != null) {
            scientificObjectRdf4jDAO.rdfType = rdfType;
        }
        
        scientificObjectRdf4jDAO.user = userSession.getUser();
        scientificObjectRdf4jDAO.setPage(page);
        scientificObjectRdf4jDAO.setPageSize(limit);
        
        return getScientificObjectsData(scientificObjectRdf4jDAO);
    }
}
