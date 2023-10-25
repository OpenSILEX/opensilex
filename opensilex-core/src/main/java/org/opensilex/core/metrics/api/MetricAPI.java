//******************************************************************************
//                          MetricsAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.api;

import io.swagger.annotations.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.opensilex.OpenSilex;
import org.opensilex.core.CoreConfig;
import org.opensilex.core.CoreModule;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.exception.DateMappingExceptionResponse;
import org.opensilex.core.exception.DateValidationException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.metrics.dal.*;
import org.opensilex.core.metrics.service.MetricsService;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_MAXIMAL_DATE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_MINIMAL_DATE;

/**
 * API for metrics
 * @author Arnaud Charleroy
 */
@Api(MetricAPI.CREDENTIAL_METRICS_GROUP_ID)
@Path("/core/metrics")
@ApiCredentialGroup(
        groupId = MetricAPI.CREDENTIAL_METRICS_GROUP_ID,
        groupLabelKey = MetricAPI.CREDENTIAL_METRICS_GROUP_LABEL_KEY
)
public class MetricAPI {

    public static final String CREDENTIAL_METRICS_GROUP_ID = "Metrics";
    public static final String CREDENTIAL_METRICS_GROUP_LABEL_KEY = "credential-groups.metrics";

    public static final String EXPERIMENT_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";
    public static final String EXPERIMENT_API_VALUE = "Metrics URI";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private OpenSilex openSilex;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService mongodb;

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

        MetricsService metricsService = new MetricsService(mongodb, sparql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, mongodb);
        Set<URI> runningUserExperiments = experimentDAO.getRunningUserExperiments(currentUser);

        // Build filter, run search and concert to DTOs
        ExperimentSummarySearchFilter filter = new ExperimentSummarySearchFilter();
        filter.setStartInstant(startInstant)
                .setEndInstant(endInstant);

        filter.setExperiments(runningUserExperiments)
                .setPage(page)
                .setPageSize(pageSize)
                .setLang(currentUser.getLanguage());

        ListWithPagination<ExperimentSummaryModel> experimentSummaries = metricsService.searchExperimentSummaries(filter);

        // #TODO allow the use of direct transformation of model inside DTO (avoid double list create)
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
        @ApiResponse(code = 200, message = "System metrics retrieved", response = MetricDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "System metrics not found", response = ErrorResponse.class)
    })
    public Response getSystemMetrics(
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

        MetricsService metricsService = new MetricsService(mongodb, sparql);

        // Build filter, run search and concert to DTOs
        SystemSummarySearchFilter filter = new SystemSummarySearchFilter();
        filter.setStartInstant(startInstant)
                .setEndInstant(endInstant)
                .setPage(page)
                .setPageSize(pageSize)
                .setLang(currentUser.getLanguage());

        ListWithPagination<SystemSummaryModel> systemSummaries = metricsService.searchSystemSummaries(filter);
        ListWithPagination<MetricDTO> dtoList = systemSummaries.convert(MetricDTO.class, MetricDTO::getDTOfromSystemSummaryModel);

        return new PaginatedListResponse<>(dtoList).getResponse();
    }


    @GET
    @Path("system/summary")
    @ApiOperation("Get system metrics summary")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "System metrics retrieved", response = MetricPeriodDTO.class),
            @ApiResponse(code = 404, message = "System metrics not found", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "System metrics not available", response = ErrorResponse.class)
    })
    public Response getSystemMetricsSummary(
            @ApiParam(value = "Search by minimal date", example = "DAY, WEEK, MONTH, YEAR") @QueryParam("period") String period,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        if (!openSilex.getModuleConfig(CoreModule.class, CoreConfig.class).metrics().enableMetrics()) {
            return new ErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Service Unavailable", "Metrics are not enabled on this instance").getResponse();
        }

        Instant startInstant = null;
        Instant endInstant = Instant.now();

        //get Instant of a specific 'period'
        switch(period){
            case "day":
                startInstant = ZonedDateTime.now().minusDays(1).toInstant();
                break;
            case "week" :
                startInstant = ZonedDateTime.now().minusWeeks(1).toInstant();
                break;
            case "month" :
                startInstant = ZonedDateTime.now().minusMonths(1).toInstant();
                break;
            default: //year
                startInstant = ZonedDateTime.now().minusYears(1).toInstant();
        }

        MetricsService metricsService = new MetricsService(mongodb, sparql);


        //Get SystemSummaryModels for startInstant and endInstant in order to calculate later the differences between both SystemSummaryModels
        Pair<SystemSummaryModel, SystemSummaryModel> firstAndLastPeriodSystemSummaries = metricsService.getSystemSummaryFirstAndLastByPeriod(startInstant, endInstant, currentUser.getLanguage());

        //Calculate differences between start and end instant summaries -> substract entity-specific counts
        if(firstAndLastPeriodSystemSummaries != null){
            MetricPeriodDTO dto = substractSummaries(firstAndLastPeriodSystemSummaries);
            dto.setStartDate(startInstant);
            dto.setEndDate(endInstant);

            return new SingleObjectResponse<>(dto).getResponse();
        }
        else{
            throw new NotFoundException("No metrics available for selected period");
        }
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

        validateContextAccess(experimentURI);

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

        MetricsService metricsService = new MetricsService(mongodb, sparql);

        // Build filter, run search and concert to DTOs
        ExperimentSummarySearchFilter filter = new ExperimentSummarySearchFilter();
        filter.setStartInstant(startInstant)
                .setEndInstant(endInstant);

        filter.setExperiments(Collections.singletonList(experimentURI))
                .setPage(page)
                .setPageSize(1)
                .setLang(currentUser.getLanguage());

        List<ExperimentSummaryModel> experimentSummaries = metricsService.searchExperimentSummaries(filter).getList();

        if (CollectionUtils.isEmpty(experimentSummaries)) {
            throw new NotFoundURIException("Metrics on experiment URI not found:", experimentURI);
        }
        return new SingleObjectResponse<>(MetricDTO.getDTOfromExperimentSummaryModel(experimentSummaries.get(0))).getResponse();
    }

    private CountListItemPeriodDTO substractItemListModel(CountListItemModel latest, CountListItemModel oldest) {
        Objects.requireNonNull(latest);

        CountListItemPeriodDTO periodDTO = new CountListItemPeriodDTO();
        periodDTO.setType(latest.getType());
        periodDTO.setName(latest.getName());
        periodDTO.setTotalItemsCount(latest.getTotalCount());

        if (oldest != null) {
            periodDTO.setTotalDifferenceItemsCount(latest.getTotalCount() - oldest.getTotalCount());
        } else {
            periodDTO.setTotalDifferenceItemsCount(0);
        }
        return periodDTO;
    }

    //substractMetricModel() substracts two SystemSummaryModels (latest and oldest)
    //and returns a MetricPeriodDTO containing the differences as entity-specific CountListItemPeriodDTO lists
    private MetricPeriodDTO substractSummaries(Pair<SystemSummaryModel, SystemSummaryModel> latestAndOldestSummaries) {
        MetricPeriodDTO periodDTO = new MetricPeriodDTO();
        if (latestAndOldestSummaries == null) {
            return periodDTO;
        }

        SystemSummaryModel latestSummary = latestAndOldestSummaries.getLeft();
        SystemSummaryModel oldestSummary = latestAndOldestSummaries.getRight(); //check if there is a second SystemSummaryModel or not (searchFirstLast.size() == 1)

        if (latestSummary.getExperimentByType() != null) {
            periodDTO.setExperimentList(substractItemListModel(latestSummary.getExperimentByType(), oldestSummary.getExperimentByType()));
        }
        if (latestSummary.getScientificObjectsByType() != null) {
            periodDTO.setScientificObjectList(substractItemListModel(latestSummary.getScientificObjectsByType(), oldestSummary.getScientificObjectsByType()));
        }
        if (latestSummary.getGermplasmByType() != null) {
            periodDTO.setGermplasmList(substractItemListModel(latestSummary.getGermplasmByType(), oldestSummary.getGermplasmByType()));
        }
        if (latestSummary.getDeviceByType() != null) {
            periodDTO.setDeviceList(substractItemListModel(latestSummary.getDeviceByType(), oldestSummary.getDeviceByType()));
        }
        if (latestSummary.getDataByVariables() != null) {
            periodDTO.setDataList(substractItemListModel(latestSummary.getDataByVariables(), oldestSummary.getDataByVariables()));
        }

        return periodDTO;
    }



    private void validateContextAccess(URI contextURI) throws Exception {
        if (sparql.uriExists(ExperimentModel.class, contextURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, mongodb);

            xpDAO.validateExperimentAccess(contextURI, currentUser);
        } else {
            throw new NotFoundURIException("Experiment URI not found:", contextURI);
        }
    }

}
