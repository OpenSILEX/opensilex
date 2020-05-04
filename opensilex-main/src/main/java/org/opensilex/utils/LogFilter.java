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
import java.util.LinkedList;
import java.util.List;

/**
 * Logging filter for SLF4J.
 *
 * @author Vincent Migot
 */
public class LogFilter extends ThresholdFilter {

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
    private List<String> loggerIncludeList = new LinkedList<String>();

    /**
     * List of logger to exclude from custom level.
     */
    private List<String> loggerExcludeList = new LinkedList<String>();

    /**
     * List of logger to use at debug level.
     */
    private List<String> debug = new LinkedList<String>();

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (debug.contains(event.getLoggerName())) {
            return FilterReply.ACCEPT;
        }

        boolean hasMatch = (loggerIncludeList.size() == 0);
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
