/*******************************************************************************
 *                         StatusLevel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 17/05/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server.response;

/**
 * Status message level for {@link StatusDTO}
 *
 * @author Valentin Rigolle
 */
public enum StatusLevel {
    ERROR,
    WARNING,
    INFO,
    DEBUG
}
