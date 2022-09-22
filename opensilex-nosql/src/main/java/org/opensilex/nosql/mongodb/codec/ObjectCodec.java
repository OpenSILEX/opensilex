//******************************************************************************
//                          ObjectCodec.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.nosql.mongodb.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 *
 * @author Alice Boizet
 */
public class ObjectCodec implements Codec<Object>{

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext ec) {
        if (value instanceof Integer) {
            writer.writeInt32((Integer) value);
        } else if (value instanceof Double) {
            writer.writeDouble((Double) value);
        } else if (value instanceof Boolean) {
            writer.writeBoolean((Boolean) value);
        } else if (value instanceof String) {
            writer.writeString((String) value);
        } else if (value instanceof LocalDate) {
            writer.writeDateTime(((LocalDate) value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
        } else if (value instanceof ZonedDateTime) {
            writer.writeDateTime(((ZonedDateTime) value).withZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
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
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(instant), ZoneOffset.UTC);
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
