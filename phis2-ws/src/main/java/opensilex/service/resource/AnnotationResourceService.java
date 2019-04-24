//******************************************************************************
//                       AnnotationResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 Jun. 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.AnnotationDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.resource.dto.annotation.AnnotationDTO;
import opensilex.service.resource.dto.annotation.AnnotationPostDTO;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.model.Annotation;
import opensilex.service.result.ResultForm;

/**
 * Annotation resource service.
 * @update [Andréas Garcia] 15 Feb. 2019: search parameters are no longer DAO 
 * class attributes but parameters sent through the search functions.
 * @update [Andreas Garcia] 19 Mar. 2019: make getAnnotations public to be 
 * able to use it from another service.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Api("/annotations")
@Path("/annotations")
public class AnnotationResourceService extends ResourceService {
    
    /**
     * Inserts the given annotations in the triplestore.
     * @example
     * [
     *   {
     *     "motivatedBy": "http://www.w3.org/ns/oa#describing",
     *     "creator": "http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy",
     *     "comments": [
     *       "commentary"
     *     ],
     *     "targets": [
     *       "http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy"
     *     ]
     *   }
     * ]
     * @param annotationsDtos annotations list to save.
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
        @ApiParam(value = DocumentationAnnotation.ANNOTATION_POST_DATA_DEFINITION) @Valid ArrayList<AnnotationPostDTO> annotationsDtos,
        @Context HttpServletRequest context) {

        AbstractResultForm postResponse = null;
        //If there are at least one list of annotations
        if (annotationsDtos != null && !annotationsDtos.isEmpty()) {
            AnnotationDAO annotationDao = new AnnotationDAO(userSession.getUser());
            if (context.getRemoteAddr() != null) {
                annotationDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            ArrayList<Annotation> annotations = new ArrayList<>();
            annotationsDtos.forEach((annotationDTO) -> {
                annotations.add(annotationDTO.createObjectFromDTO());
            });
            POSTResultsReturn insertResult = annotationDao.checkAndInsert(annotations);
            
            // annotations inserted
            if (insertResult.getHttpStatus().equals(Response.Status.CREATED)) {
                postResponse = new ResponseFormPOST(insertResult.statusList);
                postResponse.getMetadata().setDatafiles(insertResult.getCreatedResources());
            } else if (insertResult.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || insertResult.getHttpStatus().equals(Response.Status.OK)
                    || insertResult.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(insertResult.statusList);
            }
            return Response.status(insertResult.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty annotations to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Searches annotations by URI, creator, comment, date of creation, target.
     * @example { 
     * "metadata": { 
     *      "pagination": { 
     *          "pageSize": 20,
     *          "currentPage": 0,
     *          "totalCount": 297,
     *          "totalPages": 15 
     *      }, 
     *      "status": [],
     *      "datafiles": [] 
     *      },
     *      "result": { 
     *          "data": [ { 
     *              "uri": "http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2",
     *              "creator": "http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy",
     *              "creationDate": "2018-06-22 14:54:42+0200",
     *              "comments": ["Ustilago maydis infection"],
     *          "targets": [
     *              "http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy" 
     *            ] 
     *      },{...} )}}
     * @param pageSize
     * @param page
     * @param uri
     * @param creator
     * @param bodyValue
     * @param target
     * @param motivatedBy
     * @return the annotations corresponding to the search parameters given
     */
    @GET
    @ApiOperation(value = "Get all annotations corresponding to the search params given",
            notes = "Retrieve all annotations authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all annotations", response = AnnotationDTO.class, responseContainer = "List"),
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
    public Response getAnnotationsBySearch(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by annotation uri", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_URI) @QueryParam("uri") @URL String uri,
            @ApiParam(value = "Search by creator", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_CREATOR) @QueryParam("creator") @URL String creator,
            @ApiParam(value = "Search by target", example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI) @QueryParam("target") @URL String target,
            @ApiParam(value = "Search by comment", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_BODY_VALUE) @QueryParam("description") String bodyValue,
            @ApiParam(value = "Search by motivation", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_MOTIVATED_BY) @QueryParam("motivatedBy") @URL String motivatedBy) {

        return getAnnotations(uri, creator, target, bodyValue, motivatedBy, page, pageSize);
    }

    /**
     * Gets the information about an annotation.
     * @example
     * {
     * "metadata": { "pagination": null, "status": [], "datafiles": [] },
     * "result": { 
     * "data": [ { 
     *   "uri": "http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2",
     *   "creator": "http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy",
     *   "creationDate": "2018-06-22 14:54:42+0200",
     *   "comments": [
     *          "Ustilago maydis infection"
     *    ], 
     *    "targets": [ 
     *      "http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy" 
     *    ] 
     * } ] } }
     * @param uri
     * @param pageSize
     * @param page
     * @return the information about the annotation if it exists
     */
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get a annotation",
            notes = "Retrieve a annotation. Need URL encoded annotation URI")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a annotation", response = AnnotationDTO.class, responseContainer = "List"),
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
    public Response getAnnotationByUri(
            @ApiParam(value = DocumentationAnnotation.SENSOR_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @URL @PathParam("uri") String uri,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        if (uri == null) {
            final Status status = new Status(StatusCodeMsg.ACCESS_ERROR, StatusCodeMsg.ERR, "Empty annotation uri");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }

        return getAnnotations(uri, null, null, null, null, page, pageSize);
    }

    /**
     * Searches annotations corresponding to search parameters.
     * @param uri
     * @param creator
     * @param target
     * @param bodyValue
     * @param motivatedBy
     * @param page
     * @param pageSize
     * @return the annotations corresponding to the search
     */
    public Response getAnnotations(String uri, String creator, String target, String bodyValue, String motivatedBy, int page, int pageSize) {
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<AnnotationDTO> getResponse;
        AnnotationDAO annotationDao = new AnnotationDAO(userSession.getUser());

        // Count all annotations for this specific request
        Integer totalCount = annotationDao.count(uri, creator, target, bodyValue, motivatedBy);
        
        // Retreive all annotations returned by the query
        ArrayList<Annotation> annotations = annotationDao.searchAnnotations(
                uri, 
                creator, 
                target, 
                bodyValue, 
                motivatedBy, 
                page, 
                pageSize);
        
        ArrayList<AnnotationDTO> annotationDTOs = new ArrayList();

        if (annotations == null) {
            getResponse = new ResultForm<>(0, 0, annotationDTOs, true);
            return noResultFound(getResponse, statusList);
        } else if (annotations.isEmpty()) {
            getResponse = new ResultForm<>(0, 0, annotationDTOs, true);
            return noResultFound(getResponse, statusList);
        } else {
            // Generate DTOs
            annotations.forEach((annotation) -> {
                annotationDTOs.add(new AnnotationDTO(annotation));
            });
            getResponse = new ResultForm<>(pageSize, page, annotationDTOs, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}
