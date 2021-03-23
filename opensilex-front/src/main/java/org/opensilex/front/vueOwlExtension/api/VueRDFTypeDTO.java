/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.core.ontology.api.RDFTypeTranslatedDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.sparql.model.SPARQLLabel;

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

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

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

    public static VueRDFTypeDTO fromModel(VueRDFTypeDTO dto, ClassModel model, VueClassExtensionModel extClass) {
        RDFTypeTranslatedDTO.fromModel(dto, model);

        if (extClass != null) {
            dto.setIsAbstract(extClass.getIsAbstractClass());
            dto.setIcon(extClass.getIcon());
        } else {
            dto.setIsAbstract(false);
        }

        return dto;
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

    public ClassModel getClassModel(String lang) throws URISyntaxException {
        ClassModel model = new ClassModel();

        model.setUri(getUri());

        SPARQLLabel sparqlLabel = new SPARQLLabel();
        sparqlLabel.addAllTranslations(getLabelTranslations());
        model.setLabel(sparqlLabel);

        SPARQLLabel sparqlComment = new SPARQLLabel();
        sparqlComment.addAllTranslations(getCommentTranslations());
        model.setComment(sparqlComment);

        if (getParent() == null) {
            ClassModel parentClass = new ClassModel();
            parentClass.setUri(new URI(OWL2.Class.getURI()));
            model.setParent(parentClass);
        } else {
            ClassModel parentClass = new ClassModel();
            parentClass.setUri(getParent());
            model.setParent(parentClass);
        }

        return model;
    }

    @JsonIgnore
    public VueClassExtensionModel getExtClassModel() {
        VueClassExtensionModel model = new VueClassExtensionModel();

        model.setUri(getUri());
        model.setIcon(getIcon());
        model.setIsAbstractClass(getIsAbstract());

        return model;
    }

}
