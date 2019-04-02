//******************************************************************************
//                                   Status.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi;

/**
 * Implémentation de la plant breeding api pour les status des messages de retour.
 * Il est possible de mettre des codes Error ou autre
 * @author Arnaud Charleroy
 */
public class Status {

    public String message = null;
    public StatusException exception = null;

    public Status() {
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

