package org.opensilex.front;

import org.opensilex.config.ConfigDescription;

public interface GraphConfig {
    @ConfigDescription(
            value = "Variable for the graphic component"
    )
    String variable();

    @ConfigDescription(
            value = "Data location for the graphic component"
    )
    String dataLocationInformations();
}
