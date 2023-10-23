package org.opensilex.front;

import org.opensilex.config.ConfigDescription;
import org.opensilex.front.config.OntologyPortalItem;

import java.util.List;

public interface AgroportalOntologiesConfig {

    @ConfigDescription(
            value = "Ontology portals"
    )
    List<OntologyPortalItem> ontologyPortals();

    @ConfigDescription(
            value = "Ontologies for entities"
    )
    List<String> entity();

    @ConfigDescription(
            value = "Ontologies for traits"
    )
    List<String> trait();

}

