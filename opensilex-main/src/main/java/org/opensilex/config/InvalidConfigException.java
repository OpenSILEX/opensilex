//******************************************************************************
//                      InvalidConfigException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.config;

/**
 * Excption in case of invalid configuration detection
 *
 * @author Vincent Migot
 */
public class InvalidConfigException extends Exception {

    /**
     * Constructor based on another exception.
     *
     * @param ex Exception causing invalid configuration
     */
    public InvalidConfigException(Exception ex) {
        super(ex);
    }

    /**
     * Constructor based on a direct message.
     *
     * @param message Message of the exception
     */
    public InvalidConfigException(String message) {
        super(message);
    }

}
