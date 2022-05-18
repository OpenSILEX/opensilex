//******************************************************************************
//                          GermplasmGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 * DTO representing JSON for posting germplasm
 * @author Alice Boizet
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "name", "synonyms", "code", "production_year",
    "description", "species", "variety", "accession", "institute", "website", "metadata"})
public class GermplasmCreationDTO {
    
    /**
     * Germplasm URI
     */
    @ValidURI
    @ApiModelProperty(value = "Germplasm URI", example = "http://opensilex.dev/opensilex/id/plantMaterialLot#SL_001")
    protected URI uri;
    
    /**
     * Germplasm Type : Species, Variety, Accession or subclass of PlantMaterialLot
     */
    @NotNull
    @ApiModelProperty(value = "rdfType URI", example = "http://www.opensilex.org/vocabulary/oeso#SeedLot")
    @JsonProperty("rdf_type")
    protected URI rdfType;
    
    /**
     * Germplasm label
     */
    @NotNull
    @ApiModelProperty(value = "Germplasm name", example = "SL_001", required = true)
    protected String name;
    
    /**
     * Germplasm id (accessionNumber, varietyCode...)
     */
    @ApiModelProperty(value = "Germplasm code (accessionNumber, varietyCode...)", example = "")
    protected String code;
    
    /**
     * Germplasm species URI
     */
    @ValidURI
    @ApiModelProperty(value = "species URI", example = "http://opensilex.dev/opensilex/id/species#zeamays")
    protected URI species;
    
    /**
     * Germplasm Variety URI
     */
    @ValidURI
    @ApiModelProperty(value = "variety URI", example = "http://opensilex.dev/opensilex/id/variety#B73")
    protected URI variety;
    
    /**
     * Germplasm Accession URI
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

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
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
    
    public GermplasmModel newModel() {
        GermplasmModel model = new GermplasmModel();
        
        if (uri != null) {
            model.setUri(uri);
        }
        if (name != null) {
            model.setLabel(new SPARQLLabel(name, ""));
        }
        if (rdfType != null) {
            model.setType(rdfType);
        }
        
        if (species != null) {
            GermplasmModel speciesModel = new GermplasmModel();
            speciesModel.setUri(this.species);
            model.setSpecies(speciesModel);
        }
        if (variety != null) {
            GermplasmModel varietyModel = new GermplasmModel();
            varietyModel.setUri(this.variety);
            model.setVariety(varietyModel);
        }
        if (accession != null) {
            GermplasmModel accessionModel = new GermplasmModel();
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
            synonyms.forEach((String synonym) -> {
                synonymsList.add(synonym);
            });
            model.setSynonyms(synonymsList);
        }        
        
        if (code != null) {
            model.setCode(code);
        }
        
        if (website != null) {
            model.setWebsite(website);                    
        }

        return model;
    }   

}
