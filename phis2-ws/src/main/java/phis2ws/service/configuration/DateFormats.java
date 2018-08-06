//**********************************************************************************************
//                                       DateFormats.java 
//
// Author(s): Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  February, 05 2018 - add constants 
// Subject: Dates formats used in the WS
//***********************************************************************************************
package phis2ws.service.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * contains the date formats used in the web service
 * @author Arnaud Charleroy
 * SILEX:todo : - Element: Datetime
 *         Attribute : YMDHMSZ_FORMAT
 *         Purpose : wrong date time format @link https://www.w3.org/TR/xmlschema-2/#dateTime
 *                    must add 'T'separator indicating that time-of-day follows;
 *                    Impact the API clients, internal WS search method parameters and 
 *                    class model with datetime attribute.
 * SILEX:todo
 */
public final class DateFormats {

    
    public final static String DATETIME_MONGO_MEASURE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZZ";
    public final static String DATETIME_JSON_SERIALISATION_FORMAT = "yyyy-MM-dd HH:mm:ssZZ";
    public final static String DATETIME_METEO_DB_FORMAT = "yyyy-MM-dd HH:mm:ssZZ";
    public final static String DATETIME_SPARQL_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";
    public final static List<String> DATETIME_MONGO_FORMAT = Arrays.asList("yyyy-MM-dd HH:mm:ss.SSSZZ", "yyyy-MM-dd HH:mm:ss.SSSSSS", "yyyy-MM-dd HH:mm:ss.SSSSSSZZ", "yyyy-MM-dd HH:mm:ssZZ");

    public final static List<String> AUTHORIZED_DATE_FORMATS = Arrays.asList("yyyy-MM-dd HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ssZZ", "yyyy-MM-dd");
    public final static List<String> AUTHORIZED_USER_ENVIRONMENT_DATE_FORMATS = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ssZZ");
    public final static List<String> AUTHORIZED_USER_TRIPLESTORE_DATE_FORMATS = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ssZZ");
    public final static List<String> AUTHORIZED_USER_MONGO_DATE_FORMATS = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ssZZ", "yyyy-MM-dd HH:mm:ss.SSSZZ");

    //Dates formats
    public final static String YMD_FORMAT = "yyyy-MM-dd";
    public final static String YMDHMSZ_FORMAT = "yyyy-MM-dd HH:mm:ssZ"; // To change to yyyy-MM-ddTHH:mm:ssZ see todo above 
    public final static String YMDHMS_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    //Timezones
    public final static String TIMEZONE_EU_PARIS = "Europe/paris";
}
