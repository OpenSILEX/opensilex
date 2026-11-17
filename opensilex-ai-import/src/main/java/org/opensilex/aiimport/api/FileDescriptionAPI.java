//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.api.dto.FileDescriptionDTO;
import org.opensilex.aiimport.api.dto.FileDescriptionUpdateDTO;
import org.opensilex.aiimport.api.dto.SheetDescriptionUpdateDTO;
import org.opensilex.aiimport.api.dto.TypePropertyDTO;
import org.opensilex.aiimport.create.objects.ObjectSheetPlan;
import org.opensilex.aiimport.create.objects.TypeProperties;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.DescribedSheet;
import org.opensilex.aiimport.profile.custom.SheetDescription;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.profile.custom.UserDefinedProfile;
import org.opensilex.aiimport.resolve.ReportCategory;
import org.opensilex.aiimport.resolve.ResolutionService;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.AiImportSessionCache;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.aiimport.service.SessionProfile;
import org.opensilex.aiimport.service.store.FileTemplateStore;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateAuthor;
import org.opensilex.aiimport.template.TemplateNames;
import org.opensilex.aiimport.template.csvw.CsvwWriter;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.core.CoreModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The description a user makes of a file the module recognises no template for: what each sheet is
 * and what each column is. Kept with the conversation; applying it recomputes the report, and
 * nothing is written to the instance.
 *
 * @author Arnaud Charleroy
 */
@Api(AiImportAPI.API_TAG)
@Path(AiImportAPI.PATH)
public class FileDescriptionAPI {

    public static final String PATH = "sheet_descriptions";
    public static final String TYPE_PROPERTIES_PATH = "type_properties";
    public static final String CSVW_PATH = "csvw";
    public static final String CSVW_MEDIA_TYPE = "application/csvm+json";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDescriptionAPI.class);

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @Inject
    private AiImportConfig config;

    @Inject
    private LlmService llm;

    @Inject
    private AiImportSessionCache sessions;

    @Inject
    private CoreModule coreModule;

    @GET
    @Path(AiImportAPI.SESSIONS_PATH + "/{sessionId}/" + PATH)
    @ApiOperation(
            value = "Get how the files of a conversation are described",
            notes = "Each sheet as it is read — its kind, the role of each column, what is wrong with that — "
                    + "with its first rows, and the kinds and roles that can be chosen. Nothing is written."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The description", response = FileDescriptionDTO.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getDescription(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(sessionId);
        if (!found.isPresent()) {
            return access.notFound();
        }
        return new SingleObjectResponse<>(describedBy(found.get(), access)).getResponse();
    }

    @POST
    @Path(PATH)
    @ApiOperation(
            value = "Describe the files of a conversation",
            notes = "Everything asked is checked first and applied together, or nothing is. The report is "
                    + "recomputed against the instance, and a conversation about an unrecognised file is "
                    + "from then on read as described. Nothing is written to the instance."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The description as it now stands", response = FileDescriptionDTO.class),
            @ApiResponse(code = 400, message = "A sheet, a column, a kind, a role, a type or an experiment that "
                    + "cannot be used as asked, or a file read by a template", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response describe(
            @ApiParam(value = "What changes, and in which conversation", required = true)
            @NotNull @Valid FileDescriptionUpdateDTO request
    ) throws Exception {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(request.getSessionId());
        if (!found.isPresent()) {
            return access.notFound();
        }
        AiImportSession session = found.get();
        if (!SessionProfile.isDescribable(session)) {
            return access.badRequest("This file is read by a template.",
                    "Its sheets and columns are known to the module, and are not described by the user.");
        }

        Map<String, SheetDescription> drafts = new LinkedHashMap<>();
        ResourceReference experiment = null;
        try {
            for (SheetDescriptionUpdateDTO update : request.getSheets()) {
                drafts.put(update.getSheet(), draftOf(session, update));
            }
            if (request.getExperiment() != null) {
                experiment = new ResolutionService(sparql, nosql, fs, currentUser, null)
                        .visibleResource(ReportCategory.EXPERIMENTS, request.getExperiment())
                        .orElseThrow(() -> new Refusal("Not an experiment you can see.",
                                "No experiment with the URI '" + request.getExperiment() + "' is visible to you."));
            }
        } catch (Refusal refusal) {
            return access.badRequest(refusal.title, refusal.detail);
        }

        commit(session, drafts, request.getSheets());
        if (experiment != null) {
            session.getDescription().setExperiment(experiment.getUri().toString(), experiment.getName());
            session.getConfirmedMatches().confirm(ReportCategory.EXPERIMENTS, experiment.getName(), experiment);
        }
        session.setProfileId(UserDefinedProfile.ID);
        access.chat().revalidate(session);
        if (request.remembers() && !request.getSheets().isEmpty()) {
            remember(session, access);
        }
        access.persist(session);
        return new SingleObjectResponse<>(describedBy(session, access)).getResponse();
    }

    @GET
    @Path(AiImportAPI.SESSIONS_PATH + "/{sessionId}/" + PATH + "/" + TYPE_PROPERTIES_PATH)
    @ApiOperation(
            value = "List the properties a type of objects or facilities has",
            notes = "Read from the ontology of the instance for the kind of sheet: a type or a property added "
                    + "in the vocabulary screens is offered at once. What a column of the sheet can be written to."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The properties", response = TypePropertyDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 400, message = "A kind of sheet without types, or a type that is none of its",
                    response = ErrorResponse.class)
    })
    public Response getDescriptionTypeProperties(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Kind of sheet", required = true, example = "FACILITIES")
            @QueryParam("kind") @NotNull String kind,
            @ApiParam(value = "Type", required = true, example = "vocabulary:Facility")
            @QueryParam("type") @NotNull URI type
    ) throws Exception {
        SessionAccess access = access();
        Optional<SheetKind> parsed = SheetKind.parse(kind);
        if (!parsed.isPresent() || !parsed.get().baseType().isPresent()) {
            return access.badRequest("This kind of sheet has no type.", "'" + kind + "' has no objects or "
                    + "facilities of a type: choose objects, objects and observations, or facilities.");
        }
        TypeProperties properties = new TypeProperties(currentUser.getLanguage(), parsed.get().baseType().get());
        if (!properties.isObjectType(type)) {
            return access.badRequest("Not a type of this kind.",
                    "'" + type + "' does not descend from " + parsed.get().baseType().get() + ".");
        }
        List<TypePropertyDTO> dtos = new ArrayList<>();
        properties.of(type).forEach(property -> dtos.add(TypePropertyDTO.fromModel(property)));
        return new PaginatedListResponse<>(dtos).getResponse();
    }

    @GET
    @Path(AiImportAPI.SESSIONS_PATH + "/{sessionId}/" + PATH + "/" + CSVW_PATH)
    @ApiOperation(
            value = "Download how the files are described, as CSVW",
            notes = "The description as it stands, whether or not it is kept as a template: the W3C's way of saying "
                    + "what the columns of a CSV file are, with the module's own annotations for what a sheet is."
    )
    @ApiProtected
    @Produces({CSVW_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The CSVW document"),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getCsvw(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId
    ) throws Exception {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(sessionId);
        if (!found.isPresent()) {
            return access.notFound();
        }
        AiImportSession session = found.get();
        FileTemplate template = FileTemplate.of(session, TemplateNames.of(session), currentUser.getUri(),
                TemplateAuthor.nameOf(currentUser));
        String document = ObjectMapperContextResolver.getObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(new CsvwWriter(ObjectMapperContextResolver.getObjectMapper()).write(template));
        return Response.ok(document, CSVW_MEDIA_TYPE)
                .header("Content-Disposition", "attachment; filename=\"" + TemplateNames.fileName(template) + "\"")
                .build();
    }

    //#region keeping what is said

    /**
     * The description, with what the user's templates propose for the sheets they have not described.
     */
    private FileDescriptionDTO describedBy(AiImportSession session, SessionAccess access) {
        List<FileTemplate> templates = List.of();
        try {
            templates = access.templates().list(currentUser.getUri());
        } catch (IOException | RuntimeException e) {
            // A proposal is a convenience: the description is shown without when templates cannot be read.
            LOGGER.warn("Could not read the templates of {}", currentUser.getUri(), e);
        }
        return FileDescriptionDTO.fromModel(session).propose(session, templates);
    }

    /**
     * Keeps what the user said of the sheets as their template, creating it the first time and
     * following their last word after. A failure costs the next proposal, not the step the user took.
     */
    private void remember(AiImportSession session, SessionAccess access) {
        try {
            FileTemplateStore store = access.templates();
            Optional<FileTemplate> own = session.getTemplateId() == null
                    ? Optional.empty()
                    : store.find(session.getTemplateId(), currentUser.getUri()).filter(t -> t.isOwnedBy(currentUser.getUri()));
            FileTemplate template = own.map(existing -> existing.update(session)).orElseGet(() ->
                    FileTemplate.of(session, TemplateNames.of(session), currentUser.getUri(),
                            TemplateAuthor.nameOf(currentUser)));
            store.save(template);
            session.setTemplateId(template.getId());
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not keep the template of import conversation {}", session.getId(), e);
        }
    }

    //#endregion

    //#region checking a change

    /**
     * The description of a sheet as it would be once the change is applied, checked against the file
     * and the ontology. Worked on a copy, so a change that fails changes nothing.
     */
    private SheetDescription draftOf(AiImportSession session, SheetDescriptionUpdateDTO update) throws Refusal {
        SheetStructure sheet = session.getWorkbook().getSheet(update.getSheet()).filter(SheetStructure::isTabular)
                .orElseThrow(() -> new Refusal("Unknown sheet.", "The file has no sheet '" + update.getSheet()
                        + "' with a table to describe."));
        SheetDescription draft = session.getDescription().find(update.getSheet())
                .map(SheetDescription::copy).orElseGet(() -> new SheetDescription(update.getSheet()));

        if (update.getKind() != null) {
            draft.setKind(update.getKind().isBlank() ? null : SheetKind.parse(update.getKind())
                    .orElseThrow(() -> new Refusal("Unknown kind of sheet.", "'" + update.getKind()
                            + "' is none of " + Arrays.toString(SheetKind.values()) + ".")));
        }
        if (update.getRoles() != null) {
            for (Map.Entry<String, String> role : update.getRoles().entrySet()) {
                if (!sheet.hasHeader(role.getKey())) {
                    throw new Refusal("Unknown column.", "The sheet '" + sheet.getName() + "' has no column '"
                            + role.getKey() + "'.");
                }
                if (role.getValue() == null || role.getValue().isBlank()) {
                    draft.getRoles().remove(role.getKey());
                } else {
                    draft.getRoles().put(role.getKey(), DescribedRole.parse(role.getValue())
                            .orElseThrow(() -> new Refusal("Unknown role.", "'" + role.getValue()
                                    + "' is none of " + Arrays.toString(DescribedRole.values()) + ".")));
                }
            }
        }
        if (update.getProperties() != null) {
            for (Map.Entry<String, String> property : update.getProperties().entrySet()) {
                if (!sheet.hasHeader(property.getKey())) {
                    throw new Refusal("Unknown column.", "The sheet '" + sheet.getName() + "' has no column '"
                            + property.getKey() + "'.");
                }
                if (property.getValue() == null || property.getValue().isBlank()) {
                    draft.getProperties().remove(property.getKey());
                } else if (!isAbsoluteUri(property.getValue())) {
                    throw new Refusal("Not a property.", "'" + property.getValue() + "' is not the URI of a property.");
                } else {
                    draft.getProperties().put(property.getKey(), property.getValue());
                }
            }
        }
        if (update.getFactor() != null) {
            draft.setFactor(update.getFactor());
        }
        if (update.getRdfType() != null) {
            checkType(DescribedSheet.of(sheet, draft).kind(), update.getRdfType());
            draft.setType(update.getRdfType());
        }
        return draft;
    }

    private void checkType(SheetKind kind, URI type) throws Refusal {
        URI base = kind.baseType().orElseThrow(() -> new Refusal("This kind of sheet has no type.",
                "Rows of kind " + kind + " are not objects or facilities of a type."));
        if (!new TypeProperties(currentUser.getLanguage(), base).isObjectType(type)) {
            throw new Refusal("Not a type of this kind.", "'" + type + "' does not descend from " + base + ".");
        }
    }

    private static boolean isAbsoluteUri(String value) {
        try {
            return URI.create(value).isAbsolute();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Keeps the checked descriptions. What the user chose here for a sheet's type or properties
     * replaces what they had chosen for it in the panel of object sheets, which would otherwise
     * contradict it.
     */
    private void commit(AiImportSession session, Map<String, SheetDescription> drafts,
                        List<SheetDescriptionUpdateDTO> updates) {
        drafts.forEach((name, draft) -> session.getDescription().getSheets().put(name, draft));
        for (SheetDescriptionUpdateDTO update : updates) {
            ObjectSheetPlan plan = session.getObjectPlans().get(update.getSheet());
            if (plan == null) {
                continue;
            }
            if (update.getRdfType() != null) {
                plan.setType(null);
            }
            if (update.getProperties() != null) {
                update.getProperties().keySet().forEach(column -> plan.getMapping().remove(column));
            }
        }
    }

    /**
     * A change the user is told is not possible, and why.
     */
    private static final class Refusal extends Exception {
        private final String title;
        private final String detail;

        private Refusal(String title, String detail) {
            super(detail);
            this.title = title;
            this.detail = detail;
        }
    }

    //#endregion

    private SessionAccess access() {
        return new SessionAccess(sparql, nosql, fs, config, llm, sessions, coreModule, currentUser);
    }
}
