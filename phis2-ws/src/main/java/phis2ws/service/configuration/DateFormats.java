//**********************************************************************************************
//                                       DateFormats.java 
//
// Author(s): Arnaud CHARLEROY
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: Dates formats used in the WS
//***********************************************************************************************
package phis2ws.service.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Regroupe les formats de date utilisées dans tout le webService
 * @date 08/16
 * @author A. CHARLEROY
 */
public final class DateFormats {

    
    public final static String DATETIME_MONGO_MEASURE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZZ";
    public final static String DATETIME_JSON_SERIALISATION_FORMAT = "yyyy-MM-dd HH:mm:ssZZ";
    public final static String DATETIME_METEO_DB_FORMAT = "yyyy-MM-dd HH:mm:ssZZ";
    public final static String DATETIME_SPARQL_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";
    public final static List<String> DATETIME_MONGO_FORMAT = Arrays.asList("yyyy-MM-dd HH:mm:ss.SSSZZ", "yyyy-MM-dd HH:mm:ss.SSSSSS", "yyyy-MM-dd HH:mm:ss.SSSSSSZZ", "yyyy-MM-dd HH:mm:ssZZ");

    public final static List<String> AUTHORIZED_DATE_FORMATS = Arrays.asList("yyyy-MM-dd HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ssZZ", "yyyy-MM-dd");
    public final static List<String> AUTHORIZED_USER_METEO_DATE_FORMATS = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ssZZ");
    public final static List<String> AUTHORIZED_USER_SPARQL_DATE_FORMATS = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ssZZ");
    public final static List<String> AUTHORIZED_USER_MONGO_DATE_FORMATS = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ssZZ", "yyyy-MM-dd HH:mm:ss.SSSZZ");

    public final static String DATETIME_YMD_FORMAT = "yyyy-MM-dd";
}
