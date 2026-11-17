//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.api.dto.FileTemplateDTO;
import org.opensilex.aiimport.api.dto.FileTemplateUpdateDTO;
import org.opensilex.aiimport.export.ExperimentSnapshot;
import org.opensilex.aiimport.export.ExperimentSnapshotReader;
import org.opensilex.aiimport.export.TemplateWorkbookBuilder;
import org.opensilex.aiimport.service.AiImportSessionCache;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.aiimport.service.store.FileTemplateStore;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateAuthor;
import org.opensilex.aiimport.template.TemplateNames;
import org.opensilex.aiimport.template.csvw.CsvwException;
import org.opensilex.aiimport.template.csvw.CsvwReader;
import org.opensilex.aiimport.template.csvw.CsvwWriter;
import org.opensilex.aiimport.template.rml.RmlWriter;
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

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The templates a user keeps — what they said of a file, to be proposed for the next of the same
 * shape — and how they leave and come back: CSVW, which this module reads again, and RML, which
 * it only writes. A template is the user's own until they share it; only they change or delete it.
 *
 * @author Arnaud Charleroy
 */
@Api(AiImportAPI.API_TAG)
@Path(AiImportAPI.PATH + "/" + FileTemplateAPI.PATH)
public class FileTemplateAPI {

    public static final String PATH = "templates";
    public static final String CSVW_PATH = "csvw";
    public static final String RML_PATH = "rml";
    public static final String IMPORT_PATH = "import";
    public static final String EXPORT_PATH = "export";
    public static final String TURTLE_MEDIA_TYPE = "text/turtle";

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
    @ApiOperation(value = "List the templates the user may use", notes = "Their own and those others shared, "
            + "most recently changed first.")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The templates", response = FileTemplateDTO.class,
                    responseContainer = "List")
    })
    public Response getTemplates() throws IOException {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        return new PaginatedListResponse<>(access.templates().list(currentUser.getUri()).stream()
                .map(template -> FileTemplateDTO.fromModel(template, currentUser.getUri()))
                .collect(Collectors.toList())).getResponse();
    }

    @PUT
    @ApiOperation(value = "Rename or share a template", notes = "Only its author does. What is left out stays.")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The template as it now stands", response = FileTemplateDTO.class),
            @ApiResponse(code = 404, message = "Unknown template, or not the user's own", response = ErrorResponse.class)
    })
    public Response updateTemplate(
            @ApiParam(value = "What changes", required = true) @NotNull @Valid FileTemplateUpdateDTO request
    ) throws IOException {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        FileTemplateStore store = access.templates();
        Optional<FileTemplate> own = store.find(request.getId(), currentUser.getUri())
                .filter(template -> template.isOwnedBy(currentUser.getUri()));
        if (!own.isPresent()) {
            return notFound();
        }
        FileTemplate template = own.get();
        if (request.getName() != null && !request.getName().isBlank()) {
            template.rename(request.getName().trim());
        }
        if (request.getShared() != null) {
            template.share(request.getShared());
        }
        store.save(template);
        return new SingleObjectResponse<>(FileTemplateDTO.fromModel(template, currentUser.getUri())).getResponse();
    }

    @DELETE
    @Path("{id}")
    @ApiOperation(value = "Delete a template", notes = "Only its author does. Conversations that were kept "
            + "up to date under it simply stop.")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted"),
            @ApiResponse(code = 404, message = "Unknown template, or not the user's own", response = ErrorResponse.class)
    })
    public Response deleteTemplate(
            @ApiParam(value = "Template identifier", required = true) @PathParam("id") @NotNull String id
    ) {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        return access.templates().delete(id, currentUser.getUri())
                ? Response.ok().build()
                : notFound();
    }

    @GET
    @Path("{id}/" + CSVW_PATH)
    @ApiOperation(value = "Download a template as CSVW", notes = "The W3C's way of saying what the columns of a CSV "
            + "file are. It can be uploaded again, here or on another instance.")
    @ApiProtected
    @Produces({FileDescriptionAPI.CSVW_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The CSVW document"),
            @ApiResponse(code = 404, message = "Unknown template, or not visible to the user",
                    response = ErrorResponse.class)
    })
    public Response getCsvw(
            @ApiParam(value = "Template identifier", required = true) @PathParam("id") @NotNull String id
    ) throws IOException {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<FileTemplate> found = access.templates().find(id, currentUser.getUri());
        if (!found.isPresent()) {
            return notFound();
        }
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        String document = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(new CsvwWriter(mapper).write(found.get()));
        return Response.ok(document, FileDescriptionAPI.CSVW_MEDIA_TYPE)
                .header("Content-Disposition", "attachment; filename=\"" + TemplateNames.fileName(found.get()) + "\"")
                .build();
    }

    @GET
    @Path("{id}/" + RML_PATH)
    @ApiOperation(value = "Download a template as RML", notes = "The mapping language that says how a CSV file becomes "
            + "RDF. Written for tools that run it: this module does not read it back.")
    @ApiProtected
    @Produces({TURTLE_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The RML mapping, in Turtle"),
            @ApiResponse(code = 404, message = "Unknown template, or not visible to the user",
                    response = ErrorResponse.class)
    })
    public Response getRml(
            @ApiParam(value = "Template identifier", required = true) @PathParam("id") @NotNull String id
    ) throws IOException {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<FileTemplate> found = access.templates().find(id, currentUser.getUri());
        if (!found.isPresent()) {
            return notFound();
        }
        return Response.ok(new RmlWriter().write(found.get()), TURTLE_MEDIA_TYPE)
                .header("Content-Disposition", "attachment; filename=\""
                        + TemplateNames.fileName(found.get()).replace(".csvm.json", ".rml.ttl") + "\"")
                .build();
    }

    @GET
    @Path("{id}/" + EXPORT_PATH)
    @ApiOperation(
            value = "Export an experiment in the shape of a template",
            notes = "One sheet for each sheet of the template, its columns in the template's order, each filled "
                    + "by its role. What the template has no column for, and what the experiment cannot answer, is "
                    + "said on a readme sheet. The workbook reads back with the same template."
    )
    @ApiProtected
    @Produces({StarExportAPI.XLSX_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The workbook"),
            @ApiResponse(code = 400, message = "Too many values for a workbook", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown template or experiment, or not visible to the user",
                    response = ErrorResponse.class)
    })
    public Response exportExperiment(
            @ApiParam(value = "Template identifier", required = true) @PathParam("id") @NotNull String id,
            @ApiParam(value = "Experiment URI", required = true, example = "test:id/experiment/xp")
            @QueryParam("experiment") @NotNull URI experiment
    ) throws Exception {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<FileTemplate> found = access.templates().find(id, currentUser.getUri());
        if (!found.isPresent()) {
            return notFound();
        }
        ExperimentSnapshot snapshot;
        try {
            snapshot = new ExperimentSnapshotReader(sparql, nosql, fs, currentUser).read(experiment);
        } catch (ExperimentSnapshotReader.TooManyObservationsException e) {
            return access.badRequest("Too many values for a workbook", e.getCount() + " values are recorded in "
                    + "this experiment, beyond the " + ExperimentSnapshotReader.MAX_OBSERVATIONS + " a workbook "
                    + "carries. Use the platform's data export, which filters by variable and by date.");
        }
        byte[] workbook = new TemplateWorkbookBuilder(found.get(), LocalDate.now()).build(snapshot);
        String name = (found.get().getName() + "_" + (snapshot.getName() == null ? "experiment" : snapshot.getName()))
                .replaceAll("[^A-Za-z0-9._-]+", "_") + ".xlsx";
        return Response.ok(workbook, StarExportAPI.XLSX_MEDIA_TYPE)
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"").build();
    }

    @POST
    @Path(IMPORT_PATH)
    @ApiOperation(value = "Upload a template written as CSVW", notes = "Becomes a new template of the user, who is "
            + "its author. A document this module did not write, or cannot read, is refused with the reason.")
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "The new template", response = FileTemplateDTO.class),
            @ApiResponse(code = 400, message = "A document that cannot be read as a template",
                    response = ErrorResponse.class)
    })
    public Response importTemplate(
            @ApiParam(value = "The CSVW document", required = true, type = "file")
            @NotNull @FormDataParam("file") File file
    ) throws IOException {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        if (file == null) {
            return access.badRequest("No file was uploaded.", "Attach the CSVW document in the 'file' part.");
        }
        if (file.length() > (long) config.maxFileSizeMb() * 1024L * 1024L) {
            return access.badRequest("The document is too large.", "A template is not this big.");
        }
        FileTemplate template;
        try {
            template = new CsvwReader(ObjectMapperContextResolver.getObjectMapper())
                    .read(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (CsvwException e) {
            return access.badRequest("This is not a template.", e.getMessage());
        }
        template.ownedBy(currentUser.getUri(), TemplateAuthor.nameOf(currentUser));
        if (template.getName() == null || template.getName().isBlank()) {
            template.rename("template");
        }
        access.templates().save(template);
        return Response.status(Response.Status.CREATED)
                .entity(new SingleObjectResponse<>(FileTemplateDTO.fromModel(template, currentUser.getUri())))
                .type(MediaType.APPLICATION_JSON).build();
    }

    private Response notFound() {
        return new ErrorResponse(Response.Status.NOT_FOUND, "Unknown template",
                "No template of yours, or shared with you, carries this identifier.").getResponse();
    }

    private SessionAccess access() {
        return new SessionAccess(sparql, nosql, fs, config, llm, sessions, coreModule, currentUser);
    }
}
