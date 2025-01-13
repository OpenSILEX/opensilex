package org.opensilex.core.dataV2.api;

import io.swagger.annotations.*;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.api.DataCSVValidationDTO;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.dataV2.api.dto.BatchHistoryGetDTO;
import org.opensilex.core.dataV2.service.BatchHistoryService;
import org.opensilex.core.dataV2.service.DataService;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.MongoDbUniqueIndexConstraintViolation;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * @author MKourdi
 */
@Api(DataAPIV2.CREDENTIAL_DATA_GROUP_ID)
@Path(DataAPIV2.PATH)
@ApiCredentialGroup(
        groupId = DataAPIV2.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPIV2.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class DataAPIV2 {
    public static final int SIZE_MAX = 10000;

    public static final String PATH = "/core/data-v2";

    protected final static Logger LOGGER = LoggerFactory.getLogger(DataAPIV2.class);

    public static final String CREDENTIAL_DATA_GROUP_ID = "Data";
    public static final String CREDENTIAL_DATA_GROUP_LABEL_KEY = "credential-groups.data";
    public static final String DATA_EXAMPLE_MINIMAL_DATE = "2025-01-01T00:00:00+01:00";
    public static final String DATA_EXAMPLE_MAXIMAL_DATE = "2025-02-01T00:00:00+01:00";
    public static final String CREDENTIAL_DATA_MODIFICATION_ID = "data-modification";
    public static final String CREDENTIAL_DATA_MODIFICATION_LABEL_KEY = "credential.default.modification";
    public static final String CSV_EXTENSION = ".csv";
    public static final String DATA_ALREADY_EXISTS = "Data already exists";
    public static final String DUPLICATED_DATA_FOUND = "Duplicated data found ";
    public static final String CREDENTIAL_BATCH_HISTORY_DELETE_ID = "data-delete";
    public static final String CREDENTIAL_BATCH_HISTORY_DELETE_LABEL_KEY = "credential.default.delete";
    public static final String BATCH_HISTORY_URI = "Batch history URI";
    public static final String BATCH_HISTORY_EXAMPLE_URI = "http://opensilex.dev/id/batchHistory/cd3dde33d6f5dc2";


    @Inject
    private MongoDBService nosql;

    @Inject
    private SPARQLService sparql;

    @Inject
    private FileStorageService fs;

    @CurrentUser
    AccountModel user;

    DataService dataService;
    BatchHistoryService batchHistoryService;


    @POST
    @Path("import_v2")
    @ApiOperation(value = "Import a CSV file for the given provenanceURI")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Data are imported", response = DataCSVValidationDTO.class),
            @ApiResponse(code = 409, message = "A Data already exists", response = ErrorResponse.class)})
    @ApiProtected
    @ApiCredential(
            groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
            groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSVData(
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") @ValidURI URI experiment,
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file, @FormDataParam("file") FormDataContentDisposition fileDisposition,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition,
            @ApiParam(value = "The key for file that have already been validated by the API (/core/data-v2/import_validation_v2)",
                    example = "JohnDoe_20241120123045_ab12cd34") @QueryParam("validationKey") String validationKey) throws Exception {
        String fileName = getFileName(fileDisposition);
        this.dataService = new DataService(nosql, sparql, fs, user);
        DataCSVValidationDTO csvValidation;
        try {
            csvValidation = dataService.importCSVDataV2(provenance, experiment, file, fileName, validationKey);
        } catch (MongoDbUniqueIndexConstraintViolation e) {
            return new ErrorResponse(Response.Status.CONFLICT, DATA_ALREADY_EXISTS,
                    DUPLICATED_DATA_FOUND + e.getMessage()).getResponse();
        }
        return new SingleObjectResponse<>(csvValidation).getResponse();
    }

    @POST
    @Path("import_validation_v2")
    @ApiOperation(value = "Import a CSV file for the given provenanceURI.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Data are validated", response = DataCSVValidationDTO.class)})
    @ApiProtected
    @ApiCredential(
            groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
            groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") @ValidURI URI experiment,
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file, @FormDataParam("file") FormDataContentDisposition fileDisposition,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        this.dataService = new DataService(nosql, sparql, fs, user);
        String fileName = getFileName(fileDisposition);
        DataCSVValidationModel csvValidationModel = dataService.validateWholeCsvV2(provenance, experiment, file, fileName);
        DataCSVValidationDTO csvValidation = dataService.buildDataCSVValidationDTO(csvValidationModel);
        return new SingleObjectResponse<>(csvValidation).getResponse();
    }

    @GET
    @Path("batch_history")
    @ApiOperation("Get data batch history")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return batch history list", response = BatchHistoryGetDTO.class, responseContainer = "List")
    })
    public Response searchBatchHistory(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=asc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) {
        this.batchHistoryService = new BatchHistoryService(user, nosql);
        ListWithPagination<BatchHistoryGetDTO> results = batchHistoryService.getBatchHistoryWithPagination(startDate, endDate, orderByList, page, pageSize);
        return new PaginatedListResponse<>(results).getResponse();
    }

    @DELETE
    @Path("batch_history/{uri}")
    @ApiOperation("Delete batch history by URI")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_BATCH_HISTORY_DELETE_ID, credentialLabelKey = CREDENTIAL_BATCH_HISTORY_DELETE_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Data deleted", response = URI.class),
            @ApiResponse(code = 400, message = "Invalid or unknown Data URI", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteBatchHistoryByURI(
            @ApiParam(value = BATCH_HISTORY_URI, example = BATCH_HISTORY_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri
    ) throws NoSQLInvalidURIException {
        this.batchHistoryService = new BatchHistoryService(user, nosql);
        return new SingleObjectResponse<>(batchHistoryService.deleteBatchHistoryByURI(uri)).getResponse();
    }

    @GET
    @Path("/download/{fileName}")
    @ApiOperation("Download csv file")
    @ApiProtected
    @Produces("application/zip")
    public Response downloadFile(@PathParam("fileName") String fileName) {
        this.dataService = new DataService(nosql, sparql, fs, user);
        String filePath = "/home/kourdi/work/imported-csv-files/" + user.getName() + "/" + fileName + ".zip";
        // Construct the full file path
        File file = new File(filePath);

        // Check if the file exists
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            LOGGER.info("File exists: {}, Can read: {}", file.exists(), file.canRead());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("File not found or not readable: " + fileName)
                    .build();
        }

        return Response.ok(dataService.createFileStreamingOutput(file))
                .header("Content-Disposition", "attachment; filename=" + file.getName())
                .header("Content-Type", "application/zip")
                .header("Content-Length", file.length())
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Content-Transfer-Encoding", "binary")
                .header("Accept-Ranges", "none")
                .build();
    }

    private String getFileName(FormDataContentDisposition fileDisposition) {
        return fileDisposition.getFileName().split(CSV_EXTENSION)[0];
    }
}
