//******************************************************************************
//                            ResourceService.java
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
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.DAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.injection.SessionInject;
import static opensilex.service.resource.DocumentResourceService.LOGGER;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * Resource service mother class.
 * @update [Andréas Garcia] 8 Apr. 2019: Refactor resource service classes generic functions (get a response from a GET
 * request, get responses from POST requests, etc.). Add unimplemented functions (as getDTOsFromObjects) to make them
 * implemented by the child classes to permit specific behaviours.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public abstract class ResourceService {
    
    // User session.
    @SessionInject
    protected Session userSession;
    
    // The default language of the application.
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
        throw new UnsupportedOperationException("Not supported yet: getDTOsFromObjects function.");
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
        throw new UnsupportedOperationException("Not supported yet: getObjectsFromDTOs function.");
    }
    
    /**
     * Gets a URI list from a list of objects having a URI.
     * @param objectsWithUris
     * @return the objects list.
     */
    protected List<String> getUrisFromObjects (List<? extends Object> objectsWithUris) {
        throw new UnsupportedOperationException("Not supported yet: getUrisCreatedFromObjects function.");
    }
    
    /**
     * Gets the response when no result found.
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
     * Gets the response when a SQL error message occured.
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
     * Gets a response when an internal error occured during the operation.
     * @param exception
     * @return the response. 
     */
    protected Response getResponseWhenInternalError(Exception exception) {
        return getPostPutResponseFromSingleOperationStatus(
                Response.Status.INTERNAL_SERVER_ERROR,
                StatusCodeMsg.INTERNAL_ERROR,
                StatusCodeMsg.ERR,
                exception.getMessage());
    }

    /**
     * Gets a response in the case of a persistence system error.
     * @param exception
     * @return the response. 
     */
    protected Response getResponseWhenPersistenceError(DAOPersistenceException exception) {
        return getPostPutResponseFromSingleOperationStatus(
                Response.Status.INTERNAL_SERVER_ERROR,
                StatusCodeMsg.PERSISTENCE_ERROR,
                StatusCodeMsg.ERR,
                exception.getMessage());
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
        ResultForm resultForm = new ResultForm<>(pageSize, page, getDTOsFromObjects(objects), true, totalCount);
        resultForm.setStatus(new ArrayList<Status>());
        return Response.status(Response.Status.OK).entity(resultForm).build();
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
     * Gets a response from a search by URI using a DAO.
     * @param dao
     * @param uri
     * @return 
     */
    protected Response getGETByUriResponseFromDAOResults(DAO dao, String uri) {
        ArrayList<Object> objects = new ArrayList();
        try {
            Object object = dao.findById(uri);
            objects.add(object);
            // Analyse results
            if (object == null) { // Request failure
                return getGETResponseWhenNoResult();
            } else if (objects.isEmpty()) {
                    return getGETResponseWhenNoResult();
            } else {
                return getGETResponseWhenSuccess(objects, 0, 0, 0);
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return getResponseWhenInternalError(ex);
        }
    }
    
    /**
     * Gets a response for a POST request depending on the results.
     * @param objectDao DAO of the manipulated object.
     * @param objectsDtos DTOs sent through the POST.
     * @param userIpAddress
     * @param statusMessageIfEmptyDtosSent
     * @return a success response when success.
     *         an internal error response when an non handled exception occured.
     *         an access denied response when the resource isn't available for the user.
     *         a data error response when the data sent is incorrect.
     */
    protected Response getPostResponse (DAO objectDao, ArrayList<? extends AbstractVerifiedClass> objectsDtos, String userIpAddress, String statusMessageIfEmptyDtosSent) {
        return getPostPutResponse(objectDao, objectsDtos, userIpAddress, statusMessageIfEmptyDtosSent, true);
    }
    
    /**
     * Gets a response for a PUT request depending on the results.
     * @param objectDao DAO of the manipulated object.
     * @param objectsDtos DTOs sent through the PUT.
     * @param userIpAddress
     * @param statusMessageIfEmptyDtosSent
     * @return a success response when success.
     *         an internal error response when an non handled exception occured.
     *         an access denied response when the resource isn't available for the user.
     *         a data error response when the data sent is incorrect.
     */
    protected Response getPutResponse (DAO objectDao, ArrayList<? extends AbstractVerifiedClass> objectsDtos, String userIpAddress, String statusMessageIfEmptyDtosSent) {
        return getPostPutResponse(objectDao, objectsDtos, userIpAddress, statusMessageIfEmptyDtosSent, false);
    }
    
    /**
     * Gets a response for a POST or PUT request depending on the results.
     * @param objectDao DAO of the manipulated object.
     * @param objectsDtos DTOs sent through the POST or PUT.
     * @param userIpAddress
     * @param statusMessageIfEmptyDtosSent
     * @param isPost is the operation a POST or a PUT.
     * @return a success response when success.
     *         an internal error response when an non handled exception occured.
     *         an access denied response when the resource isn't available for the user.
     *         a data error response when the data sent is incorrect.
     */
    protected Response getPostPutResponse (DAO objectDao, ArrayList<? extends AbstractVerifiedClass> objectsDtos, String userIpAddress, String statusMessageIfEmptyDtosSent, boolean isPost) {
        if (objectsDtos == null || objectsDtos.isEmpty()) {
            // Empty object list
            return getPostPutResponseWhenEmptyListGiven(statusMessageIfEmptyDtosSent);
        } 
        else {
            try {
                // Process operation
                objectDao.remoteUserAdress = userIpAddress;
                List<? extends Object> objectsToImpact = getObjectsFromDTOs(objectsDtos);
                List<? extends Object> impactedObjects;
                if (isPost) { // POST
                    impactedObjects = objectDao.validateAndCreate(objectsToImpact);
                }
                else { // PUT
                    impactedObjects = objectDao.validateAndUpdate(objectsToImpact);
                }
                
                // Return according to operation results
                List<String> impactedUris = getUrisFromObjects(impactedObjects);
                return getPostPutResponseWhenSuccess(impactedUris);
                
            } catch (ResourceAccessDeniedException ex) {
                LOGGER.error(ex.getMessage(), ex);
                return getPostPutResponseWhenResourceAccessDenied(ex);
                
            } catch (DAODataErrorAggregateException ex) {
                ex.getExceptions().forEach((exception) -> {
                        LOGGER.error(ex.getMessage(), exception);
                });
                return getPostPutResponseFromDAODataErrorExceptions(ex);
                
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                return getResponseWhenInternalError(ex);
            }  
        }
    }

    /**
     * Gets a response for a POST operation in success.
     * @param urisCreated
     * @return the response. 
     */
    private Response getPostPutResponseWhenSuccess(List<String> urisCreated) {
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
    private Response getPostPutResponseFromDAODataErrorExceptions(DAODataErrorAggregateException aggregateException) {
        List<Status> statusList = new ArrayList<>();
        aggregateException.getExceptions().forEach((ex) -> {
            statusList.add(new Status(
                    ex.getGenericMessage(), 
                    StatusCodeMsg.DATA_ERROR, 
                    ex.getMessage()));
        });
        return getPostPutResponseFromMultipleOperationStatus(Response.Status.BAD_REQUEST, statusList);
    }

    /**
     * Gets a response when an empty list is received by a POST.
     * @param statusMessageDetails
     * @return the response. 
     */
    private Response getPostPutResponseWhenEmptyListGiven(String statusMessageDetails) {
        return getPostPutResponseFromSingleOperationStatus(
                Response.Status.BAD_REQUEST,
                StatusCodeMsg.REQUEST_ERROR,
                StatusCodeMsg.ERR,
                statusMessageDetails);
    }

    /**
     * Gets a response when an empty list is received by a POST.
     * @param exception
     * @return the response. 
     */
    private Response getPostPutResponseWhenResourceAccessDenied(ResourceAccessDeniedException exception) {
        return getPostPutResponseFromSingleOperationStatus(
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
    private Response getPostPutResponseFromSingleOperationStatus (Response.Status httpStatus, String responseFormMessage, String responseFormCode, String responseFormDetails) {
        return buildResponse(httpStatus, new ResponseFormPOST(
                new Status(responseFormMessage, responseFormCode, responseFormDetails)));
    }
    
    /**
     * Gets a response with the HTTP status given and a list of operation status.
     * @param httpStatus
     * @param statusList
     * @return the response. 
     */
    private Response getPostPutResponseFromMultipleOperationStatus (Response.Status httpStatus, List<Status> statusList) {
        return buildResponse(httpStatus, new ResponseFormPOST(statusList));
    }
    
    /**
     * Builds a response from a status and a result form.
     * @param status
     * @param resultForm
     * @return the response. 
     */
    private Response buildResponse(Response.Status status, AbstractResultForm resultForm) {
        return Response.status(status).entity(resultForm).build();
    }
}
