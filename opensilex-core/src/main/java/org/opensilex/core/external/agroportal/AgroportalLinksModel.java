package org.opensilex.core.external.agroportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AgroportalLinksModel {

    private String self;
    private String ontology;
    private String children;
    private String parents;
    private String descendants;
    private String ancestors;
    private String instances;
    private String tree;
    private String notes;
    private String ui;


    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getOntology() {
        return ontology;
    }

    public void setOntology(String ontology) {
        this.ontology = ontology;
    }

    public String getChildren() {
        return children;
    }

    public void setChildren(String children) {
        this.children = children;
    }

    public String getParents() {
        return parents;
    }

    public void setParents(String parents) {
        this.parents = parents;
    }

    public String getDescendants() {
        return descendants;
    }

    public void setDescendants(String descendants) {
        this.descendants = descendants;
    }

    public String getAncestors() {
        return ancestors;
    }

    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
    }

    public String getInstances() {
        return instances;
    }

    public void setInstances(String instances) {
        this.instances = instances;
    }

    public String getTree() {
        return tree;
    }

    public void setTree(String tree) {
        this.tree = tree;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUi() {
        return ui;
    }

    public void setUi(String ui) {
        this.ui = ui;
    }
}
