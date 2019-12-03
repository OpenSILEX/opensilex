//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vincent
 */
public class LogFilter extends Filter<ILoggingEvent> {

    public static void forceDebug() {
        levelOverride = Level.DEBUG;
    }
    private static Level levelOverride;
    
    private Level level;
    private List<String> loggerIncludeList = new ArrayList<String>();
    private List<String> loggerExcludeList = new ArrayList<String>();

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
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

        if (hasMatch && event.getLevel().isGreaterOrEqual(getLevel())) {
            return FilterReply.ACCEPT;
        } else {
            return FilterReply.NEUTRAL;
        }
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        if (levelOverride != null) {
            return levelOverride;
        }
        return level;
    }

    public void setInclude(String logger) {
        this.loggerIncludeList.add(logger);
    }

    public void setExclude(String logger) {
        this.loggerExcludeList.add(logger);
    }

    public void start() {
        if (this.getLevel() != null) {
            super.start();
        }
    }

}
