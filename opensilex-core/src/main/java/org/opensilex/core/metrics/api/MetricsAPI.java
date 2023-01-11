//******************************************************************************
//                          MetricsAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.api;

import io.swagger.annotations.*;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.Min;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_MAXIMAL_DATE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_MINIMAL_DATE;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.exception.DateMappingExceptionResponse;
import org.opensilex.core.exception.DateValidationException;
import org.opensilex.core.metrics.dal.ExperimentSummaryModel;
import org.opensilex.core.metrics.dal.MetricsDAO;
import org.opensilex.core.metrics.dal.SystemSummaryModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.utils.ListWithPagination;

/**
 * API for metrics
 * @author Arnaud Charleroy
 */
@Api(MetricsAPI.CREDENTIAL_METRICS_GROUP_ID)
@Path("/core/metrics")
@ApiCredentialGroup(
        groupId = MetricsAPI.CREDENTIAL_METRICS_GROUP_ID,
        groupLabelKey = MetricsAPI.CREDENTIAL_METRICS_GROUP_LABEL_KEY
)
public class MetricsAPI {

    public static final String CREDENTIAL_METRICS_GROUP_ID = "Metrics";
    public static final String CREDENTIAL_METRICS_GROUP_LABEL_KEY = "credential-groups.metrics";

    public static final String EXPERIMENT_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";
    public static final String EXPERIMENT_API_VALUE = "Metrics URI";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @GET
    @Path("running_experiments")
    @ApiOperation("Get running experiments metrics")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Metrics retrieved", response = MetricDTO.class),
        @ApiResponse(code = 404, message = "Metrics not found", response = ErrorResponse.class)
    })
    public Response getRunningExperimentsSummary(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        
        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            try {
                startInstant = DataValidateUtils.getDateInstant(startDate, null, Boolean.FALSE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }

        if (endDate != null) {
            try {
                endInstant = DataValidateUtils.getDateInstant(endDate, null, Boolean.TRUE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }
        
        MetricsDAO metricsDao = new MetricsDAO(sparql, nosql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        Set<URI> runningUserExperiments = experimentDAO.getRunningUserExperiments(currentUser);
        ListWithPagination<ExperimentSummaryModel> experimentSummaries = metricsDao.getExperimentSummaries(new ArrayList<>(runningUserExperiments), startInstant, endInstant, page, pageSize, currentUser.getLanguage());

        // Convert paginated list to DTO
        ListWithPagination<MetricDTO> dtoList = experimentSummaries.convert(MetricDTO.class, MetricDTO::getDTOfromExperimentSummaryModel);

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("system")
    @ApiOperation("Get system metrics")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "System metrics retrieved", response = MetricDTO.class),
        @ApiResponse(code = 404, message = "System metrics not found", response = ErrorResponse.class)
    })
    public Response getSystemSummary(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        
        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            try {
                startInstant = DataValidateUtils.getDateInstant(startDate, null, Boolean.FALSE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }

        if (endDate != null) {
            try {
                endInstant = DataValidateUtils.getDateInstant(endDate, null, Boolean.TRUE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }
        
        MetricsDAO metricsDao = new MetricsDAO(sparql, nosql);
        ListWithPagination<SystemSummaryModel> systemSummaries = metricsDao.getSystemSummary(startInstant, endInstant,page, pageSize, currentUser.getLanguage());

        //  Convert paginated list to DTO
        ListWithPagination<MetricDTO> dtoList = systemSummaries.convert(MetricDTO.class, MetricDTO::getDTOfromSystemSummaryModel);

        return new PaginatedListResponse<>(dtoList).getResponse();
    }


    @GET
    @Path("experiment/{uri}")
    @ApiOperation("Get an experiment summary history")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment metrics history retrieved", response = MetricDTO.class),
        @ApiResponse(code = 404, message = "Experiment metrics not found", response = ErrorResponse.class)
    })
    public Response getExperimentSummaryHistory(
            @ApiParam(value = EXPERIMENT_API_VALUE, example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull @ValidURI URI experimentURI,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            try {
                startInstant = DataValidateUtils.getDateInstant(startDate, null, Boolean.FALSE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }

        if (endDate != null) {
            try {
                endInstant = DataValidateUtils.getDateInstant(endDate, null, Boolean.TRUE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }

        MetricsDAO metricsDao = new MetricsDAO(sparql, nosql);

        validateContextAccess(experimentURI);
        List<URI> experimentUri = Arrays.asList(experimentURI);
        ListWithPagination<ExperimentSummaryModel> experimentSummaries = metricsDao.getExperimentSummaries(experimentUri, startInstant, endInstant, page, pageSize, currentUser.getLanguage());
        ExperimentSummaryModel experimentSummary = null;
        if (experimentSummaries != null && !experimentSummaries.getList().isEmpty()) {
            experimentSummary = experimentSummaries.getList().get(0);
        }
        if (experimentSummary == null) {
            throw new NotFoundURIException("Metrics on experiment URI not found:", experimentURI);
        }
        return new SingleObjectResponse<>(MetricDTO.getDTOfromExperimentSummaryModel(experimentSummary)).getResponse();
    }

    private void validateContextAccess(URI contextURI) throws Exception {
        if (sparql.uriExists(ExperimentModel.class, contextURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);

            xpDAO.validateExperimentAccess(contextURI, currentUser);
        } else {
            throw new NotFoundURIException("Experiment URI not found:", contextURI);
        }
    }

}
