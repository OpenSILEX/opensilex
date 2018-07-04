//******************************************************************************
//                                       AnnotationResourceService.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 21 June 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  23 June 2018
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
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
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
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.AnnotationDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.resources.dto.AnnotationDTO;
import phis2ws.service.view.brapi.form.ResponseFormAnnotation;
import phis2ws.service.view.model.phis.Annotation;

/**
 * Represents the annotation service.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Api("/annotations")
@Path("/annotations")
public class AnnotationResourceService {

    final static Logger LOGGER = LoggerFactory.getLogger(AnnotationResourceService.class);

    //User session
    @SessionInject
    Session userSession;

    /**
     * insert given annotations in the triplestore
     *
     * @param annotations annotations list to save.
     * @param context
     * @return
     */
    @POST
    @ApiOperation(value = "Post annotations",
            notes = "Register new annotations in the triplestore")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Annotations saved", response = ResponseFormPOST.class)
        ,
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION)
        ,
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED)
        ,
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
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST()).build();
        }
    }

    /**
     * search annotation by uri, creator, comment, date of creation, targets
     *
     *
     * @param pageSize
     * @param page
     * @param uri
     * @param creator
     * @param comment
     * @param target
     * @param motivatedBy 
     * @return list of the annotation corresponding to the search params given
     * e.g { "metadata": { "pagination": { "pageSize": 20, "currentPage": 0,
     * "totalCount": 297, "totalPages": 15 }, "status": [], "datafiles": [] },
     * "result": { "data": [ { "uri":
     * "http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2",
     * "creator": "http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy",
     * "creationDate": "2018-06-22T14:54:42+0200", "comment": "Ustilago maydis
     * infection", "targets": [
     * "http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy" ] },{...} )}}
     */
    @GET
    @ApiOperation(value = "Get all annotations corresponding to the search params given",
            notes = "Retrieve all annotations authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all annotations", response = Annotation.class, responseContainer = "List")
        ,
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION)
        ,
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED)
        ,
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
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_URI) @QueryParam("uri") String uri,
            //SILEX:conception
            // Need to specify if it necessary 
            // @ApiParam(value = "Search by creation date", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_CREATED) @hasValidDateFormat  @QueryParam("created") String created,
            //\SILEX:conception
            @ApiParam(value = "Search by creator", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_CREATOR)  @QueryParam("creator") String creator,
            @ApiParam(value = "Search by motivation", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_MOTIVATEDBY) @QueryParam("motivatedBy") String motivatedBy,
            @ApiParam(value = "Search by comment", example = DocumentationAnnotation.EXAMPLE_ANNOTATION_COMMENT) @QueryParam("comment") String comment,
            @ApiParam(value = "Search by target", example = DocumentationAnnotation.EXAMPLE_AGRONOMICAL_OBJECT_URI) @QueryParam("target") String target) {

        AnnotationDAOSesame annotationDAO = new AnnotationDAOSesame();
        if (uri != null) {
            annotationDAO.uri = uri;
        }
        //SILEX:conception
        // Need to specify if it necessary
        //\SILEX:conception
//        if (date != null) {
//            annotationDAO.created = date;
//        }
        if (creator != null) {
            annotationDAO.creator = creator;
        }
        if (target != null) {
            annotationDAO.targets.add(target);
        }
        if (comment != null) {
            annotationDAO.bodyValue = comment;
        }

        if (motivatedBy != null) {
            annotationDAO.motivatedBy = motivatedBy;
        }

        annotationDAO.user = userSession.getUser();
        annotationDAO.setPage(page);
        annotationDAO.setPageSize(pageSize);

        return getAnnotationData(annotationDAO);
    }

    /**
     * Search annotations corresponding to search params given by a user
     *
     * @param annotationDAOSesame
     * @return the annotations corresponding to the search
     */
    private Response getAnnotationData(AnnotationDAOSesame annotationDAOSesame) {
        ArrayList<Annotation> annotations;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormAnnotation getResponse;

        // Count all annotations for this specific request
        Integer totalCount = annotationDAOSesame.count();
        // Retreive all annotations returned by the query
        annotations = annotationDAOSesame.allPaginate();

        if (annotations == null) {
            getResponse = new ResponseFormAnnotation(0, 0, annotations, true);
            return noResultFound(getResponse, statusList);
        } else if (annotations.isEmpty()) {
            getResponse = new ResponseFormAnnotation(0, 0, annotations, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormAnnotation(annotationDAOSesame.getPageSize(), annotationDAOSesame.getPage(), annotations, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }

    private Response noResultFound(ResponseFormAnnotation getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS, StatusCodeMsg.INFO, "No results for the annotations"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
}
