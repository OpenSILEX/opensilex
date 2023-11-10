//******************************************************************************
//                          Faidarev1ObservationVariableDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.variable.dal.VariableModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1ObservationVariableDTO {

    private ArrayList<String> contextOfUse;
    private String crop;
    private String defaultValue;
    private String documentationURL;
    private String growthStage;
    private String institution;
    private String language; // This will change once the multilabel development is done
    private Faidarev1MethodDTO method;
    private Faidarev1ScaleDTO scale;
    private String scientist;
    private String status;
    private ArrayList<String> synonyms;
    private Faidarev1TraitDTO trait;
    private String xref;
    private String observationVariableDbId;
    private String observationVariableName;
    private String ontologyDbId;
    private String ontologyName;

    public String getOntologyDbId() {
        return ontologyDbId;
    }

    public void setOntologyDbId(String ontologyDbId) {
        this.ontologyDbId = ontologyDbId;
    }

    public String getOntologyName() {
        return ontologyName;
    }

    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
    }

    public ArrayList<String> getContextOfUse() {
        return contextOfUse;
    }

    public void setContextOfUse(ArrayList<String> contextOfUse) {
        this.contextOfUse = contextOfUse;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
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

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Faidarev1MethodDTO getMethod() {
        return method;
    }

    public void setMethod(Faidarev1MethodDTO method) {
        this.method = method;
    }

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

    public Faidarev1ScaleDTO getScale() {
        return scale;
    }

    public void setScale(Faidarev1ScaleDTO scale) {
        this.scale = scale;
    }

    public String getScientist() {
        return scientist;
    }

    public void setScientist(String scientist) {
        this.scientist = scientist;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public Faidarev1TraitDTO getTrait() {
        return trait;
    }

    public void setTrait(Faidarev1TraitDTO trait) {
        this.trait = trait;
    }

    public String getXref() {
        return xref;
    }

    public void setXref(String xref) {
        this.xref = xref;
    }

    public static Faidarev1ObservationVariableDTO fromModel(VariableModel variableModel) {

        Faidarev1ObservationVariableDTO variable = new Faidarev1ObservationVariableDTO();
        if (variableModel.getUri() != null) {
            variable.setObservationVariableDbId(variableModel.getUri().toString());
        }
        variable.setObservationVariableName(variableModel.getName());
        
        Faidarev1TraitDTO trait = new Faidarev1TraitDTO();
        if (variableModel.getTraitName() != null) {
            trait.setName(variableModel.getTraitName());
        } else {
            String traitName = "";
            if (variableModel.getEntity() != null) {
                traitName = traitName + variableModel.getEntity().getName() + "_";
            }
            if (variableModel.getCharacteristic() != null) {
                traitName = traitName + variableModel.getCharacteristic().getName();
            }
            trait.setName(traitName);
        }
        
        if (variableModel.getTraitUri() != null) {
          trait.setTraitDbId(variableModel.getTraitUri().toString());
        }        
        variable.setTrait(trait);
        
        Faidarev1MethodDTO method = new Faidarev1MethodDTO();
        if (variableModel.getMethod() != null) {
            method.setMethodName(variableModel.getMethod().getName());
            if (variableModel.getMethod().getUri() != null) {
                method.setMethodDbId(variableModel.getMethod().getUri().toString());
            }   
        }
        variable.setMethod(method);
        
        Faidarev1ScaleDTO scale = new Faidarev1ScaleDTO();
        if (variableModel.getUnit() != null) {
            scale.setScaleName(variableModel.getUnit().getName());
            if (variableModel.getUnit().getUri() != null) {
                scale.setScaleDbId(variableModel.getUnit().getUri().toString());
            }
        }        
        variable.setScale(scale);

        if (variableModel.getSpecies() != null && variableModel.getSpecies().size() == 1) {
            variable.setCrop(variableModel.getSpecies().get(0).toString());
        }

        if (variableModel.getAlternativeName() != null) {
            variable.setSynonyms(new ArrayList<>(Collections.singletonList(variableModel.getAlternativeName())));
        }

        if (!CollectionUtils.isEmpty(variableModel.getExactMatch())) {
            variable.setXref(variableModel.getExactMatch().get(0).toString());
        }

        if (variableModel.getMethod() != null){
            variable.setMethod(Faidarev1MethodDTO.fromModel(variableModel.getMethod()));
        }

        if (variableModel.getTraitUri() != null){
            Faidarev1TraitDTO variableTrait = new Faidarev1TraitDTO();
            variableTrait.setTraitDbId(variableModel.getTraitUri().toString());
            if (variableModel.getTraitName() != null) {
                variableTrait.setName(variableModel.getTraitName());
            }
            variable.setTrait(variableTrait);
        }

        if (variableModel.getUnit() != null){
            Faidarev1ScaleDTO variableScale = Faidarev1ScaleDTO.fromModel(variableModel.getUnit());

            String dataTypeUri = variableModel.getDataType().toString();
            if (Objects.equals(dataTypeUri, "xsd:decimal") | Objects.equals(dataTypeUri, "xsd:integer")){
                variableScale.setDataType("Numerical");
            }
            if (Objects.equals(dataTypeUri, "xsd:date") | Objects.equals(dataTypeUri, "xsd:dateTime")){
                variableScale.setDataType("Date");
            }
            if (Objects.equals(dataTypeUri, "xsd:string")){
                variableScale.setDataType("Text");
            }
            if (Objects.equals(dataTypeUri, "xsd:boolean")){
                variableScale.setDataType("Nominal");
            }

            variable.setScale(variableScale);
        }

        return variable;
    }
}
