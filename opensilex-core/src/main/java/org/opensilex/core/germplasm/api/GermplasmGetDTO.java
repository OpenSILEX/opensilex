//******************************************************************************
//                          GermplasmGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 * DTO representing JSON for searching germplasm or getting them by uri
 * @author Alice Boizet
 */
public class GermplasmGetDTO extends GermplasmSearchDTO {  
    /**
     * typeLabel
     */
    protected String typeLabel; 
    
    /**
     * speciesLabel
     */
    protected String speciesLabel;   
    
    /**
     * varietyLabel
     */
    protected String varietyLabel;   
    
    /**
     * accessionLabel
     */
    protected String accessionLabel;   
    
    /**
     * comment
     */
    protected String comment;  

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getSpeciesLabel() {
        return speciesLabel;
    }

    public void setSpeciesLabel(String speciesLabel) {
        this.speciesLabel = speciesLabel;
    }

    public String getVarietyLabel() {
        return varietyLabel;
    }

    public void setVarietyLabel(String varietyLabel) {
        this.varietyLabel = varietyLabel;
    }

    public String getAccessionLabel() {
        return accessionLabel;
    }

    public void setAccessionLabel(String accessionLabel) {
        this.accessionLabel = accessionLabel;
    }
    
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Convert Germplasm Model into Germplasm DTO
     *
     * @param model Germplasm Model to convert
     * @return Corresponding user DTO
     */
    public static GermplasmGetDTO fromModel(GermplasmModel model) {
        GermplasmGetDTO dto = new GermplasmGetDTO();

        dto.setUri(model.getUri());
        dto.setRdfType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setLabel(model.getLabel().getDefaultValue());
        
        if (model.getSpecies() != null) {
            dto.setFromSpecies(model.getSpecies().getUri());
            try {                
                dto.setSpeciesLabel(model.getSpecies().getLabel().getDefaultValue());
            } catch (Exception e){                
            }
        }
        
        if (model.getVariety() != null) {
            dto.setFromVariety(model.getVariety().getUri());
            try {                
                dto.setVarietyLabel(model.getVariety().getLabel().getDefaultValue());
            } catch (Exception e){                
            }
            
        }
        
        if (model.getAccession() != null) {
            dto.setFromAccession(model.getAccession().getUri());
            try {                
                dto.setAccessionLabel(model.getAccession().getLabel().getDefaultValue());
            } catch (Exception e){                
            }
           
        }         
        
        if (model.getComment() != null) {
            dto.setComment(model.getComment());
        }
        
        if (model.getInstitute() != null) {
            dto.setInstitute(model.getInstitute());
        }
        
        if (model.getProductionYear() != null) {
            dto.setProductionYear(model.getProductionYear());
        }  

        return dto;
    }

}
