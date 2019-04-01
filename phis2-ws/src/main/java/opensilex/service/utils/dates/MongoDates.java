//******************************************************************************
//                                       PhisMongoDates.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 5 févr. 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  5 févr. 2018 - the resourcesUtils has all of the 
// methods of dates, a Dates class has been now created, with it's specializations
// Subject: Classes which represents the mongodb dates. With this class we can 
// check if a date format corresponds to the mongodb authorized formats.
//******************************************************************************
package opensilex.service.utils.dates;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import opensilex.service.configuration.DateFormats;

/**
 * represents the mongodb dates. Manipulates thoses dates (e.g. check formats)
 * @see opensilex.service.utils.dates.Dates
 * @author Arnaud Charleroy
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class MongoDates extends Dates {
    
    /**
     * generate a date parser using the list of formats defined for mongo
     * @author Arnaud Charleroy
     * @see opensilex.service.configuration.DateFormats DATETIME_MONGO_FORMAT
     * @return the mongodb dates parser
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
     * generate a date parser using the list of formats defined which are 
     * expected to be used for data saved in mongodb 
     * @author Arnaud Charleroy
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
     * try to parse the given date in one of the dates formats defined which are 
     * expected to be recieved
     * @author Arnaud Charleroy
     * @see opensilex.service.configuration.DateFormats AUTHORIZED_USER_MONGO_DATE_FORMATS
     * @param date the date string to parse
     * @return true if the date format is correct
     *         false if the date format is not in the 
     *         DateFormats.AUTHORIZED_USER_MONGO_DATE_FORMATS list
     */
    public static boolean isUsedDate(String date) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUsedParsers()).toFormatter();
        try {
            DateTime d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
     
     /**
     * Convert a string in DateTime by using the default parsers of the 
     * Joda library
     * @author Arnaud Charleroy
     * @see org.joda.time.format.DateTimeFormatter
     * @see opensilex.service.configuration.DateFormats DATETIME_MONGO_FORMAT
     * @param date the date string to parse
     * @return Date the string given in parameter, converted into DateTime. 
     *         null if the date could not be converted
     */
    public static DateTime stringToMongoJodaDateTime(String date) {
        DateTime d = null;
        final DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return null;
        }
        return d;
    }
    
    /**
     * Convert a string date in DateTime if the date format corresponds to one 
     * of the used date format specified in the mongodb used dates formats
     * @author Arnaud Charleroy
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
