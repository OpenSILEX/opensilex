//******************************************************************************
//                          URIStringConverter.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import java.net.URI;
import javax.jdo.AttributeConverter;

/**
 * Class used to convert URI and store them as string
 * @author boizetal
 */
public class URIStringConverter implements AttributeConverter<URI, String> {
    
    @Override
    public String convertToDatastore(URI url) {
        return url != null ? url.toString() : null;
    }

    @Override
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
