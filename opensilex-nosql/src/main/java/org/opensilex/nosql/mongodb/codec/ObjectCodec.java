//******************************************************************************
//                          ObjectCodec.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.nosql.mongodb.codec;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author Alice Boizet
 */
public class ObjectCodec implements Codec<Object>{

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext ec) {        
        
        try { 
            DateFormat[] formats = {DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ};
            for (DateFormat dateCheckFormat : formats) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateCheckFormat.toString());
                ZonedDateTime zdt = ZonedDateTime.parse(value.toString(), dtf);
                long dateTime = zdt.withZoneSameLocal(ZoneId.of("UTC")).toInstant().toEpochMilli();
                writer.writeDateTime(dateTime);
                break;
            }
        } catch (DateTimeParseException dateTimeExc) {
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DateFormat.YMD.toString());
                LocalDate date = LocalDate.parse(value.toString(), dtf);
                long dateTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                writer.writeDateTime(dateTime);
            } catch (DateTimeParseException dateExc){
                try {
                Integer intValue = Integer.parseInt(value.toString());
                writer.writeInt32(intValue);
                } catch (NumberFormatException intExc) {            
                    try {
                        Double doubleValue = Double.valueOf(value.toString());
                        writer.writeDouble(doubleValue);
                    } catch (NumberFormatException doubleExc) {    
                        if (value.toString().equalsIgnoreCase("true") || value.toString().equalsIgnoreCase("false")) {
                            try {
                                boolean boolValue = Boolean.parseBoolean(value.toString()); 
                                writer.writeBoolean(boolValue);                      

                            } catch (Exception boolExc){
                                try {
                                    String stringValue = value.toString();
                                    writer.writeString(stringValue);
                                } catch (Exception stringExc) {
                                    dateTimeExc.addSuppressed(dateExc);
                                    dateTimeExc.addSuppressed(intExc);
                                    dateTimeExc.addSuppressed(doubleExc);
                                    dateTimeExc.addSuppressed(boolExc);
                                    dateTimeExc.addSuppressed(stringExc);
                                    throw dateExc;
                                }
                            }
                        } else {
                            try {
                                String stringValue = value.toString();
                                writer.writeString(stringValue);
                            } catch (Exception stringExc) {
                                dateTimeExc.addSuppressed(dateExc);
                                dateTimeExc.addSuppressed(intExc);
                                dateTimeExc.addSuppressed(doubleExc);
                                dateTimeExc.addSuppressed(stringExc);
                                throw dateExc;
                            }
                        }
                    }
                }
            }               

        }
        
        
  
    }

    @Override
    public Class<Object> getEncoderClass() {
        return Object.class;
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext dc) {       
        
        if (reader.getCurrentBsonType() != null) {
            switch (reader.getCurrentBsonType()) {
            case DATE_TIME: 
                long instant = reader.readDateTime();
                ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(instant), ZoneId.of("UTC"));
                return dateTime;
            case INT32: 
                return reader.readInt32();
            case INT64:
                return reader.readInt64();
            case DOUBLE:
                return reader.readDouble();
            case BOOLEAN:
                return reader.readBoolean();
            case STRING:
                return reader.readString();
            }
        }
        return null;
         
    }
    
}
