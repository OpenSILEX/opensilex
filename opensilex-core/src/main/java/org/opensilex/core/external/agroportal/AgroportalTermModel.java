package org.opensilex.core.external.agroportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AgroportalTermModel {

    private String prefLabel;
    private String[] synonym;
    @JsonProperty("definition")
    private String[] definitions;
    private boolean obsolete;
    @JsonProperty("@id")
    private String id;
    @JsonProperty("@type")
    private String type;
    private AgroportalLinksModel links;

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

    public AgroportalLinksModel getLinks() {
        return links;
    }

    public void setLinks(AgroportalLinksModel links) {
        this.links = links;
    }
}
