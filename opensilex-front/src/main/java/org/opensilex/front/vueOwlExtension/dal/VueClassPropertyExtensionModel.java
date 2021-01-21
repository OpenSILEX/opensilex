/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.dal;

import org.opensilex.front.vueOwlExtension.VueOwlExtension;
import java.net.URI;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = VueOwlExtension.class,
        resource = "ClassPropertyExtension",
        graph = "opensilex-owl-extension",
        prefix = "oowl-ext"
)
public class VueClassPropertyExtensionModel extends SPARQLResourceModel implements ClassURIGenerator<VueClassPropertyExtensionModel> {

    @SPARQLProperty(
            ontology = VueOwlExtension.class,
            property = "fromOwlClass",
            required = true
    )
    protected URI fromOwlClass;
    public static final String OWL_CLASS_FIELD = "fromOwlClass";
    @SPARQLProperty(
            ontology = VueOwlExtension.class,
            property = "toOwlProperty",
            required = true
    )
    protected URI toOwlProperty;
    public static final String OWL_PROPERTY_FIELD = "toOwlProperty";

    @SPARQLProperty(
            ontology = VueOwlExtension.class,
            property = "hasDisplayOrder"
    )
    protected Integer hasDisplayOrder;

    public URI getFromOwlClass() {
        return fromOwlClass;
    }

    public void setFromOwlClass(URI fromOwlClass) {
        this.fromOwlClass = fromOwlClass;
    }

    public URI getToOwlProperty() {
        return toOwlProperty;
    }

    public void setToOwlProperty(URI toOwlProperty) {
        this.toOwlProperty = toOwlProperty;
    }

    public Integer getHasDisplayOrder() {
        return hasDisplayOrder;
    }

    public void setHasDisplayOrder(Integer hasDisplayOrder) {
        this.hasDisplayOrder = hasDisplayOrder;
    }

    @Override
    public String[] getUriSegments(VueClassPropertyExtensionModel instance) {
        return new String[] {
            instance.getFromOwlClass().toString(),
            instance.getToOwlProperty().toString(),
        };
    }
  
}
