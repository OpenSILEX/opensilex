//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.geojson.Geometry;
import io.swagger.annotations.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.geojson.GeoJsonObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.csv.api.CSVValidationDTO;
import org.opensilex.core.data.api.CriteriaDTO;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geospatial.api.GeometryDTO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.bll.ScientificObjectLogic;
import org.opensilex.core.scientificObject.dal.*;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.server.exceptions.displayable.DisplayableResponseException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvImporter;
import org.opensilex.sparql.csv.export.CsvExporter;
import org.opensilex.sparql.csv.validation.CachedCsvImporter;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ExcludableUriList;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Julien BONNEFONT
 */
@Api(ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID)
@Path(ScientificObjectAPI.PATH)
@ApiCredentialGroup(
        groupId = ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID,
        groupLabelKey = ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_LABEL_KEY
)
public class ScientificObjectAPI {

    public static final String PATH = "/core/scientific_objects";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID = "Scientific Objects";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_LABEL_KEY = "credential-groups.scientific-objects";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID = "scientific-objects-modification";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID = "scientific-objects-delete";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_LABEL_KEY = "credential.default.delete";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScientificObjectAPI.class);

    public static final String INVALID_GEOMETRY = "Invalid geometry (longitude must be between -180 and 180 and latitude must be between -90 and 90, no self-intersection, ...)";
    public static final String DELETE_ERROR_TITLE ="Scientific object can't be deleted";

    /**
     * Name of the parameter used into translate-key about scientific object deletion error.
     * This key is related to message-en.yml and message-fr.yml translation files (located in opensilex-front)
     */
    public static final String DELETE_ERROR_KEY_PARAMETER ="scientific_object";

    /**
     * Experiment URI claim key
     */
    private static final String CLAIM_CONTEXT_URI = "context";

    public static final String SCIENTIFIC_OBJECT_EXAMPLE_URI = "http://opensilex.org/id/Plot 12";
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_TYPE = "vocabulary:Plot";
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_TYPE_NAME = "Plot";
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_NAME = "Plot 12";


    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    //TODO: waiting for XP for V2
    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @POST
    @Path("by_uris")
    @ApiOperation("Get scientific objet list of a given experiment URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return list of scientific objects corresponding to the given experiment URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsListByUris(
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") URI contextURI,
            @ApiParam(value = "Scientific object uris") List<URI> objectsURI
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);

        List<ScientificObjectModel> scientificObjects = logic.searchByURIs(contextURI, objectsURI, currentUser);

        //TODO: to location
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        HashMap<String, Geometry> mapGeo = geoDAO.getGeometryByUris(contextURI, objectsURI);
        List<ScientificObjectNodeDTO> dtoList = scientificObjects.stream().map((model) -> ScientificObjectNodeDTO.getDTOFromModel(model, mapGeo.get(SPARQLDeserializers.getExpandedURI(model.getUri())))).collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("used_types")
    @ApiOperation("get used scientific object types")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return scientific object types list", response = ListItemDTO.class, responseContainer = "List")
    })
    public Response getUsedTypes(
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") @ValidURI URI experimentURI
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);

        List<SPARQLNamedResourceModel<ScientificObjectModel>> typesModel = logic.getUsedTypes(experimentURI, currentUser);

        //TODO: error if empty??
        List<ListItemDTO> types = new ArrayList<>();

        typesModel.forEach(type -> {
            ListItemDTO listItem = new ListItemDTO();
            listItem.setUri(type.getUri());
            listItem.setName(type.getName());
            types.add(listItem);
        });

        return new PaginatedListResponse<>(types).getResponse();
    }

    @GET
    @Path("geometry")
    @ApiOperation("Get scientific objet list with geometry of a given experiment URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return list of scientific objects whose geometry corresponds to the given experiment URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response searchScientificObjectsWithGeometryListByUris(
            @ApiParam(value = "Context URI", example = "http://example.com/", required = true) @QueryParam("experiment") @NotNull URI contextURI,
            @ApiParam(value = "Search by minimal date", example = "2020-08-21") @QueryParam("start_date") @Date(DateFormat.YMD) String startDate,
            @ApiParam(value = "Search by maximal date", example = "2020-08-22") @QueryParam("end_date") @Date(DateFormat.YMD) String endDate
    ) throws Exception {
        //TODO !!!
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        ScientificObjectDAO soDAO = new ScientificObjectDAO(sparql, nosql, fs);
        List<ScientificObjectNodeDTO> dtoMapGeo = new ArrayList<>();
        List<ScientificObjectNodeDTO> dtoList =new ArrayList<>();
        int lengthMapGeo = 0;

        // Get SO with geometry for the experiment
        validateContextAccess(contextURI);

        Instant test_start = Instant.now();
        FindIterable<GeospatialModel> mapGeo = geoDAO.getGeometryByGraphList(contextURI);
        Instant test_end = Instant.now();

        // Filter OS by date and get OS details ( uri, name, rdfType, rdfTypeLabel, destruction date, creation date)
        for (GeospatialModel geospatialModel : mapGeo) {
            dtoMapGeo.add(ScientificObjectNodeDTO.getDTOFromModel(geospatialModel));
            lengthMapGeo++;
        }

        LOGGER.debug(lengthMapGeo + " space entities recovered " + Duration.between(test_start, test_end).toMillis() + " milliseconds elapsed");

        if(lengthMapGeo == 0){
            return new PaginatedListResponse<>(dtoList).getResponse();
        } else {
            dtoList = soDAO.getScientificObjectsByDate(contextURI, startDate, endDate, currentUser.getLanguage(), dtoMapGeo.stream().map(ScientificObjectNodeDTO::getUri).collect(Collectors.toList()));
            //Use a temporary list to delete objects already found and reduce the size of the list to be iterated.
            List<ScientificObjectNodeDTO> dtoMapGeoTmp = new ArrayList<>(dtoMapGeo);
            // Set the geometry coming from MongoDB in the corresponding SO of RDF4J
            for(ScientificObjectNodeDTO dto : dtoList){
                dto.setGeometry(dtoMapGeoTmp.stream().filter(o -> SPARQLDeserializers.compareURIs(o.getUri(),dto.getUri())).findAny().orElseThrow(NullPointerException::new).getGeometry());
                dtoMapGeoTmp.removeIf(g -> SPARQLDeserializers.compareURIs(g.getUri(),dto.getUri()));
            }
            return new PaginatedListResponse<>(dtoList).getResponse();
        }
    }

    @GET
    @Path("children")
    @ApiOperation("Get list of scientific object children")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return list of scientific objects children corresponding to the parent URI", response = ScientificObjectNodeWithChildrenDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsChildren(
            @ApiParam(value = "Parent object URI", example = SCIENTIFIC_OBJECT_EXAMPLE_URI) @QueryParam("parent") URI parentURI,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") @ValidURI URI experimentURI,
            @ApiParam(value = "RDF type filter", example = "vocabulary:Plant") @QueryParam("rdf_types") @ValidURI List<URI> rdfTypes,
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String pattern,
            @ApiParam(value = "Factor levels URI", example = "vocabulary:IrrigationStress") @QueryParam("factor_levels") @ValidURI List<URI> factorLevels,
            @ApiParam(value = "Facility", example = "diaphen:serre-2") @QueryParam("facility") @ValidURI URI facility,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);

        ScientificObjectSearchFilter searchFilter = new ScientificObjectSearchFilter()
                .setExperiment(experimentURI)
                .setPattern(pattern)
                .setRdfTypes(rdfTypes)
                .setParentURI(parentURI)
                .setFactorLevels(factorLevels)
                .setFacility(facility);

        searchFilter.setPage(page)
                .setPageSize(pageSize)
                .setOrderByList(orderByList)
                .setLang(currentUser.getLanguage());

        ListWithPagination<ScientificObjectNodeWithChildrenDTO> dtoList = logic.searchChildren(searchFilter,currentUser);

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @ApiOperation("Search list of scientific objects")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return scientific objects corresponding to the given search parameters", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response searchScientificObjects(
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") final URI contextURI,
            @ApiParam(value = "RDF type filter", example = "vocabulary:Plant") @QueryParam("rdf_types") @ValidURI List<URI> rdfTypes,
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String pattern,
            @ApiParam(value = "Parent URI", example = SCIENTIFIC_OBJECT_EXAMPLE_URI) @QueryParam("parent") @ValidURI URI parentURI,
            @ApiParam(value = "Germplasm URIs", example = "http://aims.fao.org/aos/agrovoc/c_1066") @QueryParam("germplasms") @ValidURI List<URI> germplasms,
            @ApiParam(value = "Factor levels URI", example = "vocabulary:IrrigationStress") @QueryParam("factor_levels") @ValidURI List<URI> factorLevels,
            @ApiParam(value = "Facility", example = "diaphen:serre-2") @QueryParam("facility") @ValidURI URI facility,
            @ApiParam(value = "Variables URI") @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Devices URI") @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Date to filter object existence") @QueryParam("existence_date") LocalDate existenceDate,
            @ApiParam(value = "Date to filter object creation") @QueryParam("creation_date") LocalDate creationDate,
            @ApiParam(value = "A CriteriaDTO to be applied to data, retain objects that are targets in returned data") @QueryParam("criteria_on_data") @Valid CriteriaDTO criteriaDTO,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);
        ScientificObjectSearchFilter searchFilter = new ScientificObjectSearchFilter();

        searchFilter.setExperiment(contextURI)
                .setPattern(pattern)
                .setRdfTypes(rdfTypes)
                .setParentURI(parentURI)
                .setGermplasm(germplasms)
                .setFactorLevels(factorLevels)
                .setFacility(facility)
                .setExistenceDate(existenceDate)
                .setCreationDate(creationDate);

        searchFilter.setPage(page)
                .setPageSize(pageSize)
                .setOrderByList(orderByList)
                .setLang(currentUser.getLanguage());

        ListWithPagination<ScientificObjectNodeDTO> dtoList = logic.searchScientificObjects(
                searchFilter,
                criteriaDTO,
                variables,
                devices,
                currentUser
        );
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get scientific object detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return scientific object details corresponding to the given object URI", response = ScientificObjectDetailDTO.class)
    })
    public Response getScientificObjectDetail(
            @ApiParam(value = "scientific object URI", example = "http://opensilex.org/set/scientific-objects/so-1357dz_pg_34zm4384wwveg_323_37arch2017-03-30", required = true)
            @PathParam("uri") @ValidURI @NotNull URI objectURI,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = "http://opensilex.org/set/experiments/21ik1_cims-on")
            @QueryParam("experiment") @ValidURI URI contextURI
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);

        ScientificObjectModel model = logic.getObjectByURI(objectURI, contextURI, currentUser);

        //TODO: new location model
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        MoveLogic moveLogic = new MoveLogic(sparql, nosql, currentUser);
        MoveModel lastMove = moveLogic.getLastMoveEvent(objectURI);

        GeospatialModel geometryByURI = geoDAO.getGeometryByURI(objectURI, contextURI);

        if (Objects.isNull(model)) {
            throw new NotFoundURIException("Scientific object uri not found:", objectURI);
        }

        ScientificObjectDetailDTO dto = ScientificObjectDetailDTO.getDTOFromModel(model, geometryByURI, lastMove);

        if (Objects.nonNull(model.getPublisher())) {
            dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path("{uri}/experiments")
    @ApiOperation("Get scientific object detail for each experiments, a null value for experiment in response means a properties defined outside of any experiment (shared object).")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return scientific object details corresponding to the given experiment and object URI", response = ScientificObjectDetailByExperimentsDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectDetailByExperiments(
            @ApiParam(value = "scientific object URI", example = SCIENTIFIC_OBJECT_EXAMPLE_URI, required = true)
            @PathParam("uri") @ValidURI @NotNull URI objectURI
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);

        //TODO: new model Location
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        MoveLogic moveLogic = new MoveLogic(sparql, nosql, currentUser);

        List<ScientificObjectDetailByExperimentsDTO> dtoList = new ArrayList<>();

        MoveModel lastMove = moveLogic.getLastMoveEvent(objectURI);

        Map<ScientificObjectModel, ExperimentModel> modelsMap = logic.getScientificObjectDetailByExperiments(objectURI, currentUser);

        modelsMap.forEach((model, experiment) -> {
            GeospatialModel geometryByURI = geoDAO.getGeometryByURI(objectURI, experiment.getUri());
            ScientificObjectDetailByExperimentsDTO dto;
            try {
                dto = ScientificObjectDetailByExperimentsDTO.getDTOFromModel(model, experiment, geometryByURI, lastMove);

                if (Objects.nonNull(model.getPublisher())) {
                    dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            dtoList.add(dto);
        });

        if (dtoList.size() == 0) {
            throw new NotFoundURIException("Scientific object uri not found:", objectURI);
        } else {
            return new PaginatedListResponse<>(dtoList).getResponse();
        }
    }

    @POST
    @ApiOperation("Create a scientific object for the given experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a scientific object", response = URI.class),
            @ApiResponse(code = 409, message = "A scientific object with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createScientificObject(
            @ApiParam(value = "Scientific object description", required = true)
            @NotNull
            @Valid ScientificObjectCreationDTO scientificObjectDto
    ) throws Exception {
        ScientificObjectLogic soLogic = new ScientificObjectLogic(sparql, nosql, fs);
        ScientificObjectModel soModel = scientificObjectDto.newModel();

        try {
            URI soURI = soLogic.createScientificObject(soModel,scientificObjectDto.getExperiment(),scientificObjectDto.getRelations(), currentUser);

            return new CreatedUriResponse(soURI).getResponse();
        } catch (MongoWriteException | CodecConfigurationException mongoException) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
        } catch (DuplicateNameException e){
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception ex) {
            throw ex;
        }
    }

    @PUT
    @ApiOperation("Update a scientific object for the given experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Scientific object updated", response = URI.class)
    })
    public Response updateScientificObject(
            @ApiParam(value = "Scientific object description", required = true)
            @NotNull
            @Valid ScientificObjectUpdateDTO scientificObjectDto
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);
        ScientificObjectModel soModel = scientificObjectDto.newModel();

        try {
            URI soURI = logic.updateScientificObject(soModel, scientificObjectDto.getExperiment(), scientificObjectDto.getRelations(), scientificObjectDto.getPublisher(), scientificObjectDto.getPublicationDate(), currentUser);

            return new ObjectUriResponse(soURI).getResponse();
        } catch (DuplicateNameException e) {
            throw new BadRequestException(e.getMessage());
        } catch (Exception ex) {
            throw ex;
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a scientific object")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Scientific object deleted", response = URI.class),
            @ApiResponse(code = 400, message = DELETE_ERROR_TITLE+ " (If object is involved into an experiment or if associated to any data)", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Scientific object URI not found", response = ErrorResponse.class)
    })
    public Response deleteScientificObject(
            @ApiParam(value = "scientific object URI", example = SCIENTIFIC_OBJECT_EXAMPLE_URI, required = true)
            @PathParam("uri") @ValidURI @NotNull URI objectURI,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI)
            @QueryParam("experiment") @ValidURI URI contextURI
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);

        try {
            logic.deleteScientificObject(contextURI, objectURI, currentUser);

            return new ObjectUriResponse(Response.Status.OK, objectURI).getResponse();
        } catch (DisplayableResponseException ex) {
            return ex.getResponse();
        }
    }

    @POST
    @Path("import")
    @ApiOperation(value = "Import a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Data file and metadata saved", response = CSVValidationDTO.class)
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSV(
            @ApiParam(value = "File description with metadata", required = true, type = "string")
            @NotNull
            @Valid
            @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file")
            @NotNull
            @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        //TODO: pas tucheuu?

        try {
            sparql.startTransaction();
            nosql.startTransaction();

            CsvImporter<ScientificObjectModel> csvImporter = new CachedCsvImporter<>(
                    new ScientificObjectCsvImporter(sparql, nosql, fs, descriptionDto.getExperiment(), currentUser),
                    descriptionDto.getValidationToken()
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
    @Path("export_geospatial")
    @ApiOperation("Export a given list of scientific object URIs to shapefile or geojson")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Data exported")
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportGeospatial(
            @ApiParam(value = "Scientific objects") List<GeometryDTO> selectedObjects,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") URI contextURI,
            @ApiParam(value = "properties selected", example = "test") @QueryParam("selected_props") List<URI> selectedProps,
            @ApiParam(value = "export format (shp/geojson)", example = "shp") @QueryParam("format") String format,
            @ApiParam(value = "Page size limited to 10,000 objects", example = "10000") @QueryParam("pageSize") @Max(10000) int pageSize

    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);
        Map<String, byte[]> result = logic.exportFromMapScientificObjects(selectedObjects, selectedProps, contextURI, format, currentUser);

        return Response.ok(result.entrySet().stream().findFirst().get().getValue(), MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + result.entrySet().stream().findFirst().get().getValue() + "\"")
                .build();
    }

    @POST
    @Path("export")
    @ApiOperation("Export a given list of scientific object URIs to csv data file")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Data file exported")
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportCSV(
            @ApiParam("CSV export configuration") @Valid ScientificObjectExportDTO searchFilter
    ) throws Exception {
        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);

        byte[] content = logic.exportScientificObjects(searchFilter, currentUser);

        String csvName = "scientific-object-export.csv";
        return Response.ok(content, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + csvName + "\"")
                .build();
    }

    @POST
    @Path("import_validation")
    @ApiOperation(value = "Validate a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CSV validation errors or a validation token used for CSV import", response = CSVValidationDTO.class)
    })
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @ApiParam(value = "File description with metadata", required = true, type = "string")
            @NotNull
            @Valid
            @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file")
            @NotNull
            @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {
//TODO: pas toucheou
        CsvImporter<ScientificObjectModel> csvImporter = new CachedCsvImporter<>(
                new ScientificObjectCsvImporter(sparql, nosql, fs, descriptionDto.getExperiment(), currentUser),
                descriptionDto.getValidationToken()
        );

        CSVValidationModel validationModel = csvImporter.importCSV(file, true);
        return new SingleObjectResponse<>(new CSVValidationDTO(validationModel)).getResponse();
    }

    /**
     *
     * @param uri
     * @return
     * @throws Exception
     * @deprecated better use directly the service GET data/variables with the parameters objects
     */
    @Deprecated
    @GET
    @Path("{uri}/variables")
    @ApiOperation("Get variables measured on this scientific object")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return variables list", response = NamedResourceDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectVariables(
            @ApiParam(value = "Scientific Object URI", example = SCIENTIFIC_OBJECT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, currentUser);
        List<VariableModel> variables = dataLogic.getUsedVariables(null, Arrays.asList(uri), null, null);
        List<NamedResourceDTO> dtoList = variables.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();

}

    /**
     *
     * @param uri
     * @return
     * @throws Exception
     * @deprecated better use directly the service GET data/provenances with the parameters objects
     */
    @Deprecated
    @GET
    @Path("{uri}/data/provenances")
    @ApiOperation("Get provenances of data that have been measured on this scientific object")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectDataProvenances(
            @ApiParam(value = "Scientific Object URI", example = SCIENTIFIC_OBJECT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        List<ProvenanceModel> provenances = dataDAO.getProvenancesByScientificObject(currentUser, uri, DataDAO.DATA_COLLECTION_NAME);
        List<ProvenanceGetDTO> dtoList = provenances.stream().map(ProvenanceGetDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    /**
     *
     * @param uri
     * @return
     * @throws Exception
     * @deprecated better use directly the service GET datafiles/provenances with the parameters objects
     */
    @Deprecated
    @GET
    @Path("{uri}/datafiles/provenances")
    @ApiOperation("Get provenances of datafiles linked to this scientific object")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectDataFilesProvenances(
            @ApiParam(value = "Scientific Object URI", example = SCIENTIFIC_OBJECT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        List<ProvenanceModel> provenances = dataDAO.getProvenancesByScientificObject(currentUser, uri, DataDAO.FILE_COLLECTION_NAME);
        List<ProvenanceGetDTO> dtoList = provenances.stream().map(ProvenanceGetDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("count")
    @ApiOperation("Count scientific objects")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the number of scientific objects associated to a given experiment", response = Integer.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response countScientificObjects(
            @ApiParam(value = "Experiment URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("experiment") URI experiment) throws Exception {

        ScientificObjectLogic logic = new ScientificObjectLogic(sparql, nosql, fs);

               ScientificObjectSearchFilter searchFilter = new ScientificObjectSearchFilter()
                .setExperiment(experiment);

        int scientificObjectsCount = logic.getCount(searchFilter);

        return new SingleObjectResponse<>(scientificObjectsCount).getResponse();
    }
}
