package org.opensilex.core.species.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Species",
        graph = "species"
)
public class SpeciesModel extends SPARQLResourceModel  {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    SPARQLLabel label;
    public static final String LABEL_FIELD = "label";


    public SPARQLLabel getLabel() {
        return label;
    }

    public void setLabel(SPARQLLabel label) {
        this.label = label;
    }
}