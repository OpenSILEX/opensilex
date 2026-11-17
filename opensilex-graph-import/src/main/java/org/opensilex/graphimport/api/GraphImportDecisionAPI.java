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
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.graphimport.api.dto.ImportExperimentDTO;
import org.opensilex.graphimport.api.dto.ImportProvenanceDTO;
import org.opensilex.graphimport.api.dto.InterpretationDTO;
import org.opensilex.graphimport.api.dto.ResourceDeclarationDTO;
import org.opensilex.graphimport.api.dto.SheetDecisionsDTO;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.binding.SheetDecisionRules;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.interpret.alias.LearnedAliasStore;
import org.opensilex.graphimport.session.GraphImportSession;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What the user decides about an import: the experiment it relates to, and for each sheet its class,
 * its identifier column and the property of its columns. Each answer is the interpretation that
 * follows, so that the screen shows at once what the decision changed. Nothing here writes to the
 * instance.
 *
 * @author Arnaud Charleroy
 */
@Api(GraphImportAPI.API_TAG)
@Path(GraphImportAPI.PATH)
public class GraphImportDecisionAPI {

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

    @PUT
    @Path("sheet_decisions")
    @ApiOperation(
            value = "Decide the class, the identifier column and column properties of a sheet",
            notes = "Replaces what was decided about the sheet. A property chosen for a column is remembered for "
                    + "the same header and class in the user's next imports."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The interpretation with the decisions", response = InterpretationDTO.class),
            @ApiResponse(code = 400, message = "A class, column or property the sheet cannot take",
                    response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import or sheet", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response decideSheet(
            @ApiParam(value = "The decisions about one sheet", required = true) @NotNull @Valid SheetDecisionsDTO dto
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(dto.getSessionId());
        Optional<SheetStructure> sheet = session.flatMap(found -> found.getWorkbook().getSheet(dto.getSheet()));
        if (sheet.isEmpty()) {
            return access.notFound();
        }

        Interpretations interpretations = interpretations();
        // How the dates of the columns are written is set apart, through column_readings: kept as it was.
        ImportDecisions.SheetDecisions decided = dto.toModel()
                .withReadings(session.get().getDecisions().of(dto.getSheet()).readings());
        Optional<String> refused = SheetDecisionRules.whyNot(decided, sheet.get(), interpretations.catalog(),
                interpretations.views());
        if (refused.isPresent()) {
            return access.badRequest("This decision cannot be kept", refused.get());
        }
        learnAliases(session.get().getDecisions().of(dto.getSheet()), decided, sheet.get());
        session.get().decide(dto.getSheet(), decided);
        access.persist(session.get());
        return new SingleObjectResponse<>(interpretations.interpret(session.get())).getResponse();
    }

    @DELETE
    @Path(GraphImportAPI.SESSIONS_PATH + "/{sessionId}/sheets/{sheet}/decisions")
    @ApiOperation(value = "Forget what was decided about a sheet", notes = "The sheet goes back to the suggestions.")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The interpretation", response = InterpretationDTO.class),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response forgetSheetDecisions(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Sheet name, as the import shows it", required = true) @PathParam("sheet") @NotNull
            String sheet
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        session.get().decide(sheet, ImportDecisions.SheetDecisions.NONE);
        access.persist(session.get());
        return new SingleObjectResponse<>(interpretations().interpret(session.get())).getResponse();
    }

    @PUT
    @Path("import_experiments")
    @ApiOperation(
            value = "Choose the experiment of an import",
            notes = "Its scientific objects can then be recognised by name. Only an experiment the user may read "
                    + "can be chosen."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The interpretation in that experiment", response = InterpretationDTO.class),
            @ApiResponse(code = 403, message = "The experiment is not readable by the user",
                    response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import or experiment", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response chooseExperiment(
            @ApiParam(value = "The import and its experiment", required = true) @NotNull @Valid ImportExperimentDTO dto
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(dto.getSessionId());
        if (session.isEmpty()) {
            return access.notFound();
        }
        // Throws, and so answers 403 or 404, before anything is kept.
        LookupContext.of(sparql, nosql, fs, currentUser, dto.getExperiment());
        session.get().setExperiment(dto.getExperiment());
        access.persist(session.get());
        return new SingleObjectResponse<>(interpretations().interpret(session.get())).getResponse();
    }

    @PUT
    @Path("declared_resources")
    @ApiOperation(
            value = "Declare a resource the files do not describe, or change one",
            notes = "Such as the factor a sheet of levels belongs to. It becomes a row of a sheet the assistant "
                    + "adds, so that rows of the files may point to it by its name. Nothing is written to the "
                    + "instance."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The interpretation with the resource", response = InterpretationDTO.class),
            @ApiResponse(code = 400, message = "A class, property or value the resource cannot take",
                    response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response declareResource(
            @ApiParam(value = "The import and the resource", required = true) @NotNull @Valid ResourceDeclarationDTO dto
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(dto.getSessionId());
        if (session.isEmpty()) {
            return access.notFound();
        }
        Interpretations interpretations = interpretations();
        ImportDecisions.DeclaredResource resource = dto.getResource().toModel(UUID.randomUUID().toString());
        Optional<String> refused = SheetDecisionRules.whyNot(resource, interpretations.catalog(),
                interpretations.views());
        if (refused.isPresent()) {
            return access.badRequest("This resource cannot be declared", refused.get());
        }
        session.get().declare(resource);
        access.persist(session.get());
        return new SingleObjectResponse<>(interpretations.interpret(session.get())).getResponse();
    }

    @DELETE
    @Path(GraphImportAPI.SESSIONS_PATH + "/{sessionId}/declared_resources/{resourceId}")
    @ApiOperation(value = "Forget a declared resource", notes = "Rows that pointed to it point to nothing again.")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The interpretation", response = InterpretationDTO.class),
            @ApiResponse(code = 404, message = "Unknown import or resource", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response forgetDeclaredResource(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Resource identifier", required = true) @PathParam("resourceId") @NotNull
            String resourceId
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty() || !session.get().forgetDeclared(resourceId)) {
            return access.notFound();
        }
        access.persist(session.get());
        return new SingleObjectResponse<>(interpretations().interpret(session.get())).getResponse();
    }

    @PUT
    @Path("import_provenances")
    @ApiOperation(
            value = "Choose the provenance of the data of an import",
            notes = "Every datum of the import is created with it. Only an existing provenance can be chosen."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The interpretation", response = InterpretationDTO.class),
            @ApiResponse(code = 404, message = "Unknown import or provenance", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response chooseProvenance(
            @ApiParam(value = "The import and its provenance", required = true) @NotNull @Valid ImportProvenanceDTO dto
    ) throws Exception {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(dto.getSessionId());
        if (session.isEmpty()) {
            return access.notFound();
        }
        if (dto.getProvenance() != null) {
            try {
                new ProvenanceDAO(nosql, sparql).get(dto.getProvenance());
            } catch (NoSQLInvalidURIException e) {
                return access.notFound();
            }
        }
        session.get().setProvenance(dto.getProvenance());
        access.persist(session.get());
        return new SingleObjectResponse<>(interpretations().interpret(session.get())).getResponse();
    }

    private Interpretations interpretations() throws Exception {
        return new Interpretations(sparql, nosql, fs, opensilex, currentUser);
    }

    /** Remembers, for the user's next imports, the properties this decision chose anew. */
    private void learnAliases(ImportDecisions.SheetDecisions before, ImportDecisions.SheetDecisions after,
                              SheetStructure sheet) {
        if (after.classUri() == null) {
            return;
        }
        LearnedAliasStore aliases = new LearnedAliasStore(nosql);
        for (Map.Entry<Integer, URI> column : after.properties().entrySet()) {
            if (!column.getValue().equals(before.properties().get(column.getKey()))) {
                aliases.learn(currentUser.getUri(), after.classUri(), sheet.getHeaders().get(column.getKey()),
                        column.getValue());
            }
        }
    }
}
