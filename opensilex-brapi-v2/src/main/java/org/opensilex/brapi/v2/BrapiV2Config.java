package org.opensilex.brapi.v2;

import org.opensilex.config.ConfigDescription;

public interface BrapiV2Config {

    @ConfigDescription(
            value = "BrapiV2 host base URL"
    )
    String host();

    @ConfigDescription(
            value = "BrapiV2 API token"
    )
    String token();
}