package org.opensilex.faidare.model;

import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.util.List;

public class Faidarev1GermplasmModel extends SPARQLNamedResourceModel<Faidarev1GermplasmModel> implements ClassURIGenerator<Faidarev1GermplasmModel> {

    public static final String GRAPH = "germplasm";

    protected String name;

    protected SPARQLLabel label;

    protected String code;

    protected URI species;

    protected URI variety;

    protected String varietyName;

    protected String institute;

    protected URI website;

    protected List<URI> experiments;

    public SPARQLLabel getLabel() {
        return label;
    }

    public Faidarev1GermplasmModel setLabel(SPARQLLabel label) {
        this.label = label;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Faidarev1GermplasmModel setCode(String code) {
        this.code = code;
        return this;
    }

    public URI getSpecies() {
        return species;
    }

    public Faidarev1GermplasmModel setSpecies(URI species) {
        this.species = species;
        return this;
    }

    public URI getVariety() {
        return variety;
    }

    public Faidarev1GermplasmModel setVariety(URI variety) {
        this.variety = variety;
        return this;
    }

    public String getVarietyName() {
        return varietyName;
    }

    public Faidarev1GermplasmModel setVarietyName(String varietyName) {
        this.varietyName = varietyName;
        return this;
    }

    public String getInstitute() {
        return institute;
    }

    public Faidarev1GermplasmModel setInstitute(String institute) {
        this.institute = institute;
        return this;
    }

    public URI getWebsite() {
        return website;
    }

    public Faidarev1GermplasmModel setWebsite(URI website) {
        this.website = website;
        return this;
    }

    public List<URI> getExperiments() {
        return experiments;
    }

    public Faidarev1GermplasmModel setExperiments(List<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    @Override
    public String getName() {
        return getLabel().getDefaultValue();
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        if(label == null){
            label = new SPARQLLabel();
        }
        label.setDefaultValue(name);
    }

    public String[] getInstancePathSegments(GermplasmModel instance) {
        String germplasmType = "";
        if (instance.getType().getFragment() != null) {
            germplasmType = instance.getType().getFragment();
        } else {
            if (instance.getType().getSchemeSpecificPart() != null) {
                germplasmType = instance.getType().getSchemeSpecificPart();
            }
        }

        if (germplasmType.isEmpty()) {
            return new String[]{
                    instance.getName()
            };
        } else {

            return new String[]{
                    germplasmType, instance.getName()
            };
        }
    }
}
