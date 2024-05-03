//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import net.logstash.logback.argument.StructuredArgument;

import java.util.ArrayList;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

/**
 * Logging filter for SLF4J.
 *
 * @author Vincent Migot
 */
public class LogFilter extends ThresholdFilter {

    public static final String LOG_TYPE_KEY = "log_type";

    public static final String TRANSACTION = "TRANSACTION";

    public static final String LOG_STATUS_LOG_KEY = "status";
    public static final String LOG_ERROR_MESSAGE_KEY = "error_message";

    public static final String LOG_STATUS_START = "START";
    public static final String LOG_STATUS_OK = "OK";
    public static final String LOG_STATUS_ERROR = "ERROR";
    public static final String LOG_STATUS_ROLLBACK = "ROLLBACK";
    public static final String LOG_DURATION_MS_KEY = "duration_ms";

    public static final StructuredArgument STATUS_START_LOG_ARG = keyValue(LOG_STATUS_LOG_KEY, LOG_STATUS_START);
    public static final StructuredArgument STATUS_OK_LOG_ARG = keyValue(LOG_STATUS_LOG_KEY, LOG_STATUS_OK);

    public static final StructuredArgument STATUS_ERROR_LOG_ARG = keyValue(LOG_STATUS_LOG_KEY, LOG_STATUS_ERROR);

    /**
     * Method to force debug output.
     */
    public static void forceDebug() {
        levelOverride = Level.DEBUG;
    }

    /**
     * Log level override.
     */
    private static Level levelOverride;

    /**
     * Log level for included and excluded patterns.
     */
    private Level customLevel;

    /**
     * List of logger to include at custom level.
     */
    private final List<String> loggerIncludeList = new ArrayList<>();

    /**
     * List of logger to exclude from custom level.
     */
    private final List<String> loggerExcludeList = new ArrayList<>();

    /**
     * List of logger to use at debug level.
     */
    private final List<String> debug = new ArrayList<>();

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (debug.contains(event.getLoggerName())) {
            return FilterReply.ACCEPT;
        }

        boolean hasMatch = (loggerIncludeList.isEmpty());
        for (String logger : loggerIncludeList) {
            if (event.getLoggerName().startsWith(logger)) {
                hasMatch = true;
                break;
            }
        }

        if (hasMatch) {
            for (String logger : loggerExcludeList) {
                if (event.getLoggerName().startsWith(logger)) {
                    hasMatch = false;
                    break;
                }
            }
        }

        if (hasMatch && event.getLevel().isGreaterOrEqual(getCustomLevel())) {
            return FilterReply.ACCEPT;
        } else {
            return super.decide(event);
        }
    }

    /**
     * Setter to add logger at debug level.
     *
     * @param logger
     */
    public void setDebug(String logger) {
        this.debug.add(logger);
    }

    /**
     * Setter to define custom level.
     *
     * @param level
     */
    public void setCustomLevel(Level level) {
        this.customLevel = level;
    }

    /**
     * Getter for custom level.
     *
     * @return custom level
     */
    public Level getCustomLevel() {
        if (levelOverride != null) {
            return levelOverride;
        }
        return customLevel;
    }

    /**
     * Setter to add logger in custom level include list.
     *
     * @param logger
     */
    public void setInclude(String logger) {
        this.loggerIncludeList.add(logger);
    }

    /**
     * Setter to add logger in custom level exclude list.
     *
     * @param logger
     */
    public void setExclude(String logger) {
        this.loggerExcludeList.add(logger);
    }

    @Override
    public void start() {
        if (this.getCustomLevel() != null) {
            super.start();
        }
    }

}
