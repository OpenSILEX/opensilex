package org.opensilex.core.species;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Species",
        graph = "set/species"
)
public class SpeciesModel extends SPARQLResourceModel implements ClassURIGenerator<SpeciesModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    String label;
    public static final String LABEL_FIELD = "label";

    @Override
    public String[] getUriSegments(SpeciesModel instance) {
        return new String[]{
                instance.getLabel()
        };
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}