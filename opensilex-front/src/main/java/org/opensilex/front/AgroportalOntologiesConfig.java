package org.opensilex.front;

import org.opensilex.config.ConfigDescription;

import java.util.List;

public interface AgroportalOntologiesConfig {
    @ConfigDescription(
            value = "Ontologies for entities"
    )
    List<String> entity();

    @ConfigDescription(
            value = "Ontologies for traits"
    )
    List<String> trait();

    @ConfigDescription(
            value = "Ontologies for methods"
    )
    List<String> method();

    @ConfigDescription(
            value = "Ontologies for units"
    )
    List<String> unit();
}

