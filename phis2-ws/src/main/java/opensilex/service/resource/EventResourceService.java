//******************************************************************************
//                         EventResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.text.ParseException;
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
import opensilex.service.configuration.DateFormat;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.EventDAO;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.DeleteDTO;
import opensilex.service.resource.dto.event.EventGetDTO;
import opensilex.service.resource.dto.event.EventPostDTO;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.model.Event;
import opensilex.service.resource.dto.event.EventPutDTO;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;

/**
 * Service to handle events
 * @update [Andréas Garcia] 14 Feb. 2019: Add GET detail service
 * @update [Andréas Garcia] 5 Mar. 2019: Add POST service
 * @update [Andréas Garcia] 15 Mar. 2019: Add GET {uri}/annotations service
 * @update [Andréas Garcia] 8 Apr. 2019: Refactor generic functions into the ResourceService class
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
@Api("/events")
@Path("/events")
public class EventResourceService  extends ResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(EventResourceService.class);
    
    /**
     * Searches events with filters.
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
     * @return  list of events filtered.
     */
    @GET
    @ApiOperation(value = "Get all events corresponding to the search parameters given.", 
            notes = "Retrieve all events authorized for the user corresponding to the " + "search parameters given")
    @ApiResponses(value = {
        @ApiResponse(
                code = 200, 
                message = "Retrieve all events", 
                response = EventGetDTO.class, 
                responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({@ApiImplicitParam(
            name = GlobalWebserviceValues.AUTHORIZATION, 
            required = true, dataType = GlobalWebserviceValues.DATA_TYPE_STRING, 
            paramType = GlobalWebserviceValues.HEADER, 
            value = DocumentationAnnotation.ACCES_TOKEN, 
            example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) 
            @QueryParam(GlobalWebserviceValues.PAGE_SIZE) 
            @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) 
            @Min(0) int pageSize, 
        @ApiParam(value = DocumentationAnnotation.PAGE) 
            @QueryParam(GlobalWebserviceValues.PAGE) 
            @DefaultValue(DefaultBrapiPaginationValues.PAGE)
            @Min(0) int page, 
        @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_EVENT_URI) 
            @QueryParam("uri") 
            @URL String uri, 
        @ApiParam(value = "Search by type", example = DocumentationAnnotation.EXAMPLE_EVENT_TYPE) 
            @QueryParam("type") 
            @URL String type, 
        @ApiParam(
                value = "Search by concerned item uri", 
                example = DocumentationAnnotation.EXAMPLE_EVENT_CONCERNED_ITEM_URI) 
            @QueryParam("concernedItemUri") @URL String concernedItemUri, 
        @ApiParam(
                value = "Search by concerned item label", 
                example = DocumentationAnnotation.EXAMPLE_EVENT_CONCERNED_ITEM_LABEL) 
            @QueryParam("concernedItemLabel") String concernedItemLabel, 
        @ApiParam(
                value = "Search by date - start of the range", 
                example = DocumentationAnnotation.EXAMPLE_EVENT_SEARCH_START_DATE) 
            @QueryParam("startDate") 
            @Date({DateFormat.YMDTHMSZZ, DateFormat.YMD}) String startDate, 
        @ApiParam(
                value = "Search by date - end of the range", 
                example = DocumentationAnnotation.EXAMPLE_EVENT_SEARCH_END_DATE) 
            @QueryParam("endDate") 
            @Date({DateFormat.YMDTHMSZZ, DateFormat.YMD}) String endDate
    ) {
        EventDAO eventDAO = new EventDAO(userSession.getUser());
        
        // Search events with parameters
        ArrayList<Event> events;
        try {
            java.util.Date start = DateFormat.parseDateOrDateTime(startDate, false);
            java.util.Date end = DateFormat.parseDateOrDateTime(endDate, true);
                 
            events = eventDAO.find(
                    uri,
                    type,
                    concernedItemLabel,
                    concernedItemUri,
                    start,
                    end,
                    page,
                    pageSize);
        // handle exceptions
        } catch (DAOPersistenceException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return getResponseWhenPersistenceError(ex);
        } catch (ParseException ex) {
            return getResponseWhenInternalError(ex);
        }

        if (events == null) {
            return getGETResponseWhenNoResult();
        } else if (events.isEmpty()) {
            return getGETResponseWhenNoResult();
        } else {
            // count results
            try {
                java.util.Date start = DateFormat.parseDateOrDateTime(startDate, false);
                 java.util.Date end = DateFormat.parseDateOrDateTime(endDate, true);
                int totalCount = eventDAO.count(uri, type, concernedItemLabel, concernedItemUri, start, end);
                return getGETResponseWhenSuccess(events, pageSize, page, totalCount);
                
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
     * Gets an event from its URI.
     * @example
     * {
     *   "metadata": {
     *     "pagination": null,
     *     "status": [],
     *     "datafiles": []
     *   },
     *   "result": {
     *     "data": [
     *       {
     *         "uri": "http://www.opensilex.org/id/event/12590c87-1c34-426b-a231-beb7acb33415",
     *         "type": "http://www.opensilex.org/vocabulary/oeev#PestAttack",
     *         "concernedItems": [
     *           {
     *             "labels": [
     *               "Parcelle Lavalette",
     *               "Plot Lavalette"
     *             ],
     *             "uri": "http://www.opensilex.org/ues/2018/o18000124",
     *             "typeURI": "http://www.opensilex.org/vocabulary/oeso#Plot"
     *           }
     *         ],
     *         "date": "2017-09-08T12:00:00+01:00",
     *         "properties": [
     *           {
     *             "rdfType": null,
     *             "relation": "http://www.opensilex.org/vocabulary/oeev#hasPest",
     *             "value": "http://aims.fao.org/aos/agrovoc/c_34724"
     *           }
     *         ]
     *       }
     *     ]
     *   }
     * }
     * @param uri
     * @return the event found
     */
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get the event corresponding to the search uri",
                  notes = "Get the event corresponding to the search uri")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Get an event", response = EventGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, 
            required = true,
            dataType = GlobalWebserviceValues.DATA_TYPE_STRING, 
            paramType = GlobalWebserviceValues.HEADER,
            value = DocumentationAnnotation.ACCES_TOKEN,
            example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEventByUri(
        @ApiParam(
                value = DocumentationAnnotation.EVENT_URI_DEFINITION, 
                required = true, 
                example = DocumentationAnnotation.EXAMPLE_EVENT_URI) 
            @PathParam("uri") @URL @Required String uri) {
        
        return getGETByUriResponseFromDAOResults(new EventDAO(userSession.getUser()), uri);
    }
    
    /**
     * Gets an event's annotations.
     * @param pageSize
     * @param page
     * @example
     * [  
     *   {
     *     "uri": "http://www.opensilex.org/phenome-fppn/id/annotation/896325c3-85f7-4ad3-bf96-34ba497108c3",
     *     "creationDate": "2019-03-11T09:40:03+01:00",
     *     "creator": "http://www.phenome-fppn.fr/diaphen/id/agent/admin_phis",
     *     "motivatedBy": "http://www.w3.org/ns/oa#describing",
     *     "bodyValues": [
     *       "fth"
     *     ],
     *     "targets": [
     *       "http://www.opensilex.org/phenome-fppn/id/event/c8e0173b-ce8a-4190-ad0b-f30ac07d4edd"
     *     ]
     *   }
     * ]
     * @param uri
     * @return an event's annotations
     */
    @GET
    @Path("{uri}/annotations")
    @ApiOperation(value = "Get an event's annotations",
                  notes = "Get an event's annotations")
    @ApiResponses(value = {
        @ApiResponse(
                code = 200, message = "Get an event's annotations", 
                response = RdfResourceDefinitionDTO.class, 
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
    public Response getEventAnnotations(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) 
        @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) 
            @QueryParam(GlobalWebserviceValues.PAGE) 
            @DefaultValue(DefaultBrapiPaginationValues.PAGE) 
            @Min(0) int page,
        @ApiParam(
                value = DocumentationAnnotation.EVENT_URI_DEFINITION, 
                required = true, 
                example = DocumentationAnnotation.EXAMPLE_EVENT_URI) 
            @PathParam("uri") @URL @Required String uri) {
        
        AnnotationResourceService annotationResourceService = new AnnotationResourceService();
        annotationResourceService.userSession = userSession;
        return annotationResourceService.getAnnotationsBySearch(pageSize, page, null, null, uri, null, null, true);
    }
        
    /**
     * Service to insert events
     * @example
     * {
     *  [
     *   {
     *     "rdfType": "http://www.opensilex.org/vocabulary/oeev#MoveFrom",
     *     "description": "The pest attack lasted 20 minutes",
     *     "creator": "http://www.phenome-fppn.fr/diaphen/id/agent/marie_dupond",
     *     "concernedItemsUris": [
     *       "string"
     *     ],
     *     "date": "2017-09-08T12:00:00+01:00",
     *     "properties": [
     *       {
     *         "rdfType": "http://xmlns.com/foaf/0.1/Agent",
     *         "relation": "http://www.phenome-fppn.fr/vocabulary/2018#hasContact",
     *         "value": "http://www.phenome-fppn.fr/diaphen/id/agent/marie_dupond"
     *       }
     *     ]
     *   }
     *  ]
     * }
     * @param eventsDtos
     * @param context
     * @return  The found errors
     *          The list of the URIs of the created events
     */
    @POST
    @ApiOperation(value = "POST event(s)", notes = "Register event(s)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Event(s) saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, 
                required = true, 
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, 
                paramType = GlobalWebserviceValues.HEADER, 
                value = DocumentationAnnotation.ACCES_TOKEN, 
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(
        @ApiParam(value = DocumentationAnnotation.EVENT_POST_DEFINITION) @Valid ArrayList<EventPostDTO> eventsDtos,
        @Context HttpServletRequest context) {
        
        // Set DAO
        EventDAO objectDao = new EventDAO(userSession.getUser());
        if (context.getRemoteAddr() != null) {
            objectDao.remoteUserAdress = context.getRemoteAddr();
        }
        
        // Get POST response
        return getPostResponse(objectDao, eventsDtos, context.getRemoteAddr(), StatusCodeMsg.EMPTY_EVENT_LIST);
    }
    
    /**
     * Radiometric target PUT service.
     * @param eventsDtos
     * @param context
     * @return the response of the service.
     */
    @PUT
    @ApiOperation(value = "Update events")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Event(s) updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Event(s) not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(
        @ApiParam(value = DocumentationAnnotation.EVENT_PUT_DEFINITION) @Valid ArrayList<EventPutDTO> eventsDtos,
        @Context HttpServletRequest context) {
        
        // Set DAO
        EventDAO objectDao = new EventDAO(userSession.getUser());
        if (context.getRemoteAddr() != null) {
            objectDao.remoteUserAdress = context.getRemoteAddr();
        }
        
        // Get POST response
        return getPutResponse(objectDao, eventsDtos, context.getRemoteAddr(), StatusCodeMsg.EMPTY_EVENT_LIST);
    }
    
    
    /**
     * @example
     *[
     *	http://www.phenome-fppn.fr/platform/id/event/8247af37-769c-495b-8e7e-78b1141176c2,
     *  http://www.phenome-fppn.fr/platform/id/event/8247gt37-769c-495b-8e7e-91jh633151k4
     *]
     * 
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation(
    	value = "Delete a list of event",
    	notes = "Delete a list of event. Need URL encoded event URI"
    	
    		)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Event(s) deleted", response = ResponseFormPOST.class), 
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
    public Response deleteEventUri( 
    		@ApiParam(
		        value = DocumentationAnnotation.ANNOTATION_URI_DEFINITION,
		        required = true, 
		        example = DocumentationAnnotation.EXAMPLE_ANNOTATION_URI
		    ) 
        	@Valid @NotNull DeleteDTO deleteDTO, @Context HttpServletRequest context) {
    	
    	EventDAO eventDao = new EventDAO(userSession.getUser());
		if (context.getRemoteAddr() != null) {
			 eventDao.setRemoteUserAdress(context.getRemoteAddr());
	    }
		Response response = buildDeleteObjectsByUriResponse(eventDao, deleteDTO,"Event(s) deleted");
    	eventDao.getConnection().close();
    	return response;
    }

    @Override
    protected ArrayList<AbstractVerifiedClass> getDTOsFromObjects(List<? extends Object> objects) {
        ArrayList<AbstractVerifiedClass> dtos = new ArrayList<>();
        objects.forEach((object) -> {
            dtos.add(new EventGetDTO((Event)object));
        });
        return dtos;
    }
    
    @Override
    protected List<? extends Object> getObjectsFromDTOs (List<? extends AbstractVerifiedClass> dtos)
            throws Exception {
        List<Object> objects = new ArrayList<>();
        for (AbstractVerifiedClass objectDto : dtos) {
            objects.add((Event)objectDto.createObjectFromDTO());
        }
        return objects;
    }
    
    @Override
    protected List<String> getUrisFromObjects (List<? extends Object> createdObjects) {
        List<String> createdUris = new ArrayList<>();
        createdObjects.forEach(object -> {
            createdUris.add(((Event)object).getUri());
        });
        return createdUris;
    }
}
