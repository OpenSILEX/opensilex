//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import com.mongodb.client.ClientSession;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import com.auth0.jwt.interfaces.Claim;
import com.mongodb.client.MongoClient;
import org.opensilex.core.ontology.api.CSVValidationDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.jena.ext.com.google.common.cache.Cache;
import org.apache.jena.ext.com.google.common.cache.CacheBuilder;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.ontology.dal.CSVValidationModel;
import org.opensilex.core.scientificObject.dal.ExperimentalObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.nosql.service.NoSQLService;
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
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.TokenGenerator;

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

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private NoSQLService nosql;

    @POST
    @Path("get-by-uris/{xpuri}")
    @ApiOperation("Get scientific objet list for an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objetcs corresponding to the given experiment URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsListByUris(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpuri") @NotNull URI experimentURI,
            @ApiParam(value = "Scientific object uris", required = true) List<URI> objectsURI
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);
        List<ScientificObjectModel> sientificObjects = dao.searchByURIs(experimentURI, objectsURI, currentUser);
        List<ScientificObjectNodeDTO> dtoList = sientificObjects.stream().map(ScientificObjectNodeDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @GET
    @Path("get-children/{xpuri}")
    @ApiOperation("Get list of scientific object children")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objetcs children corresponding to the given experiment URI", response = ScientificObjectNodeWithChildrenDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsChildren(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpuri") @NotNull URI experimentURI,
            @ApiParam(value = "Parent object URI", example = "http://example.com/") @QueryParam("parenturi") URI parentURI,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);
        ListWithPagination<ScientificObjectModel> scientificObjects = dao.searchChildrenByExperiment(experimentURI, parentURI, page, pageSize, currentUser);

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
        @ApiResponse(code = 200, message = "Return list of scientific objetcs children corresponding to the given experiment URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response searchScientificObjects(
            @ApiParam(value = "Experiment URI", example = "http://example.com/") @QueryParam("xpuri") URI experimentURI,
            @ApiParam(value = "Regex pattern for filtering by name", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "RDF type filter", example = "vocabulary:Plant") @QueryParam("rdfType") URI rdfType,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);
        ListWithPagination<ScientificObjectModel> scientificObjects = dao.search(experimentURI, pattern, rdfType, page, pageSize, currentUser);

        ListWithPagination<ScientificObjectNodeDTO> dtoList = scientificObjects.convert(ScientificObjectNodeDTO.class, ScientificObjectNodeDTO::getDTOFromModel);
        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @GET
    @Path("get-detail/{xpuri}/{objuri}")
    @ApiOperation("Get scientific object detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return scientific object details corresponding to the given experiment URI", response = ScientificObjectDetailDTO.class)
    })
    public Response getScientificObjectDetail(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpuri") @NotNull URI experimentURI,
            @ApiParam(value = "scientific object URI", example = "http://example.com/", required = true) @PathParam("objuri") URI objectURI
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        MongoClient mongoClient = nosql.getMongoDBClient();
        GeospatialDAO geoDAO = new GeospatialDAO(mongoClient);

        ExperimentalObjectModel model = dao.getByURIAndExperiment(experimentURI, objectURI, currentUser);
        GeospatialModel geometryByURI = geoDAO.getGeometryByURI(objectURI, experimentURI);

        Response response;
        if (model != null) {
            response = new SingleObjectResponse<>(ScientificObjectDetailDTO.getDTOFromModel(model, geometryByURI)).getResponse();
        } else {
            response = Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        mongoClient.close();
        return response;
    }

    @POST
    @Path("create")
    @ApiOperation("Create a scientific object for the given experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a project", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A scientific object with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createScientificObject(
            @ApiParam(value = "Scientific object description", required = true) @NotNull @Valid ScientificObjectDescriptionDTO descriptionDto
    ) throws Exception {

        URI xpURI = descriptionDto.getExperiment();
        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        MongoClient mongoClient = nosql.getMongoDBClient();
        GeospatialDAO geoDAO = new GeospatialDAO(mongoClient);

        ClientSession session = mongoClient.startSession();
        session.startTransaction();
        try {
            sparql.startTransaction();
            URI soURI = dao.create(xpURI, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser);

            if (descriptionDto.getGeometry() != null) {
                GeospatialModel geospatialModel = new GeospatialModel();
                geospatialModel.setUri(soURI);
                geospatialModel.setType(soType);
                geospatialModel.setGraph(descriptionDto.getExperiment());
                geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(descriptionDto.getGeometry()));
                geoDAO.create(geospatialModel);
                session.commitTransaction();
            } else {
                session.abortTransaction();
            }

            sparql.commitTransaction();

            return new ObjectUriResponse(Response.Status.CREATED, soURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
            session.abortTransaction();
            throw ex;
        } finally {
            mongoClient.close();
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
            @ApiParam(value = "Scientific object description", required = true) @NotNull @Valid ScientificObjectDescriptionDTO descriptionDto
    ) throws Exception {

        URI xpURI = descriptionDto.getExperiment();
        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        MongoClient mongoClient = nosql.getMongoDBClient();
        GeospatialDAO geoDAO = new GeospatialDAO(mongoClient);

        ClientSession session = mongoClient.startSession();
        session.startTransaction();
        try {
            sparql.startTransaction();
            URI soURI = dao.update(xpURI, soType, descriptionDto.getUri(), descriptionDto.getName(), descriptionDto.getRelations(), currentUser);

            if (descriptionDto.getGeometry() != null) {
                GeospatialModel geospatialModel = new GeospatialModel();
                geospatialModel.setUri(soURI);
                geospatialModel.setType(soType);
                geospatialModel.setGraph(descriptionDto.getExperiment());
                geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(descriptionDto.getGeometry()));
                geoDAO.update(geospatialModel, soURI, xpURI);
            } else {
                geoDAO.delete(soURI, xpURI);
            }

            sparql.commitTransaction();
            session.commitTransaction();

            return new ObjectUriResponse(soURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            session.abortTransaction();
            throw ex;
        } finally {
            mongoClient.close();
        }
    }

    @DELETE
    @Path("delete/{xpURI}/{objURI}")
    @ApiOperation("Delete a scientific object")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Infrastructure facility deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Infrastructure facility URI not found", response = ErrorResponse.class)
    })
    public Response deleteScientificObject(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpURI") @NotNull @ValidURI URI xpURI,
            @ApiParam(value = "Scientific object URI", example = "http://example.com/", required = true) @PathParam("objURI") @NotNull @ValidURI URI objURI
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        MongoClient mongoClient = nosql.getMongoDBClient();
        GeospatialDAO geoDAO = new GeospatialDAO(mongoClient);

        ClientSession session = mongoClient.startSession();
        session.startTransaction();
        try {
            sparql.startTransaction();
            dao.delete(xpURI, objURI, currentUser);
            geoDAO.delete(objURI, xpURI);

            sparql.commitTransaction();
            session.commitTransaction();

            return new ObjectUriResponse(Response.Status.OK, objURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            session.abortTransaction();
            throw ex;
        } finally {
            mongoClient.close();
        }
    }

    @POST
    @Path("csv-import")
    @ApiOperation(value = "Import a CSV file for the given experiement URI and scientific object type.")
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
            @ApiParam(value = "File description with metadata", required = true, type = "string") @NotNull @Valid @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {
        URI xpURI = descriptionDto.getExperiment();
        URI soType = descriptionDto.getType();
        String validationToken = descriptionDto.getValidationToken();
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        CSVValidationModel errors;
        if (validationToken == null) {
            errors = dao.validateCSV(xpURI, soType, file, currentUser);
        } else {
            errors = filesValidationCache.getIfPresent(validationToken);
            if (errors == null) {
                errors = dao.validateCSV(xpURI, soType, file, currentUser);
            } else {
                Map<String, Claim> claims = TokenGenerator.getTokenClaims(validationToken);
                xpURI = new URI(claims.get(CLAIM_EXPERIMENT_URI).asString());
            }
        }

        CSVValidationDTO csvValidation = new CSVValidationDTO();

        csvValidation.setErrors(errors);

        if (!errors.hasErrors()) {
            sparql.create(SPARQLDeserializers.nodeURI(xpURI), errors.getObjects());
            csvValidation.setValidationToken("done");
        }

        return new SingleObjectResponse<CSVValidationDTO>(csvValidation).getResponse();
    }

    @POST
    @Path("csv-validate")
    @ApiOperation(value = "Validate a CSV file for the given experiement URI and scientific object type.")
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
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        URI xpURI = descriptionDto.getExperiment();
        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql, nosql);

        CSVValidationModel errors = dao.validateCSV(xpURI, soType, file, currentUser);

        CSVValidationDTO csvValidation = new CSVValidationDTO();

        csvValidation.setErrors(errors);

        if (!errors.hasErrors()) {
            csvValidation.setValidationToken(generateCSVValidationToken(xpURI, soType));
            filesValidationCache.put(csvValidation.getValidationToken(), errors);
        }

        return new SingleObjectResponse<CSVValidationDTO>(csvValidation).getResponse();
    }

    private static String generateCSVValidationToken(URI experiementURI, URI objectTypeURI) throws NoSuchAlgorithmException, IOException {
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put(CLAIM_EXPERIMENT_URI, experiementURI);
        return TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, additionalClaims);
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
    private static final String CLAIM_EXPERIMENT_URI = "experiment_uri";

}
