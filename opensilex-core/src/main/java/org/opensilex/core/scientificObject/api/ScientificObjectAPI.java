//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

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
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geospatial.api.GeometryDTO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.geospatial.dal.ShapeFileExporter;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.*;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvImporter;
import org.opensilex.sparql.csv.export.CsvExporter;
import org.opensilex.sparql.csv.validation.CachedCsvImporter;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.response.CreatedUriResponse;
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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    AccountModel currentUser;

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
                .setGermplasm((germplasm!=null ? Collections.singletonList(germplasm) : null))
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

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);
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
        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);

        List<URI> contexts = dao.getObjectContexts(objectURI);

        List<ScientificObjectDetailByExperimentsDTO> dtoList = new ArrayList<>();

        MoveModel lastMove = moveDAO.getLastMoveEvent(objectURI);

        for (URI contextURI : contexts) {
            ExperimentModel experiment;
            URI globalScientificObjectGraph = new URI(SPARQLDeserializers.getShortURI(sparql.getDefaultGraphURI(ScientificObjectModel.class)));
            //assign the global uri "dev:set/scientific-object" when the OS is not linked to an experiment
           if(contextURI.equals(globalScientificObjectGraph)){
               experiment = new ExperimentModel();
               experiment.setUri(globalScientificObjectGraph);
            }
           else{
               experiment = getExperiment(contextURI);
           }

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
            @ApiResponse(code = 201, message = "Create a scientific object", response = URI.class),
            @ApiResponse(code = 409, message = "A scientific object with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createScientificObject(
            @ApiParam(value = "Scientific object description", required = true)
            @NotNull
            @Valid ScientificObjectCreationDTO descriptionDto
    ) throws Exception {

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);

        URI contextURI = descriptionDto.getExperiment();
        ExperimentModel experiment = null;
        validateContextAccess(contextURI);

        URI globalScientificObjectGraph = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        boolean globalCopy = false;
        if (contextURI == null) {
            contextURI = globalScientificObjectGraph;
        } else {
            globalCopy = true;
            experiment = experimentDAO.get(contextURI, currentUser);
            if(experiment == null){
                throw new NotFoundURIException("Unknown experiment",contextURI);
            }
        }

        URI soType = descriptionDto.getType();

        sparql.startTransaction();
        try {
            ScientificObjectModel model = dao.create(contextURI, experiment, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser);
            URI soURI = model.getUri();

            if (experiment != null) {
                experimentDAO.updateExperimentSpeciesFromScientificObjects(contextURI);
            }

            Node graphNode = SPARQLDeserializers.nodeURI(globalScientificObjectGraph);
            if (globalCopy && !sparql.uriExists(graphNode, soURI)) {
                dao.copyIntoGlobalGraph(Collections.singletonList(model));
            }

            if (descriptionDto.getGeometry() != null) {
                nosql.startTransaction();
                GeospatialModel geospatialModel = new GeospatialModel();
                geospatialModel.setUri(soURI);
                geospatialModel.setName(descriptionDto.getName());
                geospatialModel.setRdfType(soType);
                geospatialModel.setGraph(contextURI);
                geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(descriptionDto.getGeometry()));
                geoDAO.create(geospatialModel);
                nosql.commitTransaction();
            } else {
                nosql.rollbackTransaction();
            }

            sparql.commitTransaction();

            return new CreatedUriResponse(soURI).getResponse();
        } catch (MongoWriteException | CodecConfigurationException mongoException) {
            try {
                sparql.rollbackTransaction(mongoException);
                nosql.rollbackTransaction();
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
            }
            throw mongoException;
        } catch (DuplicateNameException e){
            throw new IllegalArgumentException(e.getMessage());
        }
        catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
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
            @Valid ScientificObjectUpdateDTO descriptionDto
    ) throws Exception {

        URI contextURI = descriptionDto.getExperiment();
        validateContextAccess(contextURI);
        boolean hasExperiment = contextURI != null;
        if (contextURI == null) {
            contextURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        }
        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);

        nosql.startTransaction();
        sparql.startTransaction();
        try {

            URI soURI = dao.update(contextURI, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser);

            if (hasExperiment) {
                experimentDAO.updateExperimentSpeciesFromScientificObjects(contextURI);
            }

            if (descriptionDto.getGeometry() != null) {
                GeospatialModel geospatialModel = new GeospatialModel();
                geospatialModel.setUri(soURI);
                geospatialModel.setName(descriptionDto.getName());
                geospatialModel.setRdfType(soType);
                geospatialModel.setGraph(contextURI);
                geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(descriptionDto.getGeometry()));
                geoDAO.update(geospatialModel, soURI, contextURI);
            } else {
                geoDAO.delete(soURI, contextURI);
            }

            sparql.commitTransaction();
            nosql.commitTransaction();

            return new ObjectUriResponse(soURI).getResponse();
        } catch (MongoWriteException | CodecConfigurationException mongoException) {
            try {
                sparql.rollbackTransaction(mongoException);
                nosql.rollbackTransaction();
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
            }
            throw mongoException;
        }catch (DuplicateNameException e){
            throw new IllegalArgumentException(e.getMessage());
        }
        catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw ex;
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

        validateContextAccess(contextURI);

        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        DataDAO dataDAO = new DataDAO(nosql,sparql,null);

        nosql.startTransaction();
        sparql.startTransaction();
        try {
            List<URI> xpList;
            List<URI> osList = Collections.singletonList(objectURI);
            boolean global = contextURI == null;

            if (global) {
                // global OS suppression -> ensure that the OS is not used into any experiment
                if (scientificObjectDAO.isInvolvedIntoAnyExperiment(osList.stream(), osList.size())) {
                    throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object is used into an experiment",
                            "component.scientificObjects.error.delete.used-in-experiment",
                            Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER,objectURI.toString())
                    );
                }
                // set empty list to pass to DataDao count and countFiles
                xpList = Collections.emptyList();
            }else{
                // check that the OS has no children
                if (scientificObjectDAO.hasChildren(contextURI, osList.stream(), osList.size())) {
                    throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object has child",
                            "component.scientificObjects.error.delete.has-child",
                            Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER,objectURI.toString())
                    );
                }
                // set list composed of the experiment, to pass to DataDao count and countFiles
                xpList = Collections.singletonList(contextURI);
            }

            // check that no data are associated (dao handle empty or not list)
            int dataCount = dataDAO.count(null,xpList, osList,null,null,null,null,null,null,null,null, null);
            if(dataCount > 0){
                throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object has associated data",
                        "component.scientificObjects.error.delete.associated-data",
                        Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
                );
            }

            // check that no data file are associated (dao handle empty or not list)
            int dataFileCount = dataDAO.countFiles(null,null,xpList,osList,null,null,null,null,null, null);
            if(dataFileCount > 0){
                throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object has associated data files",
                        "component.scientificObjects.error.delete.associated-data-files",
                        Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
                );
            }

            // delete OS and OS geometry (dao handle null or not null contextURI)
            scientificObjectDAO.delete(contextURI,objectURI);
            geoDAO.delete(objectURI, contextURI);

            if(!global){
                experimentDAO.updateExperimentSpeciesFromScientificObjects(contextURI);
            }

            sparql.commitTransaction();
            nosql.commitTransaction();

            return new ObjectUriResponse(Response.Status.OK, objectURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw ex;
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

        try {
            sparql.startTransaction();
            nosql.startTransaction();

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
    @Path("export_shp")
    @ApiOperation("Export a given list of scientific object URIs to shapefile")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Data shapefile exported")
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportShp(
            @ApiParam(value = "Scientific objects") List<GeometryDTO> selectedObjects,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") URI contextURI,
            @ApiParam(value = "properties selected", example = "test") @QueryParam("selected_props") List<URI> selectedProps,
            @ApiParam(value = "Page size limited to 10,000 objects", example = "10000") @QueryParam("pageSize") @Max(10000) int pageSize

    ) throws Exception {

        ScientificObjectDAO soDao = new ScientificObjectDAO(sparql, nosql);
        Map<URI, GeoJsonObject> selectedObjectsMap = new HashMap<>();

        //Get OS exported URI
        selectedObjects.forEach(o ->{
            selectedObjectsMap.put(o.getUri(), o.getGeometry());
        });

        // Search exported OS detail according the XP and selected uris, fetch os factors
        ScientificObjectSearchFilter searchFilter = new ScientificObjectSearchFilter()
                .setExperiment(contextURI)
                .setUris( new ArrayList<>(selectedObjectsMap.keySet()));

        searchFilter.setLang(currentUser.getLanguage())
                .setPageSize(10000)
                .setPage(0);

        List<ScientificObjectModel> objDetailList = soDao.search(searchFilter, Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD)).getList();

        //Convert to shapefiles and create a zip file to export
        ShapeFileExporter shpExport = new ShapeFileExporter();
        byte[] zippedFile =shpExport.shpExport(selectedProps, objDetailList, selectedObjectsMap);

        String zipName = "shpZip_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".zip" ;

        return Response.ok(zippedFile, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + zipName + "\"")
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

        validateContextAccess(searchFilter.getExperiment());

        ScientificObjectDAO soDao = new ScientificObjectDAO(sparql, nosql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);

        searchFilter.setLang(currentUser.getLanguage());

        // search objects according filtering and selected uris, fetch os factors
        ListWithPagination<ScientificObjectModel> objects = soDao.search(searchFilter, Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD));

        // compute URI list in order to call getGeometryByUris()
        List<URI> objectsUris;
        if (!CollectionUtils.isEmpty(searchFilter.getUris())) {
            objectsUris = searchFilter.getUris();
        } else {
            objectsUris = objects.getList().stream().map(ScientificObjectModel::getUri).collect(Collectors.toList());
        }

        // get geometry of objects
        HashMap<String, Geometry> geospatialMap = geoDAO.getGeometryByUris(searchFilter.getExperiment(), objectsUris);

        // get last location of objects
        Map<URI, URI> arrivalFacilityByOs = moveDAO.getLastLocations(objectsUris.stream(), objects.getList().size());

        CsvExporter<ScientificObjectModel> csvExporter = new ScientificObjectCsvExporter(
                sparql,
                objects.getList(),
                searchFilter.getExperiment(),
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
