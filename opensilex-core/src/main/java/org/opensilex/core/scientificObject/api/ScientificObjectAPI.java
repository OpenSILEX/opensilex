//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.geojson.Geometry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.util.ArrayList;
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
import org.apache.jena.ext.com.google.common.cache.Cache;
import org.apache.jena.ext.com.google.common.cache.CacheBuilder;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.api.CSVValidationDTO;
import org.opensilex.core.ontology.dal.CSVValidationModel;
import org.opensilex.core.scientificObject.dal.ExperimentalObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.TokenGenerator;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectURIGenerator;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.response.ListItemDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.utils.Ontology;

/**
 * @author Julien BONNEFONT
 */
@Api(ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID)
@Path("/core/scientific-object")
@ApiCredentialGroup(
        groupId = ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID,
        groupLabelKey = ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_LABEL_KEY
)
public class ScientificObjectAPI {

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID = "Scientific Objects";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_LABEL_KEY = "credential-groups.scientific-objects";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID = "scientific-objects-modification";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY = "credential.scientific-objects.modification";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID = "scientific-objects-delete";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_LABEL_KEY = "credential.scientific-objects.delete";

    public static final String GEOMETRY_COLUMN_ID = "geometry";
    public static final String INVALID_GEOMETRY = "Invalid geometry (longitude must be between -180 and 180 and latitude must be between -90 and 90, no self-intersection, ...)";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @POST
    @Path("get-by-uris/{contextURI}")
    @ApiOperation("Get scientific objet list of a given context (experiment or organization) URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objects corresponding to the given context URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsListByUris(
            @ApiParam(value = "Context URI", example = "http://example.com/", required = true) @PathParam("contextURI") @NotNull URI contextURI,
            @ApiParam(value = "Scientific object uris", required = true) List<URI> objectsURI
    ) throws Exception {

        validateContextAccess(contextURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        List<ScientificObjectModel> scientificObjects = dao.searchByURIs(contextURI, objectsURI, currentUser);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        HashMap<String, Geometry> mapGeo = geoDAO.getGeometryByUris(contextURI, objectsURI);
        List<ScientificObjectNodeDTO> dtoList = scientificObjects.stream().map((model) -> ScientificObjectNodeDTO.getDTOFromModel(model, mapGeo.get(SPARQLDeserializers.getExpandedURI(model.getUri())))).collect(Collectors.toList());

        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @GET
    @Path("get-used-types/{contextURI}")
    @ApiOperation("get all scientific object types associated to a given context")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return scientific object types list", response = ListItemDTO.class, responseContainer = "List")
    })
    public Response getUsedTypes(
            @ApiParam(value = "Context URI", example = "http://example.com/", required = true) @PathParam("contextURI") @NotNull URI contextURI
    ) throws Exception {
        validateContextAccess(contextURI);

        SelectBuilder select = new SelectBuilder();

        Node context = SPARQLDeserializers.nodeURI(contextURI);

        select.addVar("?type ?label");
        select.setDistinct(true);
        select.addGraph(context, "?uri", RDF.type, "?type");
        select.addWhere("?type", Ontology.subClassAny, Oeso.ScientificObject);
        select.addWhere("?type", RDFS.label, "?label");
        select.addFilter(SPARQLQueryHelper.langFilter("label", currentUser.getLanguage()));

        List<ListItemDTO> types = new ArrayList<>();

        sparql.executeSelectQuery(select, (row) -> {
            try {
                URI uri = new URI(row.getStringValue("type"));
                String label = row.getStringValue("label");
                ListItemDTO listItem = new ListItemDTO();
                listItem.setUri(uri);
                listItem.setLabel(label);
                types.add(listItem);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        return new PaginatedListResponse<>(types).getResponse();
    }

    @GET
    @Path("search-with-geometry/{contextURI}")
    @ApiOperation("Get scientific objet list with geometry of a given context (experiment or organization) URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objects whose geometry corresponds to the given context URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response searchScientificObjectsWithGeometryListByUris(
            @ApiParam(value = "Context URI", example = "http://example.com/", required = true) @PathParam("contextURI") @NotNull URI contextURI
    ) throws Exception {

        validateContextAccess(contextURI);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        HashMap<String, Geometry> mapGeo = geoDAO.getGeometryByGraph(contextURI);

        // retrieving the uri list with geometries in the experiment
        List<URI> objectsURI = new LinkedList<>();
        for (Map.Entry<String, Geometry> entry : mapGeo.entrySet()) {
            objectsURI.add(new URI(entry.getKey()));
        }

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        List<ScientificObjectModel> scientificObjects = dao.searchByURIs(contextURI, objectsURI, currentUser);

        List<ScientificObjectNodeDTO> dtoList = scientificObjects.stream().map((model) -> ScientificObjectNodeDTO.getDTOFromModel(model, mapGeo.get(SPARQLDeserializers.getExpandedURI(model.getUri())))).collect(Collectors.toList());

        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @GET
    @Path("get-children/{contextURI}")
    @ApiOperation("Get list of scientific object children of a given context (experiment or organization) URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objects children corresponding to the given context URI", response = ScientificObjectNodeWithChildrenDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsChildren(
            @ApiParam(value = "Context URI", example = "http://example.com/", required = true) @PathParam("contextURI") @NotNull URI contextURI,
            @ApiParam(value = "Parent object URI", example = "http://example.com/") @QueryParam("parenturi") URI parentURI,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        validateContextAccess(contextURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        ListWithPagination<ScientificObjectModel> scientificObjects = dao.searchChildrenByContext(contextURI, parentURI, page, pageSize, currentUser);

        ListWithPagination<ScientificObjectNodeWithChildrenDTO> dtoList = scientificObjects.convert(ScientificObjectNodeWithChildrenDTO.class, ScientificObjectNodeWithChildrenDTO::getDTOFromModel);
        return new PaginatedListResponse<ScientificObjectNodeWithChildrenDTO>(dtoList).getResponse();
    }

    @GET
    @Path("search")
    @ApiOperation("Search list of scientific objects")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objects children corresponding to the given search parameters", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response searchScientificObjects(
            @ApiParam(value = "Context URI", example = "http://example.com/") @QueryParam("contextURI") final URI contextURI,
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "RDF type filter", example = "vocabulary:Plant") @QueryParam("rdfTypes") @ValidURI List<URI> rdfTypes,
            @ApiParam(value = "Parent URI", example = "http://example.com/") @QueryParam("parentURI") @ValidURI URI parentURI,
            @ApiParam(value = "Factor levels URI", example = "vocabulary:Plant") @QueryParam("factorLevels") @ValidURI List<URI> factorLevels,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);

        List<URI> contextURIs = new ArrayList<>();

        if (contextURI != null) {
            if (sparql.uriExists(ExperimentModel.class, contextURI)) {
                xpDAO.validateExperimentAccess(contextURI, currentUser);
                contextURIs = new ArrayList<>();
                contextURIs.add(contextURI);
            } else if (sparql.uriExists(InfrastructureModel.class, contextURI)) {

            }
        } else if (!currentUser.isAdmin()) {
            contextURIs.addAll(xpDAO.getUserExperiments(currentUser));
        }

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        ListWithPagination<ScientificObjectModel> scientificObjects = dao.search(contextURIs, pattern, rdfTypes, parentURI, factorLevels, page, pageSize, currentUser);

        ListWithPagination<ScientificObjectNodeDTO> dtoList = scientificObjects.convert(ScientificObjectNodeDTO.class, ScientificObjectNodeDTO::getDTOFromModel);

        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @GET
    @Path("get-detail/{contextURI}/{objURI}")
    @ApiOperation("Get scientific object detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return scientific object details corresponding to the given context and object URI", response = ScientificObjectDetailDTO.class)
    })
    public Response getScientificObjectDetail(
            @ApiParam(value = "Context URI", example = "http://example.com/", required = true)
            @PathParam("contextURI")
            @NotNull URI contextURI,
            @ApiParam(value = "scientific object URI", example = "http://example.com/", required = true)
            @PathParam("objURI") URI objectURI
    ) throws Exception {

        validateContextAccess(contextURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        ExperimentalObjectModel model = dao.geObjectByURI(objectURI, contextURI, currentUser);
        GeospatialModel geometryByURI = geoDAO.getGeometryByURI(objectURI, contextURI);

        Response response;
        if (model != null) {
            response = new SingleObjectResponse<>(ScientificObjectDetailDTO.getDTOFromModel(model, geometryByURI)).getResponse();
        } else {
            response = Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        return response;
    }

    @POST
    @Path("create")
    @ApiOperation("Create a scientific object for the given context")
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
            @Valid ScientificObjectDescriptionDTO descriptionDto
    ) throws Exception {

        URI contextURI = descriptionDto.getContext();
        validateContextAccess(descriptionDto.getContext());

        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        nosql.startTransaction();
        sparql.startTransaction();
        try {
            URI soURI = dao.create(contextURI, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser);

            if (descriptionDto.getGeometry() != null) {
                GeospatialModel geospatialModel = new GeospatialModel();
                geospatialModel.setUri(soURI);
                geospatialModel.setRdfType(soType);
                geospatialModel.setGraph(contextURI);
                geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(descriptionDto.getGeometry()));
                geoDAO.create(geospatialModel);
                nosql.commitTransaction();
            } else {
                nosql.rollbackTransaction();
            }

            sparql.commitTransaction();

            return new ObjectUriResponse(Response.Status.CREATED, soURI).getResponse();
        } catch (MongoWriteException | CodecConfigurationException mongoException) {
            try {
                sparql.rollbackTransaction(mongoException);
                nosql.rollbackTransaction();
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
            }
            throw mongoException;
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    @PUT
    @Path("update")
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
            @Valid ScientificObjectDescriptionDTO descriptionDto
    ) throws Exception {

        URI contextURI = descriptionDto.getContext();
        validateContextAccess(contextURI);
        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        nosql.startTransaction();
        sparql.startTransaction();
        try {

            URI soURI = dao.update(contextURI, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser);

            if (descriptionDto.getGeometry() != null) {
                GeospatialModel geospatialModel = new GeospatialModel();
                geospatialModel.setUri(soURI);
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
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    @DELETE
    @Path("delete/{contextURI}/{objURI}")
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
        @ApiResponse(code = 404, message = "Scientific object URI not found", response = ErrorResponse.class)
    })
    public Response deleteScientificObject(
            @ApiParam(value = "Context URI", example = "http://example.com/", required = true)
            @PathParam("contextURI")
            @NotNull
            @ValidURI URI contextURI,
            @ApiParam(value = "Scientific object URI", example = "http://example.com/", required = true)
            @PathParam("objURI")
            @NotNull
            @ValidURI URI objURI
    ) throws Exception {

        validateContextAccess(contextURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        nosql.startTransaction();
        sparql.startTransaction();
        try {
            dao.delete(contextURI, objURI, currentUser);
            geoDAO.delete(objURI, contextURI);

            sparql.commitTransaction();
            nosql.commitTransaction();

            return new ObjectUriResponse(Response.Status.OK, objURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    private static String generateCSVValidationToken(URI experimentURI) throws NoSuchAlgorithmException, IOException {
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put(CLAIM_CONTEXT_URI, experimentURI);
        return TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, additionalClaims);
    }

    @POST
    @Path("csv-import")
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
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {
        URI contextURI = descriptionDto.getContext();
        URI soType = descriptionDto.getType();
        String validationToken = descriptionDto.getValidationToken();

        CSVValidationModel errors;
        if (validationToken == null) {
            errors = getCSVValidationModel(contextURI, soType, file, currentUser);
        } else {
            errors = filesValidationCache.getIfPresent(validationToken);
            if (errors == null) {
                errors = getCSVValidationModel(contextURI, soType, file, currentUser);
            } else {
                Map<String, Claim> claims = TokenGenerator.getTokenClaims(validationToken);
                contextURI = new URI(claims.get(CLAIM_CONTEXT_URI).asString());
            }
        }

        CSVValidationDTO csvValidation = new CSVValidationDTO();

        csvValidation.setErrors(errors);

        final URI graphURI = contextURI;
        if (!errors.hasErrors()) {
            Map<Integer, Geometry> geometries = (Map<Integer, Geometry>) errors.getObjectsMetadata().get(GEOMETRY_COLUMN_ID);
            if (geometries != null && geometries.size() > 0) {
                GeospatialDAO geoDAO = new GeospatialDAO(nosql);

                nosql.startTransaction();
                sparql.startTransaction();
                try {
                    List<SPARQLResourceModel> objects = errors.getObjects();
                    sparql.create(SPARQLDeserializers.nodeURI(graphURI), objects);

                    List<GeospatialModel> geospatialModels = new ArrayList<>();
                    geometries.forEach((rowIndex, geometry) -> {
                        SPARQLResourceModel object = objects.get(rowIndex - 1);
                        GeospatialModel geospatialModel = new GeospatialModel();
                        geospatialModel.setUri(object.getUri());
                        geospatialModel.setRdfType(object.getType());
                        geospatialModel.setGraph(graphURI);
                        geospatialModel.setGeometry(geometry);
                        geospatialModels.add(geospatialModel);
                    });

                    geoDAO.createAll(geospatialModels);
                    sparql.commitTransaction();
                    nosql.commitTransaction();

                } catch (Exception ex) {
                    nosql.rollbackTransaction();
                    sparql.rollbackTransaction(ex);
                }
            } else {

                List<SPARQLResourceModel> objects = errors.getObjects();
                sparql.create(SPARQLDeserializers.nodeURI(graphURI), objects);
            }

            csvValidation.setValidationToken("done");
        }

        return new SingleObjectResponse<CSVValidationDTO>(csvValidation).getResponse();
    }

    @POST
    @Path("csv-validate")
    @ApiOperation(value = "Validate a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "CSV validation errors or a validation token used for CSV import", response = CSVValidationDTO.class)
    })
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @ApiParam(value = "File description with metadata", required = true, type = "string")
            @Valid
            @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file")
            @NotNull
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        URI contextURI = descriptionDto.getContext();
        URI soType = descriptionDto.getType();

        CSVValidationModel csvValidationModel = getCSVValidationModel(contextURI, soType, file, currentUser);

        CSVValidationDTO csvValidation = new CSVValidationDTO();
        csvValidation.setErrors(csvValidationModel);

        if (!csvValidationModel.hasErrors()) {
            csvValidation.setValidationToken(generateCSVValidationToken(contextURI));
            filesValidationCache.put(csvValidation.getValidationToken(), csvValidationModel);
        }

        return new SingleObjectResponse<CSVValidationDTO>(csvValidation).getResponse();
    }

    private CSVValidationModel getCSVValidationModel(URI contextURI, URI soType, InputStream file, UserModel currentUser) throws Exception {
        if (sparql.uriExists(ExperimentModel.class, contextURI)) {
            return getCSVExperimentValidationModel(contextURI, soType, file, currentUser);
        } else if (sparql.uriExists(InfrastructureModel.class, contextURI)) {
            return null;
        }

        return null;
    }

    private CSVValidationModel getCSVExperimentValidationModel(URI xpURI, URI soType, InputStream file, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        HashMap<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators = new HashMap<>();

        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpURI, currentUser.getLanguage());

        List<String> factorLevelURIs = new ArrayList<>();
        for (FactorModel factor : xp.getFactors()) {
            for (FactorLevelModel factorLevel : factor.getFactorLevels()) {
                factorLevelURIs.add(SPARQLDeserializers.getExpandedURI(factorLevel.getUri()));
            }
        }

        List<String> germplasmStringURIs = new ArrayList<>();
        List<URI> germplasmURIs = new ArrayList<>();
        List<SpeciesModel> species = xp.getSpecies();
        for (SpeciesModel germplasm : species) {
            germplasmStringURIs.add(SPARQLDeserializers.getExpandedURI(germplasm.getUri()));
            germplasmURIs.add(germplasm.getUri());
        }

        List<String> facilityStringURIs = new ArrayList<>();
        List<InfrastructureFacilityModel> facilities = xpDAO.getAvailableFacilities(xpURI, currentUser);
        for (InfrastructureFacilityModel facility : facilities) {
            facilityStringURIs.add(SPARQLDeserializers.getExpandedURI(facility.getUri()));
        }

        if (germplasmURIs.size() > 0) {
            GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
            List<URI> subSpecies = dao.getGermplasmURIsBySpecies(germplasmURIs, currentUser.getLanguage());
            for (URI germplasmURI : subSpecies) {
                germplasmStringURIs.add(SPARQLDeserializers.getExpandedURI(germplasmURI));
            }
        }

        customValidators.put(Oeso.hasFactorLevel.toString(), (cell, csvErrors) -> {
            try {
                String factorLevelURI = SPARQLDeserializers.getExpandedURI(new URI(cell.getValue()));
                if (!factorLevelURIs.contains(factorLevelURI)) {
                    csvErrors.addInvalidValueError(cell);
                }
            } catch (URISyntaxException ex) {
                csvErrors.addInvalidURIError(cell);
            }
        });

        customValidators.put(Oeso.hasGermplasm.toString(), (cell, csvErrors) -> {
            try {
                String germplasmURI = SPARQLDeserializers.getExpandedURI(new URI(cell.getValue()));
                if (!germplasmStringURIs.contains(germplasmURI)) {
                    csvErrors.addInvalidValueError(cell);
                }
            } catch (URISyntaxException ex) {
                csvErrors.addInvalidURIError(cell);
            }
        });

        customValidators.put(Oeso.hasFacility.toString(), (cell, csvErrors) -> {
            try {
                String facilityURI = SPARQLDeserializers.getExpandedURI(new URI(cell.getValue()));
                if (!facilityStringURIs.contains(facilityURI)) {
                    csvErrors.addInvalidValueError(cell);
                }
            } catch (URISyntaxException ex) {
                csvErrors.addInvalidURIError(cell);
            }
        });

        List<String> customColumns = new ArrayList<>();
        customColumns.add(GEOMETRY_COLUMN_ID);

        Map<Integer, Geometry> geometries = new HashMap<>();
        customValidators.put(GEOMETRY_COLUMN_ID, (cell, csvErrors) -> {
            String wktGeometry = cell.getValue();
            if (wktGeometry != null && !wktGeometry.isEmpty()) {
                try {
                    Geometry geometry = GeospatialDAO.wktToGeometry(wktGeometry);
                    geometries.put(cell.getRowIndex(), geometry);
                } catch (JsonProcessingException | ParseException ex) {
                    csvErrors.addInvalidValueError(cell);
                }
            }
        });

        CSVValidationModel validationResult = ontologyDAO.validateCSV(xpURI, soType, new URI(Oeso.ScientificObject.getURI()), file, currentUser, customValidators, customColumns, new ScientificObjectURIGenerator(xpURI));

        validationResult.addObjectMetadata(GEOMETRY_COLUMN_ID, geometries);

        return validationResult;
    }

    private void validateContextAccess(URI contextURI) throws Exception {
        if (sparql.uriExists(ExperimentModel.class, contextURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql);

            xpDAO.validateExperimentAccess(contextURI, currentUser);
        } else if (sparql.uriExists(InfrastructureModel.class, contextURI)) {

        }

    }

    @GET
    @Path("csv-export")
    @ApiOperation(value = "Export a CSV file for the given context URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = CSVValidationDTO.class)
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportCSV(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true)
            @QueryParam("contextURI")
            @ValidURI
            @NotNull URI contextURI,
            @ApiParam(value = "RDF type filter", example = "vocabulary:Plant", required = true)
            @QueryParam("rdfType")
            @ValidURI
            @NotNull URI rdfType,
            @ApiParam(value = "Parent URI", example = "http://example.com/")
            @QueryParam("parentURI")
            @ValidURI URI parentURI
    ) throws Exception {

        validateContextAccess(contextURI);

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        List<ScientificObjectModel> objects = dao.searchAll(contextURI, rdfType, parentURI, currentUser);

        Map<String, GeospatialModel> geospatialMap = new HashMap<>();

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        List<String> customColumns = new ArrayList<>();
        customColumns.add(GEOMETRY_COLUMN_ID);

        BiFunction<String, SPARQLResourceModel, String> customValueGenerator = (columnID, value) -> {
            if (columnID.equals(GEOMETRY_COLUMN_ID) && value != null) {
                String uriString = SPARQLDeserializers.getExpandedURI(value.getUri());
                if (geospatialMap.containsKey(uriString)) {
                    GeospatialModel geoModel = geospatialMap.get(uriString);
                    try {
                        return GeospatialDAO.geometryToWkt(geoModel.getGeometry());
                    } catch (JsonProcessingException | ParseException ex) {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        };
        File csvFile = ontologyDAO.exportCSV(contextURI, rdfType, new URI(Oeso.ScientificObject.getURI()), objects, currentUser, customValueGenerator, customColumns);

        byte[] csvContent = FileUtils.readFileToByteArray(csvFile);

        String csvName = "scientific-object-export.csv";
        return Response.ok(csvContent, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + csvName + "\"")
                .build();
    }

    private static Cache<String, CSVValidationModel> filesValidationCache;

    static {
        filesValidationCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Experiment URI claim key
     */
    private static final String CLAIM_CONTEXT_URI = "context";

}
