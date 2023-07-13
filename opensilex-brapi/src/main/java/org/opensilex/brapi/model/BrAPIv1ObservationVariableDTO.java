//******************************************************************************
//                          BrAPIv1ObservationVariableDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.util.ArrayList;
import org.opensilex.core.variable.dal.VariableModel;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1ObservationVariableDTO {
    private String observationVariableDbId;
    private String observationVariableName;
    private String ontologyReference;
    private ArrayList<String> synonyms;
    private ArrayList<String> contextOfUse;
    private String growthStage;
    private String status;
    private String xref;
    private String institution;
    private String scientist;
    private String submissionTimesTamp;
    private String language;
    private String crop;
    private BrAPIv1TraitDTO trait;
    private BrAPIv1MethodDTO method;
    private BrAPIv1ScaleDTO scale;
    private String defaultValue;    
    private String documentationURL;

    public String getObservationVariableDbId() {
        return observationVariableDbId;
    }

    public void setObservationVariableDbId(String observationVariableDbId) {
        this.observationVariableDbId = observationVariableDbId;
    }

    public String getObservationVariableName() {
        return observationVariableName;
    }

    public void setObservationVariableName(String observationVariableName) {
        this.observationVariableName = observationVariableName;
    }

    public String getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(String ontologyReference) {
        this.ontologyReference = ontologyReference;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public ArrayList<String> getContextOfUse() {
        return contextOfUse;
    }

    public void setContextOfUse(ArrayList<String> contextOfUse) {
        this.contextOfUse = contextOfUse;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getXref() {
        return xref;
    }

    public void setXref(String xref) {
        this.xref = xref;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getScientist() {
        return scientist;
    }

    public void setScientist(String scientist) {
        this.scientist = scientist;
    }

    public String getSubmissionTimesTamp() {
        return submissionTimesTamp;
    }

    public void setSubmissionTimesTamp(String submissionTimesTamp) {
        this.submissionTimesTamp = submissionTimesTamp;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public BrAPIv1TraitDTO getTrait() {
        return trait;
    }

    public void setTrait(BrAPIv1TraitDTO trait) {
        this.trait = trait;
    }

    public BrAPIv1MethodDTO getMethod() {
        return method;
    }

    public void setMethod(BrAPIv1MethodDTO method) {
        this.method = method;
    }

    public BrAPIv1ScaleDTO getScale() {
        return scale;
    }

    public void setScale(BrAPIv1ScaleDTO scale) {
        this.scale = scale;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
    }
    
    public static BrAPIv1ObservationVariableDTO fromModel(VariableModel model) {
        BrAPIv1ObservationVariableDTO variable = new BrAPIv1ObservationVariableDTO();
        if (model.getUri() != null) {
            variable.setObservationVariableDbId(model.getUri().toString());
        }
        variable.setObservationVariableName(model.getName());
        
        BrAPIv1TraitDTO trait = new BrAPIv1TraitDTO();
        if (model.getTraitName() != null) {
            trait.setName(model.getTraitName());
        } else {
            String traitName = "";
            if (model.getEntity() != null) {
                traitName = traitName + model.getEntity().getName() + "_";
            }
            if (model.getCharacteristic() != null) {
                traitName = traitName + model.getCharacteristic().getName();
            }
            trait.setName(traitName);
        }
        
        if (model.getTraitUri() != null) {
          trait.setTraitDbId(model.getTraitUri().toString());  
        }        
        variable.setTrait(trait);
        
        BrAPIv1MethodDTO method = new BrAPIv1MethodDTO();
        if (model.getMethod() != null) {
            method.setMethodName(model.getMethod().getName());
            if (model.getMethod().getUri() != null) {
                method.setMethodDbId(model.getMethod().getUri().toString());
            }   
        }
        variable.setMethod(method);
        
        BrAPIv1ScaleDTO scale = new BrAPIv1ScaleDTO();
        if (model.getUnit() != null) {
            scale.setScaleName(model.getUnit().getName());
            if (model.getUnit().getUri() != null) {
                scale.setScaleDbId(model.getUnit().getUri().toString());
            }
        }        
        variable.setScale(scale);

        return variable;
    }
}
