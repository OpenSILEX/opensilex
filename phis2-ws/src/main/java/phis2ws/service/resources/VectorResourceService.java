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
import javax.validation.Valid;
import javax.validation.constraints.Email;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.VectorDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.VectorDTO;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
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
public class VectorResourceService extends ResourceService {
    /**
     * Search vectors corresponding to search params given by a user
     * @param vectorDAOSesame
     * @return the vectors corresponding to the search
     */
    private Response getVectorsData(VectorDAOSesame vectorDAOSesame) {
        ArrayList<Vector> vectors;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormVector getResponse;
        
        //1. Get number of vectors corresponding to the search params
        Integer totalCount = vectorDAOSesame.count();
        //2. Get vectors to return
        vectors = vectorDAOSesame.allPaginate();
        
        //3. Return the result
        if (vectors == null) { //Request error
            getResponse = new ResponseFormVector(0, 0, vectors, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (vectors.isEmpty()) { //No result
            getResponse = new ResponseFormVector(0, 0, vectors, true, 0);
            return noResultFound(getResponse, statusList);
        } else { //Results founded. Return the results
            getResponse = new ResponseFormVector(vectorDAOSesame.getPageSize(), vectorDAOSesame.getPage(), vectors, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
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
     * @param serialNumber
     * @param inServiceDate
     * @param dateOfPurchase
     * @param personInCharge
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
     *                  "rdfType": "http://www.opensilex.org/vocabulary/oeso#UAV",
     *                  "label": "alias",
     *                  "brand": "brand",
     *                  "serialNumber" : "serialNumber",
     *                  "inServiceDate": null,
     *                  "dateOfPurchase": null,
     *                  "personInCharge": "user@mail.fr"
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
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_VECTOR_URI) @QueryParam("uri") @URL String uri,
            @ApiParam(value = "Search by rdf type", example = DocumentationAnnotation.EXAMPLE_VECTOR_RDF_TYPE) @QueryParam("rdfType") @URL String rdfType,
            @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_VECTOR_LABEL) @QueryParam("label") String label,
            @ApiParam(value = "Search by brand", example = DocumentationAnnotation.EXAMPLE_VECTOR_BRAND) @QueryParam("brand") String brand,
            @ApiParam(value = "Search by serial number", example = DocumentationAnnotation.EXAMPLE_VECTOR_SERIAL_NUMBER) @QueryParam("serialNumber") String serialNumber,
            @ApiParam(value = "Search by service date", example = DocumentationAnnotation.EXAMPLE_VECTOR_IN_SERVICE_DATE) @QueryParam("inServiceDate") @Date(DateFormat.YMD) String inServiceDate,
            @ApiParam(value = "Search by date of purchase", example = DocumentationAnnotation.EXAMPLE_VECTOR_DATE_OF_PURCHASE) @QueryParam("dateOfPurchase") @Date(DateFormat.YMD) String dateOfPurchase,
            @ApiParam(value = "Search by person in charge", example = DocumentationAnnotation.EXAMPLE_VECTOR_PERSON_IN_CHARGE) @QueryParam("personInCharge") @Email String personInCharge) {
        
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
        if (serialNumber != null) {
            vectorDAO.serialNumber = serialNumber;
        }
        if (inServiceDate != null) {
            vectorDAO.inServiceDate = inServiceDate;
        }
        if (dateOfPurchase != null) {
            vectorDAO.dateOfPurchase = dateOfPurchase;
        }
        if (personInCharge != null) {
            vectorDAO.personInCharge = personInCharge;
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
     *                 "rdfType": "http://www.opensilex.org/vocabulary/oeso#UAV",
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
        @ApiParam(value = DocumentationAnnotation.SENSOR_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_VECTOR_URI) @PathParam("uri") @URL @Required String uri,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

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
     *      "rdfType": "http://www.opensilex.org/vocabulary/oeso#UAV",
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
        @ApiParam(value = DocumentationAnnotation.VECTOR_POST_DEFINITION) @Valid ArrayList<VectorDTO> vectors,
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
    
    /**
     * update the given vectors
     * e.g. 
     * [
     *      {
     *          "uri": "http://www.phenome-fppn.fr/diaphen/2018/v18142",
     *          "rdfType": "http://www.opensilex.org/vocabulary/oeso#UAV",
     *          "label": "testNewLabel",
     *          "brand": "Skye Instrdfgduments",
     *          "serialNumber": "A1E34qsf5F32",
     *          "inServiceDate": "2017-06-15",
     *          "dateOfPurchase": "2017-06-15",
     *          "personInCharge": "morgane.vidal@inra.fr"
     *      }
     * ]
     * @param vectors
     * @param context
     * @return the post result with the founded errors or the uris of the updated vectors
     */
    @PUT
    @ApiOperation(value = "Update vector")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Vector updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Vector not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    public Response put(
        @ApiParam(value = DocumentationAnnotation.VECTOR_POST_DEFINITION) @Valid ArrayList<VectorDTO> vectors,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (vectors != null && !vectors.isEmpty()) {
            VectorDAOSesame vectorDAOSesame = new VectorDAOSesame();
            if (context.getRemoteAddr() != null) {
                vectorDAOSesame.remoteUserAdress = context.getRemoteAddr();
            }
            
            vectorDAOSesame.user = userSession.getUser();
            
            POSTResultsReturn result = vectorDAOSesame.checkAndUpdate(vectors);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) {
                //Code 200, traits modifiés
                postResponse = new ResponseFormPOST(result.statusList);
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty vector(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
}
