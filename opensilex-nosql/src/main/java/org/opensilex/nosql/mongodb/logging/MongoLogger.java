package org.opensilex.nosql.mongodb.logging;

import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.log.OpenSilexStructuredLogger;
import org.slf4j.Logger;


/**
 * Utility class for easier logging of current MongoDB operations for a given collection.
 * The class provide constants for these operations and methods for logging by using structured JSON logging and duration measure
 *
 * @author rcolin
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

    public MongoLogger(String collectionName, Logger logger) {
        super(logger, StringUtils.isEmpty(collectionName) ?
                null :
                StructuredArguments.kv(COLLECTION, collectionName));
    }

}
