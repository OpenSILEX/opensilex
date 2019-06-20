/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.deserializer;

/**
 *
 * @author vincent
 */
public class StringDeserializer implements Deserializer<String> {

    @Override
    public String fromString(String value) throws Exception {
        return value;
    }
}
