/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.deserializer;

import java.math.BigInteger;

/**
 *
 * @author vincent
 */
public class BigIntegerDeserializer implements Deserializer<BigInteger> {

    @Override
    public BigInteger fromString(String value) throws Exception {
        return new BigInteger(value);
    }
}
