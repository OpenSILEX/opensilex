/*
 * *****************************************************************************
 *                         YvanModule.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 26/06/2026 17:09
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan;

import org.apache.jena.riot.Lang;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opensilex.OpenSilexModule;
import org.opensilex.core.scientificObject.bll.ScientificObjectLogicExtendedRules;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.yvan.ontology.YvanOntology;
import org.opensilex.yvan.spidermutagen.bll.SpiderMutagenLogicExtendedRules;

import java.util.List;

/**
 * opensilex-yvan opensilex module implementation
 */
public class YvanModule extends OpenSilexModule implements APIExtension, SPARQLExtension {
    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                YvanOntology.NS,
                "ontologies/yvan.owl",
                Lang.RDFXML,
                YvanOntology.PREFIX,
                null,
                true
        ));

        return list;
    }

    @Override
    public void bindServices(AbstractBinder binder) {
        binder.bind(SpiderMutagenLogicExtendedRules.class).to(ScientificObjectLogicExtendedRules.class);
    }
}
