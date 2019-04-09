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
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.injection.SessionInject;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * Resource service mother class.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResourceService {
    
    // The default language of the application
    protected static final String DEFAULT_LANGUAGE = PropertiesFileManager.getConfigFileProperty("service", "defaultLanguage");
    
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
     * Gets a response for a POST operation in success.
     * @param urisCreated
     * @return 
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
     * @return 
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
     * @return 
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
     * @return 
     */
    protected Response getPostResponseWhenInternalError(Exception exception) {
        return getPostResponseFromSingleOperationStatus(
                Response.Status.INTERNAL_SERVER_ERROR,
                StatusCodeMsg.INTERNAL_ERROR,
                StatusCodeMsg.ERR,
                exception.getMessage());
    }

    /**
     * Gets a response when an empty list is received by a POST.
     * @param exception
     * @return 
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
     * @return 
     */
    private Response getPostResponseFromSingleOperationStatus (Response.Status httpStatus, String responseFormMessage, String responseFormCode, String responseFormDetails) {
        return buildResponse(httpStatus, new ResponseFormPOST(
                new Status(responseFormMessage, responseFormCode, responseFormDetails)));
    }
    
    /**
     * Gets a response with the HTTP status given and a list of operation status.
     * @param httpStatus
     * @param statusList
     * @return 
     */
    private Response getPostResponseFromMultipleOperationStatus (Response.Status httpStatus, List<Status> statusList) {
        return buildResponse(httpStatus, new ResponseFormPOST(statusList));
    }
    
    /**
     * Builds a response from a status and a form result form.
     * @param status
     * @param resultForm
     * @return 
     */
    private Response buildResponse(Response.Status status, AbstractResultForm resultForm) {
        return Response.status(status).entity(resultForm).build();
    }
}
