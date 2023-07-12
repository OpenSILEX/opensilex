package org.opensilex.core.variable.dal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "self", "ontology", "children", "parents", "ontology",
        "children", "parents", "descendants", "ancestors",
        "instances", "tree", "notes", "ui"
})
public class LinksAgroportalModel {

    @JsonProperty("self")
    private String self;
    @JsonProperty("ontology")
    private String ontology;
    @JsonProperty("children")
    private String children;
    @JsonProperty("parents")
    private String parents;
    @JsonProperty("descendants")
    private String descendants;
    @JsonProperty("ancestors")
    private String ancestors;
    @JsonProperty("instances")
    private String instances;
    @JsonProperty("tree")
    private String tree;
    @JsonProperty("notes")
    private String notes;
    @JsonProperty("ui")
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
