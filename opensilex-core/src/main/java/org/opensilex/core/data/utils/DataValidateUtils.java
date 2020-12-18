/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.utils;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
                    if ((value instanceof Integer)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:decimal":
                    if ((value instanceof Double)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:boolean":
                    if ((value instanceof Boolean)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:date":
                    if ((value instanceof String)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:datetime":
                    if ((value instanceof String)) {
                        checkCoherence = true;
                    }
                    break;
                case "xsd:string":
                    if ((value instanceof String)) {
                        checkCoherence = true;
                    }
                    break;
                default:
                    break;
            }
        }

        return checkCoherence;
    }
    
    public static ParsedDateTimeMongo validateDataDateTime(String value){
       ParsedDateTimeMongo parsedDateTimeMongo = null;
        for (DateFormat dateCheckFormat : dateTimeFormats) {
                    try {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateCheckFormat.toString());
                        OffsetDateTime ost = OffsetDateTime.parse(value, dtf);
                        LocalDateTime dateTimeUTC = ost.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
                        String offset = ost.getOffset().toString();
                        parsedDateTimeMongo = new ParsedDateTimeMongo(dateTimeUTC,offset);
                        break;
                    } catch (DateTimeParseException e) {
                    }
                }
        return parsedDateTimeMongo;
    }
    
}
