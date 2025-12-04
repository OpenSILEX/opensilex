//******************************************************************************
//                          GeneticResourceGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.geneticResource.dal;

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

import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;

import java.net.URI;
import java.util.List;

/**
 *
 * @author Alice Boizet
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "GeneticResource",
        graph = GeneticResourceModel.GRAPH,
        prefix = "geneticResource"
)
public class GeneticResourceModel extends SPARQLNamedResourceModel<GeneticResourceModel> implements ClassURIGenerator<GeneticResourceModel>{

    public static GeneticResourceModel fromSPARQLNamedResourceModel(SPARQLNamedResourceModel sparqlNamedResourceModel){
        GeneticResourceModel geneticResourceModel = new GeneticResourceModel();
        if(sparqlNamedResourceModel.getUri()!=null){
            geneticResourceModel.setUri(sparqlNamedResourceModel.getUri());
        }
        if(sparqlNamedResourceModel.getName()!=null){
            geneticResourceModel.setName(sparqlNamedResourceModel.getName());
        }
        if(sparqlNamedResourceModel.getType()!=null){
            geneticResourceModel.setType(sparqlNamedResourceModel.getType());
        }
        if(sparqlNamedResourceModel.getTypeLabel()!=null){
            geneticResourceModel.setTypeLabel(sparqlNamedResourceModel.getTypeLabel());
        }
        return geneticResourceModel;
    }

    public static final String GRAPH = "geneticResource";

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
    GeneticResourceModel species;
    public static final String SPECIES_URI_SPARQL_VAR = "species";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromVariety"
    )
    GeneticResourceModel variety;
    public static final String VARIETY_URI_SPARQL_VAR = "variety";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromAccession"
    )
    GeneticResourceModel accession;
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
            property = "hasParentGeneticResource"
    )
    protected List<GeneticResourceModel> parentGeneticResources;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasParentGeneticResourceM"
    )
    protected List<GeneticResourceModel> parentMGeneticResources;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasParentGeneticResourceF"
    )
    protected List<GeneticResourceModel> parentFGeneticResources;

    public static final String PARENT_VAR = "parentGeneticResource";
    public static final String PARENT_M_VAR = "parentMGeneticResource";
    public static final String PARENT_F_VAR = "parentFGeneticResource";
    
    @SPARQLProperty(
            ontology = FOAF.class,
            property = "homepage"
    )
    URI website;
    public static final String WEBSITE_VAR = "website";

    @SPARQLIgnore
    private MetaDataModel metadata;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPublic"
    )
    protected Boolean isPublic;
    public static final String IS_PUBLIC_FIELD = "isPublic";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasGroup"
    )
    List<GroupModel> groups;
    public static final String GROUP_USER_FIELD = "groups";

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

    public Boolean getIsPublic() {
        return isPublic;
    }
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;

    }

    public GeneticResourceModel getSpecies() {
        return species;
    }

    public void setSpecies(GeneticResourceModel species) {
        this.species = species;
    }

    public GeneticResourceModel getVariety() {
        return variety;
    }

    public void setVariety(GeneticResourceModel variety) {
        this.variety = variety;
    }

    public GeneticResourceModel getAccession() {
        return accession;
    }

    public void setAccession(GeneticResourceModel accession) {
        this.accession = accession;
    }


    public List<GroupModel> getGroups() {
        return this.groups;
    }
    public void setGroups(List<GroupModel> groups) {
        this.groups = groups;    }
    
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

    public List<GeneticResourceModel> getParentGeneticResources() {
        return parentGeneticResources;
    }

    public void setParentGeneticResources(List<GeneticResourceModel> parentGeneticResources) {
        this.parentGeneticResources = parentGeneticResources;
    }

    public List<GeneticResourceModel> getParentMGeneticResources() {
        return parentMGeneticResources;
    }

    public void setParentMGeneticResources(List<GeneticResourceModel> parentMGeneticResources) {
        this.parentMGeneticResources = parentMGeneticResources;
    }

    public List<GeneticResourceModel> getParentFGeneticResources() {
        return parentFGeneticResources;
    }

    public void setParentFGeneticResources(List<GeneticResourceModel> parentFGeneticResources) {
        this.parentFGeneticResources = parentFGeneticResources;
    }
    
    @Override
    public String[] getInstancePathSegments(GeneticResourceModel instance) {
        String geneticResourceType = "";
        if (instance.getType().getFragment() != null) {
            geneticResourceType = instance.getType().getFragment();
        } else {
            if (instance.getType().getSchemeSpecificPart() != null) {
                geneticResourceType = instance.getType().getSchemeSpecificPart();
            }
        }

        if (geneticResourceType.isEmpty()) {
            return new String[]{
                instance.getName()
            };
        } else {

            return new String[]{
                geneticResourceType, instance.getName()
            };
        }
    }

}
