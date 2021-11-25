/*******************************************************************************
 *                         OpensilexCommandException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 25/11/2021 11:33
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.cli;

/**
 * @author
 */
public class OpensilexCommandException extends Exception{

    public OpensilexCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpensilexCommandException(OpenSilexCommand command, String message, Throwable cause){
        super("["+command.getClass().getName()+"] : "+message,cause);
    }

}
