//******************************************************************************
//                          GeneticResourceGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.geneticResource.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO representing JSON for posting geneticResource
 * @author Alice Boizet
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "name", "synonyms", "code", "production_year",
    "description", "species","variety", "accession","institute", "website", "relations", "metadata","is_public","groups"}) // "variety", "accession",
public class GeneticResourceCreationDTO extends RDFObjectDTO {

    
    /**
     * GeneticResource label
     */
    @NotNull
    @ApiModelProperty(value = "GeneticResource name", example = "SL_001", required = true)
    protected String name;
    
    /**
     * GeneticResource id (accessionNumber, varietyCode...)
     */
    @ApiModelProperty(value = "GeneticResource code (accessionNumber, varietyCode...)", example = "")
    protected String code;
    
    /**
     * GeneticResource species URI
     */
    @ValidURI
    @ApiModelProperty(value = "species URI", example = "http://opensilex.dev/opensilex/id/species#zeamays")
    protected URI species;
    
    /**
     * GeneticResource Variety URI
     */
    @ValidURI
    @ApiModelProperty(value = "variety URI", example = "http://opensilex.dev/opensilex/id/variety#B73")
    protected URI variety;
    
    /**
     * GeneticResource Accession URI
     */    
    @ValidURI
    @ApiModelProperty(value = "accession URI", example = "http://opensilex.dev/opensilex/id/accession#B73_INRA")
    protected URI accession;

    /**
     * institute where the accession has been created
     */
    @ApiModelProperty(value = "institute", example = "INRA")
    protected String institute;

    /**
     * productionYear
     */
    @ApiModelProperty(value = "production year", example = "2015")
    @JsonProperty("production_year")
    protected Integer productionYear;
        
    /**
     * comment
     */
    @ApiModelProperty(value = "comment")
    protected String description;  
    
    /**
     * website
     */
    @ValidURI
    @ApiModelProperty(value = "website")
    protected URI website;

    @Override
    @ValidURI
    @ApiModelProperty(value = "GeneticResource URI", example = "http://opensilex.dev/opensilex/id/plantMaterialLot#SL_001")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    @ValidURI
    @NotNull
    @ApiModelProperty(value = "GeneticResource type", example = "http://www.opensilex.org/vocabulary/oeso#SeedLot", required = true)
    public URI getType() {
        return type;
    }

    @JsonProperty("is_public")
    @NotNull
    @ApiModelProperty(value = "boolean", example = "True", required = true)
    protected Boolean isPublic;

    @JsonProperty("groups")
    @ApiModelProperty(value = "groups", example = "")
    protected List<URI> groups = new ArrayList<>();




    public void setRdfType(URI rdfType) {
        super.setType(rdfType);
    }

    public String getName() {
        return name;
    }

    public void setName(String label) {
        this.name = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public URI getSpecies() {
        return species;
    }

    public void setSpecies(URI species) {
        this.species = species;
    }

    public URI getVariety() {
        return variety;
    }

    public void setVariety(URI variety) {
        this.variety = variety;
    }

    public URI getAccession() {
        return accession;
    }

    public void setAccession(URI accession) {
        this.accession = accession;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    protected List<String> synonyms;

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    protected Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }  

    public URI getWebsite() {
        return website;
    }

    public void setWebsite(URI website) {
        this.website = website;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }
    public void setIsPublic(boolean isPublic) { this.isPublic = isPublic; }

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

    /**
     * WARNING: this method validate the properties of the GeneticResourceCreationDTO, that should be done in the business layer, but this would require refactoring.
     */
    public GeneticResourceModel newModel(SPARQLService sparql, String lang, ClassModel classModel) throws SPARQLException, URISyntaxException, InvalidValueException {
        GeneticResourceModel model = newModelWithoutRelations();

        if (isPublic != null) {
            model.setIsPublic(isPublic);
        }

        if(relations != null){
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            if (classModel == null) {
                classModel = ontologyDAO.getClassModel(type, new URI(Oeso.GeneticResource.getURI()), lang);
            }
            RDFObjectDTO.validatePropertiesAndAddToObject(sparql.getDefaultGraphURI(GeneticResourceModel.class), classModel, model, relations, ontologyDAO);
        }

        return model;
    }

    /**
     * Create a new GeneticResourceModel and ignore relations (like geneticResource parents)
     */
    public GeneticResourceModel newModelWithoutRelations(){
        GeneticResourceModel model = new GeneticResourceModel();

        if (uri != null) {
            model.setUri(uri);
        }

        if (name != null) {
            model.setLabel(new SPARQLLabel(name, ""));
        }
        if (type != null) {
            model.setType(type);
        }

        if (species != null) {
            GeneticResourceModel speciesModel = new GeneticResourceModel();
            speciesModel.setUri(this.species);
            model.setSpecies(speciesModel);
        }
        if (variety != null) {
            GeneticResourceModel varietyModel = new GeneticResourceModel();
            varietyModel.setUri(this.variety);
            model.setVariety(varietyModel);
        }
        if (accession != null) {
            GeneticResourceModel accessionModel = new GeneticResourceModel();
            accessionModel.setUri(this.accession);
            model.setAccession(accessionModel);
        }

        if (institute != null) {
            model.setInstitute(institute);
        }

        if (productionYear != null) {
            model.setProductionYear(productionYear);
        }

        if (description != null) {
            model.setComment(description);
        }

        if (metadata != null ) {
            model.setMetadata(new MetaDataModel());
            model.getMetadata().setAttributes(metadata);
        }

        if (synonyms != null) {
            List<String> synonymsList = new ArrayList<>(synonyms.size());
            synonymsList.addAll(synonyms);
            model.setSynonyms(synonymsList);
        }

        if (code != null) {
            model.setCode(code);
        }

        if (website != null) {
            model.setWebsite(website);
        }
        List<GroupModel> groupList = new ArrayList<>(groups.size());
        groups.forEach((URI u) -> {
            GroupModel group = new GroupModel();
            group.setUri(u);
            groupList.add(group);
        });
        model.setGroups(groupList);

        return model;
    }
}
