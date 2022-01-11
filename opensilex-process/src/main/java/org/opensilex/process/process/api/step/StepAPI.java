//******************************************************************************
//                          StepAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.step.api;
import org.opensilex.process.process.dal.ProcessModel;
import org.opensilex.process.process.dal.StepModel;
import org.opensilex.process.process.dal.ProcessDAO;
import io.swagger.annotations.*;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.sql.Timestamp;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import java.io.*; 
import org.opensilex.security.authentication.NotFoundURIException;

/**
 * @author Fernandez Emilie
 */
@Api(StepAPI.CREDENTIAL_STEP_GROUP_ID)
@Path("/core/steps")
@ApiCredentialGroup(
        groupId = StepAPI.CREDENTIAL_STEP_GROUP_ID,
        groupLabelKey = StepAPI.CREDENTIAL_STEP_GROUP_LABEL_KEY
)
public class StepAPI {

    public static final String CREDENTIAL_STEP_GROUP_ID = "Steps";
    public static final String CREDENTIAL_STEP_GROUP_LABEL_KEY = "credential-groups.steps";

    public static final String CREDENTIAL_STEP_MODIFICATION_ID = "step-modification";
    public static final String CREDENTIAL_STEP_MODIFICATION_LABEL_KEY = "credential.step.modification";

    public static final String CREDENTIAL_STEP_DELETE_ID = "step-delete";
    public static final String CREDENTIAL_STEP_DELETE_LABEL_KEY = "credential.step.delete";

    protected static final String STEP_EXAMPLE_URI = "http://opendata.inrae.fr/ebo/set/step/s21";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

       /**
     * Create Step
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the created step {@link URI}
     */
    @POST
    @ApiOperation(value = "Add a step")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_STEP_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_STEP_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Step Inserted", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A step with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createStep(
        @ApiParam(value = "Step description", required =  true, type="string") @NotNull @Valid StepCreationDTO stepDto 
    ) throws Exception {
        try {
            ProcessDAO stepDAO = new ProcessDAO(sparql);
            StepModel stepModel = stepDto.newModel();
            stepDAO.createStep(stepModel);
            return new ObjectUriResponse(Response.Status.CREATED, stepModel.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            // Return error response 409 - CONFLICT if step URI already exists
            return new ErrorResponse(
                Response.Status.CONFLICT,
                "Step already exists",
                "Duplicated URI: " + e.getUri()
            ).getResponse();
        }
    }

    /**
     * Update step.
     * @param stepDto 
     * @return Response the request result
     */
    @PUT
    @ApiOperation(value = "Update step")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Step updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Step URI not found", response = ErrorResponse.class)
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStep(
        @ApiParam(value = "Step description", required =  true, type = "string") @NotNull @Valid StepCreationDTO stepDto
        ) throws Exception {
        ProcessDAO stepDAO = new ProcessDAO(sparql);
        StepModel stepModel = stepDto.newModel();
        stepDAO.update(stepModel, currentUser);
        return new ObjectUriResponse(Response.Status.OK, stepModel.getUri()).getResponse();
    }

    /**
     * @return a {@link Response} with a {@link SingleObjectResponse} containing the {@link StepGetDTO}
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get a step")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Step retrieved", response = StepGetDTO.class),
        @ApiResponse(code = 404, message = "Step URI not found", response = ErrorResponse.class)
    })
    public Response getStep(
            @ApiParam(value = "Step URI", example = "http://opensilex.dev/set/Step/S2", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        ProcessDAO dao = new ProcessDAO(sparql);
        StepModel stepModel = dao.getStepByURI(uri);

        if (stepModel != null) {
            return new SingleObjectResponse<>(StepGetDTO.fromModel(stepModel)).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Step not found",
                    "Unknown step URI: " + uri.toString()
            ).getResponse();
        }
    }

    /**
     * Search steps
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     */
    @GET
    @ApiOperation("Search steps")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return steps list", response = StepGetDTO.class, responseContainer = "List")
    })
    public Response searchSteps(
            @ApiParam(value = "Regex pattern for filtering list by name", example = "procédé 23") @QueryParam("name") String name,
            @ApiParam(value = "Regex pattern for filtering list by start date", example = "2020") @QueryParam("startDate") String startDate,
            @ApiParam(value = "Regex pattern for filtering list by end date", example = "2020") @QueryParam("endDate") String endDate,
            @ApiParam(value = "Search by input", example = "lbe:id/scientific-object/test/so-input_1") @QueryParam("input") List<URI> input,
            @ApiParam(value = "Search by output", example = "lbe:id/scientific-object/test/so-output_1") @QueryParam("output") List<URI> output,
            @ApiParam(value = "List of fields to sort as an array of fieldTitle=asc|desc", example = "date=asc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ProcessDAO processDAO = new ProcessDAO(sparql);
        ListWithPagination<StepModel> resultList = processDAO.searchStep(
                currentUser,
                name,
                startDate,
                endDate,
                input,
                output,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<StepGetDTO> resultDTOList = resultList.convert(StepGetDTO.class, StepGetDTO::fromModel);
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Remove a step
     * @param uri step uri
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the deleted step {@link URI}
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a step")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_STEP_DELETE_ID,
            credentialLabelKey = CREDENTIAL_STEP_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteStep(
            @ApiParam(value = "Step URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        ProcessDAO dao = new ProcessDAO(sparql);
        StepModel stepModel = dao.getStepByURI(uri);

        if (stepModel == null) {
            throw new NotFoundURIException("Invalid or unknown Step URI ", uri);
        }
        
        if (dao.isLinkedToProcess(stepModel)) {
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "The step is linked to another process or an experiment",
                    "You can't delete a step linked to another object"
            ).getResponse();
        } else {        
            dao.deleteStep(uri, currentUser);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        }

    }

}
