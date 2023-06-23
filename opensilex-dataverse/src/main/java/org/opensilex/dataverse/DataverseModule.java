//******************************************************************************
//                          DataverseModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dataverse;

import org.apache.jena.riot.Lang;
import org.opensilex.OpenSilexModule;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;

import java.util.List;

/**
 * @author Gabriel Besombes
 * dataverse opensilex module implementation
 */
public class DataverseModule extends OpenSilexModule implements APIExtension, SPARQLExtension {

    private static final String ONTOLOGIES_DIRECTORY = "ontologies";

    @Override
    public Class<?> getConfigClass() {
        return OpensilexDataverseConfig.class;
    }

    @Override
    public String getConfigId() {
        return "dataverse";
    }

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeso-dataverse#",
                ONTOLOGIES_DIRECTORY + "/oeso-dataverse.owl",
                Lang.RDFXML,
                "vocabulary-dataverse"
        ));
        return list;
    }
}
