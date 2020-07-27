/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import org.opensilex.core.ontology.OpenSilexOwlExtension;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = OpenSilexOwlExtension.class,
        resource = "ClassPropertyExtension",
        graph = "opensilex-owl-extension",
        prefix = "oowl-ext"
)
public class ClassPropertyExtensionModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = OpenSilexOwlExtension.class,
            property = "fromOwlClass",
            required = true
    )
    protected URI fromOwlClass;
    public static final String OWL_CLASS_FIELD = "fromOwlClass";
    @SPARQLProperty(
            ontology = OpenSilexOwlExtension.class,
            property = "toOwlProperty",
            required = true
    )
    protected URI toOwlProperty;
    public static final String OWL_PROPERTY_FIELD = "toOwlProperty";

    @SPARQLProperty(
            ontology = OpenSilexOwlExtension.class,
            property = "hasDisplayOrder"
    )
    protected Integer hasDisplayOrder;

    @SPARQLProperty(
            ontology = OpenSilexOwlExtension.class,
            property = "hasInputComponent"
    )
    protected String inputComponent;

    @SPARQLProperty(
            ontology = OpenSilexOwlExtension.class,
            property = "hasViewComponent"
    )
    protected String viewComponent;

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

    public String getInputComponent() {
        return inputComponent;
    }

    public void setInputComponent(String inputComponent) {
        this.inputComponent = inputComponent;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }

}
