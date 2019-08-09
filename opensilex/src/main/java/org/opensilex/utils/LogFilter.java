/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    private Level level;
    private List<String> loggerIncludeList = new ArrayList<String>();
    private List<String> loggerExcludeList = new ArrayList<String>();

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        boolean hasMatch = (loggerIncludeList.size() == 0);
        for (String logger: loggerIncludeList) {
            if (event.getLoggerName().startsWith(logger)) {
                hasMatch = true;
                break;
            }
        }
        
        if (hasMatch) {
            for (String logger: loggerExcludeList) {
                if (event.getLoggerName().startsWith(logger)) {
                    hasMatch = false;
                    break;
                }
            }
        }


        if (hasMatch && event.getLevel().isGreaterOrEqual(level)) {
            return FilterReply.ACCEPT;
        } else {
            return FilterReply.NEUTRAL;
        }
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setInclude(String logger) {
        this.loggerIncludeList.add(logger);
    }

    public void setExclude(String logger) {
        this.loggerExcludeList.add(logger);
    }
        
    public void start() {
        if (this.level != null) {
            super.start();
        }
    }

}
