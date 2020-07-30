/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.annotations.SPARQLResource;

/**
 *
 * @author vmigot
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "ObjectProperty",
        ignoreValidation = true
)
public class ObjectPropertyModel extends PropertyModel  {
    
}
