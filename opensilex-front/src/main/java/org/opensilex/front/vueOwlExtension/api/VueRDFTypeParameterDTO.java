/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;

/**
 *
 * @author vmigot
 */
public class VueRDFTypeParameterDTO {

    protected URI uri;

    @JsonProperty("is_abstract")
    protected boolean isAbstract;

    protected String icon;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

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

    public static VueRDFTypeParameterDTO getDTOFromModel(VueClassExtensionModel model) {
        VueRDFTypeParameterDTO dto = new VueRDFTypeParameterDTO();
        dto.setUri(model.getUri());
        dto.setIcon(model.getIcon());
        dto.setIsAbstract(model.getIsAbstractClass());
        
        return dto;
    }

}
