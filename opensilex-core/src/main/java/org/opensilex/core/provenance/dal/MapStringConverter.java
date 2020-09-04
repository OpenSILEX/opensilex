/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.provenance.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.AttributeConverter;

/**
 *
 * @author boizetal
 */
public class MapStringConverter implements AttributeConverter<Map, String> {

    @Override
    public String convertToDatastore(Map attribute) {
        if (attribute == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();
        // Create ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            // Convert object to JSON string
            str.append(mapper.writeValueAsString(attribute));
        } catch (JsonProcessingException ex) {
            System.out.println(ex.getMessage());
        }

        return str.toString();
    }

    @Override
    public Map convertToAttribute(String columnValue) {
        if (columnValue == null) {
            return null;
        }
        // Create ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        Map map = new HashMap<>();
        try {
             map = mapper.readValue(columnValue, Map.class);
        } catch (JsonProcessingException ex) {
            System.out.println(ex.getMessage());
        }
        return map;
    }

}
