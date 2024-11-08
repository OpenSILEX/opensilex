package org.opensilex.nosql.mongodb.logging;

import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.log.OpenSilexStructuredLogger;
import org.slf4j.Logger;

import java.time.Instant;


/**
 * Utility class for easier logging of current MongoDB operations for a given collection.
 * The class provide constants for these operations and methods for logging by using structured JSON logging and duration measure
 *
 * @author rcolin
 * @see #logOperationStart(String, String, Object, String, Object)
 * @see #logOperationStart(String, String, Object)
 * @see #logOperationOk(String, Instant, String, Object)
 * @see #logOperationOk(String, Instant, String, Object, String, Object)
 */
public class MongoLogger extends OpenSilexStructuredLogger {

    // region LOG TYPES
    public static final String COLLECTION = "collection";
    public static final String INDEX = "index";

    public static final String AGGREGATION_PIPELINE = "aggregation_pipeline";
    public static final String AGGREGATE = "aggregate";

    public static final String CHECK_MONGO_SERVER_CONNECTION = "mongo_server_check_connection";

    public static final String MONGO_CREATE_INDEX = "create_index";
    public static final String MONGO_DELETE_INDEX = "create_index";

    public static final String COMMAND_TYPE = "server_command";
    public static final String COMMAND_NAME_FIELD = "command_name";


    public MongoLogger(String collectionName, Logger logger) {
        super(logger, StringUtils.isEmpty(collectionName) ?
                null :
                StructuredArguments.kv(COLLECTION, collectionName));
    }

}
