/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.germplasm.dal;

import java.net.URI;
import javax.jdo.AttributeConverter;

/**
 *
 * @author boizetal
 */
public class URIStringConverter implements AttributeConverter<URI, String> {
    
    public String convertToDatastore(URI url) {
        return url != null ? url.toString() : null;
    }

    public URI convertToAttribute(String str) {
        if (str == null)
        {
            return null;
        }

        URI uri = null;
        try
        {
            uri = new URI(str);
        }
        catch (Exception mue)
        {
            throw new IllegalStateException("Error converting the URI", mue);
        }
        return uri;
    }

}

