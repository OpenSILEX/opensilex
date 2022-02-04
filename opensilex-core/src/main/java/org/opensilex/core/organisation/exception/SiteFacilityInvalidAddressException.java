/*******************************************************************************
 *                         SiteFacilityInvalidAddressException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 07/12/2021
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.organisation.exception;

import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;

import java.util.HashMap;

/**
 * Exception thrown when attempting to associate a site and a facility with different addresses
 *
 * @author Valentin RIGOLLE
 */
public class SiteFacilityInvalidAddressException extends DisplayableBadRequestException {
    public static final String TRANSLATION_KEY = "component.organization.exception.siteFacilityInvalidAddress";

    public SiteFacilityInvalidAddressException(String siteName, String facilityName) {
        super("The site " + siteName + " and the facility " + facilityName + " must have the same address",
                TRANSLATION_KEY,
                new HashMap<String, String>() {{
                    put("site", siteName);
                    put("facility", facilityName);
                }});
    }
}
