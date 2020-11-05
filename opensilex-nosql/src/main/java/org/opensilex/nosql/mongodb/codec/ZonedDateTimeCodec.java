/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.mongodb.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
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

        long instant = reader.readDateTime();
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(instant), ZoneId.of("UTC"));
        return dateTime;
    }

}
