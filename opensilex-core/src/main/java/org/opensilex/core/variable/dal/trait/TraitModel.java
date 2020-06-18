package org.opensilex.core.variable.dal.trait;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Trait",
        graph = "variable"
)
public class TraitModel extends SPARQLResourceModel implements ClassURIGenerator<TraitModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    private String label;
    public static final String LABEL_FIELD = "label";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "subClassOf"
    )
    private URI traitClass;

    public String getLabel() { return label; }

    public void setLabel(String label) { this.label = label; }

    public URI getTraitClass() { return traitClass; }

    public void setTraitClass(URI traitClass) { this.traitClass = traitClass; }

    @Override
    public String[] getUriSegments(TraitModel instance) {
        return new String[]{
                "variable",
                "trait",
                instance.getLabel()
        };
    }
}
