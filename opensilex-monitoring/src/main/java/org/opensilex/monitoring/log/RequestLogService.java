//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log;

import com.mongodb.WriteConcern;
import com.mongodb.client.model.InsertManyOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.opensilex.monitoring.config.RequestLogConfig;
import org.opensilex.monitoring.log.dal.RequestLogDao;
import org.opensilex.monitoring.log.dal.RequestLogModel;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buffers access log entries and writes them to MongoDB off the request thread.
 *
 * <p>The single guarantee this class exists to provide: <em>logging can never slow down or break
 * the API</em>. The request thread only ever calls {@link #submit}, which is a non-blocking
 * {@code offer} wrapped so that nothing can escape. When the queue is full, entries are dropped and
 * counted rather than blocking, and the drop count is surfaced by the health check — a monitoring
 * feature that silently degrades the thing it monitors is worse than no feature at all.</p>
 *
 * <p>Writes deliberately bypass {@code MongoReadWriteDao.create(List)}: that path generates a URI
 * per instance and wraps the insert in a replica-set transaction, which is precisely the cost
 * profile to avoid for fire-and-forget logging. Reads and aggregations still go through the DAO.</p>
 *
 * @author Arnaud Charleroy
 */
public class RequestLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogService.class);

    /** Never log a dropped-entry warning more often than this. */
    private static final long WARN_INTERVAL_MS = 30_000L;

    private final RequestLogConfig config;
    private final RequestLogDao dao;
    private final ArrayBlockingQueue<RequestLogModel> queue;

    private final LongAdder dropped = new LongAdder();
    private final LongAdder written = new LongAdder();
    private final LongAdder failedBatches = new LongAdder();
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong circuitOpenUntil = new AtomicLong();
    private final AtomicLong lastWarnAt = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean();

    private ScheduledExecutorService scheduler;

    public RequestLogService(MongoDBServiceV2 mongodb, RequestLogConfig config) {
        this.config = config;
        this.dao = new RequestLogDao(mongodb);
        this.queue = new ArrayBlockingQueue<>(Math.max(16, config.queueCapacity()));
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "monitoring-request-log-writer");
            thread.setDaemon(true);
            return thread;
        });
        long interval = Math.max(100, config.flushIntervalMs());
        scheduler.scheduleWithFixedDelay(this::flushQuietly, interval, interval, TimeUnit.MILLISECONDS);
        LOGGER.info("monitoring: access log writer started (queue {}, batch {}, flush {} ms)",
                queue.remainingCapacity(), config.batchSize(), interval);
    }

    /**
     * Hands an entry over for writing. Called from the request thread, so it must not block, must
     * not allocate much, and must not throw.
     */
    public void submit(RequestLogModel entry) {
        try {
            if (!running.get() || isCircuitOpen()) {
                dropped.increment();
                return;
            }
            if (!queue.offer(entry)) {
                dropped.increment();
                warnRateLimited("monitoring: access log queue is full, entries are being dropped");
            }
        } catch (Throwable ignored) {
            // Nothing about logging may ever surface in an API response.
            dropped.increment();
        }
    }

    private void flushQuietly() {
        try {
            flush();
        } catch (Throwable t) {
            LOGGER.debug("monitoring: access log flush failed", t);
        }
    }

    /** @return the number of entries written */
    int flush() {
        if (isCircuitOpen()) {
            return 0;
        }
        int total = 0;
        List<RequestLogModel> batch = new ArrayList<>(Math.max(1, config.batchSize()));
        while (queue.drainTo(batch, Math.max(1, config.batchSize())) > 0) {
            try {
                dao.getCollection()
                        .withWriteConcern(WriteConcern.W1)
                        .insertMany(batch, new InsertManyOptions().ordered(false));
                written.add(batch.size());
                total += batch.size();
                consecutiveFailures.set(0);
            } catch (Exception e) {
                failedBatches.increment();
                dropped.add(batch.size());
                // The batch is discarded, never requeued: a deterministically failing batch — an
                // oversized document, say — would otherwise wedge the queue permanently.
                if (consecutiveFailures.incrementAndGet() >= Math.max(1, config.maxConsecutiveFailures())) {
                    long backoff = Math.max(1, config.failureBackoffSeconds()) * 1000L;
                    circuitOpenUntil.set(System.currentTimeMillis() + backoff);
                    consecutiveFailures.set(0);
                    LOGGER.warn("monitoring: access log writer paused for {} s after repeated"
                            + " write failures", config.failureBackoffSeconds(), e);
                } else {
                    warnRateLimited("monitoring: access log batch write failed: " + e.getMessage());
                }
            } finally {
                batch.clear();
            }
        }
        return total;
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
        // One last drain so a clean shutdown does not throw away what is already buffered.
        running.set(true);
        try {
            circuitOpenUntil.set(0L);
            flushQuietly();
        } finally {
            running.set(false);
        }
        LOGGER.info("monitoring: access log writer stopped ({} written, {} dropped)",
                written.sum(), dropped.sum());
    }

    private boolean isCircuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntil.get();
    }

    private void warnRateLimited(String message) {
        long now = System.currentTimeMillis();
        long previous = lastWarnAt.get();
        if (now - previous > WARN_INTERVAL_MS && lastWarnAt.compareAndSet(previous, now)) {
            LOGGER.warn(message);
        }
    }

    public int getQueueDepth() {
        return queue.size();
    }

    public int getQueueCapacity() {
        return queue.size() + queue.remainingCapacity();
    }

    public long getDroppedCount() {
        return dropped.sum();
    }

    public long getWrittenCount() {
        return written.sum();
    }

    public long getFailedBatchCount() {
        return failedBatches.sum();
    }

    public boolean isPaused() {
        return isCircuitOpen();
    }

    public boolean isRunning() {
        return running.get();
    }

    public RequestLogDao getDao() {
        return dao;
    }
}
