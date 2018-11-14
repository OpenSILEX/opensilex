//******************************************************************************
//                         EventResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import java.util.List;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.EventDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.event.EventDTO;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormEvent;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Event;

/**
 * Event resource service.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
@Api("/events")
@Path("/events")
public class EventResourceService {
    final static Logger LOGGER = 
            LoggerFactory.getLogger(EventResourceService.class);
    
    //user session
    @SessionInject
    Session userSession;
    
    /**
     * Generates an Event list from a given list of EventDTO
     * @param event
     * @return the list of events
     */
    private List<Event> eventDTOsToEvents(List<EventDTO> eventDTOs) {
        ArrayList<Event> events = new ArrayList<>();
        
        eventDTOs.forEach(eventDTO -> {
            events.add(eventDTO.createObjectFromDTO());
        });
        
        return events;
    }
    
    /**
     * Search all events
     * 
     * @param pageSize
     * @param page
     * @return  list of all events
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
     *                  "uri": 
     * "http://www.phenome-fppn.fr/vocabulary/2018/oeev/id/events/ev001",
     *                  "label": "event name",
     *                  "dateTime" : "2018-11-14T07:58:26.891Z"
     *              },
     *          ]
     *      }
     * }
     */
    @GET
    @ApiOperation(value = 
            "Get all events targets corresponding to the search parameters "
                    + "given",
                  notes = 
            "Retrieve all events authorized for the user corresponding to the "
                    + "search parameters given")
    @ApiResponses(value = {
        @ApiResponse(code = 200
                , message = "Retrieve all events"
                , response = Event.class
                , responseContainer = "List"),
        @ApiResponse(code = 400
                , message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401
                , message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500
                , message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(
                name = GlobalWebserviceValues.AUTHORIZATION
                , required = true
                , dataType = GlobalWebserviceValues.DATA_TYPE_STRING
                , paramType = GlobalWebserviceValues.HEADER
                , value = DocumentationAnnotation.ACCES_TOKEN
                , example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEventsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) 
        @QueryParam(GlobalWebserviceValues.PAGE_SIZE) 
        @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) 
        @Min(0) int pageSize
        , @ApiParam(value = DocumentationAnnotation.PAGE) 
        @QueryParam(GlobalWebserviceValues.PAGE) 
        @DefaultValue(DefaultBrapiPaginationValues.PAGE) 
        @Min(0) int page
        , @ApiParam(value = "Search by uri"
        , example = DocumentationAnnotation.EXAMPLE_EVENT_URI) 
        @QueryParam("uri") 
        @URL String uri
        , @ApiParam(
                value = "Search by label", 
                example = DocumentationAnnotation.EXAMPLE_EVENT_LABEL) 
        @QueryParam("label") String label
    ) {

        EventDAOSesame eventDAO = new EventDAOSesame();
        
        eventDAO.uri = uri;
        eventDAO.label = label;
        eventDAO.user = userSession.getUser();
        eventDAO.setPage(page);
        eventDAO.setPageSize(pageSize);

        Integer totalCount = eventDAO.count();
        
        ArrayList<Event> events = eventDAO.allPaginate();
        
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormEvent getResponse;
        
        if (events == null) { // Request failure
            getResponse = new ResponseFormEvent(0, 0, events, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (events.isEmpty()) { // No results
            getResponse = new ResponseFormEvent(0, 0, events, true, 0);
            return noResultFound(getResponse, statusList);
        } else { // Results
            getResponse = new ResponseFormEvent(eventDAO.getPageSize()
                    , eventDAO.getPage(), events, true, totalCount);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }
        
    /**
     * Return a generic response when no result are found
     * @param getResponse
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(
            ResultForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS
                , StatusCodeMsg.INFO
                , "No event found"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND)
                .entity(getResponse).build();
    }
}
