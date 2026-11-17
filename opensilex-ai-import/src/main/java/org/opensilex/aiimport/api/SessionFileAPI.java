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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.api.dto.SessionFileDTO;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.profile.custom.UserDefinedProfile;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.AiImportSessionCache;
import org.opensilex.aiimport.service.LlmService;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.aiimport.service.SessionFiles;
import org.opensilex.aiimport.workbook.SheetSources;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.core.CoreModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * The files of a conversation: an import is often several files — the plan in one, the
 * measurements in another — and the user brings them as they come. Each file brings its sheets, and
 * everything after reads the sheets of all of them together.
 * <p>
 * Only a conversation the user describes themselves holds several files: a file a template reads is
 * known to the module as a whole.
 *
 * @author Arnaud Charleroy
 */
@Api(AiImportAPI.API_TAG)
@Path(AiImportAPI.PATH + "/" + AiImportAPI.SESSIONS_PATH + "/{sessionId}/" + SessionFileAPI.PATH)
public class SessionFileAPI {

    public static final String PATH = "files";

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionFileAPI.class);

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
    @ApiOperation(value = "List the files of a conversation", notes = "In the order they were added, each with "
            + "the sheets it brings. Nothing is written.")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The files", response = SessionFileDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response getFiles(
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
        return new PaginatedListResponse<>(SessionFileDTO.fromModel(found.get())).getResponse();
    }

    @POST
    @ApiOperation(
            value = "Add a file to a conversation",
            notes = "A spreadsheet or a CSV file. The report is worked out again with its sheets, and the "
                    + "conversation is from then on read as the user describes it. Nothing is written "
                    + "to the instance."
    )
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The files of the conversation, the new one last",
                    response = SessionFileDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "A file that cannot be read, is too large, is already there, or "
                    + "cannot be added to this conversation", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation", response = ErrorResponse.class)
    })
    public Response addFile(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Spreadsheet or CSV file to add", required = true, type = "file")
            @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileDetail
    ) {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(sessionId);
        if (!found.isPresent()) {
            return access.notFound();
        }
        AiImportSession session = found.get();
        if (file == null) {
            return access.badRequest("No file was uploaded.", "Attach the file in the 'file' part.");
        }
        String fileName = fileDetail == null || fileDetail.getFileName() == null
                ? "uploaded-file"
                : fileDetail.getFileName();

        Optional<Response> rejected = UploadRules.reject(access, config, file, fileName);
        if (rejected.isPresent()) {
            return rejected.get();
        }
        Optional<String> why = SessionFiles.whyNotAdd(session, fileName);
        if (why.isPresent()) {
            return access.badRequest("This file cannot be added.", why.get());
        }

        WorkbookStructure structure;
        try {
            structure = SheetSources.read(file, fileName);
        } catch (WorkbookReadException e) {
            LOGGER.warn("Could not read the added file {}", fileName, e);
            return access.badRequest("The file could not be read.",
                    "Check that " + fileName + " is a valid workbook or CSV file and is not password protected.");
        }

        SessionFile added = SessionFiles.add(session, fileName, structure);
        try {
            access.store().saveFile(session.getId(), added.getId(), file);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not store the file {} of import conversation {}", fileName, session.getId(), e);
        }
        session.setProfileId(UserDefinedProfile.ID);
        access.chat().revalidate(session);
        access.persist(session);
        return new PaginatedListResponse<>(SessionFileDTO.fromModel(session)).getResponse();
    }

    @DELETE
    @Path("{fileId}")
    @ApiOperation(
            value = "Remove a file from a conversation",
            notes = "With what the user said of its sheets. A conversation keeps at least one file."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The files that remain", response = SessionFileDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 400, message = "The only file of the conversation, or a conversation read by "
                    + "a template", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown conversation or file", response = ErrorResponse.class)
    })
    public Response removeFile(
            @ApiParam(value = "Conversation identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "File identifier", required = true)
            @PathParam("fileId") @NotNull String fileId
    ) {
        SessionAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<AiImportSession> found = access.find(sessionId);
        if (!found.isPresent()) {
            return access.notFound();
        }
        AiImportSession session = found.get();
        if (session.getFiles().stream().noneMatch(file -> file.getId().equals(fileId))) {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Unknown file",
                    "This conversation holds no file with this identifier.").getResponse();
        }
        Optional<String> why = SessionFiles.whyNotRemove(session);
        if (why.isPresent()) {
            return access.badRequest("This file cannot be removed.", why.get());
        }

        SessionFiles.remove(session, fileId);
        access.store().deleteFile(session.getId(), fileId);
        access.chat().revalidate(session);
        access.persist(session);
        return new PaginatedListResponse<>(SessionFileDTO.fromModel(session)).getResponse();
    }

    private SessionAccess access() {
        return new SessionAccess(sparql, nosql, fs, config, llm, sessions, coreModule, currentUser);
    }
}
