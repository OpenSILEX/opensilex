/*******************************************************************************
 *                         ValidationContext.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.owl;

public interface ValidationContext {

    String getValue();

    void setValue(String value);

    String getProperty();

    void setProperty(String property);

}
