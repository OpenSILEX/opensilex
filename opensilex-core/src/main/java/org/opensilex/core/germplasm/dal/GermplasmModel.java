//******************************************************************************
//                          GermplasmGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.util.List;

/**
 *
 * @author Alice Boizet
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Germplasm",
        graph = GermplasmModel.GRAPH,
        prefix = "germplasm"
)
public class GermplasmModel extends SPARQLNamedResourceModel<GermplasmModel> implements ClassURIGenerator<GermplasmModel>{

    public static GermplasmModel fromSPARQLNamedResourceModel(SPARQLNamedResourceModel sparqlNamedResourceModel){
        GermplasmModel germplasmModel = new GermplasmModel();
        if(sparqlNamedResourceModel.getUri()!=null){
            germplasmModel.setUri(sparqlNamedResourceModel.getUri());
        }
        if(sparqlNamedResourceModel.getName()!=null){
            germplasmModel.setName(sparqlNamedResourceModel.getName());
        }
        if(sparqlNamedResourceModel.getType()!=null){
            germplasmModel.setType(sparqlNamedResourceModel.getType());
        }
        if(sparqlNamedResourceModel.getTypeLabel()!=null){
            germplasmModel.setTypeLabel(sparqlNamedResourceModel.getTypeLabel());
        }
        return germplasmModel;
    }

    public static final String GRAPH = "germplasm";

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


    @SPARQLProperty(
            ontology = SKOS.class,
            property = "altLabel"
    )
    List<String> synonyms;
    public static final String SYNONYM_VAR = "synonyms";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasParentGermplasm"
    )
    protected List<GermplasmModel> parentGermplasms;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasParentGermplasmM"
    )
    protected List<GermplasmModel> parentMGermplasms;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasParentGermplasmF"
    )
    protected List<GermplasmModel> parentFGermplasms;

    public static final String PARENT_VAR = "parentGermplasm";
    public static final String PARENT_M_VAR = "parentMGermplasm";
    public static final String PARENT_F_VAR = "parentFGermplasm";
    
    @SPARQLProperty(
            ontology = FOAF.class,
            property = "homepage"
    )
    URI website;
    public static final String WEBSITE_VAR = "website";

    @SPARQLIgnore
    private MetaDataModel metadata;

    public SPARQLLabel getLabel() {
        return label;
    }

    public void setLabel(SPARQLLabel label) {
        this.label = label;
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


    public MetaDataModel getMetadata() {
        return metadata;
    }

    public void setMetadata(MetaDataModel metadata) {
        this.metadata = metadata;
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

    public List<GermplasmModel> getParentGermplasms() {
        return parentGermplasms;
    }

    public void setParentGermplasms(List<GermplasmModel> parentGermplasms) {
        this.parentGermplasms = parentGermplasms;
    }

    public List<GermplasmModel> getParentMGermplasms() {
        return parentMGermplasms;
    }

    public void setParentMGermplasms(List<GermplasmModel> parentMGermplasms) {
        this.parentMGermplasms = parentMGermplasms;
    }

    public List<GermplasmModel> getParentFGermplasms() {
        return parentFGermplasms;
    }

    public void setParentFGermplasms(List<GermplasmModel> parentFGermplasms) {
        this.parentFGermplasms = parentFGermplasms;
    }
    
    @Override
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
