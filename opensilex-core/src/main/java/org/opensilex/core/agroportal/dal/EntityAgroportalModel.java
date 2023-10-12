package org.opensilex.core.agroportal.dal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityAgroportalModel {

    @JsonProperty("prefLabel")
    private String prefLabel;
    @JsonProperty("synonym")
    private String[] synonym;
    @JsonProperty("definition")
    private String[] definitions;
    @JsonProperty("obsolete")
    private boolean obsolete;
    @JsonProperty("@id")
    private String id;
    @JsonProperty("@type")
    private String type;
    @JsonProperty("links")
    private LinksAgroportalModel links;

    public String getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    public String[] getSynonym() {
        return synonym;
    }

    public void setSynonym(String[] synonym) {
        this.synonym = synonym;
    }

    public String[] getDefinitions() {
        return definitions;
    }

    public void setDefinitions(String[] definitions) {
        this.definitions = definitions;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LinksAgroportalModel getLinks() {
        return links;
    }

    public void setLinks(LinksAgroportalModel links) {
        this.links = links;
    }
}
