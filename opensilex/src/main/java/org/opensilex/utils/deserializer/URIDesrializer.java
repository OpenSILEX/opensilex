/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.deserializer;

import java.net.URI;

/**
 *
 * @author vincent
 */
public class URIDesrializer implements Deserializer<URI> {

    @Override
    public URI fromString(String value) throws Exception {
        return new URI(value);
    }
}