/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.utils;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.exception.*;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.DateFormatters;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.function.Function;

import static java.lang.Float.NaN;
import static org.opensilex.core.data.api.DataCreationDTO.NAN_VALUES;
import static org.opensilex.core.data.api.DataCreationDTO.NA_VALUES;

/**
 * @author charlero
 * @author Valentin RIGOLLE
 */
public class DataValidateUtils {
    /**
     * <p>
     * Mapping between an XSD datatype and a converter function (which takes a string and converts it to an object of
     * corresponding java type, e.g. {@link LocalDate} for a "xsd:date"). The XSD datatype is stored using the expanded URI.
     * </p>
     * <p>
     * Values should be accessed via the {@link #getDataTypeConverter(URI)} method.
     * </p>
     * <p>
     *     Supported XSD types and the java type they are converted into are listed below :
     *     <ul>
     *         <li>xsd:decimal -> {@link Double}</li>
     *         <li>xsd:integer -> {@link Integer}</li>
     *         <li>xsd:boolean -> {@link Boolean}</li>
     *         <li>xsd:date -> {@link LocalDate}</li>
     *         <li>xsd:datetime -> {@link ZonedDateTime}</li>
     *     </ul>
     * </p>
     */
    private static final Map<String, Function<String, Object>> DATATYPE_CONVERTER_MAP = new HashMap<String, Function<String, Object>>() {{
        put(XSD.decimal.getURI(), Double::parseDouble);
        put(XSD.integer.getURI(), Integer::parseInt);
        put(XSD.xboolean.getURI(), DataValidateUtils::asBoolean);
        put(XSD.date.getURI(), DataValidateUtils::asDate);
        put(XSD.dateTime.getURI(), DataValidateUtils::asDateTime);
    }};

    /**
     * <p>
     * Returns a method for converting a string to a java type corresponding to the given datatype. For example, passing
     * "xsd:decimal" as parameter will return the static method {@link Double#parseDouble(String)}, which converts a String to a Double.
     * </p>
     * <p>
     *     Supported XSD types and the java type they are converted into are listed below :
     *     <ul>
     *         <li>xsd:decimal -> {@link Double}</li>
     *         <li>xsd:integer -> {@link Integer}</li>
     *         <li>xsd:boolean -> {@link Boolean}</li>
     *         <li>xsd:date -> {@link LocalDate}</li>
     *         <li>xsd:datetime -> {@link ZonedDateTime}</li>
     *     </ul>
     * </p>
     * @param datatype The XSD datatype
     * @return A function to convert a string to the corresponding
     */
    private static Function<String, Object> getDataTypeConverter(URI datatype) {
        return DATATYPE_CONVERTER_MAP.get(SPARQLDeserializers.getExpandedURI(datatype));
    }

    /**
     * Converts a String to a {@link LocalDate}. Expected format is 'YYYY-MM-DD', for example '2022-07-18'.
     *
     * @param value a String representing a date
     * @return The parsed {@link LocalDate}
     * @throws DateTimeParseException if the value cannot be parsed
     */
    private static LocalDate asDate(String value) throws DateTimeParseException {
        return LocalDate.parse(value, DateFormatters.fromFormat(DateFormat.YMD));
    }

    /**
     * <p>
     * Converts a String to a {@link ZonedDateTime}. Multiple formats are supported, including the ISO :
     * </p>
     * <ul>
     *     <li>2020-11-21T11:12:02+01:00</li>
     *     <li>2020-11-21T11:12:02.123+01:00</li>
     *     <li>2020-11-21T11:12:02+0100</li>
     *     <li>2020-11-21T11:12:02</li>
     * </ul>
     * <p>
     *     If no timezone is specified, the system timezone will be used.
     * </p>
     *
     * @param value a String representing a datetime
     * @return The parsed {@link ZonedDateTime}
     * @throws DateTimeParseException if the value cannot be parsed
     */
    private static ZonedDateTime asDateTime(String value) throws DateTimeParseException {
        DateTimeFormatter[] offsetFormats = {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,           //"2020-11-21T11:12:02+01:00"
                DateFormatters.fromFormat(DateFormat.YMDTHMSMSZ), //"2020-11-21T11:12:02.123+0100"
                DateFormatters.fromFormat(DateFormat.YMDTHMSZ)    //"2020-11-21T11:12:02+0100"
        };

        // First, try formats with a timezone
        for (DateTimeFormatter dtf : offsetFormats) {
            try {
                return ZonedDateTime.parse(value, dtf);
            } catch (DateTimeParseException ignored) {
            }
        }

        // If no timezone is present, try using the system timezone
        LocalDateTime ldt = LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        return ldt.atZone(ZoneId.systemDefault());
    }

    /**
     * Converts a String to a Boolean. Multiple values are recognized like 'true', 'FALSE', 'yes' or 'n'. Please see
     * {@link BooleanUtils#toBooleanObject(String)} for the list of supported values.
     *
     * @param value A String representing the boolean.
     * @return The corresponding boolean
     * @throws IllegalArgumentException If the value cannot be parsed
     */
    private static Boolean asBoolean(String value) throws IllegalArgumentException {
        Boolean booleanValue = BooleanUtils.toBooleanObject(value);
        if (Objects.isNull(booleanValue)) {
            throw new IllegalArgumentException();
        }
        return booleanValue;
    }

    public static Object convertData(URI dataType, String value) {
        return getDataTypeConverter(dataType).apply(value);
    }
    
    public static ParsedDateTimeMongo setDataDateInfo(String date, String timezone) throws TimezoneAmbiguityException, TimezoneException {
        ParsedDateTimeMongo parsedDateTimeMongo = null;        
        String defaultTimezone = MongoDBService.getDefaultTimeZone();
        //try parsing simple date "2020-01-01"
        try {
            LocalDate localDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(date));
            ZoneId tz;
            if (timezone != null) {
                tz = ZoneId.of(timezone);
            } else {
                tz = ZoneId.of(defaultTimezone);
            }

            OffsetDateTime odt = localDate.atStartOfDay().atZone(tz).toOffsetDateTime();                             
            parsedDateTimeMongo = new ParsedDateTimeMongo(odt.toInstant(), odt.getOffset().toString(), Boolean.FALSE);
        } catch (ZoneRulesException e) {       
            throw new TimezoneException(timezone);    
        } catch (Exception e1) {            
        }  

        //try parsing datetime with offset 
        //"2020-11-21T11:12:00.123+01:00" (or +0100)
        //"2020-11-21T11:12:00+01:00" (or +0100)
        if (parsedDateTimeMongo == null) {

            DateTimeFormatter offsetLDTwithMS =  DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSZ.toString()); //"2020-11-21T11:12:02.123+0100"
            DateTimeFormatter offsetLDT =  DateTimeFormatter.ofPattern(DateFormat.YMDTHMSZ.toString()); //"2020-11-21T11:12:02+0100"
            DateTimeFormatter[] offsetFormats = {DateTimeFormatter.ISO_OFFSET_DATE_TIME, offsetLDT, offsetLDTwithMS};

            for (DateTimeFormatter dateTimeFormat : offsetFormats) {
                try {
                    OffsetDateTime odt = OffsetDateTime.from(dateTimeFormat.parse(date));                    
                    if (timezone != null) {            
                        ZoneId tz = ZoneId.of(timezone);
                        ZoneOffset zoneOffset = tz.getRules().getOffset(odt.toInstant());
                        if (!odt.getOffset().equals(zoneOffset)) {
                            throw new TimezoneAmbiguityException(date);
                        }
                    }
                    parsedDateTimeMongo = new ParsedDateTimeMongo(odt.toInstant(), odt.getOffset().toString(), Boolean.TRUE);
                } catch (TimezoneAmbiguityException e2) {       
                    throw e2;
                } catch (ZoneRulesException e) {
                    throw new TimezoneException(timezone);
                } catch (Exception e) {                    
                }
            }
        }

        //try parsing datetime without offset 
        // "2020-11-21T11:12:00.123" or "2020-11-21T11:12:00"
        if (parsedDateTimeMongo == null) {
            try {
                LocalDateTime ldt = LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(date));                   
                ZoneId tz;
                if (timezone != null) {
                    tz = ZoneId.of(timezone);
                } else {
                    tz = ZoneId.of(defaultTimezone);
                }
                ZoneOffset offset = tz.getRules().getOffset(ldt); 
                OffsetDateTime odt = ldt.atOffset(offset);                        
                parsedDateTimeMongo = new ParsedDateTimeMongo(odt.toInstant(), odt.getOffset().toString(), Boolean.TRUE);
            } catch (ZoneRulesException e) {
                    throw new TimezoneException(timezone);
            } catch (Exception e3) {            
            }   
        }
        
        return parsedDateTimeMongo;
    }
    
    public static Instant getDateInstant(String date, String timezone, Boolean isEndDate) throws UnableToParseDateException, TimezoneAmbiguityException, TimezoneException {
        Instant instant = null;
        String defaultTimezone = MongoDBService.getDefaultTimeZone();
        
        if(StringUtils.isBlank(date)){
            throw new UnableToParseDateException(date);
        }
        
        //try parsing simple date "2020-01-01"
        try {
            LocalDate localDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(date));
            if (isEndDate) {
                localDate = localDate.plusDays(1);
            }
            
            ZoneId tz;
            if (timezone != null) {
                tz = ZoneId.of(timezone);
            } else {
                tz = ZoneId.of(defaultTimezone);
            }
            
            instant = localDate.atStartOfDay().atZone(tz).toOffsetDateTime().toInstant();           
        } catch (ZoneRulesException e) {            
            throw new TimezoneException(timezone);
        } catch (Exception e1) {            
        }
        
        //try parsing datetime with offset 
        //"2020-11-21T11:12:00.123+01:00" (or +0100)
        //"2020-11-21T11:12:00+01:00" (or +0100)
        if (instant == null) {
            DateTimeFormatter offsetLDTwithMS =  DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSZ.toString());
            DateTimeFormatter offsetLDT =  DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMS.toString());
            DateTimeFormatter[] offsetFormats = {DateTimeFormatter.ISO_OFFSET_DATE_TIME, offsetLDT, offsetLDTwithMS};
            
            for (DateTimeFormatter dateTimeFormat : offsetFormats) {
                try {
                    OffsetDateTime ost = OffsetDateTime.from(dateTimeFormat.parse(date));
                    if (timezone != null) {            
                        ZoneId tz = ZoneId.of(timezone);
                        ZoneOffset zoneOffset = tz.getRules().getOffset(ost.toInstant());
                        if (!ost.getOffset().equals(zoneOffset)) {
                            throw new TimezoneAmbiguityException(date);
                        }
                    }
                    instant = ost.toInstant(); 
                } catch (TimezoneAmbiguityException e) {       
                    throw e;
                } catch (ZoneRulesException e) {
                    throw new TimezoneException(timezone);
                } catch (DateTimeParseException e) { 
                }
            }
        }        
        
        //try parsing datetime without offset 
        // "2020-11-21T11:12:00.123" or "2020-11-21T11:12:00"
        if (instant == null) {            
            try {
                LocalDateTime ldt = LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(date));
                ZoneId tz;
                if (timezone != null) {
                    tz = ZoneId.of(timezone);
                } else {
                    tz = ZoneId.of(defaultTimezone);
                }
                ZoneOffset offset = tz.getRules().getOffset(ldt); 
                instant = ldt.atOffset(offset).toInstant();                      
            } catch (ZoneRulesException e) {   
                throw new TimezoneException(timezone);
            }                        
        }
                
        if (instant == null) {
            throw new UnableToParseDateException(date);
        }
    
        return instant;
    }

    /**
     * Validates and converts the "value" field of a data model with the correct Java type.
     *
     * @param dataModel The data model to update
     * @param variable  The variable URI
     * @param value     The value to set
     * @param dataType  The data type of the value
     * @throws CSVDataTypeException If the value is invalid
     */
    public static void checkAndConvertValue(DataModel dataModel, URI variable, Object value, URI dataType) throws CSVDataTypeException, DataTypeException {
        checkAndConvertValue(dataModel, variable, value, dataType, null, null, null);
    }

    /**
     * Validates and converts the "value" field of a data model with the correct Java type.
     *
     * @param dataModel     The data model to update
     * @param variable      The variable URI
     * @param value         The value to set
     * @param dataType      The data type of the value
     * @param dataIndex     The index of the data (for error reporting)
     * @param colIndex      The column of the data (for error reporting)
     * @param csvValidation The validation model
     * @throws CSVDataTypeException If the value is invalid
     */
    public static void checkAndConvertValue(DataModel dataModel, URI variable, Object value, URI dataType, Integer dataIndex, Integer colIndex, DataCSVValidationModel csvValidation) throws CSVDataTypeException, DataTypeException {
        Object validValue = returnValidCSVDatum(variable, value, dataType, dataIndex, colIndex, csvValidation);

        dataModel.setValue(validValue);
    }

    /**
     * Validates and returns the "value" field converted to the correct Java type.
     *
     * @param variable      The variable URI
     * @param value         The value to set
     * @param dataType      The data type of the value
     * @param dataIndex     The index of the data (for error reporting)
     * @param colIndex      The column of the data (for error reporting)
     * @param csvValidation The validation model
     * @return The value field converted to the correct Java type
     * @throws CSVDataTypeException If the value is invalid
     */
    private static Object returnValidCSVDatum(URI variable, Object value, URI dataType, Integer dataIndex,
                                              Integer colIndex, DataCSVValidationModel csvValidation)
            throws CSVDataTypeException, DataTypeException {

        // Special values
        if (NAN_VALUES.contains(value)) {
            value = NaN;
        } else if (NA_VALUES.contains(value)) {
            return null;
        }

        // No datatype => no check
        if (dataType == null) {
            return value;
        }

        // String is a special case, we juste have to check the type
        if (SPARQLDeserializers.compareURIs(dataType, XSD.xstring.getURI())) {
            if ((value instanceof String)) {
                return value;
            } else {
                throwDataTypeException(variable, value, dataType, dataIndex, colIndex, csvValidation);
            }
        }

        // For every other type (decimal, integer, date, datetime, boolean) we use the converter map
        try {
            return getDataTypeConverter(dataType).apply(value.toString());
        } catch (Exception e) {
            throwDataTypeException(variable, value, dataType, dataIndex, colIndex, csvValidation);
        }

        return value;
    }

    /**
     * Throws either a DataTypeException or a CSVDataTypeException in the context of a CSV validation.
     *
     * @param variable      The variable URI
     * @param value         The value to set
     * @param dataType      The data type of the value
     * @param dataIndex     The index of the data (for error reporting)
     * @param colIndex      The column of the data (for error reporting)
     * @param csvValidation The validation model
     * @throws CSVDataTypeException The validation error in the context of CSV validation
     * @throws DataTypeException The default validation error
     */
    private static void throwDataTypeException(URI variable, Object value, URI dataType,
                                        Integer dataIndex, Integer colIndex, DataCSVValidationModel csvValidation)
            throws CSVDataTypeException, DataTypeException {
        if (Objects.nonNull(dataIndex) && Objects.nonNull(colIndex) && Objects.nonNull(csvValidation)) {
            String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';
            CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
            throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
        }

        throw new DataTypeException(variable, value, dataType);
    }

    /**
     * Validates and returns the "rawData" field.
     *
     * @param variable      The variable URI
     * @param rawDataCell   The raw data
     * @param dataType      The data type of the value
     * @param dataIndex     The index of the data (for error reporting)
     * @param colIndex      The column of the data (for error reporting)
     * @param csvValidation The validation model
     * @return The rawData field converted to a list of objects with the correct Java types
     * @throws CSVDataTypeException If the raw data is invalid
     */
    public static List<Object> returnValidRawData(URI variable, String rawDataCell, URI dataType, int dataIndex, int colIndex, DataCSVValidationModel csvValidation) throws CSVDataTypeException {
        String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';

        if (dataType == null) {
            return Arrays.asList(rawDataCell.split(","));
        }

        List<Object> validRawData = new ArrayList<>();
        try {
            for (Object data : rawDataCell.split(",")) {
                validRawData.add(returnValidCSVDatum(variable, data, dataType, dataIndex, colIndex, csvValidation));
            }
        } catch (DataTypeException e) {
            CSVCell errorCell = new CSVCell(dataIndex, colIndex, rawDataCell, variableName);
            throw new CSVDataTypeException(variable, rawDataCell, dataType, dataIndex, errorCell);
        }

        return validRawData;
    }
}
