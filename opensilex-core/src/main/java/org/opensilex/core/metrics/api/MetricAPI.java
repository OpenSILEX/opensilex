//******************************************************************************
//                          MetricsAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.api;

import io.swagger.annotations.*;
import org.opensilex.OpenSilex;
import org.opensilex.core.CoreConfig;
import org.opensilex.core.CoreModule;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.exception.DateMappingExceptionResponse;
import org.opensilex.core.exception.DateValidationException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.metrics.dal.*;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        MetricDAO metricsDao = new MetricDAO(sparql, nosql, currentUser);
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

        MetricDAO metricsDao = new MetricDAO(sparql, nosql, currentUser);
        ListWithPagination<SystemSummaryModel> systemSummaries = metricsDao.getSystemSummaryWithPagination(startInstant, endInstant, page, pageSize, currentUser.getLanguage());

        //Convert paginated list to DTO
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

        MetricDAO metricsDao = new MetricDAO(sparql, nosql, currentUser);

        //Get SystemSummaryModels for startInstant and endInstant in order to calculate later the differences between both SystemSummaryModels
        List<SystemSummaryModel> firstAndLastPeriodSystemSummaries = metricsDao.getSystemSummaryFirstAndLastByPeriod(startInstant, endInstant, currentUser.getLanguage());

        //Calculate differences between start and end instant summaries -> substract entity-specific counts
        if(firstAndLastPeriodSystemSummaries != null){
            MetricPeriodDTO dto =  substractMetricModel(firstAndLastPeriodSystemSummaries);
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

        MetricDAO metricsDao = new MetricDAO(sparql, nosql, currentUser);

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

    //substractMetricModel() substracts two SystemSummaryModels (latest and oldest)
    //and returns a MetricPeriodDTO containing the differences as entity-specific CountListItemPeriodDTO lists
    private MetricPeriodDTO substractMetricModel(List<SystemSummaryModel> searchFirstLast) {
        MetricPeriodDTO periodDTO = new MetricPeriodDTO();

        if(searchFirstLast != null) {
            SystemSummaryModel latestSummary = searchFirstLast.get(0);
            SystemSummaryModel oldestSummary = (searchFirstLast.size() == 1) ? null : searchFirstLast.get(1); //check if there is a second SystemSummaryModel or not (searchFirstLast.size() == 1)

            //Experiment
            if (latestSummary.getExperimentByType() != null) {
                CountListItemPeriodDTO experimentList = new CountListItemPeriodDTO();
                experimentList.setType(latestSummary.getExperimentByType().getType());
                experimentList.setName(latestSummary.getExperimentByType().getName());
                experimentList.setTotalItemsCount((latestSummary.getExperimentByType().getTotalCount()));

                if ((oldestSummary != null) && (oldestSummary.getExperimentByType() != null)) {
                    experimentList.setTotalDifferenceItemsCount(latestSummary.getExperimentByType().getTotalCount() - oldestSummary.getExperimentByType().getTotalCount());
                }
                else{
                    experimentList.setTotalDifferenceItemsCount(0);
                }

                periodDTO.setExperimentList(experimentList);
            }

            //OS
            if (latestSummary.getScientificObjectsByType() != null) {
                CountListItemPeriodDTO scientificObjectList = new CountListItemPeriodDTO();

                if ((oldestSummary != null) && (oldestSummary.getScientificObjectsByType() != null)) {
                    List<CountItemModel> latestOSitemList = latestSummary.getScientificObjectsByType().getItems();
                    List<CountItemModel> oldestOSitemList = oldestSummary.getScientificObjectsByType().getItems();

                    scientificObjectList = substractTwoCountItemModelLists(latestOSitemList, oldestOSitemList); //contains recently added Scientific Objects
                }
                else{
                    scientificObjectList.setTotalDifferenceItemsCount(0);
                }
                scientificObjectList.setType(latestSummary.getScientificObjectsByType().getType());
                scientificObjectList.setName(latestSummary.getScientificObjectsByType().getName());
                scientificObjectList.setTotalItemsCount(latestSummary.getScientificObjectsByType().getTotalCount());

                periodDTO.setScientificObjectList(scientificObjectList);
            }

            //Variables
            if (latestSummary.getDataByVariables() != null) {
                CountListItemPeriodDTO dataList = new CountListItemPeriodDTO();

                if ((oldestSummary != null) && (oldestSummary.getDataByVariables() != null)) {
                    List<CountItemModel> latestVarItemList = latestSummary.getDataByVariables().getItems();
                    List<CountItemModel> oldestVarItemList = oldestSummary.getDataByVariables().getItems();

                    dataList = substractTwoCountItemModelLists(latestVarItemList, oldestVarItemList); //contains recently added data
                }
                else{
                    dataList.setTotalDifferenceItemsCount(0);
                }

                dataList.setType(latestSummary.getDataByVariables().getType());
                dataList.setName(latestSummary.getDataByVariables().getName());
                dataList.setTotalItemsCount(latestSummary.getDataByVariables().getTotalCount());

                periodDTO.setDataList(dataList);
            }

            //Device
            if (latestSummary.getDeviceByType() != null) {
                CountListItemPeriodDTO deviceList = new CountListItemPeriodDTO();

                if ((oldestSummary != null) && (oldestSummary.getDeviceByType() != null)) {
                    List<CountItemModel> latestDeviceItemList = latestSummary.getDeviceByType().getItems();
                    List<CountItemModel> oldestDeviceItemList = oldestSummary.getDeviceByType().getItems();

                    deviceList = substractTwoCountItemModelLists(latestDeviceItemList, oldestDeviceItemList); //contains recently added devices
                }
                else{
                    deviceList.setTotalDifferenceItemsCount(0);
                }
                deviceList.setType(latestSummary.getDeviceByType().getType());
                deviceList.setName(latestSummary.getDeviceByType().getName());
                deviceList.setTotalItemsCount(latestSummary.getDeviceByType().getTotalCount());

                periodDTO.setDeviceList(deviceList);
            }

            //Germplasm
            if (latestSummary.getGermplasmByType() != null) {
                CountListItemPeriodDTO germplasmList = new CountListItemPeriodDTO();

                if ((oldestSummary != null) && (oldestSummary.getGermplasmByType() != null)) {
                    List<CountItemModel> latestGermplasmItemList = latestSummary.getGermplasmByType().getItems();
                    List<CountItemModel> oldestGermplasmItemList = oldestSummary.getGermplasmByType().getItems();

                    germplasmList = substractTwoCountItemModelLists(latestGermplasmItemList, oldestGermplasmItemList); //contains recently added germplasms
                }
                else{
                    germplasmList.setTotalDifferenceItemsCount(0);
                }
                germplasmList.setType(latestSummary.getGermplasmByType().getType());
                germplasmList.setName(latestSummary.getGermplasmByType().getName());
                germplasmList.setTotalItemsCount(latestSummary.getGermplasmByType().getTotalCount());

                periodDTO.setGermplasmList(germplasmList);
            }
        }

        return periodDTO;
    }


    //substractTwoCountItemModelLists() calculates the count differences (latest minus oldest) of two List<CountItemModel>
    //and returns a List<CountListItemPeriodDTO> containing all items which were recently added (since oldestItemList)
    public CountListItemPeriodDTO substractTwoCountItemModelLists(List<CountItemModel> latestItemList, List<CountItemModel> oldestItemList){
        CountListItemPeriodDTO periodList = new CountListItemPeriodDTO();
        Integer totalCountDiff = 0; //count over all item counts which were added within a specific entity type (i.e. over all ScientificObject item counts)

        if (latestItemList != null) {
            for (CountItemModel item : latestItemList) {
                //CountItemModel diffCountItemModel = new CountItemModel();
                Boolean itemIsInOldSysSummary = false;
                Integer countDiff = 0; //NEW

                if (oldestItemList != null) {
                    //check if actual item is present in oldestSummary()
                    for (CountItemModel oldItem : oldestItemList) {
                        if (item.getUri().equals(oldItem.getUri())) {
                            //do substraction of period specific item counts
                            countDiff = item.getCount() - oldItem.getCount();
                            itemIsInOldSysSummary = true; //indicate that this item is present in oldestSystemSummary
                            break;
                        }
                    }
                }

                //if item is NOT contained in oldestSummary => add count of latestSysSummary
                if (!itemIsInOldSysSummary) {
                    countDiff = item.getCount();//NEW
                }

                if(countDiff > 0){
                    //CountItemDTO addedItem = new CountItemDTO();
                    CountItemPeriodDTO addedItem = new CountItemPeriodDTO();
                    addedItem.setName(item.getName());
                    addedItem.setType(item.getType());
                    addedItem.setUri(item.getUri());
                    addedItem.setCount(item.getCount());
                    addedItem.setDifferenceCount(countDiff);

                    periodList.addDifferenceItem(addedItem);

                }

                totalCountDiff += countDiff;
                //diffCountListItemModel.addItem(diffCountItemModel); //add item to common item list
            }
        }
        periodList.setTotalDifferenceItemsCount(totalCountDiff);
        return periodList;
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
