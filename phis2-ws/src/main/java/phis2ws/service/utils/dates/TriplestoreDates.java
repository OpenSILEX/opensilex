//******************************************************************************
//                                       PhisTriplestoreDates.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 5 févr. 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  5 févr. 2018 - the resourcesUtils has all of the 
// methods of dates, a Dates class has been now created, with it's specializations
// Subject: Classes which represents the triplestore dates. With this class we can 
// check if a date format corresponds to the triplestore authorized formats.
//******************************************************************************
package phis2ws.service.utils.dates;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import phis2ws.service.configuration.DateFormats;

/**
 * represents the triplestore dates. Manipulates thoses dates (e.g. check formats)
 * @see phis2ws.service.utils.dates.Dates
 * @author Arnaud Charleroy
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class TriplestoreDates extends Dates {
    
    /**
     * generate a date parser using the list of formats defined which are 
     * expected to be used for data saved in triplestore 
     * @author Arnaud Charleroy
     * @see phis2ws.service.configuration.DateFormats AUTHORIZED_USER_TRIPLESTORE_DATE_FORMATS
     * @return the dates parser
     */
    private static DateTimeParser[] getUsedParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.AUTHORIZED_USER_TRIPLESTORE_DATE_FORMATS.size()];
        int count = 0;
        for (String format : DateFormats.AUTHORIZED_USER_TRIPLESTORE_DATE_FORMATS) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }
    
    /**
     * try to parse the given date in one of the dates formats defined which are 
     * expected to be recieved
     * @author Arnaud Charleroy
     * @see phis2ws.service.configuration.DateFormats AUTHORIZED_USER_TRIPLESTORE_DATE_FORMATS
     * @param date the date string to parse
     * @return true if the date format is correct
     *         false if the date format is not in the 
     *         DateFormats.AUTHORIZED_USER_SPARQL_DATE_FORMATS list
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
     * Convert a string date in DateTime if the date format corresponds to one 
     * of the used date format specified in the triplestore used dates formats
     * @author Arnaud Charleroy
     * @see phis2ws.service.configuration.DateFormats AUTHORIZED_USER_TRIPLESTORE_DATE_FORMATS
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
