//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.geojson.Geometry;
import io.swagger.annotations.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.geojson.GeoJsonObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.csv.api.CSVValidationDTO;
import org.opensilex.core.csv.dal.AbstractCsvDao;
import org.opensilex.core.event.dal.move.TargetPositionModel;
import org.opensilex.nosql.datasource.coordinator.DefaultDataSourceCoordinator;
import org.opensilex.nosql.datasource.coordinator.CompoundOperation;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameListException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


/**
 * @author Julien BONNEFONT
 */
@Api(ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID)
@Path("/core/scientific_objects")
@ApiCredentialGroup(
        groupId = ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID,
        groupLabelKey = ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_LABEL_KEY
)
public class ScientificObjectAPI {

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID = "Scientific Objects";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_LABEL_KEY = "credential-groups.scientific-objects";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID = "scientific-objects-modification";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID = "scientific-objects-delete";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_LABEL_KEY = "credential.default.delete";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScientificObjectAPI.class);

    public static final String INVALID_GEOMETRY = "Invalid geometry (longitude must be between -180 and 180 and latitude must be between -90 and 90, no self-intersection, ...)";

    public static final String SCIENTIFIC_OBJECT_EXAMPLE_URI = "http://opensilex.org/id/Plot 12";
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_TYPE = "vocabulary:Plot";
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_TYPE_NAME = "Plot";
    public static final String SCIENTIFIC_OBJECT_EXAMPLE_NAME = "Plot 12";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

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
        if (objectsURI == null) {
            objectsURI = new ArrayList<>();
        }
        validateContextAccess(contextURI);

        if (contextURI == null) {
            contextURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);
        List<ScientificObjectModel> scientificObjects = dao.searchByURIs(contextURI, objectsURI, currentUser);

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

        validateContextAccess(experimentURI);

        SelectBuilder select = new SelectBuilder();

        if (experimentURI != null) {
            Node context = SPARQLDeserializers.nodeURI(experimentURI);
            select.addGraph(context, "?uri", RDF.type, "?type");
        } else if (!currentUser.isAdmin()) {
            ExperimentDAO xpDO = new ExperimentDAO(sparql, nosql);
            Set<URI> graphFilterURIs = xpDO.getUserExperiments(currentUser);

            if (graphFilterURIs.isEmpty()) {
                return new PaginatedListResponse<>(new ArrayList<>()).getResponse();
            }

            select.addGraph("?g", "?uri", RDF.type, "?type");
            select.addFilter(SPARQLQueryHelper.inURIFilter("?g", graphFilterURIs));
        }

        select.addVar("?type ?label");
        select.setDistinct(true);
        select.addWhere("?type", Ontology.subClassStrict, Oeso.ScientificObject);
        select.addWhere("?type", RDFS.label, "?label");
        select.addFilter(SPARQLQueryHelper.langFilterWithDefault("label", currentUser.getLanguage()));

        List<ListItemDTO> types = new ArrayList<>();

        sparql.executeSelectQuery(select, (row) -> {
            try {
                URI uri = new URI(row.getStringValue("type"));
                String label = row.getStringValue("label");
                ListItemDTO listItem = new ListItemDTO();
                listItem.setUri(uri);
                listItem.setName(label);
                types.add(listItem);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
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

        validateContextAccess(contextURI);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        Instant test_start = Instant.now();
        FindIterable<GeospatialModel> mapGeo = geoDAO.getGeometryByGraphList(contextURI);
        Instant test_end = Instant.now();
        
        Collection<String> filteredUris = new HashSet<>(); 

        // Date
        if (startDate != null || endDate != null) {
            ScientificObjectDAO soDAO = new ScientificObjectDAO(sparql, nosql);
            Collection<URI> uris = new HashSet<>();
            
            for (GeospatialModel geospatialModel : mapGeo) {
                ScientificObjectNodeDTO dtoFromModel = ScientificObjectNodeDTO.getDTOFromModel(geospatialModel);
                URI uri = dtoFromModel.getUri();
                uris.add(uri);
            }
            filteredUris = soDAO.getScientificObjectsByDate(contextURI, startDate, endDate, uris);
        }

        List<ScientificObjectNodeDTO> dtoList = new ArrayList<>();
        int lengthMapGeo = 0;

        for (GeospatialModel geospatialModel : mapGeo) {
            if (startDate != null || endDate != null) {
                String uri = geospatialModel.getUri().toString();
                if (filteredUris.contains(uri) == true) {
                    ScientificObjectNodeDTO dtoFromModel = ScientificObjectNodeDTO.getDTOFromModel(geospatialModel);
                    dtoList.add(dtoFromModel);
                    lengthMapGeo++;
                }
            } else {
                ScientificObjectNodeDTO dtoFromModel = ScientificObjectNodeDTO.getDTOFromModel(geospatialModel);
                dtoList.add(dtoFromModel);
                lengthMapGeo++;
            }
        }

        LOGGER.debug(lengthMapGeo + " space entities recovered " + Duration.between(test_start, test_end).toMillis() + " milliseconds elapsed");
        return new PaginatedListResponse<>(dtoList).getResponse();
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

        validateContextAccess(experimentURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);
        if (experimentURI == null) {
            experimentURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }

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

        ListWithPagination<ScientificObjectNodeWithChildrenDTO> dtoList = dao.searchChildren(searchFilter);

        return new PaginatedListResponse<ScientificObjectNodeWithChildrenDTO>(dtoList).getResponse();
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
            @ApiParam(value = "Germplasm URI", example = "http://aims.fao.org/aos/agrovoc/c_1066") @QueryParam("germplasm") @ValidURI URI germplasm,
            @ApiParam(value = "Factor levels URI", example = "vocabulary:IrrigationStress") @QueryParam("factor_levels") @ValidURI List<URI> factorLevels,
            @ApiParam(value = "Facility", example = "diaphen:serre-2") @QueryParam("facility") @ValidURI URI facility,
            @ApiParam(value = "Date to filter object existence") @QueryParam("existence_date") LocalDate existenceDate,
            @ApiParam(value = "Date to filter object creation") @QueryParam("creation_date") LocalDate creationDate,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        if (contextURI != null) {
            if (sparql.uriExists(ExperimentModel.class, contextURI)) {
                ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
                xpDAO.validateExperimentAccess(contextURI, currentUser);
            } else {
                throw new NotFoundURIException("Experiment URI not found:", contextURI);
            }
        }

        ScientificObjectSearchFilter searchFilter = new ScientificObjectSearchFilter()
                .setExperiment(contextURI)
                .setPattern(pattern)
                .setRdfTypes(rdfTypes)
                .setParentURI(parentURI)
                .setGermplasm(germplasm)
                .setFactorLevels(factorLevels)
                .setFacility(facility)
                .setExistenceDate(existenceDate)
                .setCreationDate(creationDate);

        searchFilter.setPage(page)
                .setPageSize(pageSize)
                .setOrderByList(orderByList)
                .setLang(currentUser.getLanguage());

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);
        ListWithPagination<ScientificObjectNodeDTO> dtoList = dao.searchAsDto(searchFilter);
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

        validateContextAccess(contextURI);
        if (contextURI == null) {
            contextURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql,null);
        MoveModel lastMove = moveDAO.getLastMoveEvent(objectURI);

        ScientificObjectModel model = dao.getObjectByURI(objectURI, contextURI, currentUser.getLanguage());
        GeospatialModel geometryByURI = geoDAO.getGeometryByURI(objectURI, contextURI);

        if (model == null) {
            throw new NotFoundURIException("Scientific object uri not found:", objectURI);
        }

        return new SingleObjectResponse<>(ScientificObjectDetailDTO.getDTOFromModel(model, geometryByURI, lastMove)).getResponse();
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

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql,null);

        List<URI> contexts = dao.getObjectContexts(objectURI);

        List<ScientificObjectDetailByExperimentsDTO> dtoList = new ArrayList<>();

        MoveModel lastMove = moveDAO.getLastMoveEvent(objectURI);

        for (URI contextURI : contexts) {
            ExperimentModel experiment = getExperiment(contextURI);

            ScientificObjectModel model = dao.getObjectByURI(objectURI, contextURI, currentUser.getLanguage());
            GeospatialModel geometryByURI = geoDAO.getGeometryByURI(objectURI, contextURI);
            if (model != null) {
                ScientificObjectDetailByExperimentsDTO dto = ScientificObjectDetailByExperimentsDTO.getDTOFromModel(model, experiment, geometryByURI, lastMove);
                dtoList.add(dto);
            }
        }

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
            @ApiResponse(code = 201, message = "Create a scientific object", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A scientific object with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createScientificObject(
            @ApiParam(value = "Scientific object description", required = true)
            @NotNull
            @Valid ScientificObjectCreationDTO descriptionDto
    ) throws Exception {

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);

        validateContextAccess(descriptionDto.getExperiment());

        URI globalScientificObjectGraph = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        boolean hasExperiment = descriptionDto.getExperiment() != null;

        final URI contextURI = hasExperiment ? descriptionDto.getExperiment(): globalScientificObjectGraph;

        ExperimentModel experiment;
        if (hasExperiment) {
            experiment = experimentDAO.get(contextURI, currentUser);
            if(experiment == null){
                throw new NotFoundURIException("Unknown experiment",contextURI);
            }
        }else{
            experiment = null;
        }

        URI soType = descriptionDto.getType();

        // use AtomicReference in order to be able to use the ScientificObjectModel into lambda (for SPARQL and Mongo operations)
        AtomicReference<ScientificObjectModel> model = new AtomicReference<>();

        DefaultDataSourceCoordinator coordinator = new DefaultDataSourceCoordinator(sparql, nosql.startSession());

        // create object
        coordinator.addSparqlOperation(sparqlService -> {
                    ScientificObjectDAO dao = new ScientificObjectDAO(sparqlService, nosql);
                    model.set(dao.create(contextURI, experiment, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser));
        });

        // copy it into global OS graph
        coordinator.addSparqlOperation(sparqlService -> {
            Node graphNode = SPARQLDeserializers.nodeURI(globalScientificObjectGraph);
            if (hasExperiment && !sparql.uriExists(graphNode, model.get().getUri())) {
                UpdateBuilder update = new UpdateBuilder();
                Node soNode = SPARQLDeserializers.nodeURI(model.get().getUri());

                update.addInsert(graphNode, soNode, RDF.type, SPARQLDeserializers.nodeURI(soType));
                update.addInsert(graphNode, soNode, RDFS.label, descriptionDto.getName());
                sparql.executeUpdateQuery(update);
            }
        });

        // update xp species
        if (experiment != null) {
            coordinator.addSparqlOperation(sparqlService ->  experimentDAO.updateExperimentSpeciesFromScientificObjects(contextURI));
        }

        // set OS geometry
        if (descriptionDto.getGeometry() != null) {
            coordinator.addMongoOperation(session -> {
                GeospatialModel geospatialModel = new GeospatialModel(model.get(),contextURI,GeospatialDAO.geoJsonToGeometry(descriptionDto.getGeometry()));
                geoDAO.create(geospatialModel,session);
            });
        }

        coordinator.addMixedOperation(new CompoundOperation(
                coordinator,
                coordinator1 -> {
                    MoveModel facilityMoveEvent = new MoveModel();
                    if(ScientificObjectDAO.fillFacilityMoveEvent(facilityMoveEvent, model.get())){
                        // #TODO avoid cast
                        new MoveEventDAO(sparql, nosql, (DefaultDataSourceCoordinator) coordinator1).create(Collections.singletonList(facilityMoveEvent));
                    }
                },
                true
        ));

        coordinator.addSparqlOperation(sparqlService -> sparqlService.deletePrimitives(SPARQLDeserializers.nodeURI(contextURI), model.get().getUri(), Oeso.isHosted));

        coordinator.run();

        return new ObjectUriResponse(Response.Status.CREATED, model.get().getUri()).getResponse();

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
            @ApiResponse(code = 200, message = "Scientific object updated", response = ObjectUriResponse.class)
    })
    public Response updateScientificObject(
            @ApiParam(value = "Scientific object description", required = true)
            @NotNull
            @Valid ScientificObjectUpdateDTO dto
    ) throws Exception {

        validateContextAccess(dto.getExperiment());
        boolean hasExperiment = dto.getExperiment() != null;

        final URI contextURI = hasExperiment ? dto.getExperiment() : sparql.getDefaultGraphURI(ScientificObjectModel.class);

        URI type = dto.getType();
        URI uri = dto.getUri();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        // update model
        DefaultDataSourceCoordinator  coordinator = new DefaultDataSourceCoordinator(sparql, nosql.startSession());

        AtomicReference<ScientificObjectModel> modelReference = new AtomicReference<>();
        coordinator.addSparqlOperation(sparqlService -> {
            ScientificObjectModel model = dao.update(contextURI, type, dto.getUri(), dto.getName(), dto.getRelations(), currentUser);
            modelReference.set(model);
        });

        coordinator.addMixedOperation(new CompoundOperation(
                coordinator,
                coordinator1 -> updateAssociatedMoves(contextURI,modelReference.get(),coordinator),
                true
        ));

        // handle geometry, update geometry or delete old geometry
        coordinator.addMongoOperation(session -> {
            GeospatialDAO geoDAO = new GeospatialDAO(nosql);
            ScientificObjectModel model = modelReference.get();
            if(dto.getGeometry() != null){
                GeospatialModel geospatialModel = new GeospatialModel(model, contextURI, GeospatialDAO.geoJsonToGeometry(dto.getGeometry()));
                geoDAO.update(geospatialModel, model.getUri(), contextURI, session);
            }else{
                geoDAO.delete(model.getUri(), contextURI, session);
            }
        });

        // update experiment species
        if(hasExperiment){
            coordinator.addSparqlOperation(sparqlService -> new ExperimentDAO(sparqlService, nosql).updateExperimentSpeciesFromScientificObjects(contextURI));
        }

        coordinator.run();

        return new ObjectUriResponse(uri).getResponse();
    }

    void updateAssociatedMoves(URI graph, ScientificObjectModel model, DefaultDataSourceCoordinator coordinator) throws Exception {

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql,coordinator);

        // get last move
        MoveModel event = moveDAO.getLastMoveEvent(model.getUri());

        // check if the OS is hosted (used a facility)
        boolean hasFacilityURI = model.getRelations().stream().anyMatch(
                relation -> SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.isHosted.getURI()
        ));

        // OS is hosted within a facility, then create or update move event with the facility
        if (hasFacilityURI) {

            // existing position, then update the event which set the initial position of the OS
            if (event != null) {
                ScientificObjectDAO.fillFacilityMoveEvent(event, model);
                moveDAO.update(event);
            } else {
                // no position before, then juste create a new move
                event = new MoveModel();
                if (ScientificObjectDAO.fillFacilityMoveEvent(event, model)) {
                    moveDAO.create(Collections.singletonList(event));
                }
            }

        } else if (event != null) {
            // no facility/location, but the event can have a position (coordinates, custom or textual)

            // get other objects concerned by the move, except the current OS
            List<URI> newTargets = event.getTargets().stream()
                    .filter(item -> ! SPARQLDeserializers.compareURIs(item,model.getUri()))
                    .collect(Collectors.toList());

            // no facility and no other target concerned by a position, then juste delete the move
            if (newTargets.isEmpty()) {
                moveDAO.delete(event.getUri());
            } else {

                // update the move, by removing the current OS, since it has no location
                event.setTargets(newTargets);

                if (event.getNoSqlModel() != null) {
                    List<TargetPositionModel> newTargetsPositions = new ArrayList<>();
                    for (TargetPositionModel position : event.getNoSqlModel().getTargetPositions()) {
                        if (!SPARQLDeserializers.compareURIs(position.getTarget(), model.getUri())) {
                            newTargetsPositions.add(position);
                        }
                    }
                    event.getNoSqlModel().setTargetPositions(newTargetsPositions);
                }
                moveDAO.update(event);
            }
        }
        coordinator.addSparqlOperation(sparqlService ->  sparqlService.deletePrimitives(SPARQLDeserializers.nodeURI(graph), model.getUri(), Oeso.isHosted));
    }

    void updateAssociatedGeometry(URI graph, ScientificObjectModel model, GeoJsonObject geometry, DefaultDataSourceCoordinator coordinator){

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        // handle geometry, update geometry or delete old geometry
        if (geometry != null) {

            coordinator.addMongoOperation(session -> {
                GeospatialModel geospatialModel = new GeospatialModel(model, graph, GeospatialDAO.geoJsonToGeometry(geometry));
                geoDAO.update(geospatialModel, model.getUri(), graph, session);
            });
        } else {
            coordinator.addMongoOperation(session -> geoDAO.delete(model.getUri(), graph, session));
        }
    }


    private static final String DELETE_ERROR_TITLE ="Scientific object can't be deleted";

    /**
     * Name of the parameter used into translate-key about scientific object deletion error.
     * This key is related to message-en.yml and message-fr.yml translation files (located in opensilex-front)
     */
    private static final String DELETE_ERROR_KEY_PARAMETER ="scientific_object";

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
            @ApiResponse(code = 200, message = "Scientific object deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = DELETE_ERROR_TITLE+ " (If object is involved into an experiment or if associated to any data)", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Scientific object URI not found", response = ErrorResponse.class)
    })
    public Response deleteScientificObject(
            @ApiParam(value = "scientific object URI", example = SCIENTIFIC_OBJECT_EXAMPLE_URI, required = true)
            @PathParam("uri") @ValidURI @NotNull URI objectURI,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI)
            @QueryParam("experiment") @ValidURI URI contextURI
    ) throws Exception {

        validateContextAccess(contextURI);

        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        DataDAO dataDAO = new DataDAO(nosql,sparql,null);

        List<URI> xpList;
        List<URI> osList = Collections.singletonList(objectURI);
        boolean global = contextURI == null;

        if (global) {
            // global OS suppression -> ensure that the OS is not used into any experiment
            if (scientificObjectDAO.isInvolvedIntoAnyExperiment(osList.stream(), osList.size())) {
                throw new DisplayableBadRequestException(DELETE_ERROR_TITLE + " : object is used into an experiment",
                        "component.scientificObjects.error.delete.used-in-experiment",
                        Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
                );
            }
            // set empty list to pass to DataDao count and countFiles
            xpList = Collections.emptyList();
        } else {
            // check that the OS has no children
            if (scientificObjectDAO.hasChildren(contextURI, osList.stream(), osList.size())) {
                throw new DisplayableBadRequestException(DELETE_ERROR_TITLE + " : object has child",
                        "component.scientificObjects.error.delete.has-child",
                        Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
                );
            }
            // set list composed of the experiment, to pass to DataDao count and countFiles
            xpList = Collections.singletonList(contextURI);
        }

        // check that no data are associated (dao handle empty or not list)
        int dataCount = dataDAO.count(null, xpList, osList, null, null, null, null, null, null, null, null);
        if (dataCount > 0) {
            throw new DisplayableBadRequestException(DELETE_ERROR_TITLE + " : object has associated data",
                    "component.scientificObjects.error.delete.associated-data",
                    Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
            );
        }

        // check that no data file are associated (dao handle empty or not list)
        int dataFileCount = dataDAO.countFiles(null, null, xpList, osList, null, null, null, null, null);
        if (dataFileCount > 0) {
            throw new DisplayableBadRequestException(DELETE_ERROR_TITLE + " : object has associated data files",
                    "component.scientificObjects.error.delete.associated-data-files",
                    Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
            );
        }

        // use coordinator in order to auto handle transaction/rollback when dealing with SPARQL and MongoDB databases
        DefaultDataSourceCoordinator  coordinator = new DefaultDataSourceCoordinator(sparql, nosql.startSession())
                .addSparqlOperation(sparqlService -> scientificObjectDAO.delete(contextURI, objectURI))
                .addMongoOperation(clientSession -> geoDAO.delete(objectURI, contextURI, clientSession));

        // append operation on SPARQL : species update
        if (!global) {
            coordinator.addSparqlOperation(sparqlService -> experimentDAO.updateExperimentSpeciesFromScientificObjects(contextURI));
        }
        // run operations
        coordinator.run();

        return new ObjectUriResponse(Response.Status.OK, objectURI).getResponse();
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
        URI contextURI = descriptionDto.getExperiment();
        String validationToken = descriptionDto.getValidationToken();

        CSVValidationModel errors;
        if (validationToken == null) {
            errors = getCSVValidationModel(contextURI, file, currentUser);
        } else {
            errors = filesValidationCache.getIfPresent(validationToken);
            if (errors == null) {
                errors = getCSVValidationModel(contextURI, file, currentUser);
            } else {
                Map<String, Claim> claims = TokenGenerator.getTokenClaims(validationToken);
                contextURI = new URI(claims.get(CLAIM_CONTEXT_URI).asString());
            }
        }

        CSVValidationDTO csvValidation = new CSVValidationDTO();
        csvValidation.setErrors(errors);

        if (errors.hasErrors()) {
            return new SingleObjectResponse<>(csvValidation).getResponse();
        }

        // experiment management
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        URI globalGraph = sparql.getDefaultGraphURI(ScientificObjectModel.class);

        boolean insertIntoSomeXp = contextURI != null;
        final URI graphURI = insertIntoSomeXp ? contextURI : globalGraph;

        DefaultDataSourceCoordinator  coordinator = new DefaultDataSourceCoordinator(sparql, nosql.startSession());

        List<SPARQLNamedResourceModel> objects = errors.getObjects();

        // objects creation in SPARQL
        coordinator.addSparqlOperation(sparqlService -> sparql.create(SPARQLDeserializers.nodeURI(graphURI), objects));

        // os moves (MongoDB and SPARQL).
        // Pass the coordinator,because since moveDao also handle multiple transaction, then if the coordinator is provided, the dao will use this coordinator
        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql,coordinator);
        List<MoveModel> moves = new ArrayList<>();

        coordinator.addSparqlOperation(sparqlService -> {
            for (SPARQLNamedResourceModel<?> object : objects) {
                MoveModel facilityMoveEvent = new MoveModel();
                if (ScientificObjectDAO.fillFacilityMoveEvent(facilityMoveEvent, object)) {
                    moves.add(facilityMoveEvent);
                }
                sparqlService.deletePrimitives(SPARQLDeserializers.nodeURI(graphURI), object.getUri(), Oeso.isHosted);
            }
        });

        if(! moves.isEmpty()){
            moveDAO.create(moves);
        }

        // insert into an experiment -> copy OS type/label into global OS graph
        if(insertIntoSomeXp){
            coordinator.addSparqlOperation(sparqlService -> {
                UpdateBuilder update = new UpdateBuilder();
                boolean hasUpdateItem = false;
                Node graphNode = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(ScientificObjectModel.class));
                for (SPARQLNamedResourceModel object : objects) {
                    if (!sparql.uriExists(graphNode, object.getUri())) {
                        Node soNode = SPARQLDeserializers.nodeURI(object.getUri());
                        update.addInsert(graphNode, soNode, RDF.type, SPARQLDeserializers.nodeURI(object.getType()));
                        update.addInsert(graphNode, soNode, RDFS.label, object.getName());
                        hasUpdateItem = true;
                    }
                }

                if (hasUpdateItem) {
                    sparqlService.executeUpdateQuery(update);
                }
            });
        }

        // os species update
        if (insertIntoSomeXp) {
            coordinator.addSparqlOperation(sparqlService -> experimentDAO.updateExperimentSpeciesFromScientificObjects(graphURI));
        }

        // os geometry
        Map<Integer, Geometry> geometries = (Map<Integer, Geometry>) errors.getObjectsMetadata().get(Oeso.hasGeometry.toString());
        if(! MapUtils.isEmpty(geometries)){

            coordinator.addMongoOperation(session -> {

                List<GeospatialModel> geospatialModels = new ArrayList<>();
                geometries.forEach((rowIndex, geometry) -> {
                    SPARQLNamedResourceModel<?> object = objects.get(rowIndex - 1);
                    geospatialModels.add(new GeospatialModel(object,graphURI,geometry));
                });

                GeospatialDAO geoDAO = new GeospatialDAO(nosql);
                geoDAO.createAll(geospatialModels);
            });
        }

        coordinator.run();

        csvValidation.setNbLinesImported(errors.getObjects().size());
        csvValidation.setValidationToken(TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, Collections.emptyMap()));
        return new SingleObjectResponse<>(csvValidation).getResponse();
    }

    private void addDuplicateNameErrors(List<SPARQLNamedResourceModel> objects, CSVValidationModel validationModel, Map<String,URI> existingUriByName){

            CsvImporter<ScientificObjectModel> csvImporter = new CachedCsvImporter<>(
                    new ScientificObjectCsvImporter(sparql, nosql, descriptionDto.getExperiment(), currentUser),
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
    @Path("export")
    @ApiOperation("Export a given list of scientific object URIs to csv data file")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Data file exported")
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportCSV(
            @ApiParam("CSV export configuration") @Valid ScientificObjectSearchDTO dto
    ) throws Exception {

        validateContextAccess(dto.getExperiment());

        ScientificObjectDAO soDao = new ScientificObjectDAO(sparql, nosql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql,null);

        ScientificObjectSearchFilter searchFilter = dto.toModel();
        searchFilter.setLang(currentUser.getLanguage());

        // search objects according filtering and selected uris, fetch os factors
        ListWithPagination<ScientificObjectModel> objects = soDao.search(searchFilter, Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD));

        // compute URI list in order to call getGeometryByUris()
        List<URI> objectsUris;
        if (!CollectionUtils.isEmpty(dto.getUris())) {
            objectsUris = dto.getUris();
        } else {
            objectsUris = objects.getList().stream().map(ScientificObjectModel::getUri).collect(Collectors.toList());
        }

        // get geometry of objects
        HashMap<String, Geometry> geospatialMap = geoDAO.getGeometryByUris(dto.getExperiment(), objectsUris);

        // get last location of objects
        Map<URI, URI> arrivalFacilityByOs = moveDAO.getLastLocations(objectsUris.stream(), objects.getList().size());

        boolean hasExperiment = dto.getExperiment() != null;

        CsvExporter<ScientificObjectModel> csvExporter = new ScientificObjectCsvExporter(
                sparql,
                objects.getList(),
                dto.getExperiment(),
                currentUser.getLanguage(),
                arrivalFacilityByOs,
                geospatialMap
        );
        byte[] content = csvExporter.exportCSV();

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

        CsvImporter<ScientificObjectModel> csvImporter = new CachedCsvImporter<>(
                new ScientificObjectCsvImporter(sparql, nosql, descriptionDto.getExperiment(), currentUser),
                descriptionDto.getValidationToken()
        );

        CSVValidationModel validationModel = csvImporter.importCSV(file, true);
        return new SingleObjectResponse<>(new CSVValidationDTO(validationModel)).getResponse();
    }

    private void validateContextAccess(URI contextURI) throws Exception {
        if (contextURI == null) {
            // INFO :  no need to validate with no context defined
        } else if (sparql.uriExists(ExperimentModel.class, contextURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);

            xpDAO.validateExperimentAccess(contextURI, currentUser);
        } else {
            throw new NotFoundURIException("Experiment URI not found:", contextURI);
        }
    }

    private ExperimentModel getExperiment(URI experimentURI) throws Exception {
        if (sparql.uriExists(ExperimentModel.class, experimentURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);

            try {
                ExperimentModel xp = xpDAO.get(experimentURI, currentUser);
                return xp;
            } catch (Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Experiment URI claim key
     */
    private static final String CLAIM_CONTEXT_URI = "context";

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

        DataDAO dao = new DataDAO(nosql, sparql, null);
        List<VariableModel> variables = dao.getUsedVariables(currentUser, null, Arrays.asList(uri), null, null);
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

}
