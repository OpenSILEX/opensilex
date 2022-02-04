//******************************************************************************
//                          ProvenanceAgentTypeException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

import org.opensilex.core.data.dal.DataModel;

/**
 *
 * @author Julien Bonnefont
 */
public class ProvenanceAgentTypeException extends Exception {
    
    public ProvenanceAgentTypeException(String agent) {
        super("Wrong agent provenance format : " + agent);
        
    }

}
