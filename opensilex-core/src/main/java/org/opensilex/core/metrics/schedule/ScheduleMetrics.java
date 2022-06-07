//******************************************************************************
//                          ScheduleMetrics.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.CoreConfig;
import org.opensilex.core.CoreModule;
import org.opensilex.core.metrics.dal.MetricsDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensilex.core.MetricsConfig;

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
                MongoDBService nosql = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

                try {
                    CoreConfig coreConfig = opensilex.getModuleConfig(CoreModule.class, CoreConfig.class);
                    MetricsConfig metrics = coreConfig.metrics();

                    // Get Experiment metrics configuration
                    int experimentsTimeBeforeFirstMetric = metrics.experiments().timeBeforeFirstMetric();
                    int delayBetweenExperimentsMetrics = metrics.experiments().delayBetweenMetrics();
                    String experimentsMetricsTimeUnit = metrics.experiments().metricsTimeUnit();

                    TimeUnit experimentsTimeUnit = null;
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
                        throw new RuntimeException("Bad experiment time unit set" + systemTimeUnit);
                    }
                    MetricsDAO metricsDao = new MetricsDAO(sparql, nosql);
                    scheduler.scheduleAtFixedRate(new CreateExperimentSummaries(metricsDao), experimentsTimeBeforeFirstMetric, delayBetweenExperimentsMetrics, experimentsTimeUnit);
                    scheduler.scheduleAtFixedRate(new CreateSystemSummary(metricsDao), systemTimeBeforeFirstMetric, delayBetweenSystemMetrics, systemTimeUnit);

                    LOGGER.debug("start " + SCHEDULE_METRICS + " with parameters experimentsTimeBeforeFirstMetric : " + experimentsTimeBeforeFirstMetric + " , delayBetweenExperimentMetrics" + delayBetweenExperimentsMetrics + " with timeUnit" + experimentsTimeUnit.toString() + "and systemTimeBeforeFirstMetric : " + systemTimeBeforeFirstMetric + ", delayBetweenSystemMetrics" + delayBetweenSystemMetrics + " with timeUnit" + systemTimeUnit.toString());
                } catch (OpenSilexModuleNotFoundException ex) {
                    LOGGER.debug("error on start " + SCHEDULE_METRICS);
                    LOGGER.error(ex.getMessage(), ex);
                } catch (RuntimeException ex) {
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
    private class CreateExperimentSummaries implements Runnable {

        private final MetricsDAO metricsDao;

        public CreateExperimentSummaries(MetricsDAO metricsDao) {
            this.metricsDao = metricsDao;
        }

        @Override
        public void run() {
            try {
                metricsDao.createExperimentSummary();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }

        }
    }

    /**
     * Represent a thread that will be launched at each application start
     */
    private class CreateSystemSummary implements Runnable {

        private final MetricsDAO metricsDao;

        public CreateSystemSummary(MetricsDAO metricsDao) {
            this.metricsDao = metricsDao;
        }

        @Override
        public void run() {
            try {
                metricsDao.createSystemSummary();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }

        }
    }
}
