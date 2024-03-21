package org.opensilex.olga;

import org.opensilex.OpenSilexModule;
import org.opensilex.core.CoreModule;
import org.opensilex.server.extensions.APIExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlgaModule extends OpenSilexModule implements APIExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreModule.class);

    @Override
    public Class<?> getConfigClass() {
        return OlgaConfig.class;
    }

    @Override
    public String getConfigId() {
        return "olga";
    }
}