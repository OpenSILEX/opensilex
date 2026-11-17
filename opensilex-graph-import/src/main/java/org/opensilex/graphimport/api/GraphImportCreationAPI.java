//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.opensilex.OpenSilex;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.graphimport.api.dto.CreationOutcomeDTO;
import org.opensilex.graphimport.api.dto.CreationPlanDTO;
import org.opensilex.graphimport.api.dto.CreationRequestDTO;
import org.opensilex.graphimport.measure.DataDraft;
import org.opensilex.graphimport.session.CreationRecord;
import org.opensilex.graphimport.session.GraphImportSession;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.graphimport.write.CreationMessages;
import org.opensilex.graphimport.write.CreationPlan;
import org.opensilex.graphimport.write.DataWriter;
import org.opensilex.graphimport.write.ExperimentWriter;
import org.opensilex.graphimport.write.ResourceWriter;
import org.opensilex.graphimport.write.WriteProblem;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The creation step: what the import would create, step by step, and creating it when the user asks.
 * Each step writes through the platform's own classes, all its resources or none, after the same checks
 * a dry run makes; then the preview is read afresh, so that what was created reads as done.
 *
 * @author Arnaud Charleroy
 */
@Api(GraphImportAPI.API_TAG)
@Path(GraphImportAPI.PATH)
public class GraphImportCreationAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphImportCreationAPI.class);

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @Inject
    private GraphImportConfig config;

    @Inject
    private GraphImportSessionCache sessions;

    @Inject
    private OpenSilex opensilex;

    @GET
    @Path(GraphImportAPI.SESSIONS_PATH + "/{sessionId}/creation")
    @ApiOperation(
            value = "Get what the import would create",
            notes = "Step by step, in the order they run: the resources ready to create, those blocked and why, those "
                    + "the instance already holds. Nothing is written."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The plan of the creation", response = CreationPlanDTO.class),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response getCreationPlan(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Read the instance again rather than the last preview") @QueryParam("afresh")
            @DefaultValue("false") boolean afresh
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        Interpretations interpretations = interpretations();
        return new SingleObjectResponse<>(plan(session.get(), interpretations, interpretations
                .creation(session.get(), afresh))).getResponse();
    }

    @POST
    @Path("creation_checks")
    @ApiOperation(
            value = "Check a step of the creation without writing",
            notes = "Runs the checks the step would run before writing, and says what would stop it."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "What would stop the step, and the plan", response = CreationOutcomeDTO.class),
            @ApiResponse(code = 403, message = "The user may not create these resources", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import or step", response = ErrorResponse.class),
            @ApiResponse(code = 409, message = "The step changed since the user saw it", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response checkCreation(
            @ApiParam(value = "The step and what the user saw of it", required = true) @NotNull @Valid CreationRequestDTO dto
    ) throws Exception {
        return run(dto, false);
    }

    @POST
    @Path("creations")
    @ApiOperation(
            value = "Run a step of the creation",
            notes = "Writes the resources of the step that are ready, all or none, through the platform's own classes. "
                    + "Requires the platform to let the assistant write (graph-import.creationEnabled)."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "What was created, or what stopped it, and the plan", response = CreationOutcomeDTO.class),
            @ApiResponse(code = 403, message = "Creation is off, or the user may not create these resources",
                    response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import or step", response = ErrorResponse.class),
            @ApiResponse(code = 409, message = "The step changed since the user saw it", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response createStep(
            @ApiParam(value = "The step and what the user saw of it", required = true) @NotNull @Valid CreationRequestDTO dto
    ) throws Exception {
        return run(dto, true);
    }

    /** Checks the step, and writes it when asked and nothing stops it. */
    private Response run(CreationRequestDTO dto, boolean write) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(dto.getSessionId());
        if (session.isEmpty()) {
            return access.notFound();
        }
        Interpretations interpretations = interpretations();
        Interpretations.Creation creation = interpretations.creation(session.get(), false);
        boolean data = DataWriter.STEP.equals(dto.getStep());
        Optional<CreationPlan.Step> step = creation.plan().step(dto.getStep());
        if (!data && step.isEmpty()) {
            return access.notFound();
        }
        if (write && !config.creationEnabled()) {
            return new ErrorResponse(Response.Status.FORBIDDEN, "Creation is off",
                    "The platform does not let the assistant write: graph-import.creationEnabled is false.")
                    .getResponse();
        }
        String credential = data ? DataWriter.credentialId() : step.get().writer().credentialId();
        if (!creation.context().mayWrite(credential)) {
            return new ErrorResponse(Response.Status.FORBIDDEN, "Not allowed",
                    CreationMessages.forbidden().getEnglish()).getResponse();
        }
        int count = data ? creation.data().ready().size() : step.get().ready().size();
        if (count != dto.getExpected()) {
            return new ErrorResponse(Response.Status.CONFLICT, "The step changed",
                    "It would now create " + count + " resources, not " + dto.getExpected() + ".")
                    .getResponse();
        }
        if (data) {
            return runData(write, access, session.get(), interpretations, creation);
        }

        ResourceWriter writer = step.get().writer();
        List<WriteProblem> problems = writer.check(step.get().ready(), creation.context());
        List<URI> created = List.of();
        if (write && problems.isEmpty() && !step.get().ready().isEmpty()) {
            try {
                created = writer.write(step.get().ready(), creation.context());
            } catch (Exception e) {
                LOGGER.warn("The step {} of the import {} was refused", dto.getStep(), dto.getSessionId(), e);
                problems = List.of(WriteProblem.ofStep(CreationMessages.refused(String.valueOf(e.getMessage()))));
            }
        }
        if (!created.isEmpty()) {
            afterWriting(session.get(), dto.getStep(), created);
            access.persist(session.get());
        }
        Interpretations.Creation now = created.isEmpty() ? creation : interpretations.creation(session.get(), true);
        List<CreationPlanDTO.CreationNodeDTO> said = problems.stream()
                .map(problem -> CreationPlanDTO.CreationNodeDTO.of(problem.node(), problem.name(), problem.message()))
                .toList();
        return new SingleObjectResponse<>(new CreationOutcomeDTO(said, created, plan(session.get(), interpretations, now)))
                .getResponse();
    }

    /**
     * Checks the data, or writes them batch by batch. The batches written before a refused one stay
     * written: they are kept with the import, and read as already there when the step is run again.
     */
    private Response runData(boolean write, GraphImportAccess access, GraphImportSession session,
                             Interpretations interpretations, Interpretations.Creation creation) throws Exception {
        List<DataDraft> ready = creation.data().ready();
        List<WriteProblem> problems;
        int created = 0;
        if (!write) {
            problems = creation.dataWriter().check(ready);
        } else {
            DataWriter.Outcome outcome = creation.dataWriter().write(ready);
            created = outcome.created();
            problems = outcome.problem().map(List::of).orElse(List.of());
            outcome.problem().ifPresent(problem -> LOGGER.warn("Data of the import {} refused: {}", session.getId(),
                    problem.message().getEnglish()));
        }
        if (created > 0) {
            session.recordCreation(new CreationRecord(DataWriter.STEP, Instant.now(), created));
            access.persist(session);
        }
        Interpretations.Creation now = created > 0 ? interpretations.creation(session, false) : creation;
        List<CreationPlanDTO.CreationNodeDTO> said = problems.stream()
                .map(problem -> CreationPlanDTO.CreationNodeDTO.of(problem.node(), problem.name(), problem.message()))
                .toList();
        return new SingleObjectResponse<>(new CreationOutcomeDTO(said, List.of(), created,
                plan(session, interpretations, now))).getResponse();
    }

    /**
     * Keeps what was written with the import. An experiment just created becomes the import's when it has
     * none, so that the factors and objects of the next steps are created in it.
     */
    private void afterWriting(GraphImportSession session, String step, List<URI> created) {
        if (ExperimentWriter.STEP.equals(step) && session.getExperiment() == null && created.size() == 1) {
            session.setExperiment(created.get(0));
        }
        session.recordCreation(new CreationRecord(step, Instant.now(), created.size()));
    }

    private CreationPlanDTO plan(GraphImportSession session, Interpretations interpretations,
                                 Interpretations.Creation creation) throws Exception {
        return CreationPlanDTO.fromModel(session.getId(), config.creationEnabled(), creation.plan(), creation.data(),
                creation.allowed(), session.getCreations(), interpretations.catalog(), currentUser.getLanguage());
    }

    private Interpretations interpretations() throws Exception {
        return new Interpretations(sparql, nosql, fs, opensilex, currentUser);
    }
}
