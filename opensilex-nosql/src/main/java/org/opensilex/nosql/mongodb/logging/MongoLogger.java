package org.opensilex.nosql.mongodb.logging;

import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.opensilex.utils.LogFilter.*;

/**
 * Utility class for easier logging of current MongoDB operations for a given collection.
 * The class provide constants for these operations and methods for logging by using structured JSON logging and duration measure
 *
 * @see #logOperationStart(String, String, Object, String, Object)
 * @see #logOperationStart(String, String, Object)
 * @see #logOperationOk(String, Instant, String, Object)
 * @see #logOperationOk(String, Instant, String, Object, String, Object)
 *
 * @author rcolin
 */
public class MongoLogger {

    public static final String URI_KEY = "uri";
    public static final String COLLECTION = "collection";
    public static final String INDEX = "index";


    public static final String INSERT_ONE = "insert_one";
    public static final String INSERT_MANY = "insert_many_msg";
    public static final String INSERT_MANY_COUNT = "insert_many_count";
    public static final String UPDATE_ONE = "update_one";
    public static final String DELETE_MANY = "delete_many";
    public static final String DELETE_MANY_COUNT = "delete_many_count";
    public static final String DELETE_ONE = "delete_one";
    public static final String FILTER = "filter";
    public static final String COUNT = "count";
    public static final String COUNT_RESULT = "count_result";
    public static final String SEARCH = "search";
    public static final String SEARCH_STREAM = "search_stream";
    public static final String RESULT_COUNT = "result_count";
    public static final String DISTINCT = "distinct";
    public static final String DISTINCT_FIELD = "distinct_field";
    public static final String AGGREGATION_PIPELINE = "aggregation_pipeline";
    public static final String AGGREGATE = "aggregate";

    public static final String CHECK_MONGO_SERVER_CONNECTION = "mongo_server_check_connection";
    public static final String TIMEOUT_MS = "timeout_ms";

    public static final String MONGO_CREATE_INDEX = "create_index";

    public static final String MONGO_DELETE_INDEX = "create_index";

    public static final String COMMAND_TYPE = "server_command";
    public static final String COMMAND_NAME_FIELD = "command_name";




    /**
     * Default log message format. Intended for registration of :
     * <ul>
     *     <li>{@link org.opensilex.utils.LogFilter#LOG_TYPE_KEY}</li>
     *     <li>{@link org.opensilex.utils.LogFilter#LOG_STATUS_LOG_KEY}</li>
     * </ul>
     *
     * Note
     * <ul>
     *     <li>Warning message can be thrown by some code-quality tools since the number of argument is different
     *      from the number of placeholder (2) inside this message</li>
     *      <li>Ex : "More arguments provided (4) than placeholders specified (2)"</li>
     *      <li>This default message placeholder limit the redundancy of fields, since else, each key/value written with {@link StructuredArguments#kv(String, Object)}
     *      method is recorded as a field and inside {@code message} field</li>
     * </ul>
     */
    private static final String DEFAULT_MESSAGE_FORMAT = "{} {}";
    private final StructuredArgument collectionLogArgument;
    private final Logger logger;

    public MongoLogger(String collectionName, Logger logger) {
        this.collectionLogArgument = StringUtils.isEmpty(collectionName) ?
                null :
                StructuredArguments.kv(COLLECTION, collectionName);
        this.logger = logger;
    }

    public Instant logOperationStart(String type) {
        return logOperationStart(type, null, null);
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     *
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: START</li>
     *     <li>collection: the name of {@code collection} passed to the constructor</li>
     *     <li>{@code key}: {@code value}</li>
     * </ul>
     * @param type the type of the operation
     * @param key key to write inside log entry
     * @param value value to write inside log entry
     * @return the current {@link Instant}, pass this one to {@link #logOperationOk(String, Instant, String, Object)} method, for duration compute and write inside log
     */
    public Instant logOperationStart(String type, String key, Object value) {
        if (key != null) {
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, collectionLogArgument, kv(key, value));
        } else {
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, collectionLogArgument);
        }
        return Instant.now();
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     *
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: START</li>
     *     <li>collection: the name of {@code collection} passed to the constructor</li>
     *     <li>{@code key}: {@code value}</li>
     *     <li>{@code key2}: {@code value2}</li>
     * </ul>
     *
     * @param type the type of the operation
     * @param key key to write inside log entry
     * @param value value to write inside log entry
     * @param key2  2nd key to write inside log entry
     * @param value2 2nd value to write inside log entry
     * @return the current {@link Instant}, pass this one to {@link #logOperationOk(String, Instant, String, Object)} method, for duration compute and write inside log
     */
    public Instant logOperationStart(String type, String key, Object value, String key2, Object value2) {
        logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_START_LOG_ARG, collectionLogArgument, kv(key, value), kv(key2, value2));
        return Instant.now();
    }


    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     *     <li>collection: the name of {@code collection} passed to the constructor</li>
     *     <li>{@code key}: {@code value}</li>
     * </ul>
     * @param type the type of the operation
     * @param start Start of the operation, used to compute duration in ms
     * @param key key to write inside log entry
     * @param value value to write inside log entry
     */
    public void logOperationOk(String type, Instant start, String key, Object value){
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        if(key != null){
            if(collectionLogArgument != null){
                logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, collectionLogArgument, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value));
            }else{
                logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value));
            }
        }else{
            if(collectionLogArgument != null) {
                logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, collectionLogArgument, kv(LOG_DURATION_MS_KEY, durationMs));
            }else{
                logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs));
            }
        }
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     *     <li>collection: the name of {@code collection} passed to the constructor</li>
     * </ul>
     * @param type the type of the operation
     * @param start Start of the operation, used to compute duration in ms
     */
    public void logOperationOk(String type, Instant start){
        this.logOperationOk(type, start, null, null);
    }

    /**
     * Write a log entry with INFO level with the following key/value :
     * <ul>
     *     <li>type: the value of {@code type} param </li>
     *     <li>status: OK</li>
     *     <li>durationMs: the elapsed milliseconds between {@code start} and now
     *     <li>collection: the name of {@code collection} passed to the constructor</li>
     *     <li>{@code key}: {@code value}</li>
     *     <li>{@code key2}: {@code value2}</li>
     * </ul>
     * @param type the type of the operation
     * @param start Start of the operation, used to compute duration in ms
     * @param key key to write inside log entry
     * @param value value to write inside log entry
     * @param key2  2nd key to write inside log entry
     * @param value2 2nd value to write inside log entry
     */
    public void logOperationOk(String type, Instant start, String key, Object value, String key2, Object value2) {
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        if(collectionLogArgument != null){
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, collectionLogArgument, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value), kv(key2, value2));
        }else{
            logger.info(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(LOG_DURATION_MS_KEY, durationMs), kv(key, value), kv(key2, value2));
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
     * @param type the type of the operation
     * @param message the error message
     * @param key key to write inside log entry
     * @param value value to write inside log entry
     */
    public void logOperationError(String type, String message, String key, Object value){
        if(key != null){
            logger.error(DEFAULT_MESSAGE_FORMAT,  kv(LOG_TYPE_KEY, type), STATUS_ERROR_LOG_ARG, kv(LOG_ERROR_MESSAGE_KEY, message), kv(key, value));
        }else{
            logger.error(DEFAULT_MESSAGE_FORMAT,  kv(LOG_TYPE_KEY, type), STATUS_ERROR_LOG_ARG, kv(LOG_ERROR_MESSAGE_KEY, message));
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
     * @param type the type of the operation
     * @param message the error message
     * @param key key to write inside log entry
     * @param value value to write inside log entry
     */
    public void logOperationError(String type,String key, Object value, String key2, Object value2) {
        if(collectionLogArgument != null){
            logger.error(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, collectionLogArgument, kv(key, value), kv(key2, value2));
        }else{
            logger.error(DEFAULT_MESSAGE_FORMAT, kv(LOG_TYPE_KEY, type), STATUS_OK_LOG_ARG, kv(key, value), kv(key2, value2));
        }
    }

}
