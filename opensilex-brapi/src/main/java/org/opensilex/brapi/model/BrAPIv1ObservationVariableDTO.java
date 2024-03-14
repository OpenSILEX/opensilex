//******************************************************************************
//                          BrAPIv1ObservationVariableDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1ObservationVariableDTO {

    private ArrayList<String> contextOfUse;
    private String crop;
    private String defaultValue;
    private String documentationURL;
    private String growthStage;
    private String institution;
    private String language; // This will change once the multilabel development is done
    private BrAPIv1MethodDTO method;
    private BrAPIv1OntologyReferenceDTO ontologyReference;
    private BrAPIv1ScaleDTO scale;
    private String scientist;
    private String status;
    private String submissionTimestamp;
    private ArrayList<String> synonyms;
    private BrAPIv1TraitDTO trait;
    private String xref;
    private String observationVariableDbId;
    private String observationVariableName;
    private String studyDbId;
    private String trialName;

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

    public BrAPIv1MethodDTO getMethod() {
        return method;
    }

    public void setMethod(BrAPIv1MethodDTO method) {
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

    public BrAPIv1OntologyReferenceDTO getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(BrAPIv1OntologyReferenceDTO ontologyReference) {
        this.ontologyReference = ontologyReference;
    }

    public BrAPIv1ScaleDTO getScale() {
        return scale;
    }

    public void setScale(BrAPIv1ScaleDTO scale) {
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

    public String getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    public void setSubmissionTimestamp(String submissionTimestamp) {
        this.submissionTimestamp = submissionTimestamp;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public BrAPIv1TraitDTO getTrait() {
        return trait;
    }

    public void setTrait(BrAPIv1TraitDTO trait) {
        this.trait = trait;
    }

    public String getXref() {
        return xref;
    }

    public void setXref(String xref) {
        this.xref = xref;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getTrialName() {
        return trialName;
    }

    public void setTrialName(String trialName) {
        this.trialName = trialName;
    }

    public static BrAPIv1ObservationVariableDTO fromModel(VariableModel variableModel, BaseVariableDAO<MethodModel> baseVariableDAO) throws Exception {

        BrAPIv1ObservationVariableDTO variable = new BrAPIv1ObservationVariableDTO();
        if (variableModel.getUri() != null) {
            variable.setObservationVariableDbId(variableModel.getUri().toString());
        }
        variable.setObservationVariableName(variableModel.getName());
        
        BrAPIv1TraitDTO trait = new BrAPIv1TraitDTO();
        if (variableModel.getTraitName() != null) {
            trait.setTraitName(variableModel.getTraitName());
        } else {
            String traitName = "";
            if (variableModel.getEntity() != null) {
                traitName = traitName + variableModel.getEntity().getName() + "_";
            }
            if (variableModel.getCharacteristic() != null) {
                traitName = traitName + variableModel.getCharacteristic().getName();
            }
            trait.setTraitName(traitName);
        }
        
        if (variableModel.getTraitUri() != null) {
          trait.setTraitDbId(variableModel.getTraitUri().toString());
        }

        if(variableModel.getEntity() != null){
            trait.setEntity(variableModel.getEntity().getName());
        }
        if(variableModel.getCharacteristic() != null){
            trait.setAttribute(variableModel.getCharacteristic().getName());
        }
        variable.setTrait(trait);

        variable.setMethod(BrAPIv1MethodDTO.fromModel(variableModel.getMethod(), baseVariableDAO));
        
        BrAPIv1ScaleDTO scale = new BrAPIv1ScaleDTO();
        if (variableModel.getUnit() != null) {
            scale.setScaleName(variableModel.getUnit().getName());
            if (variableModel.getUnit().getUri() != null) {
                scale.setScaleDbId(variableModel.getUnit().getUri().toString());
            }
        }        
        variable.setScale(scale);

        if (variableModel.getSpecies() != null && variableModel.getSpecies().size() == 1) {
            variable.setCrop(variableModel.getSpecies().get(0).getName());
        }

        if (variableModel.getAlternativeName() != null) {
            variable.setSynonyms(new ArrayList<>(Collections.singletonList(variableModel.getAlternativeName())));
        }

        if (!CollectionUtils.isEmpty(variableModel.getExactMatch())) {
            variable.setXref(variableModel.getExactMatch().get(0).toString());
        }

        if (variableModel.getTraitUri() != null){
            BrAPIv1TraitDTO variableTrait = new BrAPIv1TraitDTO();
            variableTrait.setTraitDbId(variableModel.getTraitUri().toString());
            if (variableModel.getTraitName() != null) {
                variableTrait.setTraitName(variableModel.getTraitName());
            }
            variable.setTrait(variableTrait);
        }

        if (variableModel.getUnit() != null){
            BrAPIv1ScaleDTO variableScale = BrAPIv1ScaleDTO.fromModel(variableModel.getUnit());

            URI dataTypeUri = variableModel.getDataType();
            if (SPARQLDeserializers.compareURIs(dataTypeUri, URI.create(XSD.decimal.getURI()))
                    || SPARQLDeserializers.compareURIs(dataTypeUri, URI.create(XSD.integer.getURI()))){
                variableScale.setDataType("Numerical");
            }
            if (SPARQLDeserializers.compareURIs(dataTypeUri, URI.create(XSD.date.getURI()))
                    || SPARQLDeserializers.compareURIs(dataTypeUri, URI.create(XSD.dateTime.getURI()))){
                variableScale.setDataType("Date");
            }
            if (SPARQLDeserializers.compareURIs(dataTypeUri, URI.create(XSD.xstring.getURI()))){
                variableScale.setDataType("Text");
            }
            if (SPARQLDeserializers.compareURIs(dataTypeUri, URI.create(XSD.xboolean.getURI()))){
                variableScale.setDataType("Nominal");
            }

            variable.setScale(variableScale);
        }

        return variable;
    }
}
