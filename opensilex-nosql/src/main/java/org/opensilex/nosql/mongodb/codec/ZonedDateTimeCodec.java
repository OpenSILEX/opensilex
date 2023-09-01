package org.opensilex.nosql.mongodb.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 *
 * A custom Codec for encoding {@link ZonedDateTime} by taking account UTC offset
 * This codec can have the following usages :
 * <ul>
 *      <li> Encode a BSON/{@link org.bson.Document} filter which have a DateTime as value </li>
 *      <li> Decode a generic {@link org.bson.Document} which have a DateTime field </li>
 * </ul>
 * @see <a href="https://www.mongodb.com/docs/manual/reference/bson-types/#date">BSON date type</a>
 * @author Maximilian HART
 */
public class ZonedDateTimeCodec implements Codec<ZonedDateTime> {
    @Override
    public ZonedDateTime decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(bsonReader.readDateTime()), ZoneOffset.UTC);
    }

    @Override
    public void encode(BsonWriter bsonWriter, ZonedDateTime zonedDateTime, EncoderContext encoderContext) {
        bsonWriter.writeDateTime(zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    @Override
    public Class<ZonedDateTime> getEncoderClass() {
        return ZonedDateTime.class;
    }
}
