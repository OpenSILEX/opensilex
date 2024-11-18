package org.opensilex.core.dataV2.api;

import io.swagger.annotations.*;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.data.api.DataCSVValidationDTO;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.dataV2.service.DataService;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;

/**
 * @author Marouan
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path(DataAPI.PATH)
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class DataAPI {

    public static final String PATH = "/core/data-v2";

    protected final static Logger LOGGER = LoggerFactory.getLogger(DataAPI.class);

    public static final String CREDENTIAL_DATA_GROUP_ID = "Data";
    public static final String CREDENTIAL_DATA_GROUP_LABEL_KEY = "credential-groups.data";
    public static final String DATA_EXAMPLE_MINIMAL_DATE = "2020-08-21T00:00:00+01:00";
    public static final String CREDENTIAL_DATA_MODIFICATION_ID = "data-modification";
    public static final String CREDENTIAL_DATA_MODIFICATION_LABEL_KEY = "credential.default.modification";

    @Inject
    private MongoDBService nosql;

    @Inject
    private SPARQLService sparql;

    @Inject
    private FileStorageService fs;

    @CurrentUser
    AccountModel user;

    DataService dataService;


    @POST
    @Path("import_v2")
    @ApiOperation(value = "Import a CSV file for the given provenanceURI")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Data are imported", response = DataCSVValidationDTO.class)})
    @ApiProtected
    @ApiCredential(
            groupId = org.opensilex.core.data.api.DataAPI.CREDENTIAL_DATA_GROUP_ID,
            groupLabelKey = org.opensilex.core.data.api.DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSVData(
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") @ValidURI URI experiment,
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition, @QueryParam("validationId") String validationId) throws Exception {

        this.dataService = new DataService(nosql, sparql, fs, user);
        DataCSVValidationDTO csvValidation = dataService.importCSVDataV2(provenance, experiment, file, validationId);
        return new SingleObjectResponse<>(csvValidation).getResponse();
    }

    @POST
    @Path("import_validation_v2")
    @ApiOperation(value = "Import a CSV file for the given provenanceURI.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Data are validated", response = DataCSVValidationDTO.class)})
    @ApiProtected
    @ApiCredential(
            groupId = org.opensilex.core.data.api.DataAPI.CREDENTIAL_DATA_GROUP_ID,
            groupLabelKey = org.opensilex.core.data.api.DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") @ValidURI URI experiment,
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {

        this.dataService = new DataService(nosql, sparql, fs, user);
        DataCSVValidationModel csvValidationModel = dataService.validateWholeCsvV2(provenance, experiment, file);
        DataCSVValidationDTO csvValidation = dataService.buildDataCSVValidationDTO(csvValidationModel);
        return new SingleObjectResponse<>(csvValidation).getResponse();
    }


}
