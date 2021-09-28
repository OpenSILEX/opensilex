//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.exception;

import java.net.URI;
import java.util.Map;

/**
 * @author rcolin
 */
public class DuplicateNameListException extends Exception {

    private final Map<String,URI> existingUriByName;

    public DuplicateNameListException(Map<String,URI> existingUriByName){
        this.existingUriByName = existingUriByName;
    }

    /**
     *
     * @return association between existing name and corresponding uri
     */
    public Map<String, URI> getExistingUriByName() {
        return existingUriByName;
    }
}
