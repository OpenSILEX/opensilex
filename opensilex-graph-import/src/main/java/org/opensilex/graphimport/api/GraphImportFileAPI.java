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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.graphimport.api.dto.GraphImportFileDTO;
import org.opensilex.graphimport.session.GraphImportSession;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.graphimport.session.SessionFiles;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
 * The files of an import. An import is often several files — the plan in one, the measurements in
 * another — and the user brings them as they come; their sheets are read together.
 *
 * @author Arnaud Charleroy
 */
@Api(GraphImportAPI.API_TAG)
@Path(GraphImportAPI.PATH + "/" + GraphImportAPI.SESSIONS_PATH + "/{sessionId}/" + GraphImportFileAPI.PATH)
public class GraphImportFileAPI {

    public static final String PATH = "files";

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphImportFileAPI.class);

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @Inject
    private GraphImportConfig config;

    @Inject
    private GraphImportSessionCache sessions;

    @POST
    @ApiOperation(
            value = "Add a file to an import",
            notes = "A spreadsheet or a CSV file, read with the others. Nothing is written to the instance."
    )
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The files of the import, the new one last",
                    response = GraphImportFileDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "A file that is missing, too large, of another kind, unreadable or "
                    + "already there", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response addFile(
            @ApiParam(value = "Import identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Spreadsheet or CSV file to add", required = true, type = "file")
            @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileDetail
    ) {
        GraphImportAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> found = access.find(sessionId);
        if (found.isEmpty()) {
            return access.notFound();
        }
        GraphImportSession session = found.get();
        String fileName = GraphImportAPI.fileNameOf(fileDetail);
        Optional<Response> rejected = access.reject(file, fileName);
        if (rejected.isPresent()) {
            return rejected.get();
        }
        Optional<String> why = session.whyNotAdd(fileName);
        if (why.isPresent()) {
            return access.badRequest("This file cannot be added.", why.get());
        }

        SessionFile read;
        try {
            read = SessionFiles.read(file, fileName);
        } catch (WorkbookReadException e) {
            LOGGER.warn("Could not read the added file {}", fileName, e);
            return access.unreadable(fileName);
        }
        session.add(read);
        try {
            access.store().saveFile(session.getId(), read.getId(), file);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not store the file {} of import {}", fileName, session.getId(), e);
        }
        access.persist(session);
        return new PaginatedListResponse<>(GraphImportFileDTO.fromModel(session)).getResponse();
    }

    @DELETE
    @Path("{fileId}")
    @ApiOperation(value = "Remove a file from an import", notes = "An import keeps at least one file.")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The files that remain", response = GraphImportFileDTO.class,
                    responseContainer = "List"),
            @ApiResponse(code = 400, message = "The only file of the import", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import or file", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response removeFile(
            @ApiParam(value = "Import identifier", required = true)
            @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "File identifier", required = true)
            @PathParam("fileId") @NotNull String fileId
    ) {
        GraphImportAccess access = access();
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> found = access.find(sessionId);
        if (found.isEmpty()) {
            return access.notFound();
        }
        GraphImportSession session = found.get();
        if (session.getFile(fileId).isEmpty()) {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Unknown file",
                    "This import holds no file with this identifier.").getResponse();
        }
        Optional<String> why = session.whyNotRemove();
        if (why.isPresent()) {
            return access.badRequest("This file cannot be removed.", why.get());
        }
        session.remove(fileId);
        access.store().deleteFile(session.getId(), fileId);
        access.persist(session);
        return new PaginatedListResponse<>(GraphImportFileDTO.fromModel(session)).getResponse();
    }

    private GraphImportAccess access() {
        return new GraphImportAccess(nosql, fs, config, sessions, currentUser);
    }
}
