//******************************************************************************
//                                   Status.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import ch.qos.logback.classic.Level;

/**
 * BrAPI status model.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class Status {

    public final String message;
    public final Level messageType;

    public Status(String message, Level messageType) {
        this.message = message;
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public Level getMessageType() {
        return messageType;
    }
}

