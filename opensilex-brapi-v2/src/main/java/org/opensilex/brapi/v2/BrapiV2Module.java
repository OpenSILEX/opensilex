package org.opensilex.brapi.v2;

import org.opensilex.OpenSilexModule;
import org.opensilex.core.CoreModule;
import org.opensilex.server.extensions.APIExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrapiV2Module extends OpenSilexModule implements APIExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreModule.class);

    @Override
    public Class<?> getConfigClass() {
        return BrapiV2Config.class;
    }

    @Override
    public String getConfigId() {
        return "brapi-v2";
    }
}