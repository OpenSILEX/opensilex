package org.opensilex.front;

import org.opensilex.config.ConfigDescription;

public interface GraphConfig {
    @ConfigDescription(
            value = "Variable for the graphic component"
    )
    String variable();
}
