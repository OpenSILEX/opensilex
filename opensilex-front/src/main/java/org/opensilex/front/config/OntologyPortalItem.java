package org.opensilex.front.config;

import org.opensilex.config.ConfigDescription;


public interface OntologyPortalItem {

    @ConfigDescription(
            value = "name"
    )
    String name();

    @ConfigDescription(
            value = "url"
    )
    String url();
}
