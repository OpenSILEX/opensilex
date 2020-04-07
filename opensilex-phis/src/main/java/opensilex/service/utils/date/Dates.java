//******************************************************************************
//                                 Dates.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 5 Feb. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils.date;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import opensilex.service.configuration.DateFormats;

/**
 * Dates formats and manipulations
 * @author Arnaud Charleroy <arnaud.charleory@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Dates {
    
    /**
     * Gets a parser list with all the authorized formats. 
     * @see opensilex.service.configuration.DateFormats AUTHORIZED_DATE_FORMATS
     * @return DateTimeParser the list of the dates parsers
     */
    protected static DateTimeParser[] getParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.AUTHORIZED_DATE_FORMATS.size()];
        int count = 0;
        for (String format : DateFormats.AUTHORIZED_DATE_FORMATS) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }
    
    /**
     * Checks if a date is valid and if the date format corresponds to one of the authorized 
     * date formats
     * @param date the string date to check
     * @return boolean true if the date is valid and has an authorized format
     *                 false if the date is not valid
     */
    public static boolean isValid(String date) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            DateTime d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a date is valid and if the date format corresponds to the 
     * DateFormats.YMD_FORMAT date format.
     * @see opensilex.service.configuration.DateFormats YMD_FORMAT
     * @param date the string date to check
     * @return boolean true if the date is valid and has the authorized format
     *                 false if the date is not valid
     */
    public static boolean isDateYMD(String date) {
        DateTimeParser parser = DateTimeFormat.forPattern(DateFormats.YMD_FORMAT).getParser();
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(parser).toFormatter();
        try {
            DateTime dateTime = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
    
    /**
     * Converts a given string date into a Date object, the date format must be 
     * one of those in the Dates.getParsers() (AUTHORIZED_DATE_FORMATS).
     * @see opensilex.service.configuration.DateFormats AUTHORIZED_DATE_FORMATS
     * @param date the date to convert
     * @return Date the date corresponding to the string date parameter if the format 
     *              is one of the AUTHORIZED_DATE_FORMATS
     *              null if the date could not be converted
     */
    public static Date stringToDate(String date) {
        DateTime dateTime;
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            dateTime = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return null;
        }
        return dateTime.toDate();
    }

    /**
     * Converts a string to a DateTime object, the date format must be 
     * one of those in the Dates.getParsers() (AUTHORIZED_DATE_FORMATS).
     * @see getParsers()
     * @see opensilex.service.configuration.DateFormats AUTHORIZED_DATE_FORMATS
     * @param date the date to convert
     * @return DateTime the date corresponding to the string date param if the format 
     *              is one of the AUTHORIZED_DATE_FORMATS
     *              null if the date could not be converted
     */
    public static DateTime stringToDateTime(String date) {
        DateTime dateTime;
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            dateTime = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return null;
        }
        return dateTime;
    }

    /**
     * Converts a string to a TimeStamp object. 
     * @param stringDate the date to convert. It must has the format defined by
     * DateFormats.YMDHMSZ_FORMAT
     * @see opensilex.service.configuration.DateFormats YMDHMSZ_FORMAT
     * @return the Timestamp if it has been generated
     * @throws ParseException
     */
    public static Timestamp stringToTimeStamp(String stringDate) throws ParseException {
        final org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern(DateFormats.YMDHMSZ_FORMAT);
        DateTime dt = formatter.parseDateTime(stringDate);

        return new Timestamp(dt.getMillis());
    }
    
    /**
     * Converts a date into a date time with its given pattern.
     * @param stringDate the date to convert
     * @param pattern the stringDate's pattern
     * @return the date converted in DateTime
     *         null if the date could not be converted
     */
    public static DateTime stringToDateTimeWithGivenPattern(String stringDate, String pattern) {
        final org.joda.time.format.DateTimeFormatter formatter 
                = DateTimeFormat.forPattern(pattern);
        try {
            return formatter.withOffsetParsed().parseDateTime(stringDate);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Converts a given timestamp to the datetime of Paris.
     * @see opensilex.service.configuration.DateFormats TIMEZONE_EU_PARIS
     * @param timestamp the timestamp to convert
     * @return DateTime representing the timestamp, converted in the Paris
     * timezone
     */
    public static DateTime timestampToDatetimeParis(Timestamp timestamp) {
        DateTime dateTime = new DateTime(timestamp, DateTimeZone.forID(DateFormats.TIMEZONE_EU_PARIS));
        return dateTime;
    }
    
    
    /**
     * Converts a string to Joda datetime according to a given pattern.
     * @param stringDate date
     * @param pattern pattern
     * @return null|DateTime
     */
    public static DateTime convertStringToDateTime(String stringDate, String pattern) {
        final org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
        try {
            return formatter.withOffsetParsed().parseDateTime(stringDate);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
