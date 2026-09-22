//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.OpenSilex;
import org.opensilex.monitoring.config.HealthConfig;
import org.opensilex.monitoring.extension.HealthIndicator;
import org.opensilex.monitoring.extension.HealthStatus;
import org.opensilex.monitoring.extension.MonitoringExtension;
import org.opensilex.monitoring.extension.ProbeResult;
import org.opensilex.monitoring.health.stats.MongoStatsReader;
import org.opensilex.monitoring.health.stats.MongoVolumetry;
import org.opensilex.monitoring.health.stats.TripleStoreStatsReader;
import org.opensilex.monitoring.health.stats.TripleStoreVolumetry;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the health probes and serves database statistics from a cache.
 *
 * <p>The cache is not an optimisation, it is a correctness property of a monitoring page: the page
 * is polled, and a triple count computed on every poll would turn the dashboard into a sustained
 * load source on the very instance it is supposed to be watching. Freshness is reported alongside
 * the figures so the operator can see what they are looking at.</p>
 *
 * <p>The probe list is assembled once, from the module's own indicators plus whatever every
 * {@link MonitoringExtension} contributes, so adding a component to the health check never means
 * editing this class.</p>
 *
 * @author Arnaud Charleroy
 */
public class DatabaseStatsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseStatsService.class);

    private static final String CACHE_KEY = "current";

    private final HealthConfig config;
    private final HealthProbeRunner runner;
    private final List<HealthIndicator> indicators = new ArrayList<>();

    private final MongoStatsReader mongoStatsReader;
    private final TripleStoreStatsReader tripleStoreStatsReader;

    private final Cache<String, TripleStoreVolumetry> tripleStoreCache;
    private final Cache<String, MongoVolumetry> mongoCache;

    public DatabaseStatsService(OpenSilex opensilex, MongoDBServiceV2 mongodb, HealthConfig config) {
        this.config = config;
        this.runner = new HealthProbeRunner(config.maxConcurrentChecks(), config.checkTimeoutMs());
        this.mongoStatsReader = new MongoStatsReader(mongodb, config);
        this.tripleStoreStatsReader = new TripleStoreStatsReader(opensilex, config);

        this.tripleStoreCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(Math.max(1, config.tripleCountRefreshMinutes())))
                .maximumSize(1)
                .build();
        this.mongoCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(Math.max(1, config.mongoStatsRefreshSeconds())))
                .maximumSize(1)
                .build();

        this.indicators.add(new MongoHealthIndicator(mongodb));
        this.indicators.add(new TripleStoreHealthIndicator(opensilex));

        for (MonitoringExtension extension : opensilex.getModulesImplementingInterface(MonitoringExtension.class)) {
            try {
                this.indicators.addAll(extension.getHealthIndicators());
            } catch (Exception e) {
                LOGGER.warn("Module {} failed to contribute health indicators",
                        extension.getClass().getSimpleName(), e);
            }
        }
        LOGGER.debug("Monitoring health indicators: {}", this.indicators.size());
    }

    /**
     * Registers one more probe. Used by the module to add components it owns and that are only
     * available once the application is running, such as the access log writer.
     */
    public void register(HealthIndicator indicator) {
        this.indicators.add(indicator);
    }

    /**
     * @return every probe outcome, indexed by component name, in declaration order
     */
    public Map<String, ProbeResult> probeAll() {
        Map<String, ProbeResult> results = new LinkedHashMap<>();
        for (HealthIndicator indicator : indicators) {
            results.put(indicator.name(), runner.run(indicator));
        }
        return results;
    }

    /**
     * @return the worst status among all components, {@link HealthStatus#UP} when there is none
     */
    public HealthStatus overall(Map<String, ProbeResult> results) {
        HealthStatus worst = HealthStatus.UP;
        for (ProbeResult result : results.values()) {
            worst = worst.worst(result.status());
        }
        return worst;
    }

    public TripleStoreVolumetry tripleStoreVolumetry() {
        return tripleStoreCache.get(CACHE_KEY, key -> tripleStoreStatsReader.read());
    }

    public MongoVolumetry mongoVolumetry() {
        return mongoCache.get(CACHE_KEY, key -> mongoStatsReader.read());
    }

    /** @return true when the returned triple store figures came from the cache rather than the store */
    public boolean isTripleStoreVolumetryCached() {
        return tripleStoreCache.getIfPresent(CACHE_KEY) != null;
    }

    public boolean isMongoVolumetryCached() {
        return mongoCache.getIfPresent(CACHE_KEY) != null;
    }

    public HealthConfig getConfig() {
        return config;
    }

    public void shutdown() {
        runner.shutdown();
    }
}
