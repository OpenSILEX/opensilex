//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.net.URI;
import java.util.List;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.ontology.dal.CSVValidationModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLPartialTreeListModel;
import org.opensilex.sparql.response.PartialResourceTreeDTO;
import org.opensilex.sparql.response.PartialResourceTreeResponse;
import org.opensilex.sparql.service.SPARQLService;

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

    public static final int DEFAULT_CHILDREN_LIMIT = 5;
    public static final int DEFAULT_DEPTH_LIMIT = 3;

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    /**
     * @param experimentURI the experiment URI
     * @return Return list of scientific objetcs tree corresponding to the given
     * experiment URI
     */
    @GET
    @Path("get-experiment-tree/{xpuri}")
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
    public Response getExperimentScientificObjectsTree(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpuri") @NotNull URI experimentURI
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        SPARQLPartialTreeListModel<ScientificObjectModel> tree = dao.searchTreeByExperiment(experimentURI, null, DEFAULT_CHILDREN_LIMIT, DEFAULT_DEPTH_LIMIT, currentUser);
        return new PartialResourceTreeResponse(PartialResourceTreeDTO.fromResourceTree(tree)).getResponse();
    }

    @GET
    @Path("get-experiment-children/{xpuri}/{parenturi}")
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
    public Response getExperimentScientificObjectsChildren(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("xpuri") @NotNull URI experimentURI,
            @ApiParam(value = "Parent object URI", example = "http://example.com/", required = true) @PathParam("parenturi") URI parentURI
    ) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        List<ScientificObjectModel> sientificObjects = dao.searchChildrenByExperiment(experimentURI, parentURI, currentUser);
        List<ScientificObjectNodeDTO> dtoList = sientificObjects.stream().map(ScientificObjectNodeDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<ScientificObjectNodeDTO>(dtoList).getResponse();
    }

    @POST
    @Path("csv-import")
    @ApiOperation(value = "Import a CSV file for the given experiement URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = Boolean.class)
    })
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSV(
            @ApiParam(value = "File description with metadata", required = true, type = "string") @NotNull @Valid @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        URI xpURI = descriptionDto.getExperimentURI();
        URI soType = descriptionDto.getScientificObjectType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        dao.importCSV(xpURI, soType, file, currentUser);
        // TODO import CSV
        return new SingleObjectResponse<Boolean>(true).getResponse();
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
            @ApiParam(value = "File description with metadata", required = true, type = "string") @NotNull @Valid @FormDataParam("description") ScientificObjectCsvDescriptionDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {

        URI xpURI = descriptionDto.getExperimentURI();
        URI soType = descriptionDto.getScientificObjectType();

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);

        CSVValidationModel errors = dao.validateCSV(xpURI, soType, file, currentUser);

        CSVValidationDTO csvValidation = new CSVValidationDTO();

        csvValidation.setErrors(errors);

        if (!errors.hasErrors()) {
            csvValidation.setValidationToken("VALIDATION TOKEN");
        }

        return new SingleObjectResponse<CSVValidationDTO>(csvValidation).getResponse();
    }

}
