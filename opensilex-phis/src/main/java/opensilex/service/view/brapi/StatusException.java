//******************************************************************************
//                             StatusException.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi;

/**
 * Status exception model.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class StatusException {

    public String type;
    public String href;
    public String details;

    public StatusException(String type, String details) {
        this.type = type;
        this.details = details;
    }

    public StatusException(String type, String href, String details) {
        this.type = type;
        this.href = href;
        this.details = details;
    }
}
