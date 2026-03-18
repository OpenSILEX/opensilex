/*
 * *****************************************************************************
 *                         VueRDFTypeDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 16/03/2026 11:09
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.vueOwlExtension.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.ontology.api.RDFTypeTranslatedDTO;
import org.opensilex.core.ontology.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.ontology.dal.ClassModel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vmigot
 */
public class VueRDFTypeDTO extends RDFTypeTranslatedDTO {

    @JsonProperty("is_abstract")
    protected boolean isAbstract;

    protected String icon;

    @JsonProperty("data_properties")
    protected List<VueRDFTypePropertyDTO> dataProperties;

    @JsonProperty("object_properties")
    protected List<VueRDFTypePropertyDTO> objectProperties;

    @JsonProperty("properties_order")
    protected List<URI> propertiesOrder;

    public boolean getIsAbstract() {
        return isAbstract;
    }

    public void setIsAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public VueRDFTypeDTO(ClassModel classModel, VueClassExtensionModel modelExt){
        super(classModel);
        if (modelExt != null) {
            setIsAbstract(modelExt.getIsAbstractClass());
            setIcon(modelExt.getIcon());
        } else {
            setIsAbstract(false);
        }
        dataProperties = new ArrayList<>();
        objectProperties = new ArrayList<>();
    }

    public VueRDFTypeDTO(){

    }


    public List<VueRDFTypePropertyDTO> getDataProperties() {
        return dataProperties;
    }

    public void setDataProperties(List<VueRDFTypePropertyDTO> dataProperties) {
        this.dataProperties = dataProperties;
    }

    public List<VueRDFTypePropertyDTO> getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(List<VueRDFTypePropertyDTO> objectProperties) {
        this.objectProperties = objectProperties;
    }

    public List<URI> getPropertiesOrder() {
        return propertiesOrder;
    }

    public void setPropertiesOrder(List<URI> propertiesOrder) {
        this.propertiesOrder = propertiesOrder;
    }

    public ClassModel toModel(String lang) throws URISyntaxException {

        ClassModel model = new ClassModel();
        model.setUri(getUri());

        SPARQLLabel sparqlLabel = new SPARQLLabel();
        if(labelTranslations.containsKey(lang)){
            sparqlLabel.setDefaultLang(lang);
            sparqlLabel.setDefaultValue(labelTranslations.get(lang));
        }
        sparqlLabel.addAllTranslations(labelTranslations);
        model.setLabel(sparqlLabel);

        SPARQLLabel sparqlComment = new SPARQLLabel();
        if(commentTranslations.containsKey(lang)){
            sparqlComment.setDefaultLang(lang);
            sparqlComment.setDefaultValue(commentTranslations.get(lang));
        }
        sparqlComment.addAllTranslations(commentTranslations);
        model.setComment(sparqlComment);

        if (getParent() != null ){
            ClassModel parentClass = new ClassModel();
            parentClass.setUri(getParent());
            model.setParent(parentClass);
        }

        return model;
    }

}
