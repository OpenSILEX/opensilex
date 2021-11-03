//******************************************************************************
//                          GermplasmGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.InterestEntityModel;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author Alice Boizet
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Germplasm",
        graph = "set/germplasms",
        prefix = "germplasm"
)
public class GermplasmModel extends SPARQLNamedResourceModel<GermplasmModel> implements ClassURIGenerator<GermplasmModel> {

    @SPARQLIgnore
    protected String name;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    protected SPARQLLabel label;
    public static final String LABEL_FIELD = "label";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasId"
    )

    String code;
    public static final String CODE_VAR = "code";

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromSpecies"
    )
    GermplasmModel species;
    public static final String SPECIES_URI_SPARQL_VAR = "species";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromVariety"
    )
    GermplasmModel variety;
    public static final String VARIETY_URI_SPARQL_VAR = "variety";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromAccession"
    )
    GermplasmModel accession;
    public static final String ACCESSION_URI_SPARQL_VAR = "accession";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String comment;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromInstitute"
    )
    String institute;
    public static final String INSTITUTE_SPARQL_VAR = "institute";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasProductionYear"
    )
    Integer productionYear;
    public static final String PRODUCTION_YEAR_SPARQL_VAR = "productionYear";

    Map<String, String> attributes;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "altLabel"
    )
    List<String> synonyms;
    public static final String SYNONYM_VAR = "synonym";
    
    @SPARQLProperty(
            ontology = FOAF.class,
            property = "homepage"
    )
    URI website;
    public static final String WEBSITE_VAR = "website";

    public SPARQLLabel getLabel() {
        return label;
    }

    public void setLabel(SPARQLLabel label) {
        this.label = label;
    }

    public String getName() {
        return getLabel().getDefaultValue();
    }

    public void setName(String name) {
        super.setName(name);
    }

    public GermplasmModel getSpecies() {
        return species;
    }

    public void setSpecies(GermplasmModel species) {
        this.species = species;
    }

    public GermplasmModel getVariety() {
        return variety;
    }

    public void setVariety(GermplasmModel variety) {
        this.variety = variety;
    }

    public GermplasmModel getAccession() {
        return accession;
    }

    public void setAccession(GermplasmModel accession) {
        this.accession = accession;
    }
    
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    public Integer getProductionYear() {
        return productionYear;
    }

    public void setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public URI getWebsite() {
        return website;
    }

    public void setWebsite(URI website) {
        this.website = website;
    }
    
    @Override
    public String[] getUriSegments(GermplasmModel instance) {
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
