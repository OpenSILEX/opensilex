//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.exception;

import java.net.URI;
import java.util.Map;


public class DuplicateURIListException extends Exception {

    private final Map<URI,String> existingNameByURI;

    public DuplicateURIListException(Map<URI,String> existingNameByURI){
        this.existingNameByURI = existingNameByURI;
    }

    /**
     *
     * @return association between existing URI and corresponding name
     */
    public Map<URI,String> getExistingNameByURI() {
        return existingNameByURI;
    }
}
