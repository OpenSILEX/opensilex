//******************************************************************************
//                           ProcessModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process;


import java.util.List;
import org.apache.jena.riot.Lang;
import org.opensilex.OpenSilexModule;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;

/**
 *
 * @author efernandez
 */
public class ProcessModule extends OpenSilexModule implements SPARQLExtension {

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                "http://opendata.inra.fr/PO2#",
                "ontologies/PO2.owl",
                Lang.RDFXML,
                "PO2"
        ));

        return list;
    }

    
}