//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.health;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opensilex.monitoring.extension.HealthIndicator;
import org.opensilex.monitoring.extension.ProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs health probes off the request thread, under a hard deadline, and never throws.
 *
 * <p>The deadline is the whole point. RDF4J is reached through an HTTP repository whose connection
 * manager sets no socket timeout, and {@code RDF4JConfig.timeout()} defaults to zero, so a frozen
 * rdf4j-server would otherwise hang the health endpoint for as long as it stays frozen — turning
 * the page meant to diagnose the outage into another casualty of it.</p>
 *
 * <p>The pool is small and rejects rather than queueing without bound: a supervisor polling the
 * health endpoint every second must not be able to exhaust the RDF4J connection pool.</p>
 *
 * @author Arnaud Charleroy
 */
public class HealthProbeRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthProbeRunner.class);

    private final ThreadPoolExecutor pool;
    private final long timeoutMs;

    public HealthProbeRunner(int maxConcurrentChecks, long timeoutMs) {
        this.timeoutMs = timeoutMs;
        int size = Math.max(1, maxConcurrentChecks);
        this.pool = new ThreadPoolExecutor(
                size, size,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(size * 4),
                runnable -> {
                    Thread thread = new Thread(runnable, "monitoring-health-probe");
                    thread.setDaemon(true);
                    return thread;
                },
                // Abort, never CallerRuns: a saturated pool must not spill probe work back onto
                // the request thread we are trying to protect.
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * @return the probe outcome, or a DOWN/UNKNOWN result describing why there is none
     */
    public ProbeResult run(HealthIndicator indicator) {
        Future<ProbeResult> future;
        try {
            future = pool.submit(indicator::probe);
        } catch (RejectedExecutionException e) {
            return ProbeResult.unknown("probe pool saturated");
        }

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return ProbeResult.down(timeoutMs, "timeout after " + timeoutMs + " ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return ProbeResult.unknown("interrupted");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LOGGER.debug("Health probe '{}' failed", indicator.name(), cause);
            return ProbeResult.down(0L, describe(cause));
        }
    }

    private static String describe(Throwable t) {
        String message = t.getMessage();
        return message == null || message.isBlank() ? t.getClass().getSimpleName() : message;
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
