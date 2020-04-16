//******************************************************************************
//                          StatusDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import ch.qos.logback.classic.Level;

/**
 * <pre>
 * Response metadata status DTO.
 *
 * A status is composed of a message and a messageType which could be: - ERROR -
 * WARN - INFO - DEBUG
 * </pre>
 *
 * @see org.opensilex.server.response.MetadataDTO
 * @author Vincent Migot
 */
public class StatusDTO {

    /**
     * Status message.
     */
    public final String message;

    /**
     * Status message type ERROR|WARN|INFO|DEBUG.
     */
    public final Level messageType;

    /**
     * Constructor for status.
     *
     * @param message status message
     * @param messageType status type
     */
    public StatusDTO(String message, Level messageType) {
        this.message = message;
        this.messageType = messageType;
    }

    /**
     * Getter for message.
     *
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Getter for status message type.
     *
     * @return message type
     */
    public Level getMessageType() {
        return messageType;
    }
}
