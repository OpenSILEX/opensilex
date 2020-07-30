/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.core.ontology.api.RDFClassDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.front.vueOwlExtension.dal.VueClassPropertyExtensionModel;

/**
 *
 * @author vmigot
 */
public class VueClassDTO extends RDFClassDTO {

    boolean isAbstract;

    String icon;

    Map<String, VueClassPropertyDTO> propertiesExtensions;

    public boolean isIsAbstract() {
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

    public static VueClassDTO fromModel(VueClassDTO dto, ClassModel<?> model, VueClassExtensionModel extClass, List<VueClassPropertyExtensionModel> extProperties) {
        RDFClassDTO.fromModel(dto, model);

        if (extClass != null) {
            dto.setIsAbstract(extClass.getIsAbstractClass());
            dto.setIcon(extClass.getIcon());
        } else {
            dto.setIsAbstract(false);
        }

        Map<String, VueClassPropertyDTO> extPropertiesMap = new HashMap<>(extProperties.size());
        for (VueClassPropertyExtensionModel extProperty : extProperties) {
            VueClassPropertyDTO propertyDTO = VueClassPropertyDTO.fromModel(extProperty);
            extPropertiesMap.put(extProperty.getToOwlProperty().toString(), propertyDTO);
        }
        dto.setPropertiesExtensions(extPropertiesMap);

        return dto;
    }

}
