//******************************************************************************
//                                       PhisDate.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 5 févr. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  5 févr. 2018
// Subject: Represents the dates used in Phis. Contains : 
// - methods to convert dates
// - used formats
//******************************************************************************
package phis2ws.service.utils.dates;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import phis2ws.service.configuration.DateFormats;

/**
 * Represents the dates formats and manipulations in PHIS
 * @author Arnaud Charleroy
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Dates {
    
    /**
     * Get a parser list with all the authorized formats. 
     * @author Arnaud Charleroy
     * @see phis2ws.service.configuration.DateFormats AUTHORIZED_DATE_FORMATS
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
     * Check if a date is valid and if the date format corresponds to one of the authorized 
     * date formats
     * @author Arnaud Charleroy
     * @see getParsers()
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
     * Check if a date is valid and if the date format corresponds to DateFormats.YMD_FORMAT
     * date formats
     * @author Arnaud Charleroy
     * @see phis2ws.service.configuration.DateFormats YMD_FORMAT
     * @param date the string date to check
     * @return boolean true if the date is valid and has the authorized format
     *                 false if the date is not valid
     */
    public static boolean isDateYMD(String date) {
        DateTimeParser parser = DateTimeFormat.forPattern(DateFormats.YMD_FORMAT).getParser();
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(parser).toFormatter();
        try {
            DateTime d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
    
    /**
     * Convert a given string date into a Date object, the date format must be 
     * one of those in the Dates.getParsers() (AUTHORIZED_DATE_FORMATS)
     * @author Arnaud Charleroy
     * @see getParsers()
     * @see phis2ws.service.configuration.DateFormats AUTHORIZED_DATE_FORMATS
     * @param date the date to convert
     * @return Date the date corresponding to the string date param if the format 
     *              is one of the AUTHORIZED_DATE_FORMATS
     *              null if the date could not be converted
     */
    public static Date stringToDate(String date) {
        DateTime d = null;
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return null;
        }
        return d.toDate();
    }

    /**
     * Convert a string to a DateTime object, the date format must be 
     * one of those in the Dates.getParsers() (AUTHORIZED_DATE_FORMATS)
     * @author Arnaud Charleroy
     * @see getParsers()
     * @see phis2ws.service.configuration.DateFormats AUTHORIZED_DATE_FORMATS
     * @param date the date to convert
     * @return DateTime the date corresponding to the string date param if the format 
     *              is one of the AUTHORIZED_DATE_FORMATS
     *              null if the date could not be converted
     */
    public static DateTime stringToDateTime(String date) {
        DateTime d = null;
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            return null;
        }
        return d;
    }

    /**
     * Convert a string to a TimeStamp object. 
     * @param stringDate the date to convert. It must has the format defined by
     * DateFormats.YMDHMSZ_FORMAT
     * @author Arnaud Charleroy
     * @see phis2ws.service.configuration.DateFormats YMDHMSZ_FORMAT
     * @return the Timestamp if it has been generated
     * @throws ParseException
     */
    public static Timestamp stringToTimeStamp(String stringDate) throws ParseException {
        final org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern(DateFormats.YMDHMSZ_FORMAT);
        DateTime dt = formatter.parseDateTime(stringDate);

        return new Timestamp(dt.getMillis());
    }
    
    /**
     * Convert a date to date time with it's given pattern
     * @author Arnaud Charleroy
     * @param stringDate the date to convert
     * @param pattern the stringDate's pattern
     * @return the date converted in DateTime
     *         null if the date could not be converted
     */
    public static DateTime stringToDateTimeWithGivenPattern(
            String stringDate, String pattern) {
        final org.joda.time.format.DateTimeFormatter formatter 
                = DateTimeFormat.forPattern(pattern);
        try {
            return formatter.withOffsetParsed().parseDateTime(stringDate);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convert a given timestamp to the datetime of paris
     * @author Arnaud Charleroy
     * @see phis2ws.service.configuration.DateFormats TIMEZONE_EU_PARIS
     * @param timestamp the timestamp to convert
     * @return DateTime representing the timestamp, converted in the Paris
     * timezone
     */
    public static DateTime timestampToDatetimeParis(Timestamp timestamp) {
        DateTime dt = new DateTime(timestamp, DateTimeZone.forID(DateFormats.TIMEZONE_EU_PARIS));
        return dt;
    }
    
    
    /**
     * Convert a string to joda datetime according to a given pattern 
     *
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
