//******************************************************************************
//                                       VectorResourceService.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 6 avr. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  6 avr. 2018
// Subject: represents the vector service
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
import phis2ws.service.dao.sesame.VectorDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.VectorDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormVector;
import phis2ws.service.view.model.phis.Vector;

/**
 * vector service
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/vectors")
@Path("/vectors")
public class VectorResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(VectorResourceService.class);
    
    //user session
    @SessionInject
    Session userSession;
    
    /**
     * 
     * @param getResponse
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(ResponseFormVector getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS, StatusCodeMsg.INFO, "No results for the vectors"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    /**
     * Search vectors corresponding to search params given by a user
     * @param vectorDAOSesame
     * @return the vectors corresponding to the search
     */
    private Response getVectorsData(VectorDAOSesame vectorDAOSesame) {
        ArrayList<Vector> vectors;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormVector getResponse;
        
        vectors = vectorDAOSesame.allPaginate();
        
        if (vectors == null) {
            getResponse = new ResponseFormVector(0, 0, vectors, true);
            return noResultFound(getResponse, statusList);
        } else if (vectors.isEmpty()) {
            getResponse = new ResponseFormVector(0, 0, vectors, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormVector(vectorDAOSesame.getPageSize(), vectorDAOSesame.getPage(), vectors, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }  
    
    /**
     * search vectors by uri, rdfType, label, brand, in service date, 
     * date of purchase. 
     * 
     * @param pageSize
     * @param page
     * @param uri
     * @param rdfType
     * @param label
     * @param brand
     * @param inServiceDate
     * @param dateOfPurchase
     * @return list of the vectors corresponding to the search params given
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
     *                  "uri": "http://www.phenome-fppn.fr/diaphen/2018/v1801",
     *                  "rdfType": "http://www.phenome-fppn.fr/vocabulary/2017#UAV",
     *                  "label": "alias",
     *                  "brand": "brand",
     *                  "inServiceDate": null,
     *                  "dateOfPurchase": null
     *              },
     *          ]
     *      }
     * }
     */
    @GET
    @ApiOperation(value = "Get all vectors corresponding to the search params given",
                  notes = "Retrieve all vectors authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all vectors", response = Vector.class, responseContainer = "List"),
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
    public Response getVectorsBySearch(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_VECTOR_URI) @QueryParam("uri") String uri,
            @ApiParam(value = "Search by type uri", example = DocumentationAnnotation.EXAMPLE_VECTOR_RDF_TYPE) @QueryParam("rdfType") String rdfType,
            @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_VECTOR_LABEL) @QueryParam("label") String label,
            @ApiParam(value = "Search by brand", example = DocumentationAnnotation.EXAMPLE_VECTOR_BRAND) @QueryParam("brand") String brand,
            @ApiParam(value = "Search by service date", example = DocumentationAnnotation.EXAMPLE_VECTOR_IN_SERVICE_DATE) @QueryParam("inServiceDate") String inServiceDate,
            @ApiParam(value = "Search by date of purchase", example = DocumentationAnnotation.EXAMPLE_VECTOR_DATE_OF_PURCHASE) @QueryParam("dateOfPurchase") String dateOfPurchase) {
        
        VectorDAOSesame vectorDAO = new VectorDAOSesame();
        if (uri != null) {
            vectorDAO.uri = uri;
        }
        if (rdfType != null) {
            vectorDAO.rdfType = rdfType;
        }
        if (label != null) {
            vectorDAO.label = label;
        }
        if (brand != null) {
            vectorDAO.brand = brand;
        }
        if (inServiceDate != null) {
            vectorDAO.inServiceDate = inServiceDate;
        }
        if (dateOfPurchase != null) {
            vectorDAO.dateOfPurchase = dateOfPurchase;
        }
        
        vectorDAO.user = userSession.getUser();
        vectorDAO.setPage(page);
        vectorDAO.setPageSize(pageSize);
        
        return getVectorsData(vectorDAO);
    }
    
    /**
     * get the informations about a vector
     * @param uri
     * @param pageSize
     * @param page
     * @return the informations about the vector if it exists
     * e.g.
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                 "uri": "http://www.phenome-fppn.fr/diaphen/2018/v1825",
     *                 "rdfType": "http://www.phenome-fppn.fr/vocabulary/2017#UAV",
     *                 "label": "aria_hr1_p",
     *                 "brand": "unknown",
     *                 "inServiceDate": null,
     *                 "dateOfPurchase": null
     *              }
     *          ]
     *      }
     * }
     */
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get a vector",
                  notes = "Retrieve a vector. Need URL encoded vector URI")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a vector", response = Vector.class, responseContainer = "List"),
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
    public Response getVectorDetails(
        @ApiParam(value = DocumentationAnnotation.SENSOR_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_VECTOR_URI) @PathParam("uri") String uri,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {

        if (uri == null) {
            final Status status = new Status(StatusCodeMsg.ACCESS_ERROR, StatusCodeMsg.ERR, "Empty vector uri");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }

        VectorDAOSesame vectorDAO = new VectorDAOSesame();
        vectorDAO.uri = uri;
        vectorDAO.setPage(page);
        vectorDAO.setPageSize(pageSize);
        vectorDAO.user = userSession.getUser();

        return getVectorsData(vectorDAO);
    }
    
    /**
     * insert vectors in the database(s)
     * @param vectors list of the vectors to insert.
     *                e.g of vector data :
     * {
     *      "rdfType": "http://www.phenome-fppn.fr/vocabulary/2017#UAV",
     *      "label": "par03_p",
     *      "brand": "Skye Instruments",
     *      "serialNumber": "A1E345F32",
     *      "inServiceDate": "2017-06-15",
     *      "dateOfPurchase": "2017-06-15",
     *      "personInCharge": "morgane.vidal@inra.fr"
     * }
     * @param context
     * @return the post result with the errors or the uri of the inserted vectors
     */
    @POST
    @ApiOperation(value = "Post a vector",
                  notes = "Register a new vector in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Vector saved", response = ResponseFormPOST.class),
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
    public Response post(
        @ApiParam(value = DocumentationAnnotation.VECTOR_POST_DEFINITION) ArrayList<VectorDTO> vectors,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (vectors != null && !vectors.isEmpty()) {
            VectorDAOSesame vectorDAOSesame = new VectorDAOSesame();
            
            if (context.getRemoteAddr() != null) {
                vectorDAOSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            vectorDAOSesame.user = userSession.getUser();
            
            POSTResultsReturn result = vectorDAOSesame.checkAndInsert(vectors);
            
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
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty vectors(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
}
