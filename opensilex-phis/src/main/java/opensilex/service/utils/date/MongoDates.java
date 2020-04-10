//******************************************************************************
//                             MongoDates.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 5 Feb. 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils.date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import opensilex.service.configuration.DateFormats;

/**
 * MongoDB dates manager.
 * @see opensilex.service.utils.date.Dates
 * @author Arnaud Charleroy <arnaud.charleory@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class MongoDates extends Dates {
    
    /**
     * Generates a date parser using the list of formats defined for MongoDB.
     * @see opensilex.service.configuration.DateFormats DATETIME_MONGO_FORMAT
     * @return the MongoDB dates parser
     */
    protected static DateTimeParser[] getParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.DATETIME_MONGO_FORMAT.size()];
        int count = 0;
        for (String format : DateFormats.DATETIME_MONGO_FORMAT) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }
    
    /**
     * Generates a date parser using the list of formats expected to be used for 
     * data saved in MongoDB.
     * @see opensilex.service.configuration.DateFormats AUTHORIZED_USER_MONGO_DATE_FORMATS
     * @return the dates parser
     */
    private static DateTimeParser[] getUsedParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.AUTHORIZED_USER_MONGO_DATE_FORMATS.size()];
        int count = 0;
        for (String format : DateFormats.AUTHORIZED_USER_MONGO_DATE_FORMATS) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }
    
    /**
     * Tries to parse the given date in one of the dates formats expected to be 
     * received.
     * @see opensilex.service.configuration.DateFormats AUTHORIZED_USER_MONGO_DATE_FORMATS
     * @param date the date string to parse
     * @return true if the date format is correct
     *         false if the date format is not in the 
     *         DateFormats.AUTHORIZED_USER_MONGO_DATE_FORMATS list
     */
    public static boolean isUsedDate(String date) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUsedParsers()).toFormatter();
        try {
            DateTime dateTime = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
     
     /**
     * Converts a string in DateTime by using the default parsers of the 
     * Joda library.
     * @see org.joda.time.format.DateTimeFormatter
     * @see opensilex.service.configuration.DateFormats DATETIME_MONGO_FORMAT
     * @param date the date string to parse
     * @return Date the string given in parameter, converted into DateTime. 
     *         null if the date could not be converted
     */
    public static DateTime stringToMongoJodaDateTime(String date) {
        DateTime dateTime;
        final DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            dateTime = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return null;
        }
        return dateTime;
    }
    
    /**
     * Converts a string date in DateTime if the date format corresponds to one 
     * of the used date format specified in the usable MongoDB dates formats.
     * @see opensilex.service.configuration.DateFormats AUTHORIZED_USER_MONGO_DATE_FORMATS
     * @param stringDate the date to be converted
     * @return DateTime the DateTime corresponding to the date string given. 
     *         null if the date could not be converted
     */
    public static DateTime stringToUsedDateTime(String stringDate) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUsedParsers()).toFormatter();
        try {
            return formatter.withOffsetParsed().parseDateTime(stringDate);
        } catch (Exception e) {
            return null;
        }
    }
}
