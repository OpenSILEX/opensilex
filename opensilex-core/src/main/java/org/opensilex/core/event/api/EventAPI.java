//******************************************************************************
//                          EventAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.api;

import io.swagger.annotations.*;
import org.apache.commons.collections4.CollectionUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.event.api.csv.EventCsvImporter;
import org.opensilex.core.event.api.move.MoveCreationDTO;
import org.opensilex.core.event.api.move.MoveDetailsDTO;
import org.opensilex.core.event.api.move.MoveUpdateDTO;
import org.opensilex.core.event.api.move.csv.MoveEventCsvImporter;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.api.CSVValidationDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.CSVValidationModel;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.TokenGenerator;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Renaud COLIN
 */
@Api(EventAPI.CREDENTIAL_EVENT_GROUP_ID)
@Path(EventAPI.PATH)
@ApiCredentialGroup(
        groupId = EventAPI.CREDENTIAL_EVENT_GROUP_ID,
        groupLabelKey = EventAPI.CREDENTIAL_EVENT_GROUP_LABEL_KEY
)
public class EventAPI {

    public static final String PATH = "/core/events";

    public static final String MOVE_PATH_PREFIX = "moves";
    public static final String MOVE_PATH = PATH + "/" + MOVE_PATH_PREFIX;

    public static final String CREDENTIAL_EVENT_GROUP_ID = "Events";
    public static final String CREDENTIAL_EVENT_GROUP_LABEL_KEY = "credential-groups.events";

    public static final String CREDENTIAL_EVENT_MODIFICATION_ID = "event-modification";
    public static final String CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY = "credential.event.modification";

    public static final String CREDENTIAL_EVENT_DELETE_ID = "event-delete";
    public static final String CREDENTIAL_EVENT_DELETE_LABEL_KEY = "credential.event.delete";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @CurrentUser
    UserModel currentUser;


    @POST
    @ApiOperation("Create a list of event")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a list of event", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "An event with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEvents(@Valid @NotNull List<EventCreationDTO> dtoList) throws Exception {

        try {
            EventDAO dao = new EventDAO(sparql, nosql);
            URI eventGraph = dao.getGraph();

            List<EventModel> models = getEventModels(dtoList, eventGraph).collect(Collectors.toList());
            dao.create(models);

            List<URI> createdUris = models.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());
            return new PaginatedListResponse<>(Response.Status.CREATED,createdUris).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Event already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @POST
    @Path("/import")
    @ApiOperation(value = "Import a CSV file with one move and one concerned item per line")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Move file saved", response = CSVValidationDTO.class),
            @ApiResponse(code = 409, message = "A move with the same URI already exists", response = ErrorResponse.class)
    })
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importEventCSV(
            @ApiParam(value = "Event file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        EventDAO dao = new EventDAO(sparql,nosql);
        EventCsvImporter csvImporter = new EventCsvImporter(file,currentUser.getUri());
        csvImporter.readFile(false);

        CSVValidationModel validation = csvImporter.getValidation();
        CSVValidationDTO validationDTO = new CSVValidationDTO();
        validationDTO.setErrors(validation);

        SingleObjectResponse<CSVValidationDTO> importResponse = new SingleObjectResponse<>(validationDTO);

        if(validation.hasErrors()) {
            importResponse.setStatus(Response.Status.BAD_REQUEST);
        }else {
            List<EventModel> models = csvImporter.getModels();
            dao.create(models);
            validationDTO.setNbLinesImported(models.size());

            String token = TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, Collections.emptyMap());
            validationDTO.setValidationToken(token);

            importResponse.setStatus(Response.Status.CREATED);
        }
        return importResponse.getResponse();
    }

    @POST
    @Path("/import_validation")
    @ApiOperation(value = "Check a CSV file with one move and one concerned item per line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Event file checked", response = CSVValidationDTO.class)})
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateEventCSV(
            @ApiParam(value = "Event file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {

        EventCsvImporter csvImporter = new EventCsvImporter(file,currentUser.getUri());
        csvImporter.readFile(true);

        CSVValidationModel validation = csvImporter.getValidation();

        CSVValidationDTO validationDTO = new CSVValidationDTO();
        validationDTO.setErrors(validation);

        if (!validation.hasErrors()) {
            String token = TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, Collections.emptyMap());
            validationDTO.setValidationToken(token);
        }

        return new SingleObjectResponse<>(validationDTO).getResponse();
    }

    private Stream<? extends EventModel> getEventModels(List<? extends EventCreationDTO> eventDtos, URI eventGraph) throws URISyntaxException {

        URI eventUri = new URI(Oeev.Event.getURI());

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        Map<URI, ClassModel> eventClassesByUriCache = new HashMap<>();

        Function<URI, ClassModel> getEventClassFunction = (eventType) -> {
            try {
                return ontologyDAO.getClassModel(eventType, eventUri, currentUser.getLanguage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        return eventDtos.stream().map(
                dto -> {
                    EventModel model = dto.toModel();
                    model.setCreator(currentUser.getUri());

                    if (!CollectionUtils.isEmpty(dto.getRelations())) {
                        ClassModel eventClassModel = eventClassesByUriCache.computeIfAbsent(dto.getType(), getEventClassFunction);
                        setEventRelations(model, dto.getRelations(), eventGraph, ontologyDAO, eventClassModel);
                    }
                    return model;
                }
        );
    }

    private void setEventRelations(EventModel model, List<RDFObjectRelationDTO> relations, URI eventGraph, OntologyDAO ontologyDAO, ClassModel eventClassModel) throws InvalidValueException {

        for (int i = 0; i < relations.size(); i++) {
            RDFObjectRelationDTO relation = relations.get(i);
            if (relation == null) {
                throw new IllegalArgumentException("relation at index " + i + " is null");
            }

            if (relation.getProperty() == null || relation.getValue() == null) {
                throw new IllegalArgumentException("relation at index " + i + " has null property or value");
            }
            if (!ontologyDAO.validateObjectValue(eventGraph, eventClassModel, relation.getProperty(), relation.getValue(), model)) {
                throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
            }
        }
    }

    @PUT
    @ApiOperation("Update an event")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return updated event", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Event URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEvent(
            @ApiParam("Event description") @Valid @NotNull EventUpdateDTO dto
    ) throws Exception {

        EventDAO dao = new EventDAO(sparql, nosql);
        EventModel model = dto.toModel();
        model.setCreator(currentUser.getUri());

        if (!CollectionUtils.isEmpty(dto.getRelations())) {
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            ClassModel eventClassModel = ontologyDAO.getClassModel(dto.getType(), new URI(Oeev.Event.getURI()), currentUser.getLanguage());
            setEventRelations(model, dto.getRelations(), dao.getGraph(), ontologyDAO, eventClassModel);
        }

        dao.update(model);
        return new ObjectUriResponse(Response.Status.OK, dto.getUri()).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an event")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_EVENT_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Event deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Event URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEvent(
            @ApiParam(value = "Event URI", example = "http://opensilex.dev/events/deplacement/1865162374", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        EventDAO dao = new EventDAO(sparql, nosql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an event")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Event retrieved", response = EventGetDTO.class),
            @ApiResponse(code = 404, message = "Event URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvent(
            @ApiParam(value = "Event URI", example = "http://opensilex.dev/events/1865162374", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        EventDAO dao = new EventDAO(sparql, nosql);
        EventModel model = dao.get(uri, currentUser);
        if (model == null) {
            throw new NotFoundURIException(uri);
        }

        EventGetDTO dto = new EventGetDTO();
        dto.fromModel(model);
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path("{uri}/details")
    @ApiOperation("Get an event with all it's properties")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Event retrieved", response = EventDetailsDTO.class),
            @ApiResponse(code = 404, message = "Event URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEventDetails(
            @ApiParam(value = "Event URI", example = "http://opensilex.dev/events/1865162374", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        EventDAO dao = new EventDAO(sparql, nosql);

        EventModel model = dao.get(uri, currentUser);
        if (model == null) {
            throw new NotFoundURIException(uri);
        }

        EventDetailsDTO dto = new EventDetailsDTO();
        dto.fromModel(model);
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @ApiOperation("Search events")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return event list", response = EventGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchEvents(
            @ApiParam(value = "Event type", example = "http://www.opensilex.org/vocabulary/oeev#MoveFrom") @QueryParam("rdf_type") URI type,
            @ApiParam(value = "Start date : match event after the given start date", example = "2019-09-08T12:00:00+01:00") @QueryParam("start") @ValidOffsetDateTime String start,
            @ApiParam(value = "End date : match event before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("end") @ValidOffsetDateTime String end,
            @ApiParam(value = "Concerned item URI regex pattern", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") String concernedItemPattern,
            @ApiParam(value = "Description regex pattern", example = "The pest attack") @QueryParam("description") String descriptionPattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {

        EventDAO dao = new EventDAO(sparql, nosql);

        ListWithPagination<EventModel> resultList = dao.search(
                concernedItemPattern,
                descriptionPattern,
                type,
                start != null ? OffsetDateTime.parse(start) : null,
                end != null ? OffsetDateTime.parse(end) : null,
                currentUser.getLanguage(),
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<EventGetDTO> resultDTOList = resultList.convert(
                EventGetDTO.class,
                model -> {
                    EventGetDTO dto = new EventGetDTO();
                    dto.fromModel(model);
                    return dto;
                }
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }


    @POST
    @Path(MOVE_PATH_PREFIX)
    @ApiOperation("Create a list of move event")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a list of move", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A move with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMoves(@Valid @NotNull List<MoveCreationDTO> dtoList) throws Exception {
        try {
            MoveEventDAO dao = new MoveEventDAO(sparql, nosql);

            Stream<MoveModel> modelStream = (Stream<MoveModel>) getEventModels(dtoList, dao.getGraph());
            List<MoveModel> models = modelStream.collect(Collectors.toList());
            dao.createMoveEvents(models);

            List<URI> createdUris = models.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());;
            return new PaginatedListResponse<>(Response.Status.CREATED,createdUris).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Move already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @POST
    @Path(MOVE_PATH_PREFIX + "/import")
    @ApiOperation(value = "Import a CSV file with one move and one concerned item per line")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Move file saved", response = CSVValidationDTO.class),
            @ApiResponse(code = 409, message = "A move with the same URI already exists", response = ErrorResponse.class)
    })
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importMoveCSV(
            @ApiParam(value = "Move file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        MoveEventDAO dao = new MoveEventDAO(sparql,nosql);
        MoveEventCsvImporter csvImporter = new MoveEventCsvImporter(file,currentUser.getUri());
        csvImporter.readFile(false);

        CSVValidationModel validation = csvImporter.getValidation();
        CSVValidationDTO validationDTO = new CSVValidationDTO();
        validationDTO.setErrors(validation);

        SingleObjectResponse<CSVValidationDTO> importResponse = new SingleObjectResponse<>(validationDTO);

        if(validation.hasErrors()) {
            importResponse.setStatus(Response.Status.BAD_REQUEST);
        }else {
            List<MoveModel> models = csvImporter.getModels();
            dao.createMoveEvents(models);
            validationDTO.setNbLinesImported(models.size());

            String token = TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, Collections.emptyMap());
            validationDTO.setValidationToken(token);

            importResponse.setStatus(Response.Status.CREATED);
        }
        return importResponse.getResponse();
    }

    @POST
    @Path(MOVE_PATH_PREFIX + "/import_validation")
    @ApiOperation(value = "Check a CSV file with one move and one concerned item per line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Event file checked", response = CSVValidationDTO.class)})
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateMoveCSV(
            @ApiParam(value = "Move file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {

        MoveEventCsvImporter csvImporter = new MoveEventCsvImporter(file,currentUser.getUri());
        csvImporter.readFile(true);

        CSVValidationModel validation = csvImporter.getValidation();

        CSVValidationDTO validationDTO = new CSVValidationDTO();
        validationDTO.setErrors(validation);

        if (!validation.hasErrors()) {
            String token = TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, Collections.emptyMap());
            validationDTO.setValidationToken(token);
        }

        return new SingleObjectResponse<>(validationDTO).getResponse();
    }

    @PUT
    @Path(MOVE_PATH_PREFIX)
    @ApiOperation("Update a move event")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return updated move", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Move URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMoveEvent(
            @ApiParam("Event description") @Valid @NotNull MoveUpdateDTO dto
    ) throws Exception {

        MoveEventDAO dao = new MoveEventDAO(sparql, nosql);
        MoveModel model = dto.toModel();
        model.setCreator(currentUser.getUri());

        if (!CollectionUtils.isEmpty(dto.getRelations())) {
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            ClassModel eventClassModel = ontologyDAO.getClassModel(dto.getType(), new URI(Oeev.Event.getURI()), currentUser.getLanguage());
            setEventRelations(model, dto.getRelations(), dao.getGraph(), ontologyDAO, eventClassModel);
        }

        dao.update(model);
        return new ObjectUriResponse(Response.Status.OK, dto.getUri()).getResponse();
    }

    @GET
    @Path(MOVE_PATH_PREFIX + "/{uri}")
    @ApiOperation("Get a move with all it's properties")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Move retrieved", response = MoveDetailsDTO.class),
            @ApiResponse(code = 404, message = "Move URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMoveEvent(
            @ApiParam(value = "Move URI", example = "http://opensilex.dev/events/1865162374", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        MoveEventDAO dao = new MoveEventDAO(sparql, nosql);

        MoveModel model = dao.getMoveEventByURI(uri, currentUser);
        if (model == null) {
            throw new NotFoundURIException(uri);
        }

        MoveDetailsDTO dto = new MoveDetailsDTO(model);
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @DELETE
    @Path(MOVE_PATH_PREFIX + "/{uri}")
    @ApiOperation("Delete a move event")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_EVENT_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Move deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Move URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMoveEvent(
            @ApiParam(value = "Event URI", example = "http://opensilex.dev/events/deplacement/1865162374", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        MoveEventDAO dao = new MoveEventDAO(sparql, nosql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

}