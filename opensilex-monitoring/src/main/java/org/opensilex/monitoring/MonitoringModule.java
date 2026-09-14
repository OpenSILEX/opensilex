//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring;

import org.glassfish.jersey.server.ServerProperties;
import org.opensilex.OpenSilexModule;
import org.opensilex.monitoring.activity.ActivityAggregator;
import org.opensilex.monitoring.activity.MongoActivityAggregator;
import org.opensilex.monitoring.health.DatabaseStatsService;
import org.opensilex.monitoring.log.RequestLogHealthIndicator;
import org.opensilex.monitoring.log.RequestLogService;
import org.opensilex.monitoring.log.jersey.MonitoringRequestListener;
import org.opensilex.monitoring.log.dal.RequestLogDao;
import org.opensilex.monitoring.presence.ActiveUserRegistry;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.server.rest.RestApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instance monitoring: health check, web service access log and activity report.
 *
 * <p>The module owns its two long-lived collaborators rather than declaring them as configured
 * services. A module is already a singleton with a lifecycle and is already injectable into any
 * resource, so declaring them in the configuration interface would buy nothing and would force
 * their settings under a nested {@code config:} key beside an {@code implementation:} sibling —
 * turning {@code monitoring.health.checkTimeoutMs} into
 * {@code monitoring.databaseStats.config.checkTimeoutMs} for no gain.</p>
 *
 * @author Arnaud Charleroy
 */
public class MonitoringModule extends OpenSilexModule implements APIExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringModule.class);

    public static final String CONFIG_ID = "monitoring";

    private DatabaseStatsService databaseStatsService;
    private RequestLogService requestLogService;
    private ActivityAggregator activityAggregator;
    private final ActiveUserRegistry activeUserRegistry = new ActiveUserRegistry();

    @Override
    public Class<?> getConfigClass() {
        return MonitoringConfig.class;
    }

    @Override
    public String getConfigId() {
        return CONFIG_ID;
    }

    /**
     * Registers the Jersey listener, only when the access log is switched on.
     *
     * <p>{@code initRestApplication} rather than the more common conditional in
     * {@code getPackagesToScan}: the class reference is checked by the compiler, where a package
     * name spelled into a string is checked by nobody, and a rename that silently stops registering
     * a listener is a bug that only shows up as missing data weeks later.</p>
     *
     * <p>Note that this does not gate the API itself: {@code APIExtension.apiPackages()} scans every
     * {@code @Path} class on the classpath, so the resource is always reachable and each of its
     * methods checks {@link MonitoringConfig#enabled()} for itself.</p>
     */
    @Override
    public void initRestApplication(RestApplication resourceConfig) {
        MonitoringConfig config = getMonitoringConfig();
        if (!config.enabled() || !config.requestLog().enabled()) {
            return;
        }
        resourceConfig.register(MonitoringRequestListener.class);

        if (config.requestLog().recordDuration()) {
            // Jersey then keeps its own per-resource-method timing statistics, which gives a
            // "slowest endpoints" view for free on top of our per-user measurements. Off unless
            // durations are wanted, because the collection is not free.
            resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, true);
        }
    }

    @Override
    public void startup() throws Exception {
        MonitoringConfig config = getMonitoringConfig();
        if (!config.enabled()) {
            LOGGER.info("monitoring: module disabled by configuration");
            return;
        }

        MongoDBServiceV2 mongodb = getOpenSilex()
                .getServiceInstance(MongoDBServiceV2.DEFAULT_SERVICE, MongoDBServiceV2.class);

        this.databaseStatsService = new DatabaseStatsService(getOpenSilex(), mongodb, config.health());
        this.requestLogService = new RequestLogService(mongodb, config.requestLog());
        this.activityAggregator = new MongoActivityAggregator(mongodb, requestLogService.getDao());
        this.databaseStatsService.register(new RequestLogHealthIndicator(requestLogService));

        mongodb.registerIndexes(RequestLogDao.COLLECTION_NAME, RequestLogDao.getIndexes());

        if (!getOpenSilex().isTest() && !getOpenSilex().isReservedProfile()) {
            mongodb.createIndexes();
            // Kept out of registerIndexes on purpose: see RequestLogDao.ensureTtlIndex.
            RequestLogDao.ensureTtlIndex(mongodb, config.requestLog().retentionDays());
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (requestLogService != null) {
            requestLogService.stop();
        }
        if (databaseStatsService != null) {
            databaseStatsService.shutdown();
        }
    }

    public MonitoringConfig getMonitoringConfig() {
        return getConfig(MonitoringConfig.class);
    }

    public DatabaseStatsService getDatabaseStatsService() {
        return databaseStatsService;
    }

    public RequestLogService getRequestLogService() {
        return requestLogService;
    }

    public ActivityAggregator getActivityAggregator() {
        return activityAggregator;
    }

    public ActiveUserRegistry getActiveUserRegistry() {
        return activeUserRegistry;
    }
}
