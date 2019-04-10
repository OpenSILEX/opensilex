//******************************************************************************
//                                       ResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 3 Dec. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import opensilex.service.PropertiesFileManager;
import opensilex.service.authentication.Session;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.DAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.injection.SessionInject;
import opensilex.service.resource.dto.annotation.AnnotationDTO;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.dto.manager.VerifiedClassInterface;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * Resource service mother class.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public abstract class ResourceService {
    
    // The default language of the application
    protected static final String DEFAULT_LANGUAGE = PropertiesFileManager.getConfigFileProperty("service", "defaultLanguage");
    
    /**
     * Gets a DTO list from a list of objects.
     * //SILEX:todo
     * This function should be abstract but we would need to implement it on all the resource service classes.
     * We focus on event and annotation for the moment.
     * \SILEX
     * @param objects
     * @return 
     */
    protected ArrayList<AbstractVerifiedClass> getDTOsFromObjects (List<? extends Object> objects) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * Gets a list of objects from a list of DTOs.
     * //SILEX:todo 
     * This function should be abstract but we would need to implement it on all the resource service classes.
     * We focus on event and annotation for the moment.
     * \SILEX
     * @param dtos
     * @throws java.lang.Exception 
     * @return the objects list.
     */
    protected List<? extends Object> getObjectsFromDTOs (List<? extends AbstractVerifiedClass> dtos) 
            throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    // User session
    @SessionInject
    protected Session userSession;
    
    /**
     * Generic response for no result found.
     * @param getResponse
     * @param insertStatusList
     * @return the Response with the error message
     */
    protected Response noResultFound(ResultForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS, StatusCodeMsg.INFO, "No result found."));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }

    /**
     * Generic method for SQL error message.
     * @param getResponse
     * @param insertStatusList
     * @return the Response with the error message
     */
    protected Response sqlError(ResultForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("SQL error", StatusCodeMsg.ERR, "can't fetch result"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
    }

    /**
     * Gets a response for a GET operation in success.
     * @param objects
     * @param pageSize
     * @param page
     * @param totalCount
     * @return the response.
     */
    protected Response getGETResponseWhenSuccess(ArrayList<? extends Object> objects, int pageSize, int page, int totalCount) {
        return Response
                .status(Response.Status.OK)
                .entity(new ResultForm<>(pageSize, page, getDTOsFromObjects(objects), true, totalCount))
                .build();
    }

    /**
     * Gets a response for a GET operation returns no result.
     * @return the response.
     */
    protected Response getGETResponseWhenNoResult() {
        ResultForm<? extends AbstractVerifiedClass> getResponse = new ResultForm<>(0, 0, new ArrayList(), true);
        return noResultFound(getResponse, new ArrayList<>());
    }

    /**
     * Gets a response for a POST operation in success.
     * @param urisCreated
     * @return the response. 
     */
    protected Response getPostResponseWhenSuccess(ArrayList<String> urisCreated) {
        ResponseFormPOST postResponse = new ResponseFormPOST(new Status(
                StatusCodeMsg.RESOURCES_CREATED, 
                StatusCodeMsg.INFO, 
                String.format(POSTResultsReturn.NEW_RESOURCES_CREATED_MESSAGE, urisCreated.size())));
        postResponse.getMetadata().setDatafiles(urisCreated);
        return buildResponse(Response.Status.CREATED, postResponse);
    }

    /**
     * Gets a response from data exceptions thrown by a DAO.
     * @param aggregateException
     * @return the response. 
     */
    protected Response getPostResponseFromDAODataErrorExceptions(DAODataErrorAggregateException aggregateException) {
        List<Status> statusList = new ArrayList<>();
        aggregateException.getExceptions().forEach((ex) -> {
            statusList.add(new Status(
                    ex.getGenericMessage(), 
                    StatusCodeMsg.DATA_ERROR, 
                    ex.getMessage()));
        });
        return getPostResponseFromMultipleOperationStatus(Response.Status.BAD_REQUEST, statusList);
    }

    /**
     * Gets a response when an empty list is received by a POST.
     * @param statusMessageDetails
     * @return the response. 
     */
    protected Response getPostResponseWhenEmptyListGiven(String statusMessageDetails) {
        return getPostResponseFromSingleOperationStatus(
                Response.Status.BAD_REQUEST,
                StatusCodeMsg.REQUEST_ERROR,
                StatusCodeMsg.ERR,
                statusMessageDetails);
    }

    /**
     * Gets a response when an internal error occured during the operation.
     * @param exception
     * @return the response. 
     */
    protected Response getResponseWhenInternalError(Exception exception) {
        return getPostResponseFromSingleOperationStatus(
                Response.Status.INTERNAL_SERVER_ERROR,
                StatusCodeMsg.INTERNAL_ERROR,
                StatusCodeMsg.ERR,
                exception.getMessage());
    }

    /**
     * Gets a response when an empty list is received by a POST.
     * @param exception
     * @return the response. 
     */
    protected Response getPostResponseWhenResourceAccessDenied(ResourceAccessDeniedException exception) {
        return getPostResponseFromSingleOperationStatus(
                Response.Status.BAD_REQUEST,
                ResourceAccessDeniedException.GENERIC_MESSAGE,
                StatusCodeMsg.ERR,
                exception.getMessage());
    }
    
    /**
     * Gets a response with the HTTP status given and a single operation status.
     * @param httpStatus
     * @param responseFormMessage
     * @param responseFormCode
     * @param responseFormDetails
     * @return the response. 
     */
    private Response getPostResponseFromSingleOperationStatus (Response.Status httpStatus, String responseFormMessage, String responseFormCode, String responseFormDetails) {
        return buildResponse(httpStatus, new ResponseFormPOST(
                new Status(responseFormMessage, responseFormCode, responseFormDetails)));
    }
    
    /**
     * Gets a response with the HTTP status given and a list of operation status.
     * @param httpStatus
     * @param statusList
     * @return the response. 
     */
    private Response getPostResponseFromMultipleOperationStatus (Response.Status httpStatus, List<Status> statusList) {
        return buildResponse(httpStatus, new ResponseFormPOST(statusList));
    }
    
    /**
     * Builds a response from a status and a form result form.
     * @param status
     * @param resultForm
     * @return the response. 
     */
    private Response buildResponse(Response.Status status, AbstractResultForm resultForm) {
        return Response.status(status).entity(resultForm).build();
    }
    
    /**
     * Gets a response according to the object returned by a DAO search by URI.
     * @param dao
     * @param uri
     * @return 
     */
    protected Response getGETByUriResponseFromDAOResults(DAO dao, String uri) {
        ArrayList<Object> objects = new ArrayList();
        try {
            Object object = dao.findById(uri);
            objects.add(dao.findById(uri));
            // Analyse results
            if (object == null) { // Request failure
                return getGETResponseWhenNoResult();
            } else if (objects.isEmpty()) {
                    return getGETResponseWhenNoResult();
            } else {
                return getGETResponseWhenSuccess(objects, 0, 0, 0);
            }
        } catch (Exception ex) {
            return getResponseWhenInternalError(ex);
        }
    }
}
