//******************************************************************************
//                          MotivationModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.annotation.dal;

import org.apache.jena.vocabulary.OA;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

/**
 * @author Renaud COLIN
 * A simple model which define an instance of the {@link OA#Motivation} class
 */
@SPARQLResource(
        ontology = OA.class,
        resource = "Motivation",
        graph = "http://www.opensilex.org/vocabulary/oeso"
)
public class MotivationModel extends SPARQLNamedResourceModel<MotivationModel> {

}
