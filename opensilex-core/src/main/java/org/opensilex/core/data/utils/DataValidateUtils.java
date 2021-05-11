/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.utils;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author charlero
 */
public class DataValidateUtils {

    public static final DateFormat[] dateTimeFormats = {DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ, DateFormat.YMDTHMSMSGTZ};

    public static  Boolean checkTypeCoherence(URI dataType, Object value) {
        Boolean checkCoherence = false;
        if (dataType == null) {
            checkCoherence = true;

        } else {
            switch (dataType.toString()) {
                case "xsd:integer":
                    if (value instanceof Integer) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:decimal":
                    if (value instanceof Double || value instanceof Integer) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:boolean":
                    if (value instanceof Boolean) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:date":
                    if (value instanceof String && isDate(value.toString())) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:datetime":
                    if (value instanceof String && isDateTime(value.toString())) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:string":
                    if (value instanceof String) {
                        checkCoherence = true;
                    }
                    break;
                default:
                    break;
            }
        }

        return checkCoherence;
    }
    
    public static Boolean isDate(String value) {
        try {
            LocalDate localDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(value));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static Boolean isDateTime(String value) {
        DateTimeFormatter offsetLDTwithMS =  DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSZ.toString()); //"2020-11-21T11:12:02.123+0100"
        DateTimeFormatter offsetLDT =  DateTimeFormatter.ofPattern(DateFormat.YMDTHMSZ.toString()); //"2020-11-21T11:12:02+0100"
        DateTimeFormatter[] offsetFormats = {DateTimeFormatter.ISO_OFFSET_DATE_TIME, offsetLDT, offsetLDTwithMS};
        Boolean isValid = false; 
        for (DateTimeFormatter dateTimeFormat : offsetFormats) {
            try {
                OffsetDateTime odt = OffsetDateTime.from(dateTimeFormat.parse(value));
                isValid = true;
                break;
            } catch (Exception e) {
            }
        }
        
        if (!isValid) {
            try {
                LocalDateTime ldt = LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(value));
                isValid = true;
            } catch (Exception e) {                
            }
        }
        return isValid;
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
    
}
