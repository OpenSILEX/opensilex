//******************************************************************************
//                          ZonedDateTimeCodec.java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Alice Boizet
 */
public class ZonedDateTimeCodec implements Codec<ZonedDateTime> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ZonedDateTimeCodec.class);

    @Override
    public void encode(BsonWriter writer, ZonedDateTime dateTime, EncoderContext encoderContext) {
        long gmtTime = dateTime.withZoneSameLocal(ZoneId.of("UTC")).toInstant().toEpochMilli();         
        writer.writeDateTime(gmtTime);
    }

    @Override
    public Class<ZonedDateTime> getEncoderClass() {
        return ZonedDateTime.class;
    }

    @Override
    public ZonedDateTime decode(BsonReader reader, DecoderContext decoderContext) {
        try {
            long instant = reader.readDateTime();
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(instant), ZoneId.of("UTC"));
            return dateTime;
        } catch (Exception e) {
            LOGGER.warn("Exception while decoding zonedDateTime", e);
            throw e;
        }      
    }

}
