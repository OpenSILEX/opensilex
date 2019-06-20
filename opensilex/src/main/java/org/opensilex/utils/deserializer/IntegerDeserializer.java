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
public class IntegerDeserializer implements Deserializer<Integer> {

    @Override
    public Integer fromString(String value) throws Exception {
        return Integer.valueOf(value);
    }
}
