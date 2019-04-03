//******************************************************************************
//                             ResponseUnique.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: Aug. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi.form;

import opensilex.service.view.brapi.Metadata;

/**
 * Response unique.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResponseUnique {
    Metadata metadata;
    Object result;

    public ResponseUnique( Object result) {
        this.metadata = new Metadata();
        this.result = result;
    }
}
