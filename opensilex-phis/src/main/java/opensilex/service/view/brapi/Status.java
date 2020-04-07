//******************************************************************************
//                                   Status.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi;

/**
 * BrAPI status model.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class Status {

    public String message = null;
    public StatusException exception = null;

    public Status() {
    }
    
    public Status(String message) {
    	this.message = message;
    }

    public Status(String message, StatusException exception) {
        this.exception = exception;
        this.message = message;
    }

    public Status(String message, String code, String details) {
        this.exception = new StatusException(code, details);
        this.message = message;
    }
}

