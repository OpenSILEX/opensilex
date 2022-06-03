package org.opensilex.core.config;

import org.opensilex.config.ConfigDescription;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface SharedResourcesItem {

    @ConfigDescription("Shared resources uri")
    String uri();

    @ConfigDescription("Shared resources label")
    String label();

}
