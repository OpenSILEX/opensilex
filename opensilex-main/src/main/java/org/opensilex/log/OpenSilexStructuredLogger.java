package org.opensilex.log;

import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.opensilex.log.LogFilter.*;
import static org.opensilex.log.LogFilter.STATUS_OK_LOG_ARG;

/**
 * A class for an easier manipulation of structured logging.
 * This class take e a {@link Logger} as constructor argument and reuse this logger for write info/error message.
 *
 * <ul>
 *     <li>This class provides methods for INFO, DEBUG and ERROR levels</li>
 *     <li>The {@code logInfoStart(*)} and {@code logDebugStart(*)} methods writes a log entry and return the {@link Instant} just before the operation logging</li>
 *     <li>The {@code logInfoOk(*)} and {@code logDebugOk(*)} methods take an instant (should be the instant returned by a {@code logInfoStart(*)} or {@code logDebugStart(*)} method) and write the elapsed duration in millisecond as an additional log entry (key, value)</li>
 *    <li> Each {@code logInfo(*) and logDebug(*) method can take zero, one or two additional (key,value) property to write inside the log entry. These properties are in addition of the following key-pair}
 *    <ul>
 *        <li>(LOG_TYPE_KEY, log_type) </li>
 *        <li>(LOG_STATUS_LOG_KEY, status)</li>
 *        <li>(The #structuredArgument passed inside the constructor)</li>
 *    </ul>
 * </ul>
 *
 * @see #logInfoStart(String, String, Object, String, Object)
 * @see #logInfoStart(String, String, Object)
 * @see #logInfoOk(String, Instant, String, Object)
 * @see #logInfoOk(String, Instant, String, Object, String, Object)
 *
 * @author rcolin
 */
public abstract class OpenSilexStructuredLogger {

    private static final String DEFAULT_MESSAGE_FORMAT = "{} {}";
    private final StructuredArgument structuredArgument;
    private final Logger logger;

    protected OpenSilexStructuredLogger(Logger logger, StructuredArgument structuredArgument) {
        this.structuredArgument = structuredArgument;
        this.logger = logger;
    }

    public Instant logInfoStart(String type) {
        return logInfoStart(type, null, null);
    }

    // region INFO

    /**
     * Write a log entry with INFO level with the following key/value :
     *
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: START</li>
     *     <li>{@code key}: {@code value}</li>
     * </ul>
     *
     * @param type  the type of the operation
     * @param key   key to write inside log entry
     * @param value value to write inside log entry
     * @return the current {@link Instant}, pass this one to {@link #logInfoOk(String, Instant, String, Object)} method, for duration compute and write inside log
     */
    public Instant logInfoStart(String type, String key, Object value) {
        if (key != null) {
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, structuredArgument, kv(key, value));
        } else {
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, structuredArgument);
        }
        return Instant.now();
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     *
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: START</li>
     *     <li>{@code key}: {@code value}</li>
     *     <li>{@code key2}: {@code value2}</li>
     * </ul>
     *
     * @param type   the type of the operation
     * @param key    key to write inside log entry
     * @param value  value to write inside log entry
     * @param key2   2nd key to write inside log entry
     * @param value2 2nd value to write inside log entry
     * @return the current {@link Instant}, pass this one to {@link #logInfoOk(String, Instant, String, Object)} method, for duration compute and write inside log
     */
    public Instant logInfoStart(String type, String key, Object value, String key2, Object value2) {
        logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, structuredArgument, kv(key, value), kv(key2, value2));
        return Instant.now();
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     *     <li>{@code key}: {@code value}</li>
     * </ul>
     *
     * @param type  the type of the operation
     * @param start Start of the operation, used to compute duration in ms
     * @param key   key to write inside log entry
     * @param value value to write inside log entry
     */

    public void logInfoOk(String type, Instant start, String key, Object value) {
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        if (key != null) {
            if (structuredArgument != null) {
                logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value));
            } else {
                logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value));
            }
        } else {
            if (structuredArgument != null) {
                logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument, kv(LOG_DURATION_MS_KEY, durationMs));
            } else {
                logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs));
            }
        }
    }

    public void logInfoOk(String type){
        if (structuredArgument != null) {
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument);
        } else {
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG);
        }
    }


    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     * </ul>
     *
     * @param type  the type of the operation
     * @param start Start of the operation, used to compute duration in ms
     */

    public void logInfoOk(String type, Instant start) {
        this.logInfoOk(type, start, null, null);
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     *     <li>{@code key}: {@code value}</li>
     *     <li>{@code key2}: {@code value2}</li>
     * </ul>
     *
     * @param type   the type of the operation
     * @param start  Start of the operation, used to compute duration in ms
     * @param key    key to write inside log entry
     * @param value  value to write inside log entry
     * @param key2   2nd key to write inside log entry
     * @param value2 2nd value to write inside log entry
     */

    public void logInfoOk(String type, Instant start, String key, Object value, String key2, Object value2) {
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        if (structuredArgument != null) {
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value), kv(key2, value2));
        } else {
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value), kv(key2, value2));
        }
    }

    // region DEBUG

    public Instant logDebugStart(String type) {
        return logDebugStart(type, null, null);
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     *
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: START</li>
     *     <li>{@code key}: {@code value}</li>
     * </ul>
     *
     * @param type  the type of the operation
     * @param key   key to write inside log entry
     * @param value value to write inside log entry
     * @return the current {@link Instant}, pass this one to {@link #logInfoOk(String, Instant, String, Object)} method, for duration compute and write inside log
     */
    public Instant logDebugStart(String type, String key, Object value) {
        if (key != null) {
            logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, structuredArgument, kv(key, value));
        } else {
            logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, structuredArgument);
        }
        return Instant.now();
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     *
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: START</li>
     *     <li>{@code key}: {@code value}</li>
     *     <li>{@code key2}: {@code value2}</li>
     * </ul>
     *
     * @param type   the type of the operation
     * @param key    key to write inside log entry
     * @param value  value to write inside log entry
     * @param key2   2nd key to write inside log entry
     * @param value2 2nd value to write inside log entry
     * @return the current {@link Instant}, pass this one to {@link #logInfoOk(String, Instant, String, Object)} method, for duration compute and write inside log
     */
    public Instant logDebugStart(String type, String key, Object value, String key2, Object value2) {
        logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, structuredArgument, kv(key, value), kv(key2, value2));
        return Instant.now();
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     *     <li>{@code key}: {@code value}</li>
     * </ul>
     *
     * @param type  the type of the operation
     * @param start Start of the operation, used to compute duration in ms
     * @param key   key to write inside log entry
     * @param value value to write inside log entry
     */

    public void logDebugOk(String type, Instant start, String key, Object value) {
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        if (key != null) {
            if (structuredArgument != null) {
                logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value));
            } else {
                logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value));
            }
        } else {
            if (structuredArgument != null) {
                logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument, kv(LOG_DURATION_MS_KEY, durationMs));
            } else {
                logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs));
            }
        }
    }

    public void logDebugOk(String type){
        if (structuredArgument != null) {
            logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument);
        } else {
            logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG);
        }
    }


    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     * </ul>
     *
     * @param type  the type of the operation
     * @param start Start of the operation, used to compute duration in ms
     */

    public void logDebugOk(String type, Instant start) {
        this.logDebugOk(type, start, null, null);
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     *     <li>{@code key}: {@code value}</li>
     *     <li>{@code key2}: {@code value2}</li>
     * </ul>
     *
     * @param type   the type of the operation
     * @param start  Start of the operation, used to compute duration in ms
     * @param key    key to write inside log entry
     * @param value  value to write inside log entry
     * @param key2   2nd key to write inside log entry
     * @param value2 2nd value to write inside log entry
     */

    public void logDebugOk(String type, Instant start, String key, Object value, String key2, Object value2) {
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        if (structuredArgument != null) {
            logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value), kv(key2, value2));
        } else {
            logger.debug(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value), kv(key2, value2));
        }
    }

    // region ERROR

    /**
     * Write a log entry with ERROR level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: ERROR</li>
     *     <li>message: {@code message}
     *     <li>{@code key}: {@code value}</li>
     * </ul>
     *
     * @param type    the type of the operation
     * @param message the error message
     * @param key     key to write inside log entry
     * @param value   value to write inside log entry
     */

    public void logError(String type, String message, String key, Object value) {
        if (key != null) {
            logger.error(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_ERROR_LOG_ARG, kv(LOG_ERROR_MESSAGE_KEY, message), kv(key, value));
        } else {
            logger.error(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_ERROR_LOG_ARG, kv(LOG_ERROR_MESSAGE_KEY, message));
        }
    }

    /**
     * Write a log entry with ERROR level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: ERROR</li>
     *     <li>message: {@code message}
     *     <li>{@code key}: {@code value}</li>
     * </ul>
     *
     * @param type  the type of the operation
     * @param key   key to write inside log entry
     * @param value value to write inside log entry
     */

    public void logError(String type, String key, Object value, String key2, Object value2) {
        if (structuredArgument != null) {
            logger.error(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, structuredArgument, kv(key, value), kv(key2, value2));
        } else {
            logger.error(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(key, value), kv(key2, value2));
        }
    }
}