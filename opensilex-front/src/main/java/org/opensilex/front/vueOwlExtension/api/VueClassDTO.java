/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.core.ontology.api.RDFClassPropertyDTO;
import org.opensilex.core.ontology.api.RDFClassTranslatedDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.front.vueOwlExtension.dal.VueClassPropertyExtensionModel;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 *
 * @author vmigot
 */
public class VueClassDTO extends RDFClassTranslatedDTO {

    protected boolean isAbstract;

    protected String icon;

    protected Map<String, VueClassPropertyDTO> propertiesExtensions;

    public boolean isAbstract() {
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

    public Map<String, VueClassPropertyDTO> getPropertiesExtensions() {
        return propertiesExtensions;
    }

    public void setPropertiesExtensions(Map<String, VueClassPropertyDTO> propertiesExtensions) {
        this.propertiesExtensions = propertiesExtensions;
    }

    public static VueClassDTO fromModel(VueClassDTO dto, ClassModel model, VueClassExtensionModel extClass) {
        RDFClassTranslatedDTO.fromModel(dto, model);

        if (extClass != null) {
            dto.setIsAbstract(extClass.getIsAbstractClass());
            dto.setIcon(extClass.getIcon());
        } else {
            dto.setIsAbstract(false);
        }

        List<VueClassPropertyExtensionModel> extProperties = extClass.getProperties();
        Map<String, VueClassPropertyDTO> extPropertiesMap = new HashMap<>(extProperties.size());
        for (VueClassPropertyExtensionModel extProperty : extProperties) {
            VueClassPropertyDTO propertyDTO = VueClassPropertyDTO.fromModel(extProperty);
            extPropertiesMap.put(extProperty.getToOwlProperty().toString(), propertyDTO);
        }
        dto.setPropertiesExtensions(extPropertiesMap);

        return dto;
    }

    public ClassModel getClassModel(String lang) {
        ClassModel model = new ClassModel();

        model.setUri(getUri());

        SPARQLLabel sparqlLabel = new SPARQLLabel(getLabel(), lang);
        sparqlLabel.addAllTranslations(getLabelTranslations());
        model.setLabel(sparqlLabel);

        SPARQLLabel sparqlComment = new SPARQLLabel(getComment(), lang);
        sparqlComment.addAllTranslations(getCommentTranslations());
        model.setComment(sparqlComment);

        ClassModel parentClass = new ClassModel();
        parentClass.setUri(getParent());
        model.setParent(parentClass);

        Map<URI, DatatypePropertyModel> dtProperties = new HashMap<>();
        Map<URI, ObjectPropertyModel> oProperties = new HashMap<>();
        Map<URI, OwlRestrictionModel> restrictions = new HashMap<>();
        for (RDFClassPropertyDTO property : getProperties()) {
            if (property.isLiteral()) {
                DatatypePropertyModel dtProperty = property.getDatatypePropertyModel(lang);
                dtProperties.put(dtProperty.getUri(), dtProperty);
            } else {
                ObjectPropertyModel oProperty = property.getObjectPropertyModel(lang);
                oProperties.put(oProperty.getUri(), oProperty);
            }
            OwlRestrictionModel restriction = property.getOwlRestriction();
            restrictions.put(property.getUri(), restriction);
        }
        model.setDatatypeProperties(dtProperties);
        model.setObjectProperties(oProperties);
        model.setRestrictions(restrictions);

        return model;
    }

    public VueClassExtensionModel getExtClassModel() {
        VueClassExtensionModel model = new VueClassExtensionModel();

        model.setIcon(getIcon());
        model.setIsAbstractClass(isAbstract());

        List<VueClassPropertyExtensionModel> extendedProperties = new ArrayList<>();
        for (VueClassPropertyDTO extPropertyDTO : getPropertiesExtensions().values()) {
            VueClassPropertyExtensionModel propertyModel = new VueClassPropertyExtensionModel();
            propertyModel.setFromOwlClass(getUri());
            propertyModel.setToOwlProperty(extPropertyDTO.getProperty());
            propertyModel.setHasDisplayOrder(extPropertyDTO.getOrder());
            extendedProperties.add(propertyModel);
        }
        model.setProperties(extendedProperties);

        return model;
    }

}
