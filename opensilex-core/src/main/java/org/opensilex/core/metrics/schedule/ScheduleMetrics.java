//******************************************************************************
//                          ScheduleMetrics.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.schedule;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.CoreConfig;
import org.opensilex.core.CoreModule;
import org.opensilex.core.MetricsConfig;
import org.opensilex.core.metrics.service.MetricsService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Create System metrics with a delay defined by user for experiment and system
 *
 * @author Arnaud Charleroy
 */
@Provider
public class ScheduleMetrics implements ApplicationEventListener {

    @Inject
    private OpenSilex opensilex;

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleMetrics.class);
    public static final String SCHEDULE_METRICS = "ScheduleMetrics";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void onEvent(ApplicationEvent event) {
        switch (event.getType()) {
            case INITIALIZATION_APP_FINISHED:
                SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
                SPARQLService sparql = factory.provide();
                MongoDBService mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

                try {
                    CoreConfig coreConfig = opensilex.getModuleConfig(CoreModule.class, CoreConfig.class);
                    MetricsConfig metrics = coreConfig.metrics();

                    // Get Experiment metrics configuration
                    int experimentsTimeBeforeFirstMetric = metrics.experiments().timeBeforeFirstMetric();
                    int delayBetweenExperimentsMetrics = metrics.experiments().delayBetweenMetrics();
                    String experimentsMetricsTimeUnit = metrics.experiments().metricsTimeUnit();

                    TimeUnit experimentsTimeUnit;
                    try {
                        experimentsTimeUnit = TimeUnit.valueOf(experimentsMetricsTimeUnit);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Bad experiment time unit set" + experimentsMetricsTimeUnit);
                    }

                    // Get System metrics configuration
                    int systemTimeBeforeFirstMetric = metrics.system().timeBeforeFirstMetric();
                    int delayBetweenSystemMetrics = metrics.system().delayBetweenMetrics();
                    String systemMetricsTimeUnit = metrics.system().metricsTimeUnit();

                    TimeUnit systemTimeUnit = null;
                    try {
                        systemTimeUnit = TimeUnit.valueOf(systemMetricsTimeUnit);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Bad experiment time unit set" + systemMetricsTimeUnit);
                    }
                    MetricsService metricsService = new MetricsService(mongodb, sparql);
                    scheduler.scheduleAtFixedRate(new CreateExperimentSummaries(metricsService), experimentsTimeBeforeFirstMetric, delayBetweenExperimentsMetrics, experimentsTimeUnit);
                    scheduler.scheduleAtFixedRate(new CreateSystemSummary(metricsService), systemTimeBeforeFirstMetric, delayBetweenSystemMetrics, systemTimeUnit);

                    LOGGER.debug("start " + SCHEDULE_METRICS + " with parameters experimentsTimeBeforeFirstMetric : " + experimentsTimeBeforeFirstMetric + " , delayBetweenExperimentMetrics" + delayBetweenExperimentsMetrics + " with timeUnit" + experimentsTimeUnit + "and systemTimeBeforeFirstMetric : " + systemTimeBeforeFirstMetric + ", delayBetweenSystemMetrics" + delayBetweenSystemMetrics + " with timeUnit" + systemTimeUnit);
                } catch (OpenSilexModuleNotFoundException | RuntimeException ex) {
                    LOGGER.debug("error on start " + SCHEDULE_METRICS);
                    LOGGER.error(ex.getMessage(), ex);
                }

                break;
            case DESTROY_FINISHED:
                LOGGER.debug("stop " + SCHEDULE_METRICS);
                scheduler.shutdown();
                break;
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return null;
    }

    /**
     * Represent a thread that will be launched at each application start
     */
    private static class CreateExperimentSummaries implements Runnable {

        private final MetricsService metricsService;

        public CreateExperimentSummaries(MetricsService metricsService) {
            this.metricsService = metricsService;
        }

        @Override
        public void run() {
            try {
                metricsService.createExperimentSummary(null);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }

        }
    }

    /**
     * Represent a thread that will be launched at each application start
     */
    private static class CreateSystemSummary implements Runnable {

        private final MetricsService metricsService;

        public CreateSystemSummary(MetricsService metricsService) {
            this.metricsService = metricsService;
        }

        @Override
        public void run() {
            try {
                metricsService.createSystemSummary();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }

        }
    }
}
