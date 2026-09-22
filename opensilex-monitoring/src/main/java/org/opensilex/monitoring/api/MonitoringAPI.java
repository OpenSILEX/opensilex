//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.OpenSilex;
import org.opensilex.monitoring.MonitoringConfig;
import org.opensilex.monitoring.MonitoringModule;
import org.opensilex.monitoring.activity.ActivityBucket;
import org.opensilex.monitoring.activity.Granularity;
import org.opensilex.monitoring.api.dto.ActivityBucketDTO;
import org.opensilex.monitoring.api.dto.ActivityReportDTO;
import org.opensilex.monitoring.api.dto.ComponentHealthDTO;
import org.opensilex.monitoring.api.dto.ConnectedUserDTO;
import org.opensilex.monitoring.api.dto.DbStatsDTO;
import org.opensilex.monitoring.api.dto.HealthCheckDTO;
import org.opensilex.monitoring.api.dto.MongoStatsDTO;
import org.opensilex.monitoring.api.dto.RequestLogDTO;
import org.opensilex.monitoring.api.dto.RequestLogStatusDTO;
import org.opensilex.monitoring.api.dto.TripleStoreStatsDTO;
import org.opensilex.monitoring.api.dto.UserPresenceDTO;
import org.opensilex.monitoring.extension.ProbeResult;
import org.opensilex.monitoring.health.DatabaseStatsService;
import org.opensilex.monitoring.log.RequestLogService;
import org.opensilex.monitoring.log.dal.RequestLogModel;
import org.opensilex.monitoring.log.dal.RequestLogSearchFilter;
import org.opensilex.security.SecurityConfig;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.utils.ListWithPagination;

/**
 * Instance monitoring API.
 *
 * <p>The health check and the database statistics are two endpoints rather than one payload with
 * conditional fields. Splitting them lets {@code @ApiProtected(adminOnly = true)} do the enforcing
 * declaratively, before the method body runs — one refactor cannot leak volumetry to an ordinary
 * user — and it means the expensive work simply cannot be triggered by a non-admin, which is a
 * denial-of-service property and not only a privacy one.</p>
 *
 * @author Arnaud Charleroy
 */
@Api(MonitoringAPI.CREDENTIAL_GROUP_ID)
@Path(MonitoringAPI.PATH)
@ApiCredentialGroup(
        groupId = MonitoringAPI.CREDENTIAL_GROUP_ID,
        groupLabelKey = MonitoringAPI.CREDENTIAL_GROUP_LABEL_KEY
)
public class MonitoringAPI {

    public static final String PATH = "/monitoring";
    public static final String CREDENTIAL_GROUP_ID = "Monitoring";
    public static final String CREDENTIAL_GROUP_LABEL_KEY = "credential-groups.monitoring";

    public static final String HEALTHCHECK_PATH = "healthcheck";
    public static final String DB_STATS_PATH = "dbstats";
    public static final String ACTIVITY_PATH = "activity";
    public static final String REQUESTS_PATH = "requests";

    private static final String DATE_EXAMPLE = "2026-03-01T00:00:00Z";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private OpenSilex opensilex;

    @Inject
    private MonitoringModule module;

    @Inject
    private AuthenticationService authentication;

    @GET
    @Path(HEALTHCHECK_PATH)
    @ApiOperation("Check that the instance and its databases are reachable")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Health of every monitored component", response = HealthCheckDTO.class),
        @ApiResponse(code = 503, message = "Monitoring is disabled", response = ErrorResponse.class)
    })
    public Response healthcheck() {
        Response disabled = refuseWhenDisabled();
        if (disabled != null) {
            return disabled;
        }

        DatabaseStatsService stats = module.getDatabaseStatsService();
        Map<String, ProbeResult> results = stats.probeAll();

        HealthCheckDTO dto = new HealthCheckDTO();
        dto.setCheckedAt(Instant.now());
        dto.setStatus(stats.overall(results).name());

        List<ComponentHealthDTO> components = new ArrayList<>();
        results.forEach((name, result) -> {
            ComponentHealthDTO component = new ComponentHealthDTO();
            component.setName(name);
            component.setStatus(result.status().name());
            component.setResponseTimeMs(result.durationMs());
            component.setMessage(result.message());
            components.add(component);
        });
        dto.setComponents(components);

        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path(DB_STATS_PATH)
    @ApiOperation("Get database volumetry and instance presence")
    @ApiProtected(adminOnly = true)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Database statistics", response = DbStatsDTO.class),
        @ApiResponse(code = 403, message = "Reserved to administrators", response = ErrorResponse.class),
        @ApiResponse(code = 503, message = "Monitoring is disabled", response = ErrorResponse.class)
    })
    public Response dbStats() {
        Response disabled = refuseWhenDisabled();
        if (disabled != null) {
            return disabled;
        }

        DatabaseStatsService stats = module.getDatabaseStatsService();

        DbStatsDTO dto = new DbStatsDTO();
        dto.setTripleStore(TripleStoreStatsDTO.fromModel(stats.tripleStoreVolumetry(), stats.isTripleStoreVolumetryCached()));
        dto.setMongodb(MongoStatsDTO.fromModel(stats.mongoVolumetry(), stats.isMongoVolumetryCached()));
        dto.setUsers(presence());
        dto.setRequestLog(requestLogStatus());

        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path(ACTIVITY_PATH)
    @ApiOperation("Get platform activity over a period")
    @ApiProtected(adminOnly = true)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Activity report", response = ActivityReportDTO.class),
        @ApiResponse(code = 400, message = "Invalid period", response = ErrorResponse.class),
        @ApiResponse(code = 403, message = "Reserved to administrators", response = ErrorResponse.class),
        @ApiResponse(code = 503, message = "Monitoring is disabled", response = ErrorResponse.class)
    })
    public Response activity(
            @ApiParam(value = "Start of the period, inclusive", example = DATE_EXAMPLE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "End of the period, exclusive", example = DATE_EXAMPLE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Bucket size", example = "DAY") @QueryParam("granularity") @DefaultValue("DAY") String granularity
    ) {
        Response disabled = refuseWhenDisabled();
        if (disabled != null) {
            return disabled;
        }

        Instant to = endDate == null || endDate.isBlank() ? Instant.now() : parseInstant(endDate);
        Instant from = startDate == null || startDate.isBlank()
                ? to.minus(7, ChronoUnit.DAYS)
                : parseInstant(startDate);

        if (from == null || to == null) {
            return badRequest("Invalid date", "start_date and end_date must be ISO-8601 instants");
        }
        if (!from.isBefore(to)) {
            return badRequest("Invalid period", "start_date must be before end_date");
        }

        MonitoringConfig config = module.getMonitoringConfig();
        Duration period = Duration.between(from, to);
        if (period.toDays() > config.activity().maxRangeDays()) {
            return badRequest("Period too long",
                    "at most " + config.activity().maxRangeDays() + " days may be queried at once");
        }

        Granularity requested = parseGranularity(granularity);
        if (requested == null) {
            return badRequest("Invalid granularity", "expected one of HOUR, DAY, WEEK, MONTH");
        }

        // Coarsen rather than refuse: a caller asking for hourly buckets over a year gets a usable
        // answer plus a flag saying what happened, instead of a 400 they have to guess their way out
        // of. The cap itself is not negotiable, since a hand-written URL would otherwise be a
        // trivial denial of service against MongoDB.
        Granularity effective = requested;
        boolean truncated = false;
        for (Granularity candidate : Granularity.values()) {
            if (candidate.ordinal() < effective.ordinal()) {
                continue;
            }
            effective = candidate;
            if (candidate.bucketCount(period) <= config.activity().maxBuckets()) {
                break;
            }
            truncated = true;
        }

        List<ActivityBucket> buckets = module.getActivityAggregator().aggregate(from, to, effective);

        long totalRequests = 0;
        long totalClientErrors = 0;
        long totalServerErrors = 0;
        List<ActivityBucketDTO> bucketDtos = new ArrayList<>(buckets.size());
        for (ActivityBucket bucket : buckets) {
            totalRequests += bucket.requests();
            totalClientErrors += bucket.clientErrors();
            totalServerErrors += bucket.serverErrors();

            ActivityBucketDTO bucketDto = new ActivityBucketDTO();
            bucketDto.setBucket(bucket.bucket());
            bucketDto.setUsers(bucket.users());
            bucketDto.setRequests(bucket.requests());
            bucketDto.setClientErrors(bucket.clientErrors());
            bucketDto.setServerErrors(bucket.serverErrors());
            bucketDto.setErrorPercentage(bucket.errorPercentage());
            bucketDtos.add(bucketDto);
        }

        ActivityReportDTO dto = new ActivityReportDTO();
        dto.setStartDate(from);
        dto.setEndDate(to);
        dto.setGranularity(effective.name());
        dto.setTruncated(truncated);
        dto.setBuckets(bucketDtos);
        dto.setTotalRequests(totalRequests);
        dto.setTotalClientErrors(totalClientErrors);
        dto.setTotalServerErrors(totalServerErrors);
        // Computed on the totals, not by averaging the per-bucket rates: a bucket with three calls
        // must not weigh as much as one with thirty thousand.
        long totalErrors = totalClientErrors + totalServerErrors;
        dto.setFailurePercentage(totalRequests == 0 ? 0d : (totalErrors * 100d) / totalRequests);
        dto.setDistinctUsers(module.getActivityAggregator().countDistinctUsers(from, to));

        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path(REQUESTS_PATH)
    @ApiOperation("Search recorded web service calls")
    @ApiProtected(adminOnly = true)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Recorded calls", response = RequestLogDTO.class, responseContainer = "List"),
        @ApiResponse(code = 403, message = "Reserved to administrators", response = ErrorResponse.class),
        @ApiResponse(code = 503, message = "Monitoring is disabled", response = ErrorResponse.class)
    })
    public Response searchRequests(
            @ApiParam(value = "Start of the period, inclusive", example = DATE_EXAMPLE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "End of the period, exclusive", example = DATE_EXAMPLE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Filter on one account") @QueryParam("account") URI account,
            @ApiParam(value = "Filter on a path prefix", example = "core/data") @QueryParam("path") String pathPrefix,
            @ApiParam(value = "Filter on the HTTP method", example = "GET") @QueryParam("http_method") String httpMethod,
            @ApiParam(value = "Keep only successful or only failed calls") @QueryParam("success") Boolean success,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        Response disabled = refuseWhenDisabled();
        if (disabled != null) {
            return disabled;
        }

        RequestLogSearchFilter filter = new RequestLogSearchFilter();
        filter.setStartDate(startDate == null || startDate.isBlank() ? null : parseInstant(startDate))
                .setEndDate(endDate == null || endDate.isBlank() ? null : parseInstant(endDate))
                .setAccount(account)
                .setPathPrefix(pathPrefix)
                .setHttpMethod(httpMethod)
                .setSuccess(success);
        filter.setPage(page);
        filter.setPageSize(pageSize);
        filter.setOrderByList(List.of(new org.opensilex.utils.OrderBy(
                RequestLogModel.START_TIME_FIELD + "=desc")));

        ListWithPagination<RequestLogModel> results =
                module.getRequestLogService().getDao().searchWithPagination(filter);

        return new PaginatedListResponse<>(results.convert(RequestLogDTO.class, RequestLogDTO::fromModel))
                .getResponse();
    }

    // ------------------------------------------------------------------------------------------
    // Mapping and helpers
    // ------------------------------------------------------------------------------------------

    /**
     * The resource is discovered by the classpath scan whatever the configuration says, because
     * {@code APIExtension.apiPackages()} collects every {@code @Path} class. Only the Jersey
     * listener package is registered conditionally, so each method has to refuse for itself.
     */
    private Response refuseWhenDisabled() {
        if (module.getMonitoringConfig().enabled() && module.getDatabaseStatsService() != null) {
            return null;
        }
        return new ErrorResponse(Response.Status.SERVICE_UNAVAILABLE,
                "Monitoring is disabled",
                "Set monitoring.enabled to true in the instance configuration").getResponse();
    }

    private static Response badRequest(String title, String detail) {
        return new ErrorResponse(Response.Status.BAD_REQUEST, title, detail).getResponse();
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static Granularity parseGranularity(String value) {
        try {
            return Granularity.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private UserPresenceDTO presence() {
        UserPresenceDTO dto = new UserPresenceDTO();

        Collection<AccountModel> connected = authentication.getConnectedAccounts();
        dto.setConnectedAccounts(connected.size());
        dto.setConnectedAccountsReliable(isConnectedCountReliable());

        List<ConnectedUserDTO> accounts = new ArrayList<>(connected.size());
        for (AccountModel account : connected) {
            ConnectedUserDTO userDto = new ConnectedUserDTO();
            userDto.setUri(account.getUri());
            userDto.setEmail(account.getEmail() == null ? null : account.getEmail().toString());
            if (account.getLinkedPerson() != null) {
                userDto.setFirstName(account.getLinkedPerson().getFirstName());
                userDto.setLastName(account.getLinkedPerson().getLastName());
            }
            accounts.add(userDto);
        }
        dto.setAccounts(accounts);

        int window = module.getMonitoringConfig().activity().activeUserWindowMinutes();
        dto.setActiveWindowMinutes(window);
        dto.setActiveAccounts(module.getActiveUserRegistry().countActive(window));
        return dto;
    }

    /**
     * When multi connection is allowed the token registry is never pruned, so its size stops being
     * "who is connected" and becomes "who has connected since the last restart". Reporting the
     * number without the caveat would be reporting a wrong number.
     */
    private boolean isConnectedCountReliable() {
        try {
            SecurityConfig securityConfig =
                    opensilex.getModuleConfig(SecurityModule.class, SecurityConfig.class);
            return !securityConfig.allowMultiConnection();
        } catch (Exception e) {
            return false;
        }
    }

    private RequestLogStatusDTO requestLogStatus() {
        RequestLogService writer = module.getRequestLogService();
        RequestLogStatusDTO dto = new RequestLogStatusDTO();
        if (writer == null) {
            return dto;
        }
        dto.setRunning(writer.isRunning());
        dto.setPaused(writer.isPaused());
        dto.setQueued(writer.getQueueDepth());
        dto.setQueueCapacity(writer.getQueueCapacity());
        dto.setWritten(writer.getWrittenCount());
        dto.setDropped(writer.getDroppedCount());
        dto.setFailedBatches(writer.getFailedBatchCount());
        return dto;
    }



}
