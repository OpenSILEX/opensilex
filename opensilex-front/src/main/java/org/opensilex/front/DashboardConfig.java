package org.opensilex.front;

import org.opensilex.config.ConfigDescription;

public interface DashboardConfig {
    @ConfigDescription(
            value = "Variable for the graphic component"
    )
    GraphConfig graph1();

    @ConfigDescription(
            value = "Variable for the second graphic component"
    )
    GraphConfig graph2();

    @ConfigDescription(
            value = "Variable for the third graphic component"
    )
    GraphConfig graph3();
}

