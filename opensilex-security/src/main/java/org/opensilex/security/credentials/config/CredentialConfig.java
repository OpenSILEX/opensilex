/*******************************************************************************
 *                         CredentialConfig.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 02/12/2021
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.security.credentials.config;

import org.opensilex.config.ConfigDescription;

import java.util.List;

/**
 * @author Valentin RIGOLLE
 */
public interface CredentialConfig {
    @ConfigDescription("Credential groups")
    List<CredentialGroup> credentialGroups();
}
