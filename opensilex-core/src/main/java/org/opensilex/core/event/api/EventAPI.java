//******************************************************************************
//                          EventAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.api;

import com.apicatalog.jsonld.StringUtils;
import io.swagger.annotations.*;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.csv.api.CSVValidationDTO;
import org.opensilex.core.csv.api.CsvImportDTO;
import org.opensilex.core.event.api.csv.AbstractEventCsvImporter;
import org.opensilex.core.event.api.csv.EventCsvImporter;
import org.opensilex.core.event.api.move.MoveCreationDTO;
import org.opensilex.core.event.api.move.MoveDetailsDTO;
import org.opensilex.core.event.api.move.MoveUpdateDTO;
import org.opensilex.core.event.api.move.csv.MoveEventCsvImporter;
import org.opensilex.core.event.bll.EventLogic;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.EventSearchFilter;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvImporter;
import org.opensilex.sparql.csv.validation.CachedCsvImporter;
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
import java.io.File;
import java.io.InputStream;
import java.net.URI;
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
            EventLogic<EventModel, EventSearchFilter> logic = new EventLogic<>(sparql, nosql, currentUser, EventModel.class);

            List<EventModel> models = getEventModels(dtoList, logic);
            models = logic.create(models, false);

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
            @ApiParam(value = "CSV import settings", required = true) @NotNull @Valid @FormDataParam("description") CsvImportDTO importDTO,
            @ApiParam(value = "Event file", required = true, type = "file") @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        try {
            sparql.startTransaction();
            nosql.startTransaction();

            CsvImporter<EventModel> csvImporter = new CachedCsvImporter<>(
                    new EventCsvImporter(sparql, nosql, currentUser),
                    importDTO.getValidationToken()
            );

            CSVValidationModel validationModel = csvImporter.importCSV(file,false);
            sparql.commitTransaction();
            nosql.commitTransaction();

            return new SingleObjectResponse<>(new CSVValidationDTO(validationModel)).getResponse();

        }catch (Exception e){
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw e;
        }
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
            @ApiParam(value = "CSV import settings", required = true) @NotNull @Valid @FormDataParam("description") CsvImportDTO importDTO,
            @ApiParam(value = "Event file", required = true, type = "file") @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {

        CsvImporter<EventModel> csvImporter = new CachedCsvImporter<>(
                new EventCsvImporter(sparql, nosql, currentUser),
                importDTO.getValidationToken()
        );

        CSVValidationModel validationModel = csvImporter.importCSV(file, true);
        return new SingleObjectResponse<>(new CSVValidationDTO(validationModel)).getResponse();
    }

    /**
     *
     * @param eventDtos
     * @param logic : logic class previously generated in a service
     * @return a list of fresh models created using the dtos
     * @throws Exception
     */
    private <T extends EventModel, F extends EventSearchFilter> List<EventModel> getEventModels(List<? extends EventCreationDTO> eventDtos, EventLogic<T, F> logic) throws Exception {

        Map<URI, ClassModel> eventClassesByTypeCache = new HashMap<>();

        List<EventModel> models = new ArrayList<>(eventDtos.size());

        for(EventCreationDTO dto : eventDtos){
            EventModel model = dto.toModel();
            logic.setEventRelations((T)model, dto.getRelations(), dto.getType(), eventClassesByTypeCache);
            models.add(model);
        }

        return models;
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

        EventLogic<EventModel, EventSearchFilter> logic = new EventLogic<>(sparql, nosql, currentUser, EventModel.class);
        EventModel model = logic.setEventRelations(dto.toModel(), dto.getRelations(), dto.getType(), null);
        logic.updateModel(model);

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
        EventLogic<EventModel, EventSearchFilter> logic = new EventLogic<>(sparql, nosql, currentUser, EventModel.class);
        logic.delete(uri);
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
        EventModel model = new EventLogic<>(sparql, nosql, currentUser, EventModel.class).get(uri);
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

        EventModel model = new EventLogic<>(sparql, nosql, currentUser, EventModel.class).get(uri);

        EventDetailsDTO dto = new EventDetailsDTO();
        dto.fromModel(model);
        if (Objects.nonNull(model.getPublisher())){
            dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @ApiOperation(value = "Search events")
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

        EventLogic<EventModel, EventSearchFilter> logic = new EventLogic<>(sparql, nosql, currentUser, EventModel.class);
        //create search filter
        EventSearchFilter searchFilter = new EventSearchFilter();

        if(!StringUtils.isBlank(target)){
            searchFilter.setTargets(Collections.singletonList(URI.create(target)));
        }
        searchFilter.setDescriptionPattern(descriptionPattern)
                .setType(type)
                .setStart(start != null ? OffsetDateTime.parse(start) : null)
                .setEnd(end != null ? OffsetDateTime.parse(end) : null)
                .setLang(currentUser.getLanguage())
                .setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize);

        ListWithPagination<EventModel> resultList = logic.search(searchFilter);

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
            MoveLogic logic = new MoveLogic(sparql, nosql, currentUser);
            List<MoveModel> models = (List<MoveModel>)(List<?>) getEventModels(dtoList, logic);
            models = logic.create(models, false);

            List<URI> createdUris = models.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());;
            return new PaginatedListResponse<>(Response.Status.CREATED,createdUris).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Move already exists", duplicateUriException.getMessage()).getResponse();
        }catch (NotFoundURIException | SPARQLInvalidUriListException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "URI not found", e.getMessage()).getResponse();
        }
    }

    private <T extends EventModel, F extends EventSearchFilter> SingleObjectResponse<CSVValidationDTO> buildCsvResponse(
            AbstractEventCsvImporter<T> csvImporter,
            EventLogic<T, F> logic,
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
                logic.create(models, forValidation);
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

        MoveLogic logic = new MoveLogic(sparql, nosql, currentUser);
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        AbstractEventCsvImporter<MoveModel> csvImporter = new MoveEventCsvImporter(sparql,ontologyDAO,file,currentUser);

        return buildCsvResponse(csvImporter, logic, false).getResponse();
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

        MoveLogic logic = new MoveLogic(sparql, nosql, currentUser);
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        MoveEventCsvImporter csvImporter = new MoveEventCsvImporter(sparql,ontologyDAO,file,currentUser);
        return buildCsvResponse(csvImporter, logic, true).getResponse();
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
        MoveLogic logic = new MoveLogic(sparql, nosql, currentUser);
        MoveModel model = logic.setEventRelations(dto.toModel(), dto.getRelations(), dto.getType(), null);
        logic.updateModel(model);
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
        MoveModel model = new MoveLogic(sparql, nosql, currentUser).get(uri);

        MoveDetailsDTO dto = new MoveDetailsDTO(model);
        if (Objects.nonNull(model.getPublisher())){
            dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path(MOVE_PATH_PREFIX + "/by_uris")
    @ApiOperation("Get a list of moves with all positional information")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Moves retrieved", response = MoveDetailsDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Move URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMoveEventByUris(
            @ApiParam(value = "Move URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        var logic = new MoveLogic(sparql, nosql, currentUser);
        var accountDao = new AccountDAO(sparql);
        //@todo This map is used to fetch all accounts at once and fill them on the DTOs. This should be generalized
        //      to all services that need it.
        var publisherMap = new HashMap<URI, AccountModel>();
        var dtoList = new ArrayList<MoveDetailsDTO>(uris.size());

        for (var model : logic.getList(uris)) {
            var dto = new MoveDetailsDTO(model);
            if (model.getPublisher() != null) {
                publisherMap.put(model.getPublisher(), null);
                dto.setPublisher(new UserGetDTO());
                dto.getPublisher().setUri(model.getPublisher());
            }
            dtoList.add(dto);
        }

        //@todo Find a better way to fetch accounts for a list of model. Propagate this to all APIs.
        for (var publisher : accountDao.getList((publisherMap.keySet()))) {
            publisherMap.put(publisher.getUri(), publisher);
        }

        for (var dto : dtoList) {
            if (dto.getPublisher() != null) {
                dto.setPublisher(UserGetDTO.fromModel(publisherMap.get(dto.getPublisher().getUri())));
            }
        }

        return new PaginatedListResponse<>(dtoList).getResponse();
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
        MoveLogic logic = new MoveLogic(sparql, nosql, currentUser);
        logic.delete(uri);
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

        EventLogic<EventModel, EventSearchFilter> logic = new EventLogic<>(sparql, nosql, currentUser, EventModel.class);
        int count = logic.countForTargets(targets);

        return new SingleObjectResponse<>(count).getResponse();
    }

}