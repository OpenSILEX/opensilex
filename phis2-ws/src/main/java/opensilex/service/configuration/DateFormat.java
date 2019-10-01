//******************************************************************************
//                              DateFormat.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 Aug. 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.configuration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.TimeUnit;

//SILEX:todo
// Use this enum instead of the DateFormats class in all the application's code
//\SILEX:todo

/**
 * List of authorized date formats.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public enum DateFormat {
    YMDHMSZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd HH:mm:ssZ";
        }
    },
    YMDTHMSZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ssZ";
        }
    },
    YMDTHMSZZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ssZZ";
        }
    },
    YMD {
        @Override
        public String toString(){
            return "yyyy-MM-dd";
        }
    };
    
    /**
     * Parses a date or a date time into a date.
     * If a date pattern is provided, time and timezone info are added to the 
     * string before parsing.
     * If isEndDate flag is set to true time and timezone added correspond to 
     * the end of the day (ie 29:59:59+0000).
     * Otherwise the timezone added corresponds to the beginning of the day (ie:
     * 00:00:00+0000)
     * If a date time is provided, the isEndDate flag is not used.
     * @param dateStringToParse
     * @param isEndDate Flag to determine how date should be convert to date time before parsing
     * @return The parsed Date
     * @throws ParseException 
     */
    public static Date parseDateOrDateTime(String dateStringToParse, boolean isEndDate) throws ParseException {
        if (dateStringToParse == null) {
            return null;
        }
        
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
                
        if (dateStringToParse.matches("\\d{4}-\\d{2}-\\d{2}")) {
            // Set time depending of isEndDate flag
            if (isEndDate) {
                dateStringToParse += "T23:59:59+0000";
            } else {
                dateStringToParse += "T00:00:00+0000";
            }
        } else if (dateStringToParse.endsWith("Z")) {
            dateStringToParse = dateStringToParse.replaceFirst("(.*)Z$", "$1+0000");
        }
        
        // Parse datetime
        return df.parse(dateStringToParse);
    }
}
