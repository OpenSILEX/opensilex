/*
 * *****************************************************************************
 *                         VueClassExtensionModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 11/03/2026 16:06
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.vueOwlExtension.dal;

import java.net.URI;
import java.util.List;
import org.opensilex.core.ontology.vueOwlExtension.VueOwlExtension;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import static org.opensilex.sparql.ontology.dal.OntologyDAO.CUSTOM_TYPES_AND_PROPERTIES_GRAPH;

/**
 *
 * @author vmigot
 */
@SPARQLResource(
        ontology = VueOwlExtension.class,
        resource = "ClassExtension",
        graph = CUSTOM_TYPES_AND_PROPERTIES_GRAPH,
        allowBlankNode = true
)
public class VueClassExtensionModel extends SPARQLResourceModel implements ClassURIGenerator<VueClassExtensionModel> {

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

    @SPARQLProperty(
            ontology = VueOwlExtension.class,
            property = "fromOwlClass",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected List<VueClassPropertyExtensionModel> properties;

    @SPARQLProperty(
            ontology = VueOwlExtension.class,
            property = "hasVueExtensionClassModel",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected URI extendedClass;

    public Boolean getIsAbstractClass() {
        return isAbstractClass == null ? false : isAbstractClass;
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

    public List<VueClassPropertyExtensionModel> getProperties() { return properties; }

    public void setProperties(List<VueClassPropertyExtensionModel> properties) {
        this.properties = properties;
    }

    public URI getExtendedClass() { return extendedClass; }

    public void setExtendedClass(URI extendedClass) { this.extendedClass = extendedClass; }


    @Override
    public String[] getInstancePathSegments(VueClassExtensionModel instance) {
        return new String[]{
                instance.getExtendedClass().toString(),
                VueOwlExtensionDAO.URI_SUFFIX
        };
    }
}
