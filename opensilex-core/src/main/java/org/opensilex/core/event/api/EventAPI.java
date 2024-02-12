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
import org.opensilex.core.csv.api.CSVValidationDTO;
import org.opensilex.core.event.api.csv.AbstractEventCsvImporter;
import org.opensilex.core.event.api.csv.EventCsvImporter;
import org.opensilex.core.event.api.move.MoveCreationDTO;
import org.opensilex.core.event.api.move.MoveDetailsDTO;
import org.opensilex.core.event.api.move.MoveUpdateDTO;
import org.opensilex.core.event.api.move.csv.MoveEventCsvImporter;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.EventSearchFilter;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriListException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.TokenGenerator;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    public static final String CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_EVENT_DELETE_ID = "event-delete";
    public static final String CREDENTIAL_EVENT_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @CurrentUser
    AccountModel currentUser;


    @POST
    @ApiOperation("Create a list of event")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a list of event", response = URI.class),
            @ApiResponse(code = 409, message = "An event with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEvents(@Valid @NotNull List<EventCreationDTO> dtoList) throws Exception {

        try {
            EventDAO<EventModel> dao = new EventDAO<>(sparql, nosql);
            URI eventGraph = dao.getGraph();

            List<EventModel> models = getEventModels(dtoList, eventGraph);
            models.forEach(eventModel -> eventModel.setPublisher(currentUser.getUri()));
            dao.create(models);

            List<URI> createdUris = models.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());
            return new PaginatedListResponse<>(Response.Status.CREATED,createdUris).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Event already exists", duplicateUriException.getMessage()).getResponse();
        }catch (NotFoundURIException | SPARQLInvalidUriListException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "URI not found", e.getMessage()).getResponse();
        }
    }

    @POST
    @Path("/import")
    @ApiOperation(value = "Import a CSV file with one move and one target per line")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Move file saved", response = CSVValidationDTO.class),
            @ApiResponse(code = 409, message = "A move with the same URI already exists", response = ErrorResponse.class)
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importEventCSV(
            @ApiParam(value = "Event file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        EventDAO<EventModel> dao = new EventDAO<>(sparql,nosql);
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        EventCsvImporter csvImporter = new EventCsvImporter(sparql,ontologyDAO,file,currentUser);
        return buildCsvResponse(csvImporter, dao, false).getResponse();
    }

    @POST
    @Path("/import_validation")
    @ApiOperation(value = "Check a CSV file with one move and one target per line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Event file checked", response = CSVValidationDTO.class)})
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateEventCSV(
            @ApiParam(value = "Event file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        EventDAO dao = new EventDAO(sparql, nosql);
        EventCsvImporter csvImporter = new EventCsvImporter(sparql,ontologyDAO,file,currentUser);
        return buildCsvResponse(csvImporter, dao, true).getResponse();
    }

    private List<EventModel> getEventModels(List<? extends EventCreationDTO> eventDtos, URI eventGraph) throws Exception {

        URI eventBaseType = new URI(Oeev.Event.getURI());

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        Map<URI, ClassModel> eventClassesByTypeCache = new HashMap<>();

        List<EventModel> models = new ArrayList<>(eventDtos.size());

        for(EventCreationDTO dto : eventDtos){
            EventModel model = dto.toModel();

            if (!CollectionUtils.isEmpty(dto.getRelations())) {
                URI type = dto.getType();

                ClassModel eventClassModel = eventClassesByTypeCache.get(type);
                if (eventClassModel == null) {
                    eventClassModel = ontologyDAO.getClassModel(type, eventBaseType, currentUser.getLanguage());
                    eventClassesByTypeCache.put(type, eventClassModel);
                }
                setEventRelations(model, dto.getRelations(), eventGraph, ontologyDAO, eventClassModel);
            }
            models.add(model);
        }

        return models;
    }

    private void setEventRelations(EventModel model, List<RDFObjectRelationDTO> relations, URI eventGraph, OntologyDAO ontologyDAO, ClassModel eventClassModel) throws InvalidValueException, URISyntaxException {

        Map<URI,URI> shortPropertiesUris = new HashMap<>();

        for (int i = 0; i < relations.size(); i++) {
            RDFObjectRelationDTO relation = relations.get(i);
            if (relation == null) {
                throw new IllegalArgumentException("Relation at index " + i + " is null");
            }

            if (relation.getProperty() == null || relation.getValue() == null) {
                throw new IllegalArgumentException("Relation at index " + i + " has null property or value");
            }

            URI shortPropUri = shortPropertiesUris.get(relation.getProperty());
            if(shortPropUri == null){
                shortPropUri = new URI(SPARQLDeserializers.getShortURI(relation.getProperty()));
                shortPropertiesUris.put(relation.getProperty(),shortPropUri);
            }

            if (!ontologyDAO.validateObjectValue(eventGraph, eventClassModel, shortPropUri, relation.getValue(), model)) {
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
            @ApiResponse(code = 200, message = "Return updated event", response = URI.class),
            @ApiResponse(code = 404, message = "Event URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEvent(
            @ApiParam("Event description") @Valid @NotNull EventUpdateDTO dto
    ) throws Exception {

        EventDAO<EventModel> dao = new EventDAO<>(sparql, nosql);
        EventModel model = dto.toModel();

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel eventClassModel = null;

        if(dto.getType() != null){
            eventClassModel = ontologyDAO.getClassModel(dto.getType(), new URI(Oeev.Event.getURI()), currentUser.getLanguage());
            if (!CollectionUtils.isEmpty(dto.getRelations())) {
                setEventRelations(model, dto.getRelations(), dao.getGraph(), ontologyDAO, eventClassModel);
            }
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
            @ApiResponse(code = 200, message = "Event deleted", response = URI.class),
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
        EventDAO<EventModel> dao = new EventDAO<>(sparql, nosql);
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
        if (Objects.nonNull(model.getPublisher())){
            dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @ApiOperation(
            value = "Search events"

    )
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
            @ApiParam(value = "Target partial/exact URI", example = "http://www.opensilex.org/demo/2018/o18000076(exact match) or o18000076(partial match)") @QueryParam("target") String target,
            @ApiParam(value = "Description regex pattern", example = "The pest attack") @QueryParam("description") String descriptionPattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "end=asc") @DefaultValue("end=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {

        EventDAO<EventModel> dao = new EventDAO<>(sparql, nosql);
        //create search filter
        EventSearchFilter searchFilter = new EventSearchFilter();
        searchFilter.setTarget(target)
                .setDescriptionPattern(descriptionPattern)
                .setType(type)
                .setStart(start != null ? OffsetDateTime.parse(start) : null)
                .setEnd(end != null ? OffsetDateTime.parse(end) : null)
                .setLang(currentUser.getLanguage())
                .setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize);

        ListWithPagination<EventModel> resultList = dao.search(searchFilter);

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
            @ApiResponse(code = 201, message = "Create a list of move", response = URI.class),
            @ApiResponse(code = 409, message = "A move with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMoves(@Valid @NotNull List<MoveCreationDTO> dtoList) throws Exception {
        try {
            MoveEventDAO dao = new MoveEventDAO(sparql, nosql);

            List<MoveModel> models = (List<MoveModel>)(List<?>) getEventModels(dtoList, dao.getGraph());
            models.forEach(moveModel -> moveModel.setPublisher(currentUser.getUri()));
            for (var move : models) {
                if (move.getFrom() != null && move.getTo() == null) {
                    throw new BadRequestException("Cannot declare a move with a 'From' value but without a 'To' value.");
                }
            }
            dao.create(models);

            List<URI> createdUris = models.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());;
            return new PaginatedListResponse<>(Response.Status.CREATED,createdUris).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Move already exists", duplicateUriException.getMessage()).getResponse();
        }catch (NotFoundURIException | SPARQLInvalidUriListException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "URI not found", e.getMessage()).getResponse();
        }
    }

    private <T extends EventModel> SingleObjectResponse<CSVValidationDTO> buildCsvResponse(
            AbstractEventCsvImporter<T> csvImporter,
            EventDAO<T> dao,
            boolean forValidation
    ) throws Exception {

        // read file
        csvImporter.readFile();

        // build the validation dto
        CSVValidationModel validation = csvImporter.getValidation();
        CSVValidationDTO validationDTO = new CSVValidationDTO();
        validationDTO.setErrors(validation);

        SingleObjectResponse<CSVValidationDTO> importResponse = new SingleObjectResponse<>(validationDTO);

        if(validation.hasErrors()) {
            importResponse.setStatus(Response.Status.BAD_REQUEST);
        }else {

            boolean errors = false;
            List<T> models = csvImporter.getModels();

            try{
                if(forValidation){
                    dao.check(models, true);
                }else{
                    models.forEach(model -> model.setPublisher(currentUser.getUri()));
                    dao.create(models);
                }
                // update validation when some URIs are already existing or unknown
            }catch (SPARQLInvalidUriListException e){
                validation.addInvalidURIError(new CSVCell(AbstractEventCsvImporter.ROWS_BEGIN_IDX,0,e.getMessage(),e.getField()));
                errors = true;
            }catch (SPARQLAlreadyExistingUriListException e){
                validation.addAlreadyExistingURIError(new CSVCell(AbstractEventCsvImporter.ROWS_BEGIN_IDX,0,e.getMessage(),e.getField()));
                errors = true;
            }

            if(errors){
                validationDTO.setErrors(validation);
                importResponse.setStatus(Response.Status.BAD_REQUEST);
            }else {
                // update validation with token
                validationDTO.setNbLinesImported(models.size());
                String token = TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, Collections.emptyMap());
                validationDTO.setValidationToken(token);

                importResponse.setStatus(Response.Status.CREATED);
            }
        }

        return importResponse;
    }

    @POST
    @Path(MOVE_PATH_PREFIX + "/import")
    @ApiOperation(value = "Import a CSV file with one move and one target per line")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Move file saved", response = CSVValidationDTO.class),
            @ApiResponse(code = 409, message = "A move with the same URI already exists", response = ErrorResponse.class)
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EVENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EVENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importMoveCSV(
            @ApiParam(value = "Move file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        MoveEventDAO dao = new MoveEventDAO(sparql,nosql);
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        AbstractEventCsvImporter<MoveModel> csvImporter = new MoveEventCsvImporter(sparql,ontologyDAO,file,currentUser);

        return buildCsvResponse(csvImporter,dao, false).getResponse();
    }

    @POST
    @Path(MOVE_PATH_PREFIX + "/import_validation")
    @ApiOperation(value = "Check a CSV file with one move and one target per line")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Event file checked", response = CSVValidationDTO.class)})
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateMoveCSV(
            @ApiParam(value = "Move file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {

        MoveEventDAO dao = new MoveEventDAO(sparql,nosql);
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        MoveEventCsvImporter csvImporter = new MoveEventCsvImporter(sparql,ontologyDAO,file,currentUser);
        return buildCsvResponse(csvImporter, dao, true).getResponse();
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
            @ApiResponse(code = 200, message = "Return updated move", response = URI.class),
            @ApiResponse(code = 404, message = "Move URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMoveEvent(
            @ApiParam("Event description") @Valid @NotNull MoveUpdateDTO dto
    ) throws Exception {

        MoveEventDAO dao = new MoveEventDAO(sparql, nosql);
        MoveModel model = dto.toModel();

        ClassModel eventClassModel = null;

        if (!CollectionUtils.isEmpty(dto.getRelations())) {
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            eventClassModel = ontologyDAO.getClassModel(dto.getType(), new URI(Oeev.Event.getURI()), currentUser.getLanguage());
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
        if (Objects.nonNull(model.getPublisher())){
            dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
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
            @ApiResponse(code = 200, message = "Move deleted", response = URI.class),
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

    @GET
    @Path("/count")
    @ApiOperation("Count events")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return the number of events associated to targets", response = Integer.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response countEvents(
            @ApiParam(value = "Targets URIs", required = true) @QueryParam("targets") @NotNull @NotEmpty List<URI> targets) throws Exception {

        EventDAO<EventModel> dao = new EventDAO<>(sparql,nosql);
        int count = dao.count(targets);

        return new SingleObjectResponse<>(count).getResponse();
    }

}