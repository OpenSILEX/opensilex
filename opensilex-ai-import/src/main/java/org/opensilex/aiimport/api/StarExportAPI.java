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
import org.opensilex.aiimport.export.ExperimentSnapshot;
import org.opensilex.aiimport.export.ExperimentSnapshotReader;
import org.opensilex.aiimport.export.StarWorkbookBuilder;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;

/**
 * Writes an experiment of this instance as a STAR workbook — the other direction from the import.
 * <p>
 * Nothing about it needs the language model: it reads through the platform's own DAOs, under the
 * current user's rights, and writes the file the STAR profile reads. It stays available when the
 * assistant is not configured.
 *
 * @author Arnaud Charleroy
 */
@Api(AiImportAPI.API_TAG)
@Path(AiImportAPI.PATH + "/" + StarExportAPI.STAR_PATH)
public class StarExportAPI {

    public static final String STAR_PATH = "star";

    public static final String XLSX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @GET
    @ApiOperation(
            value = "Export an experiment as a STAR workbook",
            notes = "The experiment, its facilities, factor levels, scientific objects — one sheet per "
                    + "type — and their observations, with a dictionary describing every column. "
                    + "The file reads back through the STAR profile of the import assistant."
    )
    @ApiProtected
    @Produces({XLSX_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The workbook"),
            @ApiResponse(code = 400, message = "Too many values for a workbook", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Unknown experiment, or not visible to this user",
                    response = ErrorResponse.class)
    })
    public Response exportStar(
            @ApiParam(value = "Experiment URI", required = true, example = "test:id/experiment/xp")
            @QueryParam("experiment") @NotNull URI experiment
    ) throws Exception {
        ExperimentSnapshot snapshot;
        try {
            snapshot = new ExperimentSnapshotReader(sparql, nosql, fs, currentUser).read(experiment);
        } catch (ExperimentSnapshotReader.TooManyObservationsException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Too many values for a workbook",
                    e.getCount() + " values are recorded in this experiment, beyond the "
                            + ExperimentSnapshotReader.MAX_OBSERVATIONS + " a STAR workbook carries. "
                            + "Use the platform's data export, which filters by variable and by date.")
                    .getResponse();
        }
        byte[] workbook = new StarWorkbookBuilder(LocalDate.now()).build(snapshot);
        return Response.ok(workbook, XLSX_MEDIA_TYPE)
                .header("Content-Disposition", "attachment; filename=\"" + fileName(snapshot) + "\"")
                .build();
    }

    /**
     * {@code STAR_} and the experiment's name, reduced to what every file system accepts.
     */
    public static String fileName(ExperimentSnapshot snapshot) {
        String name = snapshot.getName() == null ? "experiment" : snapshot.getName();
        return "STAR_" + name.replaceAll("[^A-Za-z0-9._-]+", "_") + ".xlsx";
    }
}
