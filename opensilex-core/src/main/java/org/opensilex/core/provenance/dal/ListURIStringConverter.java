//******************************************************************************
//                          ListURIStringConverter.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.jdo.AttributeConverter;

/**
 * Class used to convert List(URI) and store them as List(string)
 * @author boizetal
 */
public class ListURIStringConverter implements AttributeConverter<List<URI>, List<String>>
{
    public List<String> convertToDatastore(List<URI> uriList) {
        List<String> stringList = new ArrayList();
        for (URI uri:uriList) {
            stringList.add(uri.toString());
        }
        return stringList;
    }

    public List<URI> convertToAttribute(List<String> stringList) {
        List<URI> uriList = new ArrayList();
        for (String str:stringList) {
            URI uri = null;
            try
            {
                uri = new URI(str);
                uriList.add(uri);
            }
            catch (Exception mue)
            {
                throw new IllegalStateException("Error converting the URI", mue);
            }            
        }
        return uriList;
    }
}
