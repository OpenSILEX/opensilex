/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import java.util.List;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 *
 * @author vmigot
 */
public class ScientificObjectModel extends SPARQLTreeModel<ScientificObjectModel> {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf"
    )
    protected ScientificObjectModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected List<ScientificObjectModel> children;
}
