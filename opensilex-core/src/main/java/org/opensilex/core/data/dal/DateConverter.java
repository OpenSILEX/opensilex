/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.dal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.AttributeConverter;

/**
 *
 * @author sammy
 */
public class DateConverter implements AttributeConverter<String,ZonedDateTime > {

    final String WANTED_FORMAT = "yyyy-MM-dd'T'H:m:s.SZ";
    @Override
    public ZonedDateTime convertToDatastore(String d) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(WANTED_FORMAT);//"yyyy-MM-dd'T'HH:mm:ss.SSSXXXX")
        ZonedDateTime date = ZonedDateTime.parse(d,dtf);
        return date;
    }

    @Override
    public String convertToAttribute(ZonedDateTime d) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(WANTED_FORMAT);
        String sd = dtf.format(d);
        return sd;
    }
    
}
