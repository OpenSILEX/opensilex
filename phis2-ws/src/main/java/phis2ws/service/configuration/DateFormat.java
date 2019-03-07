//******************************************************************************
//                                       DateFormat.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 Aug, 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.configuration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.TimeUnit;

//SILEX:todo
// Use this enum instead of the DateFormats class in all the application's code
//\SILEX:todo

/**
 * The list of the authorized date formats
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
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
     * Parse a date or a datetime into a date
     * date format is YYYY-MM-DD and datetime format is YYYY-MM-DDThh:mm:ss+xxxx
     * If a date pattern is provided time and timezone info are added to the string before parsing
     * If isEndDate flag is set to true time and timezone added correspond to the end of the day ie: 29:59:59+0000
     * Otherwise timezone added correspond to the begining of the day ie: 00:00:00+0000
     * If a date time is provided isEndDate flag is not used
     * 
     * @param dateString Date or date time to parse
     * @param isEndDate Flag to determine how date should be convert to date time before parsing
     * @return The parsed Date
     * @throws ParseException 
     */
    public static Date parseDateOrDateTime(String dateString, boolean isEndDate) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
                
        if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
            SimpleDateFormat dfshort = new SimpleDateFormat(DateFormat.YMD.toString());
            Date date = dfshort.parse(dateString);
            
            // Compute current server timezone offset
            int secondOffset = ZoneOffset.systemDefault().getRules().getOffset(date.toInstant()).getTotalSeconds();
            
            String dateTimezone = String.format("%02d%02d", 
                TimeUnit.SECONDS.toHours(secondOffset),
                TimeUnit.SECONDS.toMinutes(secondOffset) - 
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(secondOffset))
            );
            
            // Set time depending of isEndDate flag
            if (isEndDate) {
                dateString += "T23:59:59+" + dateTimezone;
            } else {
                dateString += "T00:00:00+" + dateTimezone;
            }
        }
        
        // Parse datetime
        return df.parse(dateString);
    }
}
