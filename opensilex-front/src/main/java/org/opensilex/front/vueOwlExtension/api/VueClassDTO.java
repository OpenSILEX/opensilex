/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.core.ontology.api.RDFClassPropertyDTO;
import org.opensilex.core.ontology.api.RDFClassTranslatedDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 *
 * @author vmigot
 */
public class VueClassDTO extends RDFClassTranslatedDTO {

    protected boolean isAbstract;

    protected String icon;

    private List<VueClassPropertyDTO> dataProperties;

    private List<VueClassPropertyDTO> objectProperties;

    private List<URI> propertiesOrder;

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

    public static VueClassDTO fromModel(VueClassDTO dto, ClassModel model, VueClassExtensionModel extClass) {
        RDFClassTranslatedDTO.fromModel(dto, model);

        if (extClass != null) {
            dto.setIsAbstract(extClass.getIsAbstractClass());
            dto.setIcon(extClass.getIcon());
        } else {
            dto.setIsAbstract(false);
        }

        return dto;
    }

    public List<VueClassPropertyDTO> getDataProperties() {
        return dataProperties;
    }

    public void setDataProperties(List<VueClassPropertyDTO> dataProperties) {
        this.dataProperties = dataProperties;
    }

    public List<VueClassPropertyDTO> getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(List<VueClassPropertyDTO> objectProperties) {
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
