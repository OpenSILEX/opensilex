/*******************************************************************************
 *                         OpensilexUpdateException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 25/11/2021 11:27
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.update;

/**
 * @author rcolin
 */
public class OpensilexModuleUpdateException extends Exception{

    public OpensilexModuleUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpensilexModuleUpdateException(OpenSilexModuleUpdate update, Throwable cause) {
        super(update.getDescription(), cause);
    }

}
