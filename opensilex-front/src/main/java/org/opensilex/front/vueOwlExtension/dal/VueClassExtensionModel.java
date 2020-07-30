/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.dal;

import org.opensilex.front.vueOwlExtension.VueOwlExtension;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vmigot
 */
@SPARQLResource(
        ontology = VueOwlExtension.class,
        resource = "ClassExtension",
        graph = "opensilex-owl-extension",
        prefix = "oowl-ext"
)
public class VueClassExtensionModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = VueOwlExtension.class,
            property = "isAbstractClass"
    )
    protected Boolean isAbstractClass;

    @SPARQLProperty(
            ontology = VueOwlExtension.class,
            property = "hasIcon"
    )
    protected String icon;

    public Boolean getIsAbstractClass() {
        return isAbstractClass;
    }

    public void setIsAbstractClass(Boolean isAbstractClass) {
        this.isAbstractClass = isAbstractClass;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

}
