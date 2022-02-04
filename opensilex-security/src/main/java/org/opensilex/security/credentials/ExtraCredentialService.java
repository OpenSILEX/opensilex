/*******************************************************************************
 *                         CredentialService.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 02/12/2021
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.security.credentials;

import org.opensilex.config.ConfigManager;
import org.opensilex.security.credentials.config.CredentialConfig;
import org.opensilex.utils.ClassUtils;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * This services fetches credentials from a config file. This allows to define
 * credentials without using them in the api.
 *
 * @author Valentin RIGOLLE
 */
public class ExtraCredentialService {

    protected static final String CREDENTIALS_CONFIG_PATH = "credentials/credentials.yml";

    private CredentialConfig credentialConfig;

    public CredentialConfig getCredentialConfig() {
        if (credentialConfig == null) {
            buildCredentials();
        }
        return credentialConfig;
    }

    private void buildCredentials() {
        ConfigManager cfg = new ConfigManager();
        try {
            FileInputStream configFile = new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), CREDENTIALS_CONFIG_PATH));
            cfg.addSource(configFile);
            credentialConfig = cfg.loadConfig("", CredentialConfig.class);
        } catch (IOException ignored) {
        }
    }
}
