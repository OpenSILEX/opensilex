//******************************************************************************
//                          ProcessAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.api;

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

/**
 * @author Fernandez Emilie
 */

@Api(ProcessAPI.CREDENTIAL_PROCESS_GROUP_ID)
@Path("/core/process")
@ApiCredentialGroup(
        groupId = ProcessAPI.CREDENTIAL_PROCESS_GROUP_ID,
        groupLabelKey = ProcessAPI.CREDENTIAL_PROCESS_GROUP_LABEL_KEY
)
public class ProcessAPI {

    public static final String CREDENTIAL_PROCESS_GROUP_ID = "Process";
    public static final String CREDENTIAL_PROCESS_GROUP_LABEL_KEY = "credential-groups.process";

    public static final String CREDENTIAL_PROCESS_MODIFICATION_ID = "process-modification";
    public static final String CREDENTIAL_PROCESS_MODIFICATION_LABEL_KEY = "credential.process.modification";

    public static final String CREDENTIAL_PROCESS_DELETE_ID = "process-delete";
    public static final String CREDENTIAL_PROCESS_DELETE_LABEL_KEY = "credential.process.delete";

    protected static final String PROCESS_EXAMPLE_URI = "http://opendata.inrae.fr/ebo/set/process/p21";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

       /**
     * Create Process
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the created process {@link URI}
     */
    @POST
    @ApiOperation(value = "Add a process")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROCESS_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PROCESS_MODIFICATION_LABEL_KEY
    )   
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Process created", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A process with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createProcess(
        @ApiParam(value = "Process description", required =  true, type="string") @NotNull @Valid ProcessCreationDTO processDto 
    ) throws Exception {
        try {
            ProcessDAO processDAO = new ProcessDAO(sparql);
            ProcessModel processModel = processDto.newModel();
            processDAO.create(processModel, currentUser);
            return new ObjectUriResponse(Response.Status.CREATED, processModel.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            // Return error response 409 - CONFLICT if process URI already exists
            return new ErrorResponse(
                Response.Status.CONFLICT,
                "Process already exists",
                "Duplicated URI: " + e.getUri()
            ).getResponse();
        }
    }

    /**
     * @return a {@link Response} with a {@link SingleObjectResponse} containing the {@link ProcessGetDTO}
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get a process")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Process retrieved", response = ProcessGetDTO.class),
        @ApiResponse(code = 404, message = "Process URI not found", response = ErrorResponse.class)
    })
    public Response getProcess(
            @ApiParam(value = "Process URI", example = "http://opensilex.dev/set/Process/P17", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        ProcessDAO processDAO = new ProcessDAO(sparql);
        ProcessModel processModel = processDAO.getProcessByURI(uri);

        if (processModel != null) {
            return new SingleObjectResponse<>(ProcessGetDTO.fromModel(processModel)).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Process not found",
                    "Unknown Process URI: " + uri.toString()
            ).getResponse();
        }
    }


    /**
     * Update process.
     * @param processDto 
     * @return Response the request result
     */
    @PUT
    @ApiOperation(value = "Update process")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Process updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Process URI not found", response = ErrorResponse.class)
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProcess(
        @ApiParam(value = "Process description", required =  true, type = "string") @NotNull @Valid ProcessCreationDTO processDto
        ) throws Exception {
        ProcessDAO processDAO = new ProcessDAO(sparql);
        ProcessModel processModel = processDto.newModel();
        processDAO.update(processModel, currentUser);
        return new ObjectUriResponse(Response.Status.OK, processModel.getUri()).getResponse();
    }

    /**
     * Remove a process
     * @param uri process uri
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the deleted process {@link URI}
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a process")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROCESS_DELETE_ID,
            credentialLabelKey = CREDENTIAL_PROCESS_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Process deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Process URI not found", response = ErrorResponse.class)
    })
    public Response deleteProcess(
            @ApiParam(value = "Process URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        uri = new URI(URIDeserializer.getExpandedURI(uri.toString()));
        ProcessDAO processDAO = new ProcessDAO(sparql);
        processDAO.deleteProcess(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    /**
     * Search processes
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     */
    @GET
    @ApiOperation("Search processes")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return processes list", response = ProcessGetDTO.class, responseContainer = "List")
    })
    public Response searchProcesses(
            @ApiParam(value = "Regex pattern for filtering list by name", example = "procédé 23") @QueryParam("name") String name,
            @ApiParam(value = "Regex pattern for filtering list by creation date", example = "2020") @QueryParam("creationDate") String creationDate,
            @ApiParam(value = "Regex pattern for filtering list by destruction date", example = "2020") @QueryParam("destructionDate") String destructionDate,
            @ApiParam(value = "Search by step", example = "lbe:id/step/mixing") @QueryParam("step") List<URI> step,
            @ApiParam(value = "List of fields to sort as an array of fieldTitle=asc|desc", example = "date=asc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ProcessDAO processDAO = new ProcessDAO(sparql);
        ListWithPagination<ProcessModel> resultList = processDAO.searchProcess(
                currentUser,
                name,
                creationDate,
                destructionDate,
                step,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<ProcessGetDTO> resultDTOList = resultList.convert(ProcessGetDTO.class, ProcessGetDTO::fromModel);
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

}
