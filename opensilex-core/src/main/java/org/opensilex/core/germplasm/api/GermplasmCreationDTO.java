//******************************************************************************
//                          GermplasmGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 * DTO representing JSON for posting germplasm
 * @author Alice Boizet
 */
@ApiModel
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
    protected URI rdfType;
    
    /**
     * Germplasm label
     */
    @Required
    @ApiModelProperty(value = "Germplasm label", example = "SL_001", required = true)
    protected String label;
    
    /**
     * Germplasm id (accessionNumber, varietyCode...)
     */
    @ApiModelProperty(value = "Germplasm code (accessionNumber, varietyCode...)", example = "", required = true)
    protected String code;
    
    /**
     * Germplasm species URI
     */
    @ValidURI
    @ApiModelProperty(value = "species URI", example = "http://opensilex.dev/opensilex/id/species#zeamays")
    protected URI fromSpecies;
    
    /**
     * Germplasm Variety URI
     */
    @ValidURI
    @ApiModelProperty(value = "variety URI", example = "http://opensilex.dev/opensilex/id/variety#B73")
    protected URI fromVariety;
    
    /**
     * Germplasm Accession URI
     */    
    @ValidURI
    @ApiModelProperty(value = "accession URI", example = "http://opensilex.dev/opensilex/id/accession#B73_INRA")
    protected URI fromAccession;

    /**
     * institute where the accession has been created
     */
    @ApiModelProperty(value = "institute", example = "INRA")
    protected String institute;
    
    /**
     * productionYear
     */
    @ApiModelProperty(value = "production year", example = "2015")
    protected Integer productionYear;
        
    /**
     * comment
     */
    @ApiModelProperty(value = "comment")
    protected String comment;    

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public URI getFromSpecies() {
        return fromSpecies;
    }

    public void setFromSpecies(URI fromSpecies) {
        this.fromSpecies = fromSpecies;
    }

    public URI getFromVariety() {
        return fromVariety;
    }

    public void setFromVariety(URI fromVariety) {
        this.fromVariety = fromVariety;
    }

    public URI getFromAccession() {
        return fromAccession;
    }

    public void setFromAccession(URI fromAccession) {
        this.fromAccession = fromAccession;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    
    protected List<String> synonyms;

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    protected Map<String, String> attributes;

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }  
    
    public GermplasmModel newModel(String lang) {
        GermplasmModel model = new GermplasmModel();
        
        if (uri != null) {
            model.setUri(uri);
        }
        if (label != null) {
            model.setLabel(new SPARQLLabel(label, lang));
        }
        if (rdfType != null) {
            model.setType(rdfType);
        }
        
        if (fromSpecies != null) {
            GermplasmModel species = new GermplasmModel();
            species.setUri(fromSpecies);
            model.setSpecies(species);
        }
        if (fromVariety != null) {
            GermplasmModel variety = new GermplasmModel();
            variety.setUri(fromVariety);
            model.setVariety(variety);
        }
        if (fromAccession != null) {
            GermplasmModel accession = new GermplasmModel();
            accession.setUri(fromAccession);
            model.setAccession(accession);
        }
        
        if (institute != null) {
            model.setInstitute(institute);
        }
        
        if (productionYear != null) {
            model.setProductionYear(productionYear);
        }
        
        if (comment != null) {
            model.setComment(comment);
        }        
        
        if (attributes != null ) {
           model.setAttributes(attributes);
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

        return model;
    }   

}
