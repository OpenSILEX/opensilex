/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.deserializer;

import java.text.DateFormat;
import java.util.Date;

/**
 *
 * @author vincent
 */
public class DateDeserializer implements Deserializer<Date> {

    @Override
    public Date fromString(String value) throws Exception {
        return DateFormat.getDateInstance().parse(value);
    }
}
