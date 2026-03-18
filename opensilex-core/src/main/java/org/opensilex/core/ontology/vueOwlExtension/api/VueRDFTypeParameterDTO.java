/*
 * *****************************************************************************
 *                         VueRDFTypeParameterDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 23/06/2025 13:13
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.vueOwlExtension.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.opensilex.core.ontology.vueOwlExtension.dal.VueClassExtensionModel;

/**
 *
 * @author vmigot
 */
public class VueRDFTypeParameterDTO {

    protected URI uri;

    @JsonProperty("is_abstract")
    protected boolean isAbstract;

    protected String icon;

    protected URI extendedClass;

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

    public void setExtendedClass(URI extendedClass) { this.extendedClass = extendedClass; }

    public URI getExtendedClass() {return extendedClass;}

    public static VueRDFTypeParameterDTO getDTOFromModel(VueClassExtensionModel model) {
        VueRDFTypeParameterDTO dto = new VueRDFTypeParameterDTO();
        dto.setUri(model.getUri());
        dto.setIcon(model.getIcon());
        dto.setIsAbstract(model.getIsAbstractClass());
        dto.setExtendedClass(model.getExtendedClass());
        
        return dto;
    }

}
