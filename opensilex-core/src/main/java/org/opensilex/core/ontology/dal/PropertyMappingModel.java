/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import org.opensilex.core.ontology.OpenSilexApiOntology;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = OpenSilexApiOntology.class,
        resource = "PropertyMapping",
        allowBlankNode = true
)
public class PropertyMappingModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "about",
            required = true
    )
    private URI about;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "type",
            required = true
    )
    private String className;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "isList"
    )
    private Boolean list;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "isRequired"
    )
    private Boolean required;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "isInverse"
    )
    private Boolean inverse;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "ignoreUpdateIfNull"
    )
    private Boolean ignoreUpdateIfNull;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "cascadeDelete"
    )
    private Boolean cascadeDelete;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "order"
    )
    private Integer order;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "viewComponent"
    )
    private String viewComponent;

    @SPARQLProperty(
            ontology = OpenSilexApiOntology.class,
            property = "editComponent"
    )
    private String editComponent;

    public URI getAbout() {
        return about;
    }

    public void setAbout(URI about) {
        this.about = about;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Boolean isList() {
        return list;
    }

    public void setList(Boolean list) {
        this.list = list;
    }

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean isInverse() {
        return inverse;
    }

    public void setInverse(Boolean inverse) {
        this.inverse = inverse;
    }

    public Boolean isIgnoreUpdateIfNull() {
        return ignoreUpdateIfNull;
    }

    public void setIgnoreUpdateIfNull(Boolean ignoreUpdateIfNull) {
        this.ignoreUpdateIfNull = ignoreUpdateIfNull;
    }

    public Boolean isCascadeDelete() {
        return cascadeDelete;
    }

    public void setCascadeDelete(Boolean cascadeDelete) {
        this.cascadeDelete = cascadeDelete;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }

    public String getEditComponent() {
        return editComponent;
    }

    public void setEditComponent(String editComponent) {
        this.editComponent = editComponent;
    }

}
