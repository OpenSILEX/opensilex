//**********************************************************************************************
//                                       ResourcesUtils.java 
//
// Author(s): Arnaud CHARLEROY 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: may 2016
// Contact:arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  May 31,  2017
// Subject: A class which regroup all function which are not specific or can be usable in all the webservice
//***********************************************************************************************
package phis2ws.service.utils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.configuration.GlobalWebserviceValues;

/**
 * List of functions which can be used in ressources
 *
 * @author Arnaud CHARLEROY
 */
public class ResourcesUtils {

    /**
     * Permet de transformer un flux en chaine de caractère
     *
     * @param is
     * @return
     */
//    public static String convertStreamToString(java.io.InputStream is) {
//        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
//        return s.hasNext() ? s.next() : "";
//    }
    /**
     *
     * @param original
     * @return
     */
    public static String capitalizeFirstLetter(String original) {
        return (original == null || original.length() == 0) ? original : original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }

    public static List<String> convertStringValuesSepared(String values, String pattern) {
        List<String> listValues;
        try {
            listValues = Arrays.asList(values.split(pattern));
            return listValues;
        } catch (Exception e) {
            listValues = new ArrayList<>();
            listValues.add(values);

        }
        return null;
    }

    /**
     * extentY to plantHeight
     * width to plantWidth
     * @param mongoUserVariable
     * @return
     */
    public static String formatMongoImageAnalysisVariableForDB(String mongoUserVariable) {
        String tmpMongoUserVariable = null;
        String variableFormatMongo = "";
        // elcom special case
        if (mongoUserVariable.contains("parallelBoudingBox")) {
            tmpMongoUserVariable = mongoUserVariable.replaceFirst("parallelBoudingBox", "");
            variableFormatMongo = "parallelBoudingBox_" + tmpMongoUserVariable.toLowerCase();
        } else if (mongoUserVariable.contains("nonParallelBoudingBox")) {
            tmpMongoUserVariable = mongoUserVariable.replaceFirst("nonParallelBoudingBox", "");
            variableFormatMongo = "nonParallelBoudingBox" + tmpMongoUserVariable.toLowerCase();
        } else {
            for (int i = 0; i < mongoUserVariable.length(); i++) {
                if (Character.isUpperCase(mongoUserVariable.charAt(i))) {
                    variableFormatMongo += "_";
                    variableFormatMongo += Character.toLowerCase(mongoUserVariable.charAt(i));
                } else {
                    variableFormatMongo += mongoUserVariable.charAt(i);
                }
            }
        }
        return variableFormatMongo;
    }

    /**
     * Permet de récupérer des parsers présents dans un tableau et de retourner
     * un ojet qui parse des dates.
     *
     * @return DateTimeParser
     */
    private static DateTimeParser[] getParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.AUTHORIZED_DATE_FORMATS.size()];
        int count = 0;
        for (String format : DateFormats.AUTHORIZED_DATE_FORMATS) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }

    private static DateTimeParser[] getMongoParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.DATETIME_MONGO_FORMAT.size()];
        int count = 0;
        for (String format : DateFormats.DATETIME_MONGO_FORMAT) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }

    private static DateTimeParser[] getUserEnvironmentParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.AUTHORIZED_USER_METEO_DATE_FORMATS.size()];
        int count = 0;
        for (String format : DateFormats.AUTHORIZED_USER_METEO_DATE_FORMATS) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }

    private static DateTimeParser[] getUserTripleStoreParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.AUTHORIZED_USER_SPARQL_DATE_FORMATS.size()];
        int count = 0;
        for (String format : DateFormats.AUTHORIZED_USER_SPARQL_DATE_FORMATS) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }

    private static DateTimeParser[] getUserMongoParsers() {
        DateTimeParser[] parsers = new DateTimeParser[DateFormats.AUTHORIZED_USER_MONGO_DATE_FORMATS.size()];
        int count = 0;
        for (String format : DateFormats.AUTHORIZED_USER_MONGO_DATE_FORMATS) {
            parsers[count] = DateTimeFormat.forPattern(format).getParser();
            count++;
        }
        return parsers;
    }

    /**
     * Vérifie si une date est valide et renvoie un booléen.
     *
     * @param date
     * @return boolean
     */
    public static boolean isDateIsValid(String date) {

        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            DateTime d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean isDateIsValidYMD(String date) {
        DateTimeParser parser = DateTimeFormat.forPattern("yyyy-MM-dd").getParser();
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(parser).toFormatter();
        try {
            DateTime d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean isDateIsValidEnvironmentStartEndDate(String date) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUserEnvironmentParsers()).toFormatter();
        try {
            DateTime d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean isDateIsValidTripleStoreUserDate(String date) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUserTripleStoreParsers()).toFormatter();
        try {
            DateTime d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean isDateIsMongoUserDate(String date) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUserMongoParsers()).toFormatter();
        try {
            DateTime d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getValueOfAnURI(String uri) {
        final String[] parts = uri.split("#");
        if ((uri.contains(">") && uri.contains("<"))) {
            return parts[1].substring(0, parts[1].length() - 1);
        } else {
            return parts[1];
        }
    }

    /**
     * Transforme une chaîne de caractère en date
     *
     * @param date
     * @return Date
     */
    public static Date convertStringToJavaDate(String date) {
        DateTime d = null;
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return d.toDate();
    }

    /**
     * Transforme une chaîne de caractère en DateTime avec des parsers par
     * défaut(librairie Joda)
     *
     * @see GlobalWebserviceValues
     * @param date
     * @return Date
     */
    public static DateTime convertStringToJodaDateTime(String date) {
        DateTime d = null;
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getParsers()).toFormatter();
        try {
            d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return d;
    }

    /**
     * Transforme une chaîne de caractère en DateTime avec des parsers par
     * défaut(librairie Joda)
     *
     * @see GlobalWebserviceValues
     * @param date
     * @return Date
     */
    public static DateTime convertStringToMongoJodaDateTime(String date) {
        DateTime d = null;
        final DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getMongoParsers()).toFormatter();
        try {
            d = formatter.parseDateTime(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return d;
    }

    /**
     * Vérifie la validité d'un timestamp
     *
     * @param inputString
     * @return
     */
//    public static Boolean isTimeStampValid(String inputString)
//    { 
//        SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ssX");
//        try{
//           Date date = format.parse(inputString);
//           GregorianCalendar cal = new GregorianCalendar();
//           cal.setTime(date);
//           DateTime now = new DateTime();
//           return 1900 < cal.get(Calendar.YEAR) && cal.get(Calendar.YEAR) < 2500;
//           
//        }
//        catch(ParseException e)
//        {
//             
//            return false;
//        }
//    }
    /**
     * Converti un timestamp en date avec l'heure GMT Zone Paris
     *
     * @param ts
     * @return
     */
    public static DateTime convertPostgresSqlTimestampToDatetime(Timestamp ts) {
        DateTime dt = new DateTime(ts, DateTimeZone.forID("Europe/paris"));
        return dt;
    }

    /**
     * Transforme une chaîne de caractère en timestamp
     *
     * @param stringDate
     * @return
     * @throws ParseException
     */
    public static Timestamp convertStringToTimeStamp(String stringDate) throws ParseException {
//        final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ssX";
//        final SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
//        Date utilDate = sdf.parse(stringDate);
//        return new Timestamp(utilDate.getTime());
        final org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ");
        DateTime dt = formatter.parseDateTime(stringDate);

        return new Timestamp(dt.getMillis());

    }

    /**
     * Génère une chaine unique de 32 caractères
     *
     * @return
     */
    public static String getUniqueID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Transforme une chaîne de caractère en DateTime avec un format entrée en
     * paramètre (librairie Joda)
     *
     * @param stringDate
     * @param pattern
     * @return
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

    public static DateTime convertUserEnvironementDateStringToDateTime(String stringDate) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUserEnvironmentParsers()).toFormatter();
        try {
            return formatter.withOffsetParsed().parseDateTime(stringDate);
        } catch (Exception e) {
            return null;
        }

    }

    public static DateTime convertUserTripleStoreDateStringToDateTime(String stringDate) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUserTripleStoreParsers()).toFormatter();
        try {
            return formatter.withOffsetParsed().parseDateTime(stringDate);
        } catch (Exception e) {
            return null;
        }

    }

    public static DateTime convertUserMongoDateStringToDateTime(String stringDate) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(null, getUserMongoParsers()).toFormatter();
        try {
            return formatter.withOffsetParsed().parseDateTime(stringDate);
        } catch (Exception e) {
            return null;
        }

    }

//    /**
//     * Thread safe
//     * @see http://stackoverflow.com/questions/7346508/datatypefactory-usage-in-creating-xmlgregoriancalendar-hits-performance-badly
//     */
//    final private static ThreadLocal<DatatypeFactory> datatypeFactoryHolder = new ThreadLocal<DatatypeFactory>()
//    {
//        @Override
//        protected DatatypeFactory initialValue()
//        {
//            try
//            {
//                return DatatypeFactory.newInstance();
//            } catch (DatatypeConfigurationException e)
//            {
//                throw new IllegalStateException("failed to create " + DatatypeFactory.class.getSimpleName(), e);
//            }
//        }
//    };
//    /**
//     * Transforme un objet date pour qu'il soit utilisable par l'api sesame
//     * @param date
//     * @return 
//     * @see http://stackoverflow.com/questions/7346508/datatypefactory-usage-in-creating-xmlgregoriancalendar-hits-performance-badly
//     */
//    public static XMLGregorianCalendar dateToXMLGregorianCalendar(Date date)
//    {
//        GregorianCalendar c = new GregorianCalendar();
//        c.setTime(date);
//        return datatypeFactoryHolder.get().newXMLGregorianCalendar(c);
//    }
    
     /**
     * 
     * @param bool String
     * @return la valeur booléenne du string (true pour t ou true, false pour f ou false)
     */
    public static boolean getStringBooleanValue(String bool) {
        return (bool.equalsIgnoreCase("true") || bool.equalsIgnoreCase("t"));
    }
}
