//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

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
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLPartialTreeListModel;
import org.opensilex.sparql.response.PartialResourceTreeDTO;
import org.opensilex.sparql.response.PartialResourceTreeResponse;
import org.opensilex.sparql.service.SPARQLService;
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
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_LABEL_KEY = "credential.project.modification";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID = "scientific-objects-delete";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_LABEL_KEY = "credential.project.delete";

    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_READ_ID = "scientific-objects-read";
    public static final String CREDENTIAL_SCIENTIFIC_OBJECT_READ_LABEL_KEY = "credential.project.read";

    public static final int DEFAULT_CHILDREN_LIMIT = 10;
    public static final int DEFAULT_DEPTH_LIMIT = 3;

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    /**
     * @param experimentURI the experiment URI
     * @return Return list of scientific objetcs tree corresponding to the given experiment URI
     */
    @GET
    @Path("get-tree/{xpuri}")
    @ApiOperation("Get a project by URI")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_READ_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objetcs tree corresponding to the given experiment URI", response = PartialResourceTreeDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsTree(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpuri") @NotNull URI experimentURI
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        SPARQLPartialTreeListModel<ExperimentalObjectModel> tree = dao.searchTreeByExperiment(experimentURI, null, DEFAULT_CHILDREN_LIMIT, DEFAULT_DEPTH_LIMIT, currentUser);
        return new PartialResourceTreeResponse(PartialResourceTreeDTO.fromResourceTree(tree)).getResponse();
    }

    @GET
    @Path("get-children/{xpuri}/{parenturi}")
    @ApiOperation("Get list of scientific object children")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_READ_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of scientific objetcs children corresponding to the given experiment URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectsChildren(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpuri") @NotNull URI experimentURI,
            @ApiParam(value = "Parent object URI", example = "http://example.com/", required = true) @PathParam("parenturi") URI parentURI,
            @ApiParam(value = "Children limit", example = "20") @QueryParam("limit") Integer limit,
            @ApiParam(value = "Children offset", example = "0") @QueryParam("offset") Integer offset
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        List<ScientificObjectModel> sientificObjects = dao.searchChildrenByExperiment(experimentURI, parentURI, offset, limit, currentUser);
        List<ScientificObjectNodeDTO> dtoList = sientificObjects.stream().map(ScientificObjectNodeDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @GET
    @Path("get-detail/{xpuri}/{objuri}")
    @ApiOperation("Get scientific object detail")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_READ_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return scientific object details corresponding to the given experiment URI", response = ScientificObjectNodeDTO.class, responseContainer = "List")
    })
    public Response getScientificObjectDetail(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpuri") @NotNull URI experimentURI,
            @ApiParam(value = "scientific object URI", example = "http://example.com/", required = true) @PathParam("objuri") URI objectURI
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        ExperimentalObjectModel model = dao.getByURIAndExperiment(experimentURI, objectURI, currentUser);
        return new SingleObjectResponse<ScientificObjectDetailDTO>(ScientificObjectDetailDTO.getDTOFromModel(model)).getResponse();
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

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        URI soURI = dao.create(xpURI, soType, descriptionDto.getUri(), descriptionDto.getRelations(), currentUser);

        return new ObjectUriResponse(soURI).getResponse();
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
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        CSVValidationModel errors;
        if (validationToken == null) {
            errors = dao.validateCSV(xpURI, soType, file, currentUser);
        } else {
            errors = filesValidationCache.getIfPresent(validationToken);
            if (errors == null) {
                errors = dao.validateCSV(xpURI, soType, file, currentUser);
            }
        }

        CSVValidationDTO csvValidation = new CSVValidationDTO();

        csvValidation.setErrors(errors);

        if (!errors.hasErrors()) {
            sparql.create(SPARQLDeserializers.nodeURI(xpURI), errors.getObjects());
            csvValidation.setValidationToken(generateCSVValidationToken(xpURI, soType));
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
    @ApiCredential(
            credentialId = CREDENTIAL_SCIENTIFIC_OBJECT_READ_ID,
            credentialLabelKey = CREDENTIAL_SCIENTIFIC_OBJECT_READ_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @ApiParam(value = "File description with metadata", required = true, type = "string") @Valid @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        URI xpURI = descriptionDto.getExperiment();
        URI soType = descriptionDto.getType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

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
        additionalClaims.put(CLAIM_OBJECT_TYPE_URI, objectTypeURI);
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

    /**
     * Experiment URI claim key
     */
    private static final String CLAIM_OBJECT_TYPE_URI = "object_type_uri";

    /**
     * File Hash claim key
     */
    private static final String CLAIM_FILE_HASH_URI = "file_hash";

}
