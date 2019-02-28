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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.EventDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.event.EventDetailedDTO;
import phis2ws.service.resources.dto.event.EventSimpleDTO;
import phis2ws.service.resources.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormEventDetailed;
import phis2ws.service.view.brapi.form.ResponseFormEventSimple;
import phis2ws.service.view.model.phis.Event;

/**
 * Service to handle events
 * @update [Andréas Garcia] 14 Feb. 2019: Add event detail service
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
@Api("/events")
@Path("/events")
public class EventResourceService  extends ResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(EventResourceService.class);
    
    /**
     * Generates an Event list from a given list of EventDTO
     * @param event
     * @return the list of events
     */
    private List<Event> eventDTOsToEvents(List<EventSimpleDTO> eventDTOs) {
        ArrayList<Event> events = new ArrayList<>();
        
        eventDTOs.forEach(eventDTO -> {
            events.add(eventDTO.createObjectFromDTO());
        });
        
        return events;
    }
    
    /**
     * Search all events
     * @example
     * {
     *  {
     *     "metadata": {
     *       "pagination": null,
     *       "status": [],
     *       "datafiles": []
     *     },
     *     "result": {
     *       "data": [
     *         {
     *           "uri": "http://www.phenome-fppn.fr/id/event/5a1b3c0d-58af-4cfb-811e-e141b11453b1",
     *           "type": "http://www.phenome-fppn.fr/vocabulary/2018/oeev#MoveFrom",
     *           "concernedItems": [
     *             {
     *               "labels": [
     *                 "label2",
     *                 "label3"
     *               ],
     *               "uri": "http://www.phenome-fppn.fr/m3p/arch/2017/c17000241",
     *               "typeUri": "http://www.opensilex.org/vocabulary/oeso#Thermocouple"
     *             }
     *           ],
     *           "date": "2017-09-11T12:00:00+01:00",
     *           "properties": [
     *             {
     *               "rdfType": "http://www.opensilex.org/vocabulary/oeso#Thermocouple",
     *               "relation": "http://www.phenome-fppn.fr/vocabulary/2018/oeev#from",
     *               "value": "http://www.phenome-fppn.fr/m3p/phenoarch/"
     *             }
     *           ]
     *         }
     *       ]
     *     }
     *   }
     * }
     * @param pageSize
     * @param page
     * @param uri
     * @param type
     * @param concernedItemUri
     * @param concernedItemLabel
     * @param startDate
     * @param endDate
     * @return  list of all events
     */
    @GET
    @ApiOperation(value = "Get all events corresponding to the search parameters given.", notes = "Retrieve all events authorized for the user corresponding to the " + "search parameters given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all events", response = Event.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({@ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true, dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER, value = DocumentationAnnotation.ACCES_TOKEN, example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEventsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize, 
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page, 
        @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_EVENT_URI) @QueryParam("uri") @URL String uri, 
        @ApiParam(value = "Search by type", example = DocumentationAnnotation.EXAMPLE_EVENT_TYPE) @QueryParam("type") @URL String type, 
        @ApiParam(value = "Search by concerned item uri", example = DocumentationAnnotation.EXAMPLE_EVENT_CONCERNED_ITEM_URI) @QueryParam("concernedItemUri") @URL String concernedItemUri, 
        @ApiParam(value = "Search by concerned item label", example = DocumentationAnnotation.EXAMPLE_EVENT_CONCERNED_ITEM_LABEL) @QueryParam("concernedItemLabel") String concernedItemLabel, 
        @ApiParam(value = "Search by date - start of the range", example = DocumentationAnnotation.EXAMPLE_EVENT_START_DATE) @QueryParam("startDate") @Date(DateFormat.YMDTHMSZZ) String startDate, 
        @ApiParam(value = "Search by date - end of the range", example = DocumentationAnnotation.EXAMPLE_EVENT_END_DATE) @QueryParam("endDate") @Date(DateFormat.YMDTHMSZZ) String endDate
    ) {
        EventDAOSesame eventDAO = new EventDAOSesame(userSession.getUser());
        
        // 1. Search events with parameters
        ArrayList<Event> events = eventDAO.searchEvents(
                uri,
                type,
                concernedItemLabel, 
                concernedItemUri, 
                startDate, 
                endDate, 
                page, 
                pageSize);
        
        // 2. Analyse result
        ArrayList<EventSimpleDTO> eventDTOs = new ArrayList();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormEventSimple responseForm;
        
        if (events == null) { // Request failure
            responseForm = new ResponseFormEventSimple(0, 0, eventDTOs, true, 0);
            return noResultFound(responseForm, statusList);
        } else if (events.isEmpty()) { // No result
            responseForm = new ResponseFormEventSimple(0, 0, eventDTOs, true, 0);
            return noResultFound(responseForm, statusList);
        } else { // Results
            
            // Generate DTOs
            events.forEach((event) -> {
                eventDTOs.add(new EventSimpleDTO(event));
            });
            
            // Return DTOs
            int resultsCount = eventDAO.count(
                uri,
                type,
                concernedItemLabel, 
                concernedItemUri, 
                startDate, 
                endDate);
            responseForm = new ResponseFormEventSimple(eventDAO.getPageSize(), eventDAO.getPage(), eventDTOs, true, resultsCount);
            if (responseForm.getResult().dataSize() == 0) {
                return noResultFound(responseForm, statusList);
            } else {
                responseForm.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(responseForm).build();
            }
        }
    }
    
    
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get an event's details corresponding to the search uri",
                  notes = "Get an event's details corresponding to the search uri authorized for the user corresponding to the search uri")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Get an event's details", response = RdfResourceDefinitionDTO.class, responseContainer = "List"),
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
    public Response getEventDetailed(
        @ApiParam(value = DocumentationAnnotation.EVENT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_EVENT_URI) @PathParam("uri") @URL @Required String uri) {
        
        EventDAOSesame eventDAO = new EventDAOSesame(userSession.getUser());
        
        // 1. Search an event's details with its URI
        Event event = eventDAO.searchEventDetailed(uri);
        
        // 2. Analyse result
        ArrayList<EventDetailedDTO> eventDTOs = new ArrayList();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormEventDetailed responseForm;
        
        if (event == null) { // Request failure
            responseForm = new ResponseFormEventDetailed(0, 0, eventDTOs, true, 0);
            return noResultFound(responseForm, statusList);
        } else { // Results
            
            // Generate DTO
            eventDTOs.add(new EventDetailedDTO(event));
            
            responseForm = new ResponseFormEventDetailed(0, 0, eventDTOs, true, 0);
            if (responseForm.getResult().dataSize() == 0) {
                return noResultFound(responseForm, statusList);
            } else {
                responseForm.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(responseForm).build();
            }
        }
    }
}
