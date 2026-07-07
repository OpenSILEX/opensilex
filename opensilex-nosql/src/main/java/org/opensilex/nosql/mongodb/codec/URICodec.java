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

/**
 * @author jpvert
 */
public class URICodec implements Codec<URI> {

    private final static Logger LOGGER = LoggerFactory.getLogger(URICodec.class);

    @Override
    public void encode(BsonWriter writer, URI value, EncoderContext encoderContext) {
        writer.writeString(SPARQLDeserializers.getExpandedURI(value.toString()));
    }

    @Override
    public Class<URI> getEncoderClass() {
        return URI.class;
    }

    @Override
    public URI decode(BsonReader reader, DecoderContext decoderContext) {
        try {
            String strURI = reader.readString();
            if (strURI != null && !strURI.isEmpty()) {
                return SPARQLDeserializers.formatURI(new URI(strURI));
            }
        } catch (URISyntaxException ex) {
            LOGGER.warn("Exception while decoding mongodb URI (should never append", ex);
        }

        return null;
    }

}
