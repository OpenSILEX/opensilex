package com.researchspace.dataverse.entities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * 'message' property can be a string or an object with property 'message'; this handles both
 */
public class ObjectOrStringMessageDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node =  jp.getCodec().readTree((jp));
        if (node.isTextual()){
            return node.toString();
        } else if (node.isObject()){
            return node.get("message").toString();
        } else{
            throw new IllegalArgumentException("expect a string or an object with a string property 'message'");
        }

    }
}
