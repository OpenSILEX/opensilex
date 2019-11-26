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
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.resource.dto.DeleteDTO;
import opensilex.service.resource.dto.annotation.AnnotationDTO;
import opensilex.service.resource.dto.annotation.AnnotationPostDTO;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Annotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Annotation resource service.
 * @update [Andréas Garcia] 15 Feb. 2019: search parameters are no longer DAO 
 * class attributes but parameters sent through the search functions.
 * @update [Andreas Garcia] 19 Mar. 2019: make getAnnotations public to be 
 * able to use it from another service.
 * @update [Andréas Garcia] 8 Apr. 2019: Refactor generic functions into the ResourceService class
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Api("/annotations")
@Path("/annotations")
public class AnnotationResourceService extends ResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(SensorResourceService.class);
    
    public final static String EMPTY_ANNOTATION_LIST = "the annotation list to add is empty";
    
    /**
     * Inserts the given annotations in the triplestore.
     * @param annotationsDtos annotationsDtos
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
     * @param context
     * @return
     */
    @POST
    @ApiOperation(value = "Post annotations", notes = "Register new annotations in the triplestore")
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
    public Response post(
        @ApiParam(value = DocumentationAnnotation.ANNOTATION_POST_DATA_DEFINITION) 
            @Valid ArrayList<AnnotationPostDTO> annotationsDtos,
        @Context HttpServletRequest context) {
        
        // Set DAO
        AnnotationDAO objectDao = new AnnotationDAO(userSession.getUser());
        if (context.getRemoteAddr() != null) {
            objectDao.remoteUserAdress = context.getRemoteAddr();
        }
        
        return getPostResponse(objectDao, annotationsDtos, context.getRemoteAddr(), StatusCodeMsg.EMPTY_ANNOTATION_LIST);
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
            @ApiParam(value = "Search by motivation", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_MOTIVATED_BY) @QueryParam("motivatedBy") @URL String motivatedBy,
            @ApiParam(value = "Date search result order ('true' for ascending and 'false' for descending)",example = "true") @QueryParam("dateSortAsc") boolean dateSortAsc) {

        AnnotationDAO annotationDao = new AnnotationDAO(userSession.getUser());
        ArrayList<Annotation> annotations;
        try {
            annotations = annotationDao.find(uri, creator, target, bodyValue, motivatedBy, dateSortAsc, page, pageSize);
        
        // handle search exceptions
        } catch (DAOPersistenceException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return getResponseWhenPersistenceError(ex);
        }

        // Returns result
        if (annotations == null) {
            return getGETResponseWhenNoResult();
        } else if (annotations.isEmpty()) {
            return getGETResponseWhenNoResult();
        } else {
            // count
            try {
                int totalCount = annotationDao.count(uri, creator, target, bodyValue, motivatedBy, dateSortAsc);
                return getGETResponseWhenSuccess(annotations, pageSize, page, totalCount);
                
            // handle count exceptions
            } catch (DAOPersistenceException ex) {
                LOGGER.error(ex.getMessage(), ex);
                return getResponseWhenPersistenceError(ex);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                return getResponseWhenInternalError(ex);
            }
        }
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
     * @return the information about the annotation if it exists
     */
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get a annotation",
            notes = "Retrieve a annotation. Need URL encoded annotation URI")
    @ApiResponses(value = {
        @ApiResponse(
                code = 200, 
                message = "Retrieve a annotation", 
                response = AnnotationDTO.class, 
                responseContainer = "List"),
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
            @ApiParam(
                    value = DocumentationAnnotation.ANNOTATION_URI_DEFINITION, 
                    required = true, 
                    example = DocumentationAnnotation.EXAMPLE_ANNOTATION_URI) 
                @URL @PathParam("uri") String uri) {
        
        return getGETByUriResponseFromDAOResults(new AnnotationDAO(userSession.getUser()), uri);
    }
    
    
    /**
     * @example
     *[
     *	http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2,
     *  http://www.phenome-fppn.fr/platform/id/annotation/8247gt37-769c-495b-8e7e-91jh633151k4
     *]
     * 
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation(
    	value = "Delete a list of annotation",
    	notes = "Delete a list of annotation. Need URL encoded annotation URI"
    	
    		)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Annotation(s) deleted", response = ResponseFormPOST.class), 
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.INTERNAL_SERVER_ERROR),
    })
    @ApiImplicitParams({
        @ApiImplicitParam(
    		name = GlobalWebserviceValues.AUTHORIZATION, 
    		required = true,
            dataType = GlobalWebserviceValues.DATA_TYPE_STRING, 
            paramType = GlobalWebserviceValues.HEADER,
            value = DocumentationAnnotation.ACCES_TOKEN,
            example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAnnotationByUri( 
    		@ApiParam(
		        value = DocumentationAnnotation.ANNOTATION_URI_DEFINITION,
		        required = true, 
		        example = DocumentationAnnotation.EXAMPLE_ANNOTATION_URI
		    ) 
        	@Valid @NotNull DeleteDTO deleteDTO, @Context HttpServletRequest context) {
    	
    	AnnotationDAO annotationDAO = new AnnotationDAO(userSession.getUser());
		if (context.getRemoteAddr() != null) {
			 annotationDAO.setRemoteUserAdress(context.getRemoteAddr());
	    }
		Response response = buildDeleteObjectsByUriResponse(annotationDAO, deleteDTO,"Annotation(s) deleted");
    	annotationDAO.getConnection().close();
    	return response;
    }

    @Override
    protected ArrayList<AbstractVerifiedClass> getDTOsFromObjects(List<? extends Object> objects) {
        ArrayList<AbstractVerifiedClass> dtos = new ArrayList<>(objects.size());
        // Generate DTOs
        objects.forEach((object) -> {
            dtos.add(new AnnotationDTO((Annotation)object));
        });
        return dtos;
    }
    
    @Override
    protected List<? extends Object> getObjectsFromDTOs (List<? extends AbstractVerifiedClass> dtos)
            throws Exception {
        List<Object> objects = new ArrayList<>(dtos.size());
        for (AbstractVerifiedClass objectDto : dtos) {
            objects.add((Annotation)objectDto.createObjectFromDTO());
        }
        return objects;
    }
    
    @Override
    protected List<String> getUrisFromObjects (List<? extends Object> createdObjects) {
        List<String> createdUris = new ArrayList<>(createdObjects.size());
        createdObjects.forEach(object -> {
            createdUris.add(((Annotation)object).getUri());
        });
        return createdUris;
    }
}
