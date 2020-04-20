/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.document.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Document",
        graph = "set/document",
        prefix = "doc"
)
public class DocumentModel extends SPARQLResourceModel implements ClassURIGenerator<DocumentModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    String label;
    public static final String LABEL_VAR = "label";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    
    
    @Override
    public String[] getUriSegments(DocumentModel instance) {
        return new String[]{
                instance.getLabel()
        };
    }
    
}
