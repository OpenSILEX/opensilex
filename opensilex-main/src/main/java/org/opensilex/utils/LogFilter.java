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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vincent
 */
public class LogFilter extends ThresholdFilter {

    public static void forceDebug() {
        levelOverride = Level.DEBUG;
    }
    private static Level levelOverride;

    private Level customLevel;
    private List<String> loggerIncludeList = new ArrayList<String>();
    private List<String> loggerExcludeList = new ArrayList<String>();
    private List<String> debug = new ArrayList<String>();

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

    public void setDebug(String logger) {
        this.debug.add(logger);
    }

    public void setCustomLevel(Level level) {
        this.customLevel = level;
    }

    public Level getCustomLevel() {
        if (levelOverride != null) {
            return levelOverride;
        }
        return customLevel;
    }

    public void setInclude(String logger) {
        this.loggerIncludeList.add(logger);
    }

    public void setExclude(String logger) {
        this.loggerExcludeList.add(logger);
    }

    public void start() {
        if (this.getCustomLevel() != null) {
            super.start();
        }
    }

}
