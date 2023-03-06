package org.opensilex.utils;

import org.slf4j.Logger;

public class AnsiUtils {

    private AnsiUtils(){}

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    public static void logInfo(Logger logger, String msg, Object... params){
        String greenMsg = ANSI_GREEN + msg + ANSI_RESET;
        logger.info(greenMsg,params);
    }

    public static void logDebug(Logger logger, String msg, Object... params){
        String greenMsg = ANSI_GREEN + msg + ANSI_RESET;
        logger.debug(greenMsg,params);
    }


    public static void logError(Logger logger, String msg, Object... params){
        String redMsg = ANSI_RED + msg + ANSI_RESET;
        logger.info(redMsg,params);
    }
}
