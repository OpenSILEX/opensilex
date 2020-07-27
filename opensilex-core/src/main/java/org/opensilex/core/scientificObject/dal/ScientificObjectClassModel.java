/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import org.apache.jena.vocabulary.OWL2;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "Class",
        ignoreValidation = true
)
public class ScientificObjectClassModel extends ClassModel<ScientificObjectClassModel> {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isScientificObjectManagedClass"
    )
    protected Boolean isScientificObjectManagedClass;


    public Boolean getIsScientificObjectManagedClass() {
        return isScientificObjectManagedClass;
    }

    public void setIsScientificObjectManagedClass(Boolean isScientificObjectManagedClass) {
        this.isScientificObjectManagedClass = isScientificObjectManagedClass;
    }
}
