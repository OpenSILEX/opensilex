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
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.graphimport.api.dto.GraphImportSessionDTO;
import org.opensilex.graphimport.api.dto.SheetLayoutDTO;
import org.opensilex.graphimport.api.dto.SheetRowsDTO;
import org.opensilex.graphimport.session.GraphImportSession;
import org.opensilex.graphimport.session.GraphImportSessionCache;
import org.opensilex.graphimport.session.SheetLayouts;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;

/**
 * The header row and the first row of values of a sheet: shown as the file holds them, and chosen
 * by the user when the reader guessed wrong. Nothing here writes to the instance.
 *
 * @author Arnaud Charleroy
 */
@Api(GraphImportAPI.API_TAG)
@Path(GraphImportAPI.PATH)
public class GraphImportLayoutAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphImportLayoutAPI.class);
    private static final String UNREADABLE = "The stored copy of the file cannot be read";

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

    @GET
    @Path(GraphImportAPI.SESSIONS_PATH + "/{sessionId}/sheets/{sheet}/rows")
    @ApiOperation(
            value = "Get the first rows of a sheet as the file holds them",
            notes = "Rows above the header and empty rows included, numbered as the spreadsheet numbers them, "
                    + "with the layout in use and the one the reader found."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The rows", response = SheetRowsDTO.class),
            @ApiResponse(code = 404, message = "Unknown import or sheet", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response getSheetRows(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Sheet name, as the import shows it", required = true) @PathParam("sheet") @NotNull
            String sheet
    ) {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        try {
            Optional<SheetLayouts.SheetRows> rows = SheetLayouts.rows(access.store(), session.get(), sheet);
            if (rows.isEmpty()) {
                return access.notFound();
            }
            return new SingleObjectResponse<>(SheetRowsDTO.fromModel(sheet, rows.get())).getResponse();
        } catch (IOException e) {
            LOGGER.warn("Could not read the rows of sheet {} of import {}", sheet, sessionId, e);
            return access.badRequest(UNREADABLE, e.getMessage());
        }
    }

    @DELETE
    @Path(GraphImportAPI.SESSIONS_PATH + "/{sessionId}/sheets/{sheet}/layout")
    @ApiOperation(
            value = "Give a sheet back the header row and first row of values the reader found",
            notes = "Forgets the layout the user chose; a sheet of text read as a table becomes text again."
    )
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The import, with the sheet as read", response = GraphImportSessionDTO.class),
            @ApiResponse(code = 404, message = "Unknown import or sheet", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response resetSheetLayout(
            @ApiParam(value = "Import identifier", required = true) @PathParam("sessionId") @NotNull String sessionId,
            @ApiParam(value = "Sheet name, as the import shows it", required = true) @PathParam("sheet") @NotNull
            String sheet
    ) {
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        Optional<GraphImportSession.SheetLocation> location = session.flatMap(found -> found.locate(sheet));
        if (location.isEmpty()) {
            return access.notFound();
        }
        try {
            SheetLayouts.reset(access.store(), session.get(), location.get());
        } catch (IOException e) {
            LOGGER.warn("Could not reset sheet {} of import {}", sheet, sessionId, e);
            return access.badRequest(UNREADABLE, e.getMessage());
        }
        access.persist(session.get());
        return new SingleObjectResponse<>(GraphImportSessionDTO.fromModel(session.get())).getResponse();
    }

    @PUT
    @Path("sheet_layouts")
    @ApiOperation(
            value = "Choose the header row and the first row of values of a sheet",
            notes = "Rows are numbered from 1, as in the spreadsheet. The choice is stored with the import; the "
                    + "sheet is named as the import shows it."
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The import, with the sheet laid out", response = GraphImportSessionDTO.class),
            @ApiResponse(code = 400, message = "Rows outside the sheet, or values above the header",
                    response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown import or sheet", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "The assistant is not enabled", response = ErrorResponse.class)
    })
    public Response setSheetLayout(
            @ApiParam(value = "The sheet and its layout", required = true) @NotNull @Valid SheetLayoutDTO layout
    ) {
        String sessionId = layout.getSessionId();
        String sheet = layout.getSheet();
        GraphImportAccess access = new GraphImportAccess(nosql, fs, config, sessions, currentUser);
        if (!access.isEnabled()) {
            return access.disabled();
        }
        if (sessionId == null || sheet == null) {
            return access.badRequest("Missing import or sheet", "Name the import (session_id) and the sheet.");
        }
        Optional<GraphImportSession> session = access.find(sessionId);
        if (session.isEmpty()) {
            return access.notFound();
        }
        Optional<GraphImportSession.SheetLocation> location = session.get().locate(sheet);
        if (location.isEmpty()) {
            return access.notFound();
        }
        try {
            Optional<String> unfit = SheetLayouts.apply(access.store(), session.get(), location.get(), layout.toModel());
            if (unfit.isPresent()) {
                return access.badRequest("This layout does not fit the sheet", unfit.get());
            }
        } catch (IOException e) {
            LOGGER.warn("Could not lay out sheet {} of import {}", sheet, sessionId, e);
            return access.badRequest(UNREADABLE, e.getMessage());
        }
        access.persist(session.get());
        return new SingleObjectResponse<>(GraphImportSessionDTO.fromModel(session.get())).getResponse();
    }
}
